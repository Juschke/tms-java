package com.translationagency.modules.billing.application;

import com.translationagency.modules.billing.domain.*;
import com.translationagency.modules.billing.infrastructure.InvoiceRepository;
import com.translationagency.modules.billing.infrastructure.PaymentRepository;
import com.translationagency.modules.order.domain.OrderStatus;
import com.translationagency.modules.order.domain.TranslationOrder;
import com.translationagency.modules.order.infrastructure.TranslationOrderRepository;
import com.translationagency.modules.tenant.application.NumberRangeService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class BillingService {

    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;
    private final TranslationOrderRepository orderRepository;
    private final NumberRangeService numberRangeService;

    public BillingService(InvoiceRepository invoiceRepository,
                          PaymentRepository paymentRepository,
                          TranslationOrderRepository orderRepository,
                          NumberRangeService numberRangeService) {
        this.invoiceRepository = invoiceRepository;
        this.paymentRepository = paymentRepository;
        this.orderRepository = orderRepository;
        this.numberRangeService = numberRangeService;
    }

    @Transactional(readOnly = true)
    public List<Invoice> getAllInvoices(UUID tenantId) {
        return invoiceRepository.findByTenantIdAndDeletedAtIsNull(tenantId);
    }

    /**
     * Paginated, filtered invoice query — used by {@code InvoiceEnterpriseGrid}.
     *
     * <p>Null / empty filters are treated as "show all" (no restriction applied).
     * The {@link Pageable} carries page index, page size and sort from the grid.</p>
     *
     * @param tenantId       Multi-tenant scope.
     * @param numberFilter   Partial match on invoice number (case-insensitive).
     * @param customerFilter Partial match on customer company name (case-insensitive).
     * @param status         Exact status filter — pass {@code null} to show all statuses.
     * @param pageable       Spring Data Pageable from {@link org.springframework.data.domain.PageRequest}.
     * @return A {@link Page} with the current page's content and total element count.
     */
    @Transactional(readOnly = true)
    public Page<Invoice> findInvoicesFiltered(UUID tenantId,
                                               String search,
                                               String numberFilter,
                                               String customerFilter,
                                               InvoiceStatus status,
                                               java.time.OffsetDateTime issuedFrom,
                                               java.time.OffsetDateTime issuedTo,
                                               Pageable pageable) {
        return invoiceRepository.findFiltered(
                tenantId,
                search != null ? search : "",
                numberFilter   != null ? numberFilter   : "",
                customerFilter != null ? customerFilter : "",
                status,
                issuedFrom != null,
                issuedFrom != null ? issuedFrom : OffsetDateTime.now(),
                issuedTo != null,
                issuedTo != null ? issuedTo : OffsetDateTime.now(),
                pageable
        );
    }

    public Invoice saveInvoice(Invoice invoice) {
        return invoiceRepository.save(invoice);
    }

    public Invoice createInvoiceFromOrder(TranslationOrder order, String username) {
        TranslationOrder attachedOrder = orderRepository.findById(order.getId())
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + order.getId()));

        attachedOrder.setStatus(OrderStatus.INVOICED);
        orderRepository.save(attachedOrder);

        Invoice invoice = invoiceRepository.findByTenantIdAndDeletedAtIsNull(attachedOrder.getTenant().getId()).stream()
                .filter(i -> i.getOrder() != null && i.getOrder().getId().equals(attachedOrder.getId()))
                .findFirst()
                .orElseGet(() -> {
                    Invoice newInvoice = new Invoice();
                    newInvoice.setId(UUID.randomUUID());
                    newInvoice.setTenant(attachedOrder.getTenant());
                    newInvoice.setOrder(attachedOrder);
                    newInvoice.setCustomer(attachedOrder.getCustomer());
                    newInvoice.setInvoiceNumber(numberRangeService.getNextNumber(attachedOrder.getTenant().getId(), "invoice"));
                    newInvoice.setStatus(InvoiceStatus.DRAFT);
                    newInvoice.setIssuedAt(OffsetDateTime.now());
                    newInvoice.setDueAt(OffsetDateTime.now().plusDays(14));
                    newInvoice.setCreatedBy(username);
                    return newInvoice;
                });

        invoice.setNetAmount(attachedOrder.getNetAmount());
        invoice.setVatPercent(attachedOrder.getVatPercent());
        invoice.setVatAmount(attachedOrder.getVatAmount());
        invoice.setGrossAmount(attachedOrder.getGrossAmount());
        invoice.setUpdatedBy(username);

        invoice.getLines().clear();
        attachedOrder.getItems().forEach(item -> {
            InvoiceLine line = new InvoiceLine();
            line.setId(UUID.randomUUID());
            line.setInvoice(invoice);
            line.setDescription(item.getDescription());
            line.setQuantity(item.getQuantity());
            line.setUnit(item.getUnit());
            line.setUnitPrice(item.getUnitPrice());
            line.setTotalPrice(item.getTotalPrice());
            invoice.getLines().add(line);
        });

        return invoiceRepository.save(invoice);
    }

    public Payment recordPayment(Invoice invoice, BigDecimal amount, String method, String reference, String username) {
        Payment payment = new Payment();
        payment.setId(UUID.randomUUID());
        payment.setInvoice(invoice);
        payment.setAmount(amount);
        payment.setPaymentDate(OffsetDateTime.now());
        payment.setPaymentMethod(method);
        payment.setTransactionReference(reference);
        payment.setCreatedBy(username);
        paymentRepository.save(payment);

        invoice.getPayments().add(payment);

        BigDecimal totalPaid = invoice.getPayments().stream()
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalPaid.compareTo(invoice.getGrossAmount()) >= 0) {
            invoice.setStatus(InvoiceStatus.PAID);
            if (invoice.getOrder() != null) {
                invoice.getOrder().setStatus(OrderStatus.PAID);
                orderRepository.save(invoice.getOrder());
            }
        } else if (totalPaid.compareTo(BigDecimal.ZERO) > 0) {
            invoice.setStatus(InvoiceStatus.PARTIALLY_PAID);
        }
        invoiceRepository.save(invoice);

        return payment;
    }

    @Transactional(readOnly = true)
    public List<Payment> getPaymentsForInvoice(UUID invoiceId) {
        return paymentRepository.findByInvoiceId(invoiceId);
    }
}
