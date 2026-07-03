package com.translationagency.modules.partner.application;

import com.translationagency.modules.partner.domain.Partner;
import com.translationagency.modules.partner.domain.PartnerLanguagePair;
import com.translationagency.modules.partner.infrastructure.PartnerLanguagePairRepository;
import com.translationagency.modules.partner.infrastructure.PartnerRepository;
import com.translationagency.modules.tenant.application.NumberRangeService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class PartnerService {

    private final PartnerRepository partnerRepository;
    private final PartnerLanguagePairRepository partnerLanguagePairRepository;
    private final NumberRangeService numberRangeService;

    public PartnerService(PartnerRepository partnerRepository,
            PartnerLanguagePairRepository partnerLanguagePairRepository,
            NumberRangeService numberRangeService) {
        this.partnerRepository = partnerRepository;
        this.partnerLanguagePairRepository = partnerLanguagePairRepository;
        this.numberRangeService = numberRangeService;
    }

    @Transactional(readOnly = true)
    public List<Partner> getAllPartners(UUID tenantId) {
        return partnerRepository.findByTenantIdAndDeletedAtIsNull(tenantId);
    }

    @Transactional(readOnly = true)
    public List<Partner> findPartnersByLanguagePair(UUID tenantId, UUID sourceLangId, UUID targetLangId) {
        if (sourceLangId == null || targetLangId == null) {
            return List.of();
        }
        return partnerRepository.findByLanguagePair(tenantId, sourceLangId, targetLangId);
    }

    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<Partner> findPartnersFiltered(
            UUID tenantId,
            String search,
            String firstName,
            String lastName,
            String email,
            String city,
            String classification,
            Boolean isTranslator,
            Boolean isInterpreter,
            Boolean isActive,
            org.springframework.data.domain.Pageable pageable) {
        return partnerRepository.findFiltered(
                tenantId,
                search != null ? search : "",
                firstName != null ? firstName : "",
                lastName != null ? lastName : "",
                email != null ? email : "",
                city != null ? city : "",
                classification != null ? classification : "",
                isTranslator,
                isInterpreter,
                isActive,
                pageable);
    }

    public Partner savePartner(Partner partner) {
        // Partnernummer nur bei Neuanlage fortlaufend vergeben; bestehende bleiben
        // unveraendert.
        if ((partner.getPartnerNumber() == null || partner.getPartnerNumber().isBlank())
                && partner.getTenant() != null) {
            partner.setPartnerNumber(
                    numberRangeService.getNextNumber(partner.getTenant().getId(), "partner"));
        }
        return partnerRepository.save(partner);
    }

    public void deletePartner(UUID partnerId, String username) {
        partnerRepository.findById(partnerId).ifPresent(partner -> {
            partner.setDeletedAt(OffsetDateTime.now());
            partner.setUpdatedBy(username);
            partnerRepository.save(partner);
        });
    }

    @Transactional(readOnly = true)
    public List<PartnerLanguagePair> getLanguagePairsForPartner(UUID partnerId) {
        return partnerLanguagePairRepository.findByPartnerId(partnerId);
    }

    public PartnerLanguagePair saveLanguagePair(PartnerLanguagePair languagePair) {
        return partnerLanguagePairRepository.save(languagePair);
    }

    public void deleteLanguagePair(UUID pairId) {
        partnerLanguagePairRepository.deleteById(pairId);
    }
}
