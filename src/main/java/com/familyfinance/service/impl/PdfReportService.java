package com.familyfinance.service.impl;

import com.familyfinance.dto.MonthlyReportDto;
import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.format.TextStyle;
import java.util.Locale;


@Service
public class PdfReportService {

    private static final Font TITLE_FONT = new Font(Font.HELVETICA, 18, Font.BOLD);
    private static final Font SECTION_FONT = new Font(Font.HELVETICA, 13, Font.BOLD, Color.DARK_GRAY);
    private static final Font LABEL_FONT = new Font(Font.HELVETICA, 11, Font.NORMAL);
    private static final Font VALUE_FONT = new Font(Font.HELVETICA, 11, Font.BOLD);

    public byte[] generateMonthlyReportPdf(MonthlyReportDto report, String userFullName) {
        return generateMonthlyReportPdf(report, userFullName, null);
    }


    public byte[] generateMonthlyReportPdf(MonthlyReportDto report, String userFullName,
                                           BigDecimal childrenExpenses) {
        Document document = new Document(PageSize.A4, 40, 40, 50, 40);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            String lunaText = java.time.Month.of(report.getMonth())
                    .getDisplayName(TextStyle.FULL, new Locale("ro"));

            // Titlu
            Paragraph title = new Paragraph("Raport financiar lunar", TITLE_FONT);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);

            Paragraph subtitle = new Paragraph(
                    capitalize(lunaText) + " " + report.getYear()
                            + (userFullName != null ? "  —  " + userFullName : ""),
                    LABEL_FONT);
            subtitle.setAlignment(Element.ALIGN_CENTER);
            subtitle.setSpacingAfter(20);
            document.add(subtitle);

            // Sumar principal
            document.add(new Paragraph("Sumar", SECTION_FONT));
            document.add(Chunk.NEWLINE);

            PdfPTable summaryTable = new PdfPTable(2);
            summaryTable.setWidthPercentage(100);
            summaryTable.setWidths(new float[]{2, 1});

            addSummaryRow(summaryTable, "Venituri totale", report.getTotalIncome(), " RON");
            addSummaryRow(summaryTable, "Cheltuieli totale", report.getTotalExpense(), " RON");
            addSummaryRow(summaryTable, "Economii (venituri - cheltuieli)", report.getSavings(), " RON");

            document.add(summaryTable);
            document.add(Chunk.NEWLINE);

            // Cheltuieli pe subcategorii
            document.add(new Paragraph("Cheltuieli pe subcategorii", SECTION_FONT));
            document.add(Chunk.NEWLINE);

            PdfPTable subcategoryTable = new PdfPTable(2);
            subcategoryTable.setWidthPercentage(100);
            subcategoryTable.setWidths(new float[]{2, 1});

            addSummaryRow(subcategoryTable, "Chirie", report.getChirie(), " RON");
            addSummaryRow(subcategoryTable, "Utilitati", report.getUtilitati(), " RON");
            addSummaryRow(subcategoryTable, "Alimente", report.getAlimente(), " RON");
            addSummaryRow(subcategoryTable, "Abonamente", report.getAbonamente(), " RON");
            addSummaryRow(subcategoryTable, "Medical", report.getMedical(), " RON");
            addSummaryRow(subcategoryTable, "Asigurare", report.getAsigurare(), " RON");
            addSummaryRow(subcategoryTable, "Concediu", report.getConcediu(), " RON");
            addSummaryRow(subcategoryTable, "Eveniment special", report.getEvenimentSpecial(), " RON");

            document.add(subcategoryTable);

            // Sectiune noua: Intretinere copil - afisata doar daca parintele
            // are cel putin un copil asociat (childrenExpenses != null).
            if (childrenExpenses != null) {
                document.add(Chunk.NEWLINE);
                document.add(new Paragraph("Cheltuieli familie", SECTION_FONT));
                document.add(Chunk.NEWLINE);

                PdfPTable familyTable = new PdfPTable(2);
                familyTable.setWidthPercentage(100);
                familyTable.setWidths(new float[]{2, 1});

                addSummaryRow(familyTable, "Intretinere copil (total cheltuieli copii)",
                        childrenExpenses, " RON");

                document.add(familyTable);
            }

            document.close();
        } catch (DocumentException e) {
            throw new RuntimeException("Eroare la generarea raportului PDF: " + e.getMessage(), e);
        }

        return out.toByteArray();
    }

    private void addSummaryRow(PdfPTable table, String label, BigDecimal value, String suffix) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, LABEL_FONT));
        labelCell.setBorder(Rectangle.BOTTOM);
        labelCell.setPadding(6);

        BigDecimal safeValue = value != null ? value : BigDecimal.ZERO;
        PdfPCell valueCell = new PdfPCell(new Phrase(
                safeValue.setScale(2, java.math.RoundingMode.HALF_UP) + suffix, VALUE_FONT));
        valueCell.setBorder(Rectangle.BOTTOM);
        valueCell.setPadding(6);
        valueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);

        table.addCell(labelCell);
        table.addCell(valueCell);
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }
}