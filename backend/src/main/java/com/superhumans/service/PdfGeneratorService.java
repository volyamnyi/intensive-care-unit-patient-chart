package com.superhumans.service;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.UnitValue;
import com.superhumans.entity.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PdfGeneratorService {

    private final VitalSignService vitalSignService;
    private final FluidBalanceService fluidBalanceService;
    private final ScaleService scaleService;

    public byte[] generateDayPdf(IcuDay day, IcuCard card) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);

            document.add(new Paragraph("КАРТА ІНТЕНСИВНОЇ ТЕРАПІЇ № 003-15/о")
                    .setBold().setFontSize(16));

            document.add(new Paragraph("Пацієнт: " + card.getPatientName()));
            document.add(new Paragraph("Доба №" + day.getDayNumber() + " від " +
                    day.getDate().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))));
            document.add(new Paragraph("Діагноз: " + card.getDiagnosis()));

            document.add(new Paragraph(""));
            document.add(new Paragraph("Вітальні показники:").setBold());

            Table table = new Table(8);
            table.setWidth(UnitValue.createPercentValue(100));
            table.addHeaderCell(new Cell().add(new Paragraph("Година")));
            table.addHeaderCell(new Cell().add(new Paragraph("АТ сист")));
            table.addHeaderCell(new Cell().add(new Paragraph("АТ діас")));
            table.addHeaderCell(new Cell().add(new Paragraph("ЧСС")));
            table.addHeaderCell(new Cell().add(new Paragraph("SpO2")));
            table.addHeaderCell(new Cell().add(new Paragraph("Темп")));
            table.addHeaderCell(new Cell().add(new Paragraph("ЦВТ")));
            table.addHeaderCell(new Cell().add(new Paragraph("ЧД")));

            List<HourlyVital> vitals = vitalSignService.getVitalsByDay(day.getId());
            for (HourlyVital v : vitals) {
                table.addCell(new Cell().add(new Paragraph(String.valueOf(v.getHour()))));
                table.addCell(new Cell().add(new Paragraph(v.getSystolicBp() != null ? String.valueOf(v.getSystolicBp()) : "")));
                table.addCell(new Cell().add(new Paragraph(v.getDiastolicBp() != null ? String.valueOf(v.getDiastolicBp()) : "")));
                table.addCell(new Cell().add(new Paragraph(v.getHeartRate() != null ? String.valueOf(v.getHeartRate()) : "")));
                table.addCell(new Cell().add(new Paragraph(v.getSpo2() != null ? String.valueOf(v.getSpo2()) : "")));
                table.addCell(new Cell().add(new Paragraph(v.getTemperature() != null ? String.valueOf(v.getTemperature()) : "")));
                table.addCell(new Cell().add(new Paragraph(v.getCvp() != null ? String.valueOf(v.getCvp()) : "")));
                table.addCell(new Cell().add(new Paragraph(v.getRespiratoryRate() != null ? String.valueOf(v.getRespiratoryRate()) : "")));
            }
            document.add(table);

            document.add(new Paragraph(""));
            document.add(new Paragraph("Баланс рідини:").setBold());
            var balance = fluidBalanceService.getBalance(day.getId());
            document.add(new Paragraph("Надійшло: " + balance.getTotalIntake() + " мл"));
            document.add(new Paragraph("Виділено: " + balance.getTotalOutput() + " мл"));
            document.add(new Paragraph("Добовий баланс: " + balance.getDailyBalance() + " мл"));
            document.add(new Paragraph("Кумулятивний баланс: " + balance.getCumulativeBalance() + " мл"));

            if (day.getSignedAt() != null) {
                document.add(new Paragraph(""));
                document.add(new Paragraph("Підписано лікарем: " +
                        day.getSignedAt().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))));
            }

            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate PDF", e);
        }
    }
}
