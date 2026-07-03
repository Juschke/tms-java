package com.translationagency.modules.document.infrastructure;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.translationagency.modules.billing.domain.DunningLog;
import com.translationagency.modules.billing.domain.Invoice;
import com.translationagency.modules.billing.domain.InvoiceLine;
import com.translationagency.modules.document.application.PdfService;
import com.translationagency.modules.inquiry.domain.Quote;
import com.translationagency.modules.inquiry.domain.QuoteLine;
import com.translationagency.modules.settings.application.SettingsService;
import com.translationagency.modules.settings.domain.TenantSettings;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

import java.awt.Color;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;

@Service
public class OpenPdfService implements PdfService {

    private final SettingsService settingsService;

    public OpenPdfService(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    @Override
    public ByteArrayInputStream generateQuotePdf(Quote quote) {
        Document document = new Document();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        TenantSettings settings = settingsFor(quote.getTenant().getId());

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            // Schriftstile
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, Color.DARK_GRAY);
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.BLACK);
            Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.BLACK);
            Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.BLACK);
            Font smallFont = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.GRAY);

            // 1. Firmenkopf
            PdfPTable headerTable = new PdfPTable(2);
            headerTable.setWidthPercentage(100);
            headerTable.setWidths(new float[]{1, 1});

            PdfPCell leftCell = new PdfPCell(new Paragraph(companyHeaderTitle(settings), boldFont));
            leftCell.setBorder(PdfPCell.NO_BORDER);
            headerTable.addCell(leftCell);

            PdfPCell rightCell = new PdfPCell(new Paragraph(companyAddressBlock(settings), smallFont));
            rightCell.setBorder(PdfPCell.NO_BORDER);
            rightCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            headerTable.addCell(rightCell);

            document.add(headerTable);
            document.add(new Paragraph("\n\n"));

            // 2. Empfängeradresse
            Paragraph recipient = new Paragraph();
            recipient.setFont(normalFont);
            recipient.add(quote.getCustomer().getCompanyName() + "\n");
            if (quote.getCustomer().getBillingAddressStreet() != null) {
                recipient.add(quote.getCustomer().getBillingAddressStreet() + "\n");
            }
            String zipCity = ((quote.getCustomer().getBillingAddressZip() != null ? quote.getCustomer().getBillingAddressZip() : "") + " " +
                             (quote.getCustomer().getBillingAddressCity() != null ? quote.getCustomer().getBillingAddressCity() : "")).trim();
            if (!zipCity.isEmpty()) {
                recipient.add(zipCity + "\n");
            }
            if (quote.getCustomer().getBillingAddressCountry() != null) {
                recipient.add(quote.getCustomer().getBillingAddressCountry() + "\n");
            }
            document.add(recipient);
            document.add(new Paragraph("\n\n"));

            // 3. Titel & Datum
            Paragraph title = new Paragraph("Angebot Nr. " + quote.getQuoteNumber(), titleFont);
            title.setSpacingAfter(10);
            document.add(title);

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
            String dateText = "Datum: " + (quote.getCreatedAt() != null ? quote.getCreatedAt().format(formatter) : "-");
            if (quote.getExpiresAt() != null) {
                dateText += " | Gültig bis: " + quote.getExpiresAt().format(formatter);
            }
            Paragraph datePara = new Paragraph(dateText, normalFont);
            datePara.setSpacingAfter(20);
            document.add(datePara);

            // 4. Tabelle für Angebotsposten
            PdfPTable table = new PdfPTable(5);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{3, 1, 1, 1, 1});

            table.addCell(createCell("Beschreibung", headerFont, Element.ALIGN_LEFT, Color.LIGHT_GRAY));
            table.addCell(createCell("Menge", headerFont, Element.ALIGN_RIGHT, Color.LIGHT_GRAY));
            table.addCell(createCell("Einheit", headerFont, Element.ALIGN_CENTER, Color.LIGHT_GRAY));
            table.addCell(createCell("Einzelpreis", headerFont, Element.ALIGN_RIGHT, Color.LIGHT_GRAY));
            table.addCell(createCell("Gesamt", headerFont, Element.ALIGN_RIGHT, Color.LIGHT_GRAY));

            for (QuoteLine line : quote.getLines()) {
                table.addCell(createCell(line.getDescription(), normalFont, Element.ALIGN_LEFT, null));
                table.addCell(createCell(line.getQuantity().toString(), normalFont, Element.ALIGN_RIGHT, null));
                
                String unitStr;
                switch (line.getUnit()) {
                    case WORDS: unitStr = "Worte"; break;
                    case PAGES: unitStr = "Seiten"; break;
                    case HOURS: unitStr = "Stunden"; break;
                    case FLAT_RATE: unitStr = "Pauschal"; break;
                    default: unitStr = ""; break;
                }
                table.addCell(createCell(unitStr, normalFont, Element.ALIGN_CENTER, null));
                table.addCell(createCell(String.format("%.2f EUR", line.getUnitPrice()), normalFont, Element.ALIGN_RIGHT, null));
                table.addCell(createCell(String.format("%.2f EUR", line.getTotalPrice()), normalFont, Element.ALIGN_RIGHT, null));
            }

            document.add(table);
            document.add(new Paragraph("\n"));

            // 5. Zusammenfassung der Beträge
            PdfPTable summaryTable = new PdfPTable(2);
            summaryTable.setWidthPercentage(40);
            summaryTable.setWidths(new float[]{1, 1});
            summaryTable.setHorizontalAlignment(Element.ALIGN_RIGHT);

            summaryTable.addCell(createCell("Netto:", normalFont, Element.ALIGN_LEFT, null, false));
            summaryTable.addCell(createCell(String.format("%.2f EUR", quote.getNetAmount()), normalFont, Element.ALIGN_RIGHT, null, false));

            summaryTable.addCell(createCell(String.format("MwSt. (%.0f%%):", quote.getVatPercent()), normalFont, Element.ALIGN_LEFT, null, false));
            summaryTable.addCell(createCell(String.format("%.2f EUR", quote.getVatAmount()), normalFont, Element.ALIGN_RIGHT, null, false));

            summaryTable.addCell(createCell("Brutto:", boldFont, Element.ALIGN_LEFT, null, false));
            summaryTable.addCell(createCell(String.format("%.2f EUR", quote.getGrossAmount()), boldFont, Element.ALIGN_RIGHT, null, false));

            document.add(summaryTable);
            document.add(new Paragraph("\n\n"));

            // 6. Fußbereich
            Paragraph terms = new Paragraph(defaultText(settings.getQuoteFooter(),
                    "Vielen Dank fuer Ihre Anfrage! Wir freuen uns auf Ihren Auftrag.\n"
                            + "Es gelten unsere allgemeinen Geschaeftsbedingungen."), smallFont);
            terms.setAlignment(Element.ALIGN_CENTER);
            document.add(terms);

            document.close();

        } catch (DocumentException e) {
            e.printStackTrace();
        }

        return new ByteArrayInputStream(out.toByteArray());
    }

    @Override
    public ByteArrayInputStream generateInvoicePdf(Invoice invoice) {
        Document document = new Document();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        TenantSettings settings = settingsFor(invoice.getTenant().getId());

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            // Schriftstile
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, Color.DARK_GRAY);
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.BLACK);
            Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.BLACK);
            Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.BLACK);
            Font smallFont = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.GRAY);

            // 1. Firmenkopf
            PdfPTable headerTable = new PdfPTable(2);
            headerTable.setWidthPercentage(100);
            headerTable.setWidths(new float[]{1, 1});

            PdfPCell leftCell = new PdfPCell(new Paragraph(companyHeaderTitle(settings), boldFont));
            leftCell.setBorder(PdfPCell.NO_BORDER);
            headerTable.addCell(leftCell);

            PdfPCell rightCell = new PdfPCell(new Paragraph(companyAddressBlock(settings), smallFont));
            rightCell.setBorder(PdfPCell.NO_BORDER);
            rightCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            headerTable.addCell(rightCell);

            document.add(headerTable);
            document.add(new Paragraph("\n\n"));

            // 2. Empfängeradresse
            Paragraph recipient = new Paragraph();
            recipient.setFont(normalFont);
            recipient.add(invoice.getCustomer().getCompanyName() + "\n");
            if (invoice.getCustomer().getBillingAddressStreet() != null) {
                recipient.add(invoice.getCustomer().getBillingAddressStreet() + "\n");
            }
            String zipCity = ((invoice.getCustomer().getBillingAddressZip() != null ? invoice.getCustomer().getBillingAddressZip() : "") + " " +
                             (invoice.getCustomer().getBillingAddressCity() != null ? invoice.getCustomer().getBillingAddressCity() : "")).trim();
            if (!zipCity.isEmpty()) {
                recipient.add(zipCity + "\n");
            }
            if (invoice.getCustomer().getBillingAddressCountry() != null) {
                recipient.add(invoice.getCustomer().getBillingAddressCountry() + "\n");
            }
            document.add(recipient);
            document.add(new Paragraph("\n\n"));

            // 3. Titel & Datum
            Paragraph title = new Paragraph("Rechnung Nr. " + invoice.getInvoiceNumber(), titleFont);
            title.setSpacingAfter(10);
            document.add(title);

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
            String dateText = "Rechnungsdatum: " + (invoice.getIssuedAt() != null ? invoice.getIssuedAt().format(formatter) : "-");
            if (invoice.getDueAt() != null) {
                dateText += " | Zahlungsziel: " + invoice.getDueAt().format(formatter);
            }
            Paragraph datePara = new Paragraph(dateText, normalFont);
            datePara.setSpacingAfter(20);
            document.add(datePara);

            // 4. Tabelle für Rechnungsposten
            PdfPTable table = new PdfPTable(5);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{3, 1, 1, 1, 1});

            table.addCell(createCell("Beschreibung", headerFont, Element.ALIGN_LEFT, Color.LIGHT_GRAY));
            table.addCell(createCell("Menge", headerFont, Element.ALIGN_RIGHT, Color.LIGHT_GRAY));
            table.addCell(createCell("Einheit", headerFont, Element.ALIGN_CENTER, Color.LIGHT_GRAY));
            table.addCell(createCell("Einzelpreis", headerFont, Element.ALIGN_RIGHT, Color.LIGHT_GRAY));
            table.addCell(createCell("Gesamt", headerFont, Element.ALIGN_RIGHT, Color.LIGHT_GRAY));

            for (InvoiceLine line : invoice.getLines()) {
                table.addCell(createCell(line.getDescription(), normalFont, Element.ALIGN_LEFT, null));
                table.addCell(createCell(line.getQuantity().toString(), normalFont, Element.ALIGN_RIGHT, null));
                
                String unitStr;
                switch (line.getUnit()) {
                    case "WORDS": unitStr = "Worte"; break;
                    case "PAGES": unitStr = "Seiten"; break;
                    case "HOURS": unitStr = "Stunden"; break;
                    default: unitStr = "Pauschal"; break;
                }
                table.addCell(createCell(unitStr, normalFont, Element.ALIGN_CENTER, null));
                table.addCell(createCell(String.format("%.2f EUR", line.getUnitPrice()), normalFont, Element.ALIGN_RIGHT, null));
                table.addCell(createCell(String.format("%.2f EUR", line.getTotalPrice()), normalFont, Element.ALIGN_RIGHT, null));
            }

            document.add(table);
            document.add(new Paragraph("\n"));

            // 5. Zusammenfassung der Beträge
            PdfPTable summaryTable = new PdfPTable(2);
            summaryTable.setWidthPercentage(40);
            summaryTable.setWidths(new float[]{1, 1});
            summaryTable.setHorizontalAlignment(Element.ALIGN_RIGHT);

            summaryTable.addCell(createCell("Netto:", normalFont, Element.ALIGN_LEFT, null, false));
            summaryTable.addCell(createCell(String.format("%.2f EUR", invoice.getNetAmount()), normalFont, Element.ALIGN_RIGHT, null, false));

            summaryTable.addCell(createCell(String.format("MwSt. (%.0f%%):", invoice.getVatPercent()), normalFont, Element.ALIGN_LEFT, null, false));
            summaryTable.addCell(createCell(String.format("%.2f EUR", invoice.getVatAmount()), normalFont, Element.ALIGN_RIGHT, null, false));

            summaryTable.addCell(createCell("Brutto:", boldFont, Element.ALIGN_LEFT, null, false));
            summaryTable.addCell(createCell(String.format("%.2f EUR", invoice.getGrossAmount()), boldFont, Element.ALIGN_RIGHT, null, false));

            document.add(summaryTable);
            document.add(new Paragraph("\n\n"));

            // 6. Bankdaten & Zahlungskonditionen
            Paragraph paymentTerms = new Paragraph("Bitte ueberweisen Sie den Betrag bis zum " +
                    (invoice.getDueAt() != null ? invoice.getDueAt().format(formatter) : "-") + " auf folgendes Bankkonto:\n" +
                    bankDetails(settings) + "\n" +
                    "Verwendungszweck: " + invoice.getInvoiceNumber(), normalFont);
            paymentTerms.setSpacingBefore(15);
            document.add(paymentTerms);

            if (!isBlank(settings.getInvoiceFooter())) {
                Paragraph footer = new Paragraph(settings.getInvoiceFooter(), smallFont);
                footer.setSpacingBefore(20);
                footer.setAlignment(Element.ALIGN_CENTER);
                document.add(footer);
            }

            document.close();

        } catch (DocumentException e) {
            e.printStackTrace();
        }

        return new ByteArrayInputStream(out.toByteArray());
    }

    private TenantSettings settingsFor(java.util.UUID tenantId) {
        return settingsService.getOrCreateTenantSettings(tenantId);
    }

    private String companyHeaderTitle(TenantSettings settings) {
        return "UEBERSETZUNGSDIENST\n" + defaultText(settings.getCompanyName(), "Ihre Uebersetzungsagentur");
    }

    private String companyAddressBlock(TenantSettings settings) {
        StringBuilder block = new StringBuilder();
        appendLine(block, settings.getStreet());
        String zipCity = joinWithSpace(settings.getZip(), settings.getCity());
        appendLine(block, zipCity);
        appendLine(block, settings.getCountry());
        appendLine(block, settings.getPhone());
        appendLine(block, settings.getEmail());
        appendLine(block, settings.getWebsite());
        appendLine(block, labelValue("Steuernr.", settings.getTaxNumber()));
        appendLine(block, labelValue("USt-ID", settings.getVatId()));
        return defaultText(block.toString().trim(), "Bitte Mandantendaten in den Einstellungen pflegen.");
    }

    private String bankDetails(TenantSettings settings) {
        StringBuilder block = new StringBuilder();
        appendLine(block, labelValue("Bank", settings.getBankName()));
        appendLine(block, labelValue("IBAN", settings.getIban()));
        appendLine(block, labelValue("BIC", settings.getBic()));
        return defaultText(block.toString().trim(), "Zahlungsdaten bitte der gesonderten Vereinbarung entnehmen.");
    }

    private String labelValue(String label, String value) {
        if (isBlank(value)) {
            return "";
        }
        return label + ": " + value.trim();
    }

    private String joinWithSpace(String left, String right) {
        if (isBlank(left)) {
            return defaultText(right, "");
        }
        if (isBlank(right)) {
            return left.trim();
        }
        return left.trim() + " " + right.trim();
    }

    private void appendLine(StringBuilder builder, String value) {
        if (!isBlank(value)) {
            if (builder.length() > 0) {
                builder.append('\n');
            }
            builder.append(value.trim());
        }
    }

    private String defaultText(String value, String fallback) {
        return isBlank(value) ? fallback : value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private PdfPCell createCell(String text, Font font, int alignment, Color background) {
        return createCell(text, font, alignment, background, true);
    }

    private PdfPCell createCell(String text, Font font, int alignment, Color background, boolean border) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setHorizontalAlignment(alignment);
        cell.setPadding(5);
        if (background != null) {
            cell.setBackgroundColor(background);
        }
        if (!border) {
            cell.setBorder(PdfPCell.NO_BORDER);
        }
        return cell;
    }

    @Override
    public ByteArrayInputStream generateDunningPdf(Invoice invoice, DunningLog log) {
        Document document = new Document();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        TenantSettings settings = settingsFor(invoice.getTenant().getId());

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, Color.DARK_GRAY);
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.BLACK);
            Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.BLACK);
            Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.BLACK);
            Font smallFont = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.GRAY);

            // 1. Header
            PdfPTable headerTable = new PdfPTable(2);
            headerTable.setWidthPercentage(100);
            headerTable.setWidths(new float[]{1, 1});

            PdfPCell leftCell = new PdfPCell(new Paragraph(companyHeaderTitle(settings), boldFont));
            leftCell.setBorder(PdfPCell.NO_BORDER);
            headerTable.addCell(leftCell);

            PdfPCell rightCell = new PdfPCell(new Paragraph(companyAddressBlock(settings), smallFont));
            rightCell.setBorder(PdfPCell.NO_BORDER);
            rightCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            headerTable.addCell(rightCell);

            document.add(headerTable);
            document.add(new Paragraph("\n\n"));

            // 2. Customer Address
            Paragraph recipient = new Paragraph();
            recipient.setFont(normalFont);
            recipient.add(invoice.getCustomer().getCompanyName() + "\n");
            if (invoice.getCustomer().getBillingAddressStreet() != null) {
                recipient.add(invoice.getCustomer().getBillingAddressStreet() + "\n");
            }
            String zipCity = ((invoice.getCustomer().getBillingAddressZip() != null ? invoice.getCustomer().getBillingAddressZip() : "") + " " +
                             (invoice.getCustomer().getBillingAddressCity() != null ? invoice.getCustomer().getBillingAddressCity() : "")).trim();
            if (!zipCity.isEmpty()) {
                recipient.add(zipCity + "\n");
            }
            document.add(recipient);
            document.add(new Paragraph("\n\n"));

            // 3. Title & Date
            String titleText = log.getLevel() == 1 ? "Zahlungserinnerung" : "Mahnung (Stufe " + log.getLevel() + ")";
            Paragraph title = new Paragraph(titleText, titleFont);
            title.setSpacingAfter(10);
            document.add(title);

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
            String dateText = "Datum: " + (log.getSentAt() != null ? log.getSentAt().format(formatter) : "-");
            Paragraph datePara = new Paragraph(dateText, normalFont);
            datePara.setSpacingAfter(20);
            document.add(datePara);

            // 4. Body Text
            Paragraph body = new Paragraph();
            body.setFont(normalFont);
            body.setSpacingAfter(20);

            if (log.getLevel() == 1) {
                body.add("Sehr geehrte Damen und Herren,\n\nsicherlich ist es nur ein Versäumnis, aber wir konnten für die Rechnung " 
                        + invoice.getInvoiceNumber() + " vom " + invoice.getIssuedAt().format(formatter) + " noch keinen Zahlungseingang feststellen.\n\n"
                        + "Bitte überweisen Sie den ausstehenden Betrag kurzfristig.");
            } else if (log.getLevel() == 2) {
                body.add("Sehr geehrte Damen und Herren,\n\nwir beziehen uns auf unsere Zahlungserinnerung und fordern Sie hiermit auf, den ausstehenden Betrag der Rechnung " 
                        + invoice.getInvoiceNumber() + " bis zum " + log.getSentAt().plusDays(10).format(formatter) + " zu begleichen.\n\n"
                        + "Für diese Mahnung erheben wir eine Mahngebühr in Höhe von " + log.getFeeAmount() + " EUR.");
            } else {
                body.add("Sehr geehrte Damen und Herren,\n\ndies ist unsere letzte Aufforderung zur Zahlung der Rechnung " 
                        + invoice.getInvoiceNumber() + ". Sollte der Betrag inklusive Mahngebühren nicht bis zum " 
                        + log.getSentAt().plusDays(7).format(formatter) + " auf unserem Konto eingehen, werden wir gerichtliche Schritte einleiten müssen.");
            }
            document.add(body);

            // 5. Statement
            PdfPTable table = new PdfPTable(2);
            table.setWidthPercentage(60);
            table.setWidths(new float[]{3, 1});
            table.setHorizontalAlignment(Element.ALIGN_LEFT);

            table.addCell(createCell("Rechnungsbetrag (" + invoice.getInvoiceNumber() + "):", normalFont, Element.ALIGN_LEFT, null, false));
            table.addCell(createCell(String.format("%.2f EUR", invoice.getGrossAmount()), normalFont, Element.ALIGN_RIGHT, null, false));

            if (log.getFeeAmount().compareTo(BigDecimal.ZERO) > 0) {
                table.addCell(createCell("Mahngebühr:", normalFont, Element.ALIGN_LEFT, null, false));
                table.addCell(createCell(String.format("%.2f EUR", log.getFeeAmount()), normalFont, Element.ALIGN_RIGHT, null, false));
            }

            BigDecimal total = invoice.getGrossAmount().add(log.getFeeAmount());
            table.addCell(createCell("Gesamtbetrag:", boldFont, Element.ALIGN_LEFT, null, false));
            table.addCell(createCell(String.format("%.2f EUR", total), boldFont, Element.ALIGN_RIGHT, null, false));

            document.add(table);
            document.add(new Paragraph("\n\n"));

            // 6. Bank Details
            Paragraph paymentTerms = new Paragraph("Bitte ueberweisen Sie den Gesamtbetrag auf folgendes Bankkonto:\n" +
                    bankDetails(settings) + "\n" +
                    "Verwendungszweck: " + invoice.getInvoiceNumber() + " MAHNUNG", normalFont);
            document.add(paymentTerms);

            document.close();

        } catch (DocumentException e) {
            e.printStackTrace();
        }

        return new ByteArrayInputStream(out.toByteArray());
    }
}
