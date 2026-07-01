package com.translationagency;

import com.translationagency.modules.billing.application.BillingService;
import com.translationagency.modules.billing.domain.Invoice;
import com.translationagency.modules.billing.domain.InvoiceStatus;
import com.translationagency.modules.crm.application.CustomerService;
import com.translationagency.modules.crm.domain.Customer;
import com.translationagency.modules.document.application.PdfService;
import com.translationagency.modules.inquiry.application.InquiryService;
import com.translationagency.modules.inquiry.domain.Inquiry;
import com.translationagency.modules.inquiry.domain.Quote;
import com.translationagency.modules.inquiry.domain.QuoteStatus;
import com.translationagency.modules.order.application.OrderService;
import com.translationagency.modules.order.domain.OrderStatus;
import com.translationagency.modules.order.domain.TranslationOrder;
import com.translationagency.modules.pricing.application.PricingService;
import com.translationagency.modules.pricing.domain.Language;
import com.translationagency.modules.pricing.domain.PriceRule;
import com.translationagency.modules.pricing.domain.ServiceType;
import com.translationagency.modules.pricing.infrastructure.LanguageRepository;
import com.translationagency.modules.pricing.infrastructure.PriceRuleRepository;
import com.translationagency.modules.pricing.infrastructure.ServiceTypeRepository;
import com.translationagency.modules.tenant.domain.Tenant;
import com.translationagency.modules.tenant.infrastructure.TenantRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
public class CoreWorkflowIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private CustomerService customerService;

    @Autowired
    private InquiryService inquiryService;

    @Autowired
    private PricingService pricingService;

    @Autowired
    private LanguageRepository languageRepository;

    @Autowired
    private ServiceTypeRepository serviceTypeRepository;

    @Autowired
    private PriceRuleRepository priceRuleRepository;

    @Autowired
    private OrderService orderService;

    @Autowired
    private BillingService billingService;

    @Autowired
    private PdfService pdfService;

    @Test
    void testEndToEndB2BWorkflow() {
        // 1. Mandanten holen oder anlegen
        Tenant tenant = tenantRepository.findAll().stream()
                .filter(t -> t.getSubdomain().equals("agency-1"))
                .findFirst()
                .orElseGet(() -> {
                    Tenant t = new Tenant();
                    t.setId(UUID.randomUUID());
                    t.setName("Musteragentur");
                    t.setSubdomain("agency-1");
                    return tenantRepository.save(t);
                });

        // 2. Kunden anlegen
        Customer customer = new Customer();
        customer.setTenant(tenant);
        customer.setCustomerNumber("K-100");
        customer.setCompanyName("Musterkunde GmbH");
        customer.setBillingAddressStreet("Musterweg 12");
        customer.setBillingAddressZip("12345");
        customer.setBillingAddressCity("Musterstadt");
        customer.setBillingAddressCountry("Deutschland");
        customer = customerService.saveCustomer(customer);
        assertThat(customer.getId()).isNotNull();

        // 3. Sprachen und Leistungsarten erstellen
        Language de = languageRepository.save(new Language(UUID.randomUUID(), "Deutsch", "DE"));
        Language en = languageRepository.save(new Language(UUID.randomUUID(), "Englisch", "EN"));
        ServiceType translation = serviceTypeRepository.save(new ServiceType(UUID.randomUUID(), "Fachübersetzung", "TRANSLATION"));

        // 4. Preisregel hinterlegen: 0.15 EUR pro Wort
        PriceRule rule = new PriceRule();
        rule.setId(UUID.randomUUID());
        rule.setTenant(tenant);
        rule.setSourceLanguage(de);
        rule.setTargetLanguage(en);
        rule.setServiceType(translation);
        rule.setRatePerWord(BigDecimal.valueOf(0.15));
        rule.setRatePerPage(BigDecimal.ZERO);
        rule.setMinimumFee(BigDecimal.ZERO);
        rule.setCertifiedSurcharge(BigDecimal.ZERO);
        rule.setExpressSurchargePercent(BigDecimal.ZERO);
        priceRuleRepository.save(rule);

        // 5. Anfrage erstellen
        Inquiry inquiry = new Inquiry();
        inquiry.setTenant(tenant);
        inquiry.setCustomer(customer);
        inquiry.setSourceLanguage(de);
        inquiry.setTargetLanguage(en);
        inquiry.setServiceType(translation);
        inquiry.setWordCount(1000);
        inquiry.setPageCount(0);
        inquiry.setCertified(false);
        inquiry.setExpress(false);
        inquiry = inquiryService.saveInquiry(inquiry);
        assertThat(inquiry.getId()).isNotNull();

        // 6. Preiskalkulation durchführen
        PricingService.CalculationResult calcResult = pricingService.calculatePrice(
                tenant.getId(),
                de.getId(),
                en.getId(),
                translation.getId(),
                1000,
                0,
                false,
                false
        );
        assertThat(calcResult.netAmount()).isEqualByComparingTo(BigDecimal.valueOf(150.00));

        // 7. Angebot erstellen
        Quote quote = inquiryService.createQuoteFromInquiry(inquiry, calcResult, "tester");
        assertThat(quote.getId()).isNotNull();
        assertThat(quote.getNetAmount()).isEqualByComparingTo(BigDecimal.valueOf(150.00));
        assertThat(quote.getStatus()).isEqualTo(QuoteStatus.DRAFT);

        // 8. Auftrag aus Angebot generieren
        TranslationOrder order = orderService.createOrderFromQuote(quote, "tester");
        assertThat(order.getId()).isNotNull();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CREATED);
        assertThat(order.getQuote().getStatus()).isEqualTo(QuoteStatus.ACCEPTED);

        // 9. Rechnung aus Auftrag generieren
        Invoice invoice = billingService.createInvoiceFromOrder(order, "tester");
        assertThat(invoice.getId()).isNotNull();
        assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.DRAFT);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.INVOICED);

        // 10. Zahlungseingang verbuchen
        billingService.recordPayment(invoice, BigDecimal.valueOf(178.50), "BANK_TRANSFER", "TX-100", "tester");
        assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.PAID);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);

        // 11. PDF-Generierung für Angebot und Rechnung prüfen
        ByteArrayInputStream quotePdf = pdfService.generateQuotePdf(quote);
        assertThat(quotePdf).isNotNull();
        assertThat(quotePdf.available()).isGreaterThan(0);

        ByteArrayInputStream invoicePdf = pdfService.generateInvoicePdf(invoice);
        assertThat(invoicePdf).isNotNull();
        assertThat(invoicePdf.available()).isGreaterThan(0);
    }
}
