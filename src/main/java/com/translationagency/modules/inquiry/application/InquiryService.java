package com.translationagency.modules.inquiry.application;

import com.translationagency.modules.inquiry.domain.*;
import com.translationagency.modules.inquiry.infrastructure.InquiryRepository;
import com.translationagency.modules.inquiry.infrastructure.QuoteRepository;
import com.translationagency.modules.pricing.application.PricingService;
import com.translationagency.modules.tenant.application.NumberRangeService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class InquiryService {

    private final InquiryRepository inquiryRepository;
    private final QuoteRepository quoteRepository;
    private final NumberRangeService numberRangeService;

    public InquiryService(InquiryRepository inquiryRepository, QuoteRepository quoteRepository, NumberRangeService numberRangeService) {
        this.inquiryRepository = inquiryRepository;
        this.quoteRepository = quoteRepository;
        this.numberRangeService = numberRangeService;
    }

    @Transactional(readOnly = true)
    public List<Inquiry> getAllInquiries(UUID tenantId) {
        return inquiryRepository.findByTenantIdAndDeletedAtIsNull(tenantId);
    }

    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<Inquiry> findInquiriesFiltered(
            UUID tenantId,
            String search,
            String customerName,
            InquiryStatus status,
            UUID sourceLanguageId,
            UUID targetLanguageId,
            Boolean isExpress,
            Boolean isCertified,
            org.springframework.data.domain.Pageable pageable) {
        return inquiryRepository.findFiltered(
                tenantId,
                search != null ? search : "",
                customerName != null ? customerName : "",
                status,
                sourceLanguageId,
                targetLanguageId,
                isExpress,
                isCertified,
                pageable
        );
    }

    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<Quote> findQuotesFiltered(
            UUID tenantId,
            String search,
            String quoteNumber,
            String customerName,
            QuoteStatus status,
            java.math.BigDecimal minAmount,
            java.math.BigDecimal maxAmount,
            org.springframework.data.domain.Pageable pageable) {
        return quoteRepository.findFiltered(
                tenantId,
                search != null ? search : "",
                quoteNumber != null ? quoteNumber : "",
                customerName != null ? customerName : "",
                status,
                minAmount,
                maxAmount,
                pageable
        );
    }

    public Inquiry saveInquiry(Inquiry inquiry) {
        return inquiryRepository.save(inquiry);
    }

    public void deleteInquiry(UUID inquiryId, String username) {
        inquiryRepository.findById(inquiryId).ifPresent(inq -> {
            inq.setDeletedAt(OffsetDateTime.now());
            inq.setUpdatedBy(username);
            inquiryRepository.save(inq);
        });
    }

    @Transactional(readOnly = true)
    public List<Quote> getAllQuotes(UUID tenantId) {
        return quoteRepository.findByTenantIdAndDeletedAtIsNull(tenantId);
    }

    public Quote saveQuote(Quote quote) {
        return quoteRepository.save(quote);
    }

    public void deleteQuote(UUID quoteId, String username) {
        quoteRepository.findById(quoteId).ifPresent(q -> {
            q.setDeletedAt(OffsetDateTime.now());
            q.setUpdatedBy(username);
            quoteRepository.save(q);
        });
    }

    public Quote createQuoteFromInquiry(Inquiry inquiry, PricingService.CalculationResult calculationResult, String username) {
        Quote quote = quoteRepository.findByTenantIdAndDeletedAtIsNull(inquiry.getTenant().getId()).stream()
                .filter(q -> q.getInquiry() != null && q.getInquiry().getId().equals(inquiry.getId()))
                .findFirst()
                .orElseGet(() -> {
                    Quote newQuote = new Quote();
                    newQuote.setId(UUID.randomUUID());
                    newQuote.setTenant(inquiry.getTenant());
                    newQuote.setInquiry(inquiry);
                    newQuote.setCustomer(inquiry.getCustomer());
                    newQuote.setQuoteNumber(numberRangeService.getNextNumber(inquiry.getTenant().getId(), "offer"));
                    newQuote.setStatus(QuoteStatus.DRAFT);
                    newQuote.setCreatedBy(username);
                    return newQuote;
                });

        quote.setNetAmount(calculationResult.netAmount());
        quote.setVatPercent(calculationResult.vatPercent());
        quote.setVatAmount(calculationResult.vatAmount());
        quote.setGrossAmount(calculationResult.grossAmount());
        quote.setExpiresAt(OffsetDateTime.now().plusDays(30));
        quote.setUpdatedBy(username);

        quote.getLines().clear();

        QuoteLine line = new QuoteLine();
        line.setId(UUID.randomUUID());
        line.setQuote(quote);
        
        String desc = inquiry.getServiceType() != null ? inquiry.getServiceType().getName() : "Übersetzungdienstleistung";
        if (inquiry.getSourceLanguage() != null && inquiry.getTargetLanguage() != null) {
            desc += " (" + inquiry.getSourceLanguage().getName() + " -> " + inquiry.getTargetLanguage().getName() + ")";
        }
        line.setDescription(desc);

        if (inquiry.getWordCount() != null && inquiry.getWordCount() > 0) {
            line.setQuantity(java.math.BigDecimal.valueOf(inquiry.getWordCount()));
            line.setUnit(QuoteUnit.WORDS);
            line.setUnitPrice(calculationResult.netAmount().divide(line.getQuantity(), 4, java.math.RoundingMode.HALF_UP));
        } else if (inquiry.getPageCount() != null && inquiry.getPageCount() > 0) {
            line.setQuantity(java.math.BigDecimal.valueOf(inquiry.getPageCount()));
            line.setUnit(QuoteUnit.PAGES);
            line.setUnitPrice(calculationResult.netAmount().divide(line.getQuantity(), 2, java.math.RoundingMode.HALF_UP));
        } else {
            line.setQuantity(java.math.BigDecimal.ONE);
            line.setUnit(QuoteUnit.FLAT_RATE);
            line.setUnitPrice(calculationResult.netAmount());
        }

        line.setTotalPrice(calculationResult.netAmount());
        quote.getLines().add(line);

        inquiry.setStatus(InquiryStatus.QUOTE_PREPARED);
        inquiryRepository.save(inquiry);

        return quoteRepository.save(quote);
    }
}
