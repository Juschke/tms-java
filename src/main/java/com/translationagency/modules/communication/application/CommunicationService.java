package com.translationagency.modules.communication.application;

import com.translationagency.modules.billing.domain.DunningLog;
import com.translationagency.modules.billing.domain.Invoice;
import com.translationagency.modules.billing.domain.InvoiceStatus;
import com.translationagency.modules.billing.infrastructure.DunningLogRepository;
import com.translationagency.modules.billing.infrastructure.InvoiceRepository;
import com.translationagency.modules.crm.domain.ContactPerson;
import com.translationagency.modules.crm.domain.Customer;
import com.translationagency.modules.crm.infrastructure.ContactPersonRepository;
import com.translationagency.modules.document.application.PdfService;
import com.translationagency.modules.inquiry.domain.InquiryStatus;
import com.translationagency.modules.inquiry.domain.Quote;
import com.translationagency.modules.inquiry.domain.QuoteStatus;
import com.translationagency.modules.inquiry.infrastructure.InquiryRepository;
import com.translationagency.modules.inquiry.infrastructure.QuoteRepository;
import com.translationagency.modules.settings.application.SettingsService;
import com.translationagency.modules.settings.domain.TenantSettings;
import com.translationagency.modules.settings.domain.TextTemplate;
import com.translationagency.modules.settings.domain.TextTemplateType;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional
public class CommunicationService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private final JavaMailSender mailSender;
    private final SettingsService settingsService;
    private final PdfService pdfService;
    private final QuoteRepository quoteRepository;
    private final InquiryRepository inquiryRepository;
    private final InvoiceRepository invoiceRepository;
    private final DunningLogRepository dunningLogRepository;
    private final ContactPersonRepository contactPersonRepository;
    private final boolean mailEnabled;
    private final String defaultMailUsername;

    public CommunicationService(JavaMailSender mailSender,
                                SettingsService settingsService,
                                PdfService pdfService,
                                QuoteRepository quoteRepository,
                                InquiryRepository inquiryRepository,
                                InvoiceRepository invoiceRepository,
                                DunningLogRepository dunningLogRepository,
                                ContactPersonRepository contactPersonRepository,
                                @Value("${app.mail.enabled:false}") boolean mailEnabled,
                                @Value("${spring.mail.username:}") String defaultMailUsername) {
        this.mailSender = mailSender;
        this.settingsService = settingsService;
        this.pdfService = pdfService;
        this.quoteRepository = quoteRepository;
        this.inquiryRepository = inquiryRepository;
        this.invoiceRepository = invoiceRepository;
        this.dunningLogRepository = dunningLogRepository;
        this.contactPersonRepository = contactPersonRepository;
        this.mailEnabled = mailEnabled;
        this.defaultMailUsername = defaultMailUsername;
    }

    public void sendQuoteEmail(UUID quoteId, String username) {
        ensureMailEnabled();
        Quote quote = quoteRepository.findById(quoteId)
                .orElseThrow(() -> new IllegalArgumentException("Angebot nicht gefunden: " + quoteId));

        TenantSettings settings = settingsService.getOrCreateTenantSettings(quote.getTenant().getId());
        TextTemplate template = settingsService.getOrCreateTextTemplate(quote.getTenant().getId(), TextTemplateType.QUOTE_EMAIL);
        String recipient = quoteRecipient(quote);
        Map<String, String> values = quoteValues(quote);

        sendWithPdf(settings, recipient, render(template.getSubject(), values), render(template.getBody(), values),
                quote.getQuoteNumber() + ".pdf", pdfBytes(() -> pdfService.generateQuotePdf(quote)));

        quote.setStatus(QuoteStatus.SENT);
        quote.setUpdatedBy(username);
        quoteRepository.save(quote);
        if (quote.getInquiry() != null) {
            quote.getInquiry().setStatus(InquiryStatus.QUOTE_SENT);
            quote.getInquiry().setUpdatedBy(username);
            inquiryRepository.save(quote.getInquiry());
        }
    }

    public void sendInvoiceEmail(UUID invoiceId, String username) {
        ensureMailEnabled();
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new IllegalArgumentException("Rechnung nicht gefunden: " + invoiceId));

        TenantSettings settings = settingsService.getOrCreateTenantSettings(invoice.getTenant().getId());
        TextTemplate template = settingsService.getOrCreateTextTemplate(invoice.getTenant().getId(), TextTemplateType.INVOICE_EMAIL);
        String recipient = customerRecipient(invoice.getCustomer());
        Map<String, String> values = invoiceValues(invoice);

        sendWithPdf(settings, recipient, render(template.getSubject(), values), render(template.getBody(), values),
                invoice.getInvoiceNumber() + ".pdf", pdfBytes(() -> pdfService.generateInvoicePdf(invoice)));

        invoice.setStatus(InvoiceStatus.ISSUED);
        invoice.setUpdatedBy(username);
        invoiceRepository.save(invoice);
    }

    public void sendDunningEmail(UUID dunningLogId, String username) {
        ensureMailEnabled();
        DunningLog log = dunningLogRepository.findById(dunningLogId)
                .orElseThrow(() -> new IllegalArgumentException("Mahnung nicht gefunden: " + dunningLogId));
        Invoice invoice = log.getInvoice();

        TenantSettings settings = settingsService.getOrCreateTenantSettings(invoice.getTenant().getId());
        TextTemplateType type = switch (log.getLevel()) {
            case 1 -> TextTemplateType.DUNNING_LEVEL_1;
            case 2 -> TextTemplateType.DUNNING_LEVEL_2;
            default -> TextTemplateType.DUNNING_LEVEL_3;
        };
        TextTemplate template = settingsService.getOrCreateTextTemplate(invoice.getTenant().getId(), type);
        String recipient = customerRecipient(invoice.getCustomer());
        Map<String, String> values = invoiceValues(invoice);
        values.put("mahnstufe", String.valueOf(log.getLevel()));

        sendWithPdf(settings, recipient, render(template.getSubject(), values), render(template.getBody(), values),
                "Mahnung_" + invoice.getInvoiceNumber() + "_Stufe_" + log.getLevel() + ".pdf",
                pdfBytes(() -> pdfService.generateDunningPdf(invoice, log)));

        invoice.setStatus(InvoiceStatus.DUNNED);
        invoice.setUpdatedBy(username);
        invoiceRepository.save(invoice);
    }

    private void sendWithPdf(TenantSettings settings, String recipient, String subject, String body,
                             String attachmentName, byte[] pdfBytes) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
            String from = senderAddress(settings);
            if (!isBlank(from)) {
                helper.setFrom(from);
            }
            helper.setTo(recipient);
            helper.setSubject(defaultText(subject, "Dokument"));
            helper.setText(defaultText(body, ""), false);
            helper.addAttachment(attachmentName, new ByteArrayResource(pdfBytes), "application/pdf");
            mailSender.send(message);
        } catch (MessagingException ex) {
            throw new IllegalStateException("E-Mail konnte nicht vorbereitet werden: " + ex.getMessage(), ex);
        } catch (RuntimeException ex) {
            throw new IllegalStateException("E-Mail konnte nicht versendet werden: " + ex.getMessage(), ex);
        }
    }

    private String quoteRecipient(Quote quote) {
        if (quote.getInquiry() != null && quote.getInquiry().getContactPerson() != null
                && !isBlank(quote.getInquiry().getContactPerson().getEmail())) {
            return quote.getInquiry().getContactPerson().getEmail();
        }
        return customerRecipient(quote.getCustomer());
    }

    private String customerRecipient(Customer customer) {
        if (customer == null) {
            throw new IllegalStateException("Kein Kunde fuer den Versand hinterlegt.");
        }
        return contactPersonRepository.findByCustomerIdAndDeletedAtIsNull(customer.getId()).stream()
                .map(ContactPerson::getEmail)
                .filter(email -> !isBlank(email))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Beim Kunden ist kein Ansprechpartner mit E-Mail-Adresse hinterlegt."));
    }

    private Map<String, String> quoteValues(Quote quote) {
        Map<String, String> values = baseValues(quote.getCustomer());
        values.put("angebotsnummer", defaultText(quote.getQuoteNumber(), ""));
        values.put("betrag", quote.getGrossAmount() != null ? quote.getGrossAmount() + " EUR" : "");
        values.put("gueltig_bis", quote.getExpiresAt() != null ? quote.getExpiresAt().format(DATE_FMT) : "");
        return values;
    }

    private Map<String, String> invoiceValues(Invoice invoice) {
        Map<String, String> values = baseValues(invoice.getCustomer());
        values.put("rechnungsnummer", defaultText(invoice.getInvoiceNumber(), ""));
        values.put("auftragsnummer", invoice.getOrder() != null ? defaultText(invoice.getOrder().getOrderNumber(), "") : "");
        values.put("faelligkeit", invoice.getDueAt() != null ? invoice.getDueAt().format(DATE_FMT) : "");
        values.put("betrag", invoice.getGrossAmount() != null ? invoice.getGrossAmount() + " EUR" : "");
        return values;
    }

    private Map<String, String> baseValues(Customer customer) {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("kunde", customer != null ? defaultText(customer.getCompanyName(), "") : "");
        return values;
    }

    private String render(String template, Map<String, String> values) {
        String result = defaultText(template, "");
        for (Map.Entry<String, String> entry : values.entrySet()) {
            result = result.replace("{" + entry.getKey() + "}", defaultText(entry.getValue(), ""));
        }
        return result;
    }

    private byte[] pdfBytes(PdfSupplier supplier) {
        return supplier.get().readAllBytes();
    }

    private String senderAddress(TenantSettings settings) {
        if (settings != null && !isBlank(settings.getEmailSenderAddress())) {
            return settings.getEmailSenderAddress();
        }
        return defaultMailUsername;
    }

    private void ensureMailEnabled() {
        if (!mailEnabled) {
            throw new IllegalStateException("E-Mail-Versand ist deaktiviert. Setze app.mail.enabled=true und SMTP-Daten.");
        }
    }

    private String defaultText(String value, String fallback) {
        return isBlank(value) ? fallback : value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    @FunctionalInterface
    private interface PdfSupplier {
        java.io.ByteArrayInputStream get();
    }
}
