package com.translationagency.modules.billing.application;

import com.translationagency.modules.billing.domain.DunningLog;
import com.translationagency.modules.billing.domain.DunningSetting;
import com.translationagency.modules.billing.domain.Invoice;
import com.translationagency.modules.billing.infrastructure.DunningLogRepository;
import com.translationagency.modules.billing.infrastructure.DunningSettingRepository;
import com.translationagency.modules.billing.infrastructure.InvoiceRepository;
import com.translationagency.modules.document.application.PdfService;
import com.translationagency.modules.tenant.domain.Tenant;
import com.translationagency.modules.tenant.infrastructure.TenantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class DunningService {

    private final DunningSettingRepository dunningSettingRepository;
    private final DunningLogRepository dunningLogRepository;
    private final InvoiceRepository invoiceRepository;
    private final TenantRepository tenantRepository;
    private final PdfService pdfService;

    public DunningService(DunningSettingRepository dunningSettingRepository,
                          DunningLogRepository dunningLogRepository,
                          InvoiceRepository invoiceRepository,
                          TenantRepository tenantRepository,
                          PdfService pdfService) {
        this.dunningSettingRepository = dunningSettingRepository;
        this.dunningLogRepository = dunningLogRepository;
        this.invoiceRepository = invoiceRepository;
        this.tenantRepository = tenantRepository;
        this.pdfService = pdfService;
    }

    @Transactional(readOnly = true)
    public DunningSetting getDunningSettings(UUID tenantId) {
        return dunningSettingRepository.findByTenantId(tenantId)
                .orElseGet(() -> createDefaultSettings(tenantId));
    }

    public DunningSetting saveSettings(DunningSetting settings) {
        return dunningSettingRepository.save(settings);
    }

    public DunningLog sendReminder(Invoice invoice, int reminderLevel) {
        if ("PAID".equals(invoice.getStatus().name()) || "CANCELLED".equals(invoice.getStatus().name())) {
            throw new IllegalStateException("Invoice is already paid or cancelled");
        }

        DunningSetting settings = getDunningSettings(invoice.getTenant().getId());
        BigDecimal fee = reminderLevel > 1 ? settings.getFeePerLevel() : BigDecimal.ZERO;

        // Create log
        DunningLog log = new DunningLog();
        log.setId(UUID.randomUUID());
        log.setTenant(invoice.getTenant());
        log.setInvoice(invoice);
        log.setLevel(reminderLevel);
        log.setFeeAmount(fee);
        log.setSentAt(OffsetDateTime.now());

        // We could write the file to storage here, but for our webapp it's cleaner to generate dynamically.
        // Let's set a mock pdf path:
        log.setPdfPath("dunning/" + invoice.getTenant().getId() + "/dunning_" + invoice.getInvoiceNumber() + "_level" + reminderLevel + ".pdf");
        dunningLogRepository.save(log);

        // Update invoice
        invoice.setReminderLevel(Math.max(invoice.getReminderLevel(), reminderLevel));
        invoice.setLastReminderDate(OffsetDateTime.now());
        invoiceRepository.save(invoice);

        return log;
    }

    @Transactional(readOnly = true)
    public List<DunningLog> getDunningLogsForInvoice(UUID invoiceId) {
        return dunningLogRepository.findByInvoiceId(invoiceId);
    }

    @Transactional(readOnly = true)
    public List<DunningLog> getDunningLogsForTenant(UUID tenantId) {
        return dunningLogRepository.findByTenantId(tenantId);
    }

    public ByteArrayInputStream downloadDunningPdf(DunningLog log) {
        return pdfService.generateDunningPdf(log.getInvoice(), log);
    }

    private DunningSetting createDefaultSettings(UUID tenantId) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Tenant not found: " + tenantId));

        DunningSetting settings = new DunningSetting();
        settings.setId(UUID.randomUUID());
        settings.setTenant(tenant);
        settings.setEnabled(true);
        settings.setFeePerLevel(BigDecimal.valueOf(5.00));
        settings.setDaysOverdueLevel1(3);
        settings.setDaysOverdueLevel2(10);
        settings.setDaysOverdueLevel3(20);

        return dunningSettingRepository.save(settings);
    }
}
