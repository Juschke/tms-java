package com.translationagency.modules.partner.infrastructure;

import com.translationagency.modules.partner.domain.PartnerLanguagePair;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

@Repository
public interface PartnerLanguagePairRepository extends JpaRepository<PartnerLanguagePair, UUID> {
    @Query("SELECT p FROM PartnerLanguagePair p " +
           "LEFT JOIN FETCH p.sourceLanguage " +
           "LEFT JOIN FETCH p.targetLanguage " +
           "WHERE p.partner.id = :partnerId")
    List<PartnerLanguagePair> findByPartnerId(@Param("partnerId") UUID partnerId);
}
