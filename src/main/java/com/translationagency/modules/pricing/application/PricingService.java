package com.translationagency.modules.pricing.application;

import com.translationagency.modules.pricing.domain.Language;
import com.translationagency.modules.pricing.domain.PriceRule;
import com.translationagency.modules.pricing.domain.ServiceType;
import com.translationagency.modules.pricing.infrastructure.LanguageRepository;
import com.translationagency.modules.pricing.infrastructure.PriceRuleRepository;
import com.translationagency.modules.pricing.infrastructure.ServiceTypeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class PricingService {

    private final PriceRuleRepository priceRuleRepository;
    private final LanguageRepository languageRepository;
    private final ServiceTypeRepository serviceTypeRepository;

    public PricingService(PriceRuleRepository priceRuleRepository,
                          LanguageRepository languageRepository,
                          ServiceTypeRepository serviceTypeRepository) {
        this.priceRuleRepository = priceRuleRepository;
        this.languageRepository = languageRepository;
        this.serviceTypeRepository = serviceTypeRepository;
    }

    public record CalculationResult(
            BigDecimal netAmount,
            BigDecimal vatPercent,
            BigDecimal vatAmount,
            BigDecimal grossAmount,
            String calculationDetails
    ) {}

    @Transactional(readOnly = true)
    public List<Language> getAllLanguages() {
        return languageRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<ServiceType> getAllServiceTypes() {
        return serviceTypeRepository.findAll();
    }

    @Transactional(readOnly = true)
    public PriceRule getOrInitializeDefaultRule(UUID tenantId, UUID sourceLangId, UUID targetLangId, UUID serviceTypeId) {
        return priceRuleRepository.findByTenantIdAndSourceLanguageIdAndTargetLanguageIdAndServiceTypeIdAndDeletedAtIsNull(
                tenantId, sourceLangId, targetLangId, serviceTypeId
        ).orElseGet(() -> {
            PriceRule defaultRule = new PriceRule();
            defaultRule.setRatePerWord(BigDecimal.valueOf(0.12));
            defaultRule.setRatePerPage(BigDecimal.valueOf(35.00));
            defaultRule.setMinimumFee(BigDecimal.valueOf(45.00));
            defaultRule.setExpressSurchargePercent(BigDecimal.valueOf(25.00));
            defaultRule.setCertifiedSurcharge(BigDecimal.valueOf(15.00));
            return defaultRule;
        });
    }

    public CalculationResult calculatePrice(UUID tenantId, UUID sourceLangId, UUID targetLangId, UUID serviceTypeId,
                                            int wordCount, int pageCount, boolean isCertified, boolean isExpress) {
        
        PriceRule rule = getOrInitializeDefaultRule(tenantId, sourceLangId, targetLangId, serviceTypeId);
        
        StringBuilder details = new StringBuilder();
        BigDecimal netAmount = BigDecimal.ZERO;

        BigDecimal ratePerWord = rule.getRatePerWord() != null ? rule.getRatePerWord() : BigDecimal.valueOf(0.12);
        BigDecimal ratePerPage = rule.getRatePerPage() != null ? rule.getRatePerPage() : BigDecimal.valueOf(35.00);
        BigDecimal minimumFee = rule.getMinimumFee() != null ? rule.getMinimumFee() : BigDecimal.valueOf(45.00);
        BigDecimal certifiedSurcharge = rule.getCertifiedSurcharge() != null ? rule.getCertifiedSurcharge() : BigDecimal.valueOf(15.00);
        BigDecimal expressSurchargePercent = rule.getExpressSurchargePercent() != null ? rule.getExpressSurchargePercent() : BigDecimal.valueOf(25.00);

        if (wordCount > 0) {
            BigDecimal wordPrice = BigDecimal.valueOf(wordCount).multiply(ratePerWord);
            netAmount = netAmount.add(wordPrice);
            details.append(String.format("Wortpreis: %d Wörter * %.4f EUR = %.2f EUR\n", wordCount, ratePerWord, wordPrice));
        } else if (pageCount > 0) {
            BigDecimal pagePrice = BigDecimal.valueOf(pageCount).multiply(ratePerPage);
            netAmount = netAmount.add(pagePrice);
            details.append(String.format("Seitenpreis: %d Seiten * %.2f EUR = %.2f EUR\n", pageCount, ratePerPage, pagePrice));
        }

        if (netAmount.compareTo(minimumFee) < 0) {
            netAmount = minimumFee;
            details.append(String.format("Mindestpauschale angewendet: %.2f EUR\n", minimumFee));
        }

        if (isCertified) {
            netAmount = netAmount.add(certifiedSurcharge);
            details.append(String.format("Beglaubigungszuschlag: + %.2f EUR\n", certifiedSurcharge));
        }

        if (isExpress) {
            BigDecimal expressSurcharge = netAmount.multiply(expressSurchargePercent).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            netAmount = netAmount.add(expressSurcharge);
            details.append(String.format("Expresszuschlag (%s%%): + %.2f EUR\n", expressSurchargePercent, expressSurcharge));
        }

        netAmount = netAmount.setScale(2, RoundingMode.HALF_UP);
        BigDecimal vatPercent = BigDecimal.valueOf(19.00);
        BigDecimal vatAmount = netAmount.multiply(vatPercent).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal grossAmount = netAmount.add(vatAmount);

        details.append(String.format("Netto: %.2f EUR\n", netAmount));
        details.append(String.format("MwSt. (19%%): %.2f EUR\n", vatAmount));
        details.append(String.format("Brutto: %.2f EUR\n", grossAmount));

        return new CalculationResult(netAmount, vatPercent, vatAmount, grossAmount, details.toString());
    }

    @Transactional(readOnly = true)
    public List<PriceRule> getAllPriceRules(UUID tenantId) {
        return priceRuleRepository.findByTenantIdAndDeletedAtIsNull(tenantId);
    }

    public void deletePriceRule(UUID ruleId, String username) {
        priceRuleRepository.findById(ruleId).ifPresent(rule -> {
            rule.setDeletedAt(java.time.OffsetDateTime.now());
            rule.setUpdatedBy(username);
            priceRuleRepository.save(rule);
        });
    }

    public PriceRule savePriceRule(PriceRule priceRule) {
        return priceRuleRepository.save(priceRule);
    }

    public Language saveLanguage(Language language) {
        return languageRepository.save(language);
    }

    public ServiceType saveServiceType(ServiceType serviceType) {
        return serviceTypeRepository.save(serviceType);
    }
}
