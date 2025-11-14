package com.myproject.servlet;

import com.myproject.db.DBConnection;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.sql.*;
import java.util.Date;

@WebServlet("/TicketServlet")
public class TicketServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {

        String action = req.getParameter("action");
        if (!"getTicket".equals(action)) {
            res.setContentType("text/plain");
            res.getWriter().println("Invalid action");
            return;
        }

        String idStr = req.getParameter("id");
        if (idStr == null || idStr.isEmpty()) {
            res.setContentType("text/plain");
            res.getWriter().println("Ticket ID missing");
            return;
        }

        int bookingId = Integer.parseInt(idStr);

        try (Connection con = DBConnection.getConnection()) {
            String sql = "SELECT event_name, user_name, seats, amount, status, booking_date " +
                    "FROM bookings WHERE id=? AND status='Approved'";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, bookingId);
            ResultSet rs = ps.executeQuery();

            if (!rs.next()) {
                res.setContentType("text/plain");
                res.getWriter().println("Ticket not found or not approved");
                return;
            }

            res.setContentType("application/pdf");
            res.setHeader("Content-Disposition", "attachment; filename=ticket_" + bookingId + ".pdf");

            Document doc = new Document(new Rectangle(500, 250), 20, 20, 20, 20); // Ticket size
            PdfWriter writer = PdfWriter.getInstance(doc, res.getOutputStream());
            doc.open();

            // Fonts
            Font titleFont = new Font(Font.FontFamily.HELVETICA, 20, Font.BOLD, BaseColor.WHITE);
            Font labelFont = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD, BaseColor.DARK_GRAY);
            Font valueFont = new Font(Font.FontFamily.HELVETICA, 12, Font.NORMAL, BaseColor.BLACK);
            Font footerFont = new Font(Font.FontFamily.HELVETICA, 10, Font.ITALIC, BaseColor.DARK_GRAY);

            // ---- Background Rectangle ----
            PdfContentByte canvas = writer.getDirectContentUnder();
            Rectangle rect = new Rectangle(doc.getPageSize());
            rect.setBackgroundColor(new BaseColor(255, 235, 205)); // Light yellow
            canvas.rectangle(rect);

            // ---- Header ----
            PdfPTable headerTable = new PdfPTable(1);
            headerTable.setWidthPercentage(100);
            PdfPCell headerCell = new PdfPCell(new Phrase("🎟 SAMS Event Ticket", titleFont));
            headerCell.setBackgroundColor(new BaseColor(255, 87, 34)); // Orange header
            headerCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            headerCell.setPadding(10);
            headerCell.setBorder(Rectangle.NO_BORDER);
            headerTable.addCell(headerCell);
            doc.add(headerTable);
            doc.add(Chunk.NEWLINE);

            // ---- Ticket Details Table ----
            PdfPTable table = new PdfPTable(2);
            table.setWidthPercentage(80);
            table.setWidths(new float[]{1f, 2f});
            table.setHorizontalAlignment(Element.ALIGN_LEFT);

            addRow(table, "Event:", rs.getString("event_name"), labelFont, valueFont);
            addRow(table, "Name:", rs.getString("user_name"), labelFont, valueFont);
            addRow(table, "Seats:", rs.getString("seats"), labelFont, valueFont);
            addRow(table, "Amount:", "₹" + rs.getBigDecimal("amount"), labelFont, valueFont);
            addRow(table, "Status:", rs.getString("status"), labelFont, valueFont);
            addRow(table, "Booking Date:", rs.getTimestamp("booking_date").toString(), labelFont, valueFont);

            doc.add(table);

            // ---- QR code / Scanner effect ----
            BarcodeQRCode qrCode = new BarcodeQRCode(
                    "BookingID:" + bookingId + "|Event:" + rs.getString("event_name") + "|User:" + rs.getString("user_name"),
                    100, 100, null
            );
            Image qrImage = qrCode.getImage();
            qrImage.setAbsolutePosition(doc.getPageSize().getWidth() - 120, 80);
            qrImage.scaleAbsolute(80, 80);
            doc.add(qrImage);

            // ---- Tear line ----
            PdfContentByte cb = writer.getDirectContent();
            cb.setLineWidth(1f);
            cb.setLineDash(3, 3);
            cb.moveTo(20, 70);
            cb.lineTo(doc.getPageSize().getWidth() - 20, 70);
            cb.stroke();

            // ---- Footer ----
            Paragraph footer = new Paragraph("Thank you for booking with SAMS! Present this ticket at the event entrance.", footerFont);
            footer.setAlignment(Element.ALIGN_CENTER);
            footer.setSpacingBefore(10);
            doc.add(footer);

            doc.close();

        } catch (Exception e) {
            e.printStackTrace();
            res.setContentType("text/plain");
            res.getWriter().println("Error generating ticket: " + e.getMessage());
        }
    }

    private void addRow(PdfPTable table, String label, String value, Font labelFont, Font valueFont) {
        PdfPCell cell1 = new PdfPCell(new Phrase(label, labelFont));
        cell1.setBorder(Rectangle.NO_BORDER);
        cell1.setPadding(4);
        table.addCell(cell1);

        PdfPCell cell2 = new PdfPCell(new Phrase(value, valueFont));
        cell2.setBorder(Rectangle.NO_BORDER);
        cell2.setPadding(4);
        table.addCell(cell2);
    }
}
