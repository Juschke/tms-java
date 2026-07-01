package com.translationagency.modules.billing.application;

import com.translationagency.modules.billing.domain.ExternalCost;
import com.translationagency.modules.billing.domain.VendorInvoice;
import com.translationagency.modules.billing.infrastructure.ExternalCostRepository;
import com.translationagency.modules.billing.infrastructure.VendorInvoiceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class VendorInvoiceService {

    private final VendorInvoiceRepository vendorInvoiceRepository;
    private final ExternalCostRepository externalCostRepository;

    public VendorInvoiceService(VendorInvoiceRepository vendorInvoiceRepository,
                                ExternalCostRepository externalCostRepository) {
        this.vendorInvoiceRepository = vendorInvoiceRepository;
        this.externalCostRepository = externalCostRepository;
    }

    @Transactional(readOnly = true)
    public List<VendorInvoice> getAllVendorInvoices(UUID tenantId) {
        return vendorInvoiceRepository.findByTenantId(tenantId);
    }

    public VendorInvoice saveVendorInvoice(VendorInvoice vendorInvoice) {
        vendorInvoice.syncStatus();
        return vendorInvoiceRepository.save(vendorInvoice);
    }

    public void deleteVendorInvoice(UUID id) {
        vendorInvoiceRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<ExternalCost> getAllExternalCosts(UUID tenantId) {
        return externalCostRepository.findByTenantId(tenantId);
    }

    public ExternalCost saveExternalCost(ExternalCost cost) {
        return externalCostRepository.save(cost);
    }

    public void deleteExternalCost(UUID id) {
        externalCostRepository.deleteById(id);
    }

    public VendorInvoice recordVendorPayment(VendorInvoice invoice, BigDecimal amount) {
        BigDecimal newPaidAmount = invoice.getPaidAmount().add(amount);
        invoice.setPaidAmount(newPaidAmount);
        invoice.syncStatus();
        return vendorInvoiceRepository.save(invoice);
    }
}
