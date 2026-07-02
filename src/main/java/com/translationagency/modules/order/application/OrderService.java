package com.translationagency.modules.order.application;

import com.translationagency.modules.inquiry.domain.Quote;
import com.translationagency.modules.inquiry.domain.QuoteStatus;
import com.translationagency.modules.inquiry.infrastructure.QuoteRepository;
import com.translationagency.modules.order.domain.*;
import com.translationagency.modules.order.infrastructure.PartnerAssignmentRepository;
import com.translationagency.modules.order.infrastructure.TranslationOrderRepository;
import com.translationagency.modules.partner.domain.Partner;
import com.translationagency.modules.tenant.application.NumberRangeService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional
public class OrderService {

    /**
     * Erlaubte Statusuebergaenge (Statusmaschine). Verhindert ungueltige Spruenge
     * und haelt die Geschaeftslogik zentral im Service statt in der UI.
     * CANCELLED ist aus fast jedem aktiven Status erreichbar (siehe canCancel).
     */
    private static final Map<OrderStatus, Set<OrderStatus>> ALLOWED_TRANSITIONS = buildTransitions();

    private static Map<OrderStatus, Set<OrderStatus>> buildTransitions() {
        Map<OrderStatus, Set<OrderStatus>> m = new EnumMap<>(OrderStatus.class);
        m.put(OrderStatus.CREATED, EnumSet.of(OrderStatus.ASSIGNED, OrderStatus.IN_PROGRESS, OrderStatus.CANCELLED));
        m.put(OrderStatus.ASSIGNED, EnumSet.of(OrderStatus.IN_PROGRESS, OrderStatus.CANCELLED));
        m.put(OrderStatus.IN_PROGRESS, EnumSet.of(OrderStatus.QA_READY, OrderStatus.READY_FOR_DELIVERY, OrderStatus.CANCELLED));
        m.put(OrderStatus.QA_READY, EnumSet.of(OrderStatus.READY_FOR_DELIVERY, OrderStatus.IN_PROGRESS, OrderStatus.CANCELLED));
        m.put(OrderStatus.READY_FOR_DELIVERY, EnumSet.of(OrderStatus.DELIVERED, OrderStatus.CANCELLED));
        m.put(OrderStatus.DELIVERED, EnumSet.of(OrderStatus.INVOICED, OrderStatus.READY_FOR_DELIVERY));
        m.put(OrderStatus.INVOICED, EnumSet.of(OrderStatus.PAID, OrderStatus.DELIVERED));
        m.put(OrderStatus.PAID, EnumSet.of(OrderStatus.ARCHIVED));
        m.put(OrderStatus.ARCHIVED, EnumSet.noneOf(OrderStatus.class));
        m.put(OrderStatus.CANCELLED, EnumSet.of(OrderStatus.CREATED));
        return m;
    }

    private final TranslationOrderRepository orderRepository;
    private final PartnerAssignmentRepository partnerAssignmentRepository;
    private final QuoteRepository quoteRepository;
    private final NumberRangeService numberRangeService;

    public OrderService(TranslationOrderRepository orderRepository,
                        PartnerAssignmentRepository partnerAssignmentRepository,
                        QuoteRepository quoteRepository,
                        NumberRangeService numberRangeService) {
        this.orderRepository = orderRepository;
        this.partnerAssignmentRepository = partnerAssignmentRepository;
        this.quoteRepository = quoteRepository;
        this.numberRangeService = numberRangeService;
    }

    @Transactional(readOnly = true)
    public List<TranslationOrder> getAllOrders(UUID tenantId) {
        return orderRepository.findByTenantIdAndDeletedAtIsNull(tenantId);
    }

    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<TranslationOrder> findOrdersFiltered(
            UUID tenantId,
            String search,
            String orderNumber,
            String customerName,
            OrderStatus status,
            org.springframework.data.domain.Pageable pageable) {
        return orderRepository.findFiltered(
                tenantId,
                search != null ? search : "",
                orderNumber != null ? orderNumber : "",
                customerName != null ? customerName : "",
                status,
                pageable
        );
    }

    @Transactional(readOnly = true)
    public java.util.Optional<TranslationOrder> getOrderById(UUID orderId) {
        return orderRepository.findDetailById(orderId);
    }

    public TranslationOrder saveOrder(TranslationOrder order) {
        return orderRepository.save(order);
    }

    public TranslationOrder createOrderFromQuote(Quote quote, String username) {
        Quote attachedQuote = quoteRepository.findById(quote.getId())
                .orElseThrow(() -> new IllegalArgumentException("Quote not found: " + quote.getId()));

        // Angebot auf ACCEPTED setzen
        attachedQuote.setStatus(QuoteStatus.ACCEPTED);
        quoteRepository.save(attachedQuote);

        // Prüfen, ob bereits ein Auftrag existiert
        TranslationOrder order = orderRepository.findByTenantIdAndDeletedAtIsNull(attachedQuote.getTenant().getId()).stream()
                .filter(o -> o.getQuote() != null && o.getQuote().getId().equals(attachedQuote.getId()))
                .findFirst()
                .orElseGet(() -> {
                    TranslationOrder newOrder = new TranslationOrder();
                    newOrder.setId(UUID.randomUUID());
                    newOrder.setTenant(attachedQuote.getTenant());
                    newOrder.setQuote(attachedQuote);
                    newOrder.setCustomer(attachedQuote.getCustomer());
                    newOrder.setOrderNumber(numberRangeService.getNextNumber(attachedQuote.getTenant().getId(), "order"));
                    newOrder.setStatus(OrderStatus.CREATED);
                    newOrder.setDeliveryDeadline(OffsetDateTime.now().plusDays(7));
                    newOrder.setCreatedBy(username);
                    return newOrder;
                });

        order.setNetAmount(attachedQuote.getNetAmount());
        order.setVatPercent(attachedQuote.getVatPercent());
        order.setVatAmount(attachedQuote.getVatAmount());
        order.setGrossAmount(attachedQuote.getGrossAmount());
        order.setUpdatedBy(username);

        // Positionen kopieren
        order.getItems().clear();
        attachedQuote.getLines().forEach(line -> {
            TranslationOrderItem item = new TranslationOrderItem();
            item.setId(UUID.randomUUID());
            item.setOrder(order);
            item.setDescription(line.getDescription());
            item.setQuantity(line.getQuantity());
            switch (line.getUnit()) {
                case WORDS: item.setUnit("WORDS"); break;
                case PAGES: item.setUnit("PAGES"); break;
                case HOURS: item.setUnit("HOURS"); break;
                default: item.setUnit("FLAT_RATE"); break;
            }
            item.setUnitPrice(line.getUnitPrice());
            item.setTotalPrice(line.getTotalPrice());
            order.getItems().add(item);
        });

        return orderRepository.save(order);
    }

    public PartnerAssignment assignPartnerToOrder(TranslationOrder order, Partner partner, BigDecimal fee, OffsetDateTime deadline, String username) {
        PartnerAssignment assignment = new PartnerAssignment();
        assignment.setId(UUID.randomUUID());
        assignment.setOrder(order);
        assignment.setPartner(partner);
        assignment.setFee(fee);
        assignment.setDeadline(deadline);
        assignment.setStatus(PartnerAssignmentStatus.OFFERED);
        assignment.setCreatedBy(username);
        assignment.setUpdatedBy(username);

        order.setStatus(OrderStatus.ASSIGNED);
        orderRepository.save(order);

        return partnerAssignmentRepository.save(assignment);
    }

    @Transactional(readOnly = true)
    public List<PartnerAssignment> getAssignmentsForOrder(UUID orderId) {
        return partnerAssignmentRepository.findByOrderId(orderId);
    }

    public void updateAssignmentStatus(PartnerAssignment assignment, PartnerAssignmentStatus status, String username) {
        assignment.setStatus(status);
        assignment.setUpdatedBy(username);
        partnerAssignmentRepository.save(assignment);

        TranslationOrder order = assignment.getOrder();
        if (status == PartnerAssignmentStatus.ACCEPTED) {
            order.setStatus(OrderStatus.IN_PROGRESS);
            orderRepository.save(order);
        } else if (status == PartnerAssignmentStatus.SUBMITTED) {
            order.setStatus(OrderStatus.QA_READY);
            orderRepository.save(order);
        } else if (status == PartnerAssignmentStatus.APPROVED) {
            order.setStatus(OrderStatus.READY_FOR_DELIVERY);
            orderRepository.save(order);
        }
    }

    public void recalculateOrderTotals(TranslationOrder order) {
        BigDecimal netAmount = order.getItems().stream()
                .map(TranslationOrderItem::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        order.setNetAmount(netAmount);

        BigDecimal vatPercent = order.getVatPercent() != null ? order.getVatPercent() : BigDecimal.valueOf(19.00);
        BigDecimal vatAmount = netAmount.multiply(vatPercent).divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
        order.setVatAmount(vatAmount);
        order.setGrossAmount(netAmount.add(vatAmount));
    }

    // =========================================================
    // Statusmaschine – zentrale, validierte Statusuebergaenge
    // =========================================================

    /** Prueft, ob ein Uebergang vom aktuellen in den Zielstatus erlaubt ist. */
    public boolean canTransition(OrderStatus from, OrderStatus to) {
        if (from == null || to == null) return false;
        return ALLOWED_TRANSITIONS.getOrDefault(from, Set.of()).contains(to);
    }

    /** Storno ist aus jedem aktiven Status ausser bereits abgeschlossenen moeglich. */
    public boolean canCancel(TranslationOrder order) {
        if (order == null || order.getStatus() == null) return false;
        OrderStatus s = order.getStatus();
        return s != OrderStatus.CANCELLED && s != OrderStatus.ARCHIVED
                && s != OrderStatus.PAID && s != OrderStatus.INVOICED;
    }

    /** Lieferung ist nur aus READY_FOR_DELIVERY sinnvoll. */
    public boolean canDeliver(TranslationOrder order) {
        return order != null && order.getStatus() == OrderStatus.READY_FOR_DELIVERY;
    }

    /**
     * Fuehrt einen validierten Statuswechsel durch.
     *
     * @throws IllegalStateException wenn der Uebergang nicht erlaubt ist.
     */
    public TranslationOrder changeStatus(UUID orderId, OrderStatus target, String username) {
        TranslationOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Auftrag nicht gefunden: " + orderId));

        if (order.getStatus() == target) {
            return order; // idempotent
        }
        if (!canTransition(order.getStatus(), target)) {
            throw new IllegalStateException(
                    "Statuswechsel von " + order.getStatus() + " zu " + target + " ist nicht erlaubt.");
        }
        order.setStatus(target);
        order.setUpdatedBy(username);
        return orderRepository.save(order);
    }

    /** Storniert einen Auftrag (mit Statuspruefung). */
    public TranslationOrder cancelOrder(UUID orderId, String username) {
        TranslationOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Auftrag nicht gefunden: " + orderId));
        if (!canCancel(order)) {
            throw new IllegalStateException(
                    "Auftrag im Status " + order.getStatus() + " kann nicht storniert werden.");
        }
        order.setStatus(OrderStatus.CANCELLED);
        order.setUpdatedBy(username);
        return orderRepository.save(order);
    }

    /** Markiert einen lieferbereiten Auftrag als geliefert. */
    public TranslationOrder markAsDelivered(UUID orderId, String username) {
        return changeStatus(orderId, OrderStatus.DELIVERED, username);
    }

    /** Reaktiviert einen stornierten Auftrag (zurueck auf CREATED). */
    public TranslationOrder reopenOrder(UUID orderId, String username) {
        return changeStatus(orderId, OrderStatus.CREATED, username);
    }
}
