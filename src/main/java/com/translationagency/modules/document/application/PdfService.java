package com.translationagency.modules.document.application;

import com.translationagency.modules.billing.domain.DunningLog;
import com.translationagency.modules.billing.domain.Invoice;
import com.translationagency.modules.inquiry.domain.Quote;
import java.io.ByteArrayInputStream;

public interface PdfService {
    ByteArrayInputStream generateQuotePdf(Quote quote);
    ByteArrayInputStream generateInvoicePdf(Invoice invoice);
    ByteArrayInputStream generateDunningPdf(Invoice invoice, DunningLog log);
}
