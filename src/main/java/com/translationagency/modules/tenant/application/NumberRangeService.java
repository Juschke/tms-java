package com.translationagency.modules.tenant.application;

import com.translationagency.modules.tenant.domain.NumberRange;
import com.translationagency.modules.tenant.domain.Tenant;
import com.translationagency.modules.tenant.infrastructure.NumberRangeRepository;
import com.translationagency.modules.tenant.infrastructure.TenantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class NumberRangeService {

    private final NumberRangeRepository numberRangeRepository;
    private final TenantRepository tenantRepository;

    public NumberRangeService(NumberRangeRepository numberRangeRepository, TenantRepository tenantRepository) {
        this.numberRangeRepository = numberRangeRepository;
        this.tenantRepository = tenantRepository;
    }

    /**
     * Get the next number for an entity and increment the counter. Concurrency-safe using pessimistic locking.
     */
    public String getNextNumber(UUID tenantId, String entityType) {
        NumberRange range = numberRangeRepository.findByTenantIdAndEntityTypeWithLock(tenantId, entityType)
                .orElseGet(() -> createDefaultRange(tenantId, entityType));

        LocalDate now = LocalDate.now();
        int currentYear = now.getYear();

        // Handle yearly reset
        if (range.isResetYearly() && (range.getLastYear() == null || range.getLastYear() != currentYear)) {
            range.setCurrentNumber(0);
            range.setLastYear(currentYear);
        }

        int nextVal = range.getCurrentNumber() + range.getIncrement();
        range.setCurrentNumber(nextVal);
        numberRangeRepository.save(range);

        return formatNumber(range, nextVal, now);
    }

    /**
     * Preview the next number without incrementing.
     */
    @Transactional(readOnly = true)
    public String previewNextNumber(UUID tenantId, String entityType) {
        Optional<NumberRange> optRange = numberRangeRepository.findByTenantIdAndEntityType(tenantId, entityType);
        if (optRange.isEmpty()) {
            return "DEFAULT-1";
        }
        NumberRange range = optRange.get();

        LocalDate now = LocalDate.now();
        int currentYear = now.getYear();
        int val = range.getCurrentNumber();

        if (range.isResetYearly() && (range.getLastYear() == null || range.getLastYear() != currentYear)) {
            val = 0;
        }

        return formatNumber(range, val + range.getIncrement(), now);
    }

    @Transactional(readOnly = true)
    public List<NumberRange> getRangesForTenant(UUID tenantId) {
        return numberRangeRepository.findByTenantId(tenantId);
    }

    public NumberRange saveRange(NumberRange range) {
        return numberRangeRepository.save(range);
    }

    private String formatNumber(NumberRange range, int value, LocalDate date) {
        String prefix = range.getPrefix() != null ? range.getPrefix() : "";
        String year4 = String.valueOf(date.getYear());
        String year2 = String.format("%02d", date.getYear() % 100);
        String month = String.format("%02d", date.getMonthValue());
        String day = String.format("%02d", date.getDayOfMonth());

        // Replace placeholders in prefix
        prefix = prefix.replace("{JJJJ}", year4)
                       .replace("{YYYY}", year4)
                       .replace("{JJ}", year2)
                       .replace("{YY}", year2)
                       .replace("{MM}", month)
                       .replace("{DD}", day)
                       .replace("{MONAT}", month);

        StringBuilder sb = new StringBuilder();
        if (!prefix.isEmpty()) {
            sb.append(prefix);
        }

        // Segment-based legacy formatting (if prefix doesn't contain placeholders)
        boolean hasPlaceholders = range.getPrefix() != null && range.getPrefix().contains("{");
        if (range.isYearBased() && !hasPlaceholders) {
            if (sb.length() > 0 && range.getSeparator() != null) {
                sb.append(range.getSeparator());
            }
            sb.append(year4);
        }

        // Append padding and sequential value
        if (sb.length() > 0 && range.getSeparator() != null) {
            sb.append(range.getSeparator());
        }
        sb.append(String.format("%0" + range.getPadding() + "d", value));

        return sb.toString();
    }

    private NumberRange createDefaultRange(UUID tenantId, String entityType) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Tenant not found: " + tenantId));

        NumberRange range = new NumberRange();
        range.setId(UUID.randomUUID());
        range.setTenant(tenant);
        range.setEntityType(entityType);
        range.setLastYear(LocalDate.now().getYear());

        // Defaults matching our configuration
        switch (entityType) {
            case "customer":
                range.setPrefix("K");
                range.setCurrentNumber(9999);
                range.setPadding(5);
                range.setYearBased(false);
                break;
            case "partner":
                range.setPrefix("L");
                range.setCurrentNumber(69999);
                range.setPadding(5);
                range.setYearBased(false);
                break;
            case "offer":
                range.setPrefix("AN{JJJJ}");
                range.setPadding(4);
                range.setYearBased(true);
                range.setResetYearly(true);
                break;
            case "order":
                range.setPrefix("AUF{JJ}{MM}");
                range.setPadding(4);
                range.setYearBased(true);
                range.setResetYearly(true);
                break;
            case "invoice":
                range.setPrefix("RE{JJJJ}");
                range.setPadding(4);
                range.setYearBased(true);
                range.setResetYearly(true);
                break;
            case "vendor_invoice":
                range.setPrefix("ER{JJJJ}");
                range.setPadding(4);
                range.setYearBased(true);
                range.setResetYearly(true);
                break;
            default:
                range.setPrefix(entityType.substring(0, 2).toUpperCase());
                range.setPadding(4);
        }

        return numberRangeRepository.save(range);
    }
}
