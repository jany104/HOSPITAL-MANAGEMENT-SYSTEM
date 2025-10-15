package hospital.management.system;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class PdfReceiptWriter {

    private static final DateTimeFormatter DISPLAY_FORMAT = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm", Locale.ENGLISH);
    private static final DateTimeFormatter FILE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss", Locale.ENGLISH);

    private PdfReceiptWriter() {
    }

    static Path writeDischargeSummary(Path outputDirectory, DischargeDetails details) throws IOException {
        Files.createDirectories(outputDirectory);
        String sanitizedPatientId = sanitize(details.patientNumber());
        if (sanitizedPatientId.isEmpty()) {
            sanitizedPatientId = "patient";
        }
        String filename = String.format(Locale.ENGLISH, "discharge-%s-%s.pdf",
                sanitizedPatientId,
                details.checkOut().format(FILE_FORMAT));
        Path target = outputDirectory.resolve(filename);
        Files.write(target, buildDocument(details));
        return target;
    }

    private static byte[] buildDocument(DischargeDetails details) throws IOException {
        byte[] content = buildContentStream(details);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        write(out, "%PDF-1.4\n");
        List<Integer> offsets = new ArrayList<>();
        offsets.add(0);

        offsets.add(out.size());
        write(out, "1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n");
        offsets.add(out.size());
        write(out, "2 0 obj\n<< /Type /Pages /Kids [3 0 R] /Count 1 >>\nendobj\n");
        offsets.add(out.size());
        write(out, "3 0 obj\n<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Contents 4 0 R /Resources << /Font << /F1 5 0 R >> >> >>\nendobj\n");

        offsets.add(out.size());
        write(out, "4 0 obj\n<< /Length " + content.length + " >>\nstream\n");
        out.write(content);
        write(out, "\nendstream\nendobj\n");

        offsets.add(out.size());
        write(out, "5 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>\nendobj\n");

        int xrefOffset = out.size();
        write(out, "xref\n0 " + offsets.size() + "\n");
        write(out, String.format(Locale.ENGLISH, "%010d 65535 f \n", 0));
        for (int i = 1; i < offsets.size(); i++) {
            write(out, String.format(Locale.ENGLISH, "%010d 00000 n \n", offsets.get(i)));
        }
        write(out, "trailer\n<< /Size " + offsets.size() + " /Root 1 0 R >>\nstartxref\n" + xrefOffset + "\n%%EOF");
        return out.toByteArray();
    }

    private static byte[] buildContentStream(DischargeDetails details) {
        List<PdfLine> lines = new ArrayList<>();
        lines.add(new PdfLine(72, 770, 22, "Hospital Management System"));
        lines.add(new PdfLine(72, 740, 16, "Patient Discharge Summary"));
        lines.add(new PdfLine(72, 708, 12, "Generated: " + formatTimestamp(details.checkOut())));

        String patientLine = "Patient: " + nonEmpty(details.patientNumber(), "Unknown");
        if (details.patientName() != null && !details.patientName().isBlank()) {
            patientLine += " - " + details.patientName();
        }
        lines.add(new PdfLine(72, 660, 14, patientLine));

        String roomLine = "Room: " + nonEmpty(details.roomNumber(), "N/A");
        if (details.bedType() != null && !details.bedType().isBlank()) {
            roomLine += " (Bed: " + details.bedType() + ")";
        }
        lines.add(new PdfLine(72, 632, 12, roomLine));
        lines.add(new PdfLine(72, 604, 12, "Check-in: " + formatTimestamp(details.checkIn())));
        lines.add(new PdfLine(72, 576, 12, "Check-out: " + formatTimestamp(details.checkOut())));
        lines.add(new PdfLine(72, 544, 12, "Stay length: " + details.stayDays() + (details.stayDays() == 1 ? " day" : " days")));
        lines.add(new PdfLine(72, 516, 12, "Room rate: " + formatCurrency(details.roomRate()) + " per day"));
        lines.add(new PdfLine(72, 488, 12, "Room charges: " + formatCurrency(details.roomCharge())));
        lines.add(new PdfLine(72, 460, 12, "Deposit recorded: " + formatCurrency(details.deposit())));
        long balanceAmount = Math.abs((long) details.balance());
        String balanceLabel = details.balance() >= 0 ? "Balance due: " : "Refund owed: ";
        lines.add(new PdfLine(72, 432, 12, balanceLabel + formatCurrency(balanceAmount)));
        lines.add(new PdfLine(72, 392, 12, "Prepared by: Reception desk"));
        lines.add(new PdfLine(72, 364, 11, "Note: Share this receipt with the patient and finance team."));

        StringBuilder content = new StringBuilder();
        content.append("BT\n");
        for (PdfLine line : lines) {
            content.append(String.format(Locale.ENGLISH, "/F1 %.1f Tf\n", line.fontSize()));
            content.append(String.format(Locale.ENGLISH, "1 0 0 1 %.2f %.2f Tm\n", line.x(), line.y()));
            content.append("(").append(escape(line.text())).append(") Tj\n");
        }
        content.append("ET\n");
        return content.toString().getBytes(StandardCharsets.US_ASCII);
    }

    private static String escape(String text) {
        return text.replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)");
    }

    private static void write(ByteArrayOutputStream out, String text) throws IOException {
        out.write(text.getBytes(StandardCharsets.US_ASCII));
    }

    private static String sanitize(String value) {
        if (value == null) {
            return "";
        }
        return value.replaceAll("[^A-Za-z0-9_-]", "");
    }

    private static String nonEmpty(String value, String fallback) {
        return (value == null || value.isBlank()) ? fallback : value;
    }

    private static String formatCurrency(long amount) {
        return String.format(Locale.ENGLISH, "INR %,d", amount);
    }

    private static String formatTimestamp(LocalDateTime time) {
        return time == null ? "N/A" : time.format(DISPLAY_FORMAT);
    }

    static record DischargeDetails(
            String patientNumber,
            String patientName,
            String roomNumber,
            String bedType,
            LocalDateTime checkIn,
            LocalDateTime checkOut,
            long stayDays,
            int roomRate,
            int roomCharge,
            int deposit,
            int balance) {
    }

    private record PdfLine(double x, double y, double fontSize, String text) {
    }
}
