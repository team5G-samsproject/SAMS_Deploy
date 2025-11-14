package com.myproject.servlet;

import com.myproject.db.DBConnection;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONObject;

@WebServlet("/BookingServlet")
public class BookingServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        // Default to JSON; some branches will override to HTML.
        res.setContentType("application/json");
        String action = req.getParameter("action");
        PrintWriter out = res.getWriter();
        JSONArray arr = new JSONArray();

        // Guard against null action to avoid NPE
        if (action == null || action.trim().isEmpty()) {
            out.print(new JSONObject().put("error", "missing_action").toString());
            return;
        }

        try (Connection conn = DBConnection.getConnection()) {

            switch (action) {

                case "getSeats": {
                    String event = req.getParameter("event");
                    if (event == null) { out.print("[]"); return; }
                    String sqlSeats = "SELECT id, seats, status, user_id FROM reservations WHERE event_title=? AND status IN ('PENDING','PAID','CONFIRMED')";
                    PreparedStatement psSeats = conn.prepareStatement(sqlSeats);
                    psSeats.setString(1, event);
                    ResultSet rsSeats = psSeats.executeQuery();
                    while (rsSeats.next()) {
                        JSONObject o = new JSONObject();
                        o.put("id", rsSeats.getInt("id"));
                        o.put("seats", rsSeats.getString("seats"));
                        o.put("status", rsSeats.getString("status"));
                        o.put("user", rsSeats.getString("user_id"));
                        arr.put(o);
                    }
                    out.print(arr.toString());
                    break;
                }

                case "listStudentBookings": {
                    String userId = req.getParameter("user");
                    if (userId == null) { out.print("[]"); return; }

                    String sqlStudent = "SELECT r.id, r.event_title, r.seats, r.status, r.total, e.event_date " +
                                        "FROM reservations r INNER JOIN events e ON r.event_title = e.title " +
                                        "WHERE r.user_id=? ORDER BY r.created_at DESC";
                    PreparedStatement psStudent = conn.prepareStatement(sqlStudent);
                    psStudent.setString(1, userId);
                    ResultSet rsStudent = psStudent.executeQuery();
                    while (rsStudent.next()) {
                        JSONObject o = new JSONObject();
                        o.put("id", rsStudent.getInt("id"));
                        o.put("event", rsStudent.getString("event_title"));
                        o.put("seats", rsStudent.getString("seats"));
                        o.put("status", rsStudent.getString("status"));
                        o.put("total", rsStudent.getInt("total"));
                        // Use java.sql.Date explicitly and convert to string safely
                        java.sql.Date sqlDate = rsStudent.getDate("event_date");
                        o.put("event_date", sqlDate != null ? sqlDate.toString() : JSONObject.NULL);
                        arr.put(o);
                    }
                    out.print(arr.toString());
                    break;
                }

                case "listPaid": {
                    String sqlPaid = "SELECT id,event_title,seats,user_id,total,created_at FROM reservations WHERE status='PENDING' ORDER BY created_at ASC";
                    PreparedStatement psPaid = conn.prepareStatement(sqlPaid);
                    ResultSet rsPaid = psPaid.executeQuery();
                    while (rsPaid.next()) {
                        JSONObject o = new JSONObject();
                        o.put("id", rsPaid.getInt("id"));
                        o.put("event", rsPaid.getString("event_title"));
                        o.put("seats", rsPaid.getString("seats"));
                        o.put("user", rsPaid.getString("user_id"));
                        o.put("total", rsPaid.getInt("total"));
                        o.put("created_at", rsPaid.getTimestamp("created_at").toString());
                        arr.put(o);
                    }
                    out.print(arr.toString());
                    break;
                }

                case "listBooked": {
                    String sqlConfirmed = "SELECT id,event_title,seats,user_id,total,created_at FROM reservations WHERE status='CONFIRMED' ORDER BY created_at ASC";
                    PreparedStatement psConfirmed = conn.prepareStatement(sqlConfirmed);
                    ResultSet rsConfirmed = psConfirmed.executeQuery();
                    while (rsConfirmed.next()) {
                        JSONObject o = new JSONObject();
                        o.put("id", rsConfirmed.getInt("id"));
                        o.put("event", rsConfirmed.getString("event_title"));
                        o.put("seats", rsConfirmed.getString("seats"));
                        o.put("user", rsConfirmed.getString("user_id"));
                        o.put("total", rsConfirmed.getInt("total"));
                        o.put("created_at", rsConfirmed.getTimestamp("created_at").toString());
                        arr.put(o);
                    }
                    out.print(arr.toString());
                    break;
                }

                case "listRefunds": {
                    String sqlCancelled = "SELECT r.id,r.event_title,r.seats,r.user_id,r.total,r.status,r.created_at,e.event_date " +
                                          "FROM reservations r INNER JOIN events e ON r.event_title=e.title " +
                                          "WHERE r.status IN ('CANCELLED','REFUNDED') ORDER BY r.created_at ASC";
                    PreparedStatement psCancelled = conn.prepareStatement(sqlCancelled);
                    ResultSet rsCancelled = psCancelled.executeQuery();
                    while (rsCancelled.next()) {
                        JSONObject o = new JSONObject();
                        o.put("id", rsCancelled.getInt("id"));
                        o.put("event", rsCancelled.getString("event_title"));
                        o.put("seats", rsCancelled.getString("seats"));
                        o.put("user", rsCancelled.getString("user_id"));
                        o.put("total", rsCancelled.getInt("total"));
                        o.put("status", rsCancelled.getString("status"));
                        o.put("created_at", rsCancelled.getTimestamp("created_at").toString());

                        java.sql.Date sqlShowDate = rsCancelled.getDate("event_date");
                        String seatStr = rsCancelled.getString("seats");
                        int totalAmount = rsCancelled.getInt("total");
                        int refundAmount = calculateRefund(sqlShowDate, seatStr, totalAmount);
                        o.put("refundAmount", refundAmount);

                        arr.put(o);
                    }
                    out.print(arr.toString());
                    break;
                }

                // Minimal ticket endpoint inside BookingServlet (keeps existing flow)
                case "getTicket": {
                    // switch to HTML for this response
                    res.setContentType("text/html; charset=UTF-8");
                    String bookingId = req.getParameter("id");
                    PrintWriter pw = res.getWriter();
                    if (bookingId == null || bookingId.trim().isEmpty()) {
                        pw.println("<p>Invalid Ticket ID!</p>");
                        return;
                    }

                    String ticketSql = "SELECT r.id, r.event_title, r.seats, r.total, u.username, u.email, u.department, u.year, e.event_date, r.status " +
                                       "FROM reservations r " +
                                       "JOIN users u ON r.user_id = u.user_id " +
                                       "JOIN events e ON r.event_title = e.title " +
                                       "WHERE r.id=?";
                    PreparedStatement psTicket = conn.prepareStatement(ticketSql);
                    psTicket.setString(1, bookingId);
                    ResultSet rsTicket = psTicket.executeQuery();

                    if (rsTicket.next()) {
                        // get values safely
                        String eventTitle = rsTicket.getString("event_title");
                        String seats = rsTicket.getString("seats");
                        int totalPaid = rsTicket.getInt("total");
                        String username = rsTicket.getString("username");
                        String email = rsTicket.getString("email");
                        String dept = rsTicket.getString("department");
                        String year = rsTicket.getString("year");
                        java.sql.Date sqlEvDate = rsTicket.getDate("event_date");
                        String eventDateStr = sqlEvDate != null ? sqlEvDate.toString() : "";

                        // Optional: allow only CONFIRMED tickets (uncomment to enforce)
                        // String status = rsTicket.getString("status");
                        // if (!"CONFIRMED".equalsIgnoreCase(status)) {
                        //     pw.println("<p>Ticket is not confirmed yet.</p>");
                        //     return;
                        // }

                        pw.println("<!doctype html>");
                        pw.println("<html><head><meta charset='utf-8'><title>SAMS E-Ticket</title>");
                        pw.println("<style>body{font-family:Poppins,sans-serif;background:#f8f6f1;padding:40px;} .ticket{background:white;padding:25px;border-radius:14px;box-shadow:0 5px 15px rgba(0,0,0,0.1);max-width:700px;margin:auto;} h2{color:#5a3805;margin-bottom:15px;text-align:center;} p{font-size:15px;line-height:1.5;margin:5px 0;} .meta{display:flex;justify-content:space-between;gap:10px;flex-wrap:wrap;} </style>");
                        pw.println("</head><body>");
                        pw.println("<div class='ticket'>");
                        pw.println("<h2>🎫 SAMS Event E-Ticket</h2>");
                        pw.println("<div class='meta'>");
                        pw.println("<div>");
                        pw.println("<p><b>Event:</b> " + escapeHtml(eventTitle) + "</p>");
                        pw.println("<p><b>Date:</b> " + escapeHtml(eventDateStr) + "</p>");
                        pw.println("<p><b>Seats:</b> " + escapeHtml(seats) + "</p>");
                        pw.println("<p><b>Total Paid:</b> ₹" + totalPaid + "</p>");
                        pw.println("</div>");
                        pw.println("<div>");
                        pw.println("<p><b>Name:</b> " + escapeHtml(username) + "</p>");
                        pw.println("<p><b>Email:</b> " + escapeHtml(email) + "</p>");
                        pw.println("<p><b>Department:</b> " + escapeHtml(dept) + "</p>");
                        pw.println("<p><b>Year:</b> " + escapeHtml(year) + "</p>");
                        pw.println("</div>");
                        pw.println("</div>");
                        pw.println("<hr>");
                        pw.println("<p>✅ This ticket is generated by SAMS. Please show this ticket at the entrance.</p>");
                        pw.println("</div></body></html>");
                    } else {
                        pw.println("<p>Ticket not found!</p>");
                    }
                    break;
                }

                default:
                    out.print(new JSONObject().put("error", "invalid_action").toString());
                    break;
            }

        } catch (Exception e) {
            e.printStackTrace();
            out.print(new JSONObject().put("error", "server_error").toString());
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        String action = req.getParameter("action");
        res.setContentType("application/json");
        PrintWriter out = res.getWriter();

        try (Connection conn = DBConnection.getConnection()) {

            if ("reserve".equals(action)) {
                String event = req.getParameter("event");
                String seats = req.getParameter("seats");
                String user = req.getParameter("user");
                int total = Integer.parseInt(req.getParameter("total"));

                // Check conflicts
                String checkSql = "SELECT seats,status FROM reservations WHERE event_title=? AND status IN ('PENDING','PAID','CONFIRMED')";
                PreparedStatement cps = conn.prepareStatement(checkSql);
                cps.setString(1, event);
                ResultSet crs = cps.executeQuery();

                Set<String> requested = new HashSet<>();
                for (String s : seats.split(",")) requested.add(s.trim());
                List<String> conflicts = new ArrayList<>();
                while (crs.next()) {
                    String existingSeats = crs.getString("seats");
                    if (existingSeats == null || existingSeats.trim().isEmpty()) continue;
                    for (String s : existingSeats.split(",")) if (requested.contains(s.trim())) conflicts.add(s.trim());
                }

                if (!conflicts.isEmpty()) {
                    JSONObject resp = new JSONObject();
                    resp.put("status", "conflict");
                    resp.put("conflicts", new JSONArray(conflicts));
                    out.print(resp.toString());
                    return;
                }

                // Insert reservation
                String insertSql = "INSERT INTO reservations(event_title,seats,user_id,total,status) VALUES(?,?,?,?,?)";
                PreparedStatement ips = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS);
                ips.setString(1, event);
                ips.setString(2, seats);
                ips.setString(3, user);
                ips.setInt(4, total);
                ips.setString(5, "PENDING");
                int i = ips.executeUpdate();
                if (i > 0) {
                    ResultSet gk = ips.getGeneratedKeys();
                    int id = -1;
                    if (gk.next()) id = gk.getInt(1);
                    JSONObject resp = new JSONObject();
                    resp.put("status", "success");
                    resp.put("reservationId", id);
                    out.print(resp.toString());
                } else out.print("{\"status\":\"fail\"}");

            } else if ("confirm".equals(action) || "cancel".equals(action) || "markRefunded".equals(action)) {
    int resId = Integer.parseInt(req.getParameter("reservationId"));
    String newStatus = "CONFIRMED";
    if ("cancel".equals(action)) newStatus = "CANCELLED";
    if ("markRefunded".equals(action)) newStatus = "REFUNDED";

    // 1️⃣ Update reservation status
    String sql = "UPDATE reservations SET status=? WHERE id=?";
    PreparedStatement ps = conn.prepareStatement(sql);
    ps.setString(1, newStatus);
    ps.setInt(2, resId);
    int updated = ps.executeUpdate();

    if (updated > 0 && "CONFIRMED".equals(newStatus)) {
        // 2️⃣ Fetch reservation details
        String selectSql = "SELECT event_title, seats, user_id, total FROM reservations WHERE id=?";
        PreparedStatement sel = conn.prepareStatement(selectSql);
        sel.setInt(1, resId);
        ResultSet rs = sel.executeQuery();

        if (rs.next()) {
            String eventName = rs.getString("event_title");
            String seats = rs.getString("seats");
            String user = rs.getString("user_id");
            double total = rs.getDouble("total");

            // 3️⃣ Insert into bookings
            String insertSql = "INSERT INTO bookings (event_name, seats, user_name, amount, status) VALUES (?, ?, ?, ?, ?)";
            PreparedStatement ins = conn.prepareStatement(insertSql);
            ins.setString(1, eventName);
            ins.setString(2, seats);
            ins.setString(3, user);
            ins.setDouble(4, total);
            ins.setString(5, "Approved");
            ins.executeUpdate();
        }
    }

    JSONObject resp = new JSONObject();
    resp.put("status", updated > 0 ? "success" : "fail");
    out.print(resp.toString());
}
 else {
                out.print("{\"status\":\"invalid_action\"}");
            }

        } catch (Exception e) {
            e.printStackTrace();
            out.print("{\"status\":\"error\"}");
        }
    }

    private int calculateRefund(java.sql.Date showDate, String seats, int totalAmount) {
        LocalDate today = LocalDate.now();
        LocalDate show = showDate.toLocalDate();
        long daysBeforeShow = ChronoUnit.DAYS.between(today, show);
        int seatCount = seats.split(",").length;

        if (daysBeforeShow > 3) {
            return totalAmount - (5 * seatCount);
        } else if (daysBeforeShow >= 1) {
            return totalAmount - (15 * seatCount);
        } else {
            return totalAmount / 2;
        }
    }

    // Small helper to avoid HTML injection in ticket view
    private String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#x27;");
    }
} 