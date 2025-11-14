package com.myproject.servlet;

import com.myproject.db.DBConnection;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import jakarta.servlet.*;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;

@WebServlet("/StudentBookingServlet")
public class StudentBookingServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json;charset=UTF-8");
        PrintWriter out = resp.getWriter();
        JSONObject result = new JSONObject();

        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("username") == null) {
            result.put("success", false);
            result.put("error", "user_not_logged_in");
            out.print(result.toString());
            return;
        }

        String username = (String) session.getAttribute("username");
        String action = req.getParameter("action");

        if (action == null) {
            result.put("success", false);
            result.put("error", "missing_action_parameter");
            out.print(result.toString());
            return;
        }

        try (Connection con = DBConnection.getConnection()) {

            if ("pending".equals(action)) {
                PreparedStatement ps = con.prepareStatement(
                    "SELECT id, event_title, seats, total, status, created_at " +
                    "FROM reservations WHERE user_id = ? ORDER BY created_at DESC"
                );
                ps.setString(1, username);
                ResultSet rs = ps.executeQuery();

                JSONArray arr = new JSONArray();
                while (rs.next()) {
                    String status = rs.getString("status");
                    if(!"PENDING".equalsIgnoreCase(status) && !"CONFIRMED".equalsIgnoreCase(status)) continue;

                    JSONObject o = new JSONObject();
                    o.put("id", rs.getInt("id"));
                    o.put("event_title", rs.getString("event_title"));
                    o.put("seats", rs.getString("seats"));
                    o.put("total", rs.getDouble("total"));
                    o.put("status", status);
                    o.put("created_at", rs.getString("created_at"));
                    arr.put(o);
                }
                result.put("success", true);
                result.put("pending", arr);

            } else if ("booked".equals(action)) {
                PreparedStatement ps = con.prepareStatement(
                    "SELECT id, event_name, seats, amount, status, booking_date " +
                    "FROM bookings WHERE user_name = ? ORDER BY booking_date DESC"
                );
                ps.setString(1, username);
                ResultSet rs = ps.executeQuery();

                JSONArray arr = new JSONArray();
                while (rs.next()) {
                    if (!"Approved".equalsIgnoreCase(rs.getString("status"))) continue;

                    JSONObject o = new JSONObject();
                    o.put("id", rs.getInt("id"));
                    o.put("event_name", rs.getString("event_name"));
                    o.put("seats", rs.getString("seats"));
                    o.put("amount", rs.getDouble("amount"));
                    o.put("status", rs.getString("status"));
                    o.put("booking_date", rs.getString("booking_date"));
                    arr.put(o);
                }
                result.put("success", true);
                result.put("booked", arr);

            } else if ("cancel".equals(action)) {
                String bookingId = req.getParameter("id");
                if (bookingId == null || bookingId.isEmpty()) {
                    result.put("success", false);
                    result.put("error", "missing_booking_id");
                } else {
                    PreparedStatement ps = con.prepareStatement(
                        "DELETE FROM bookings WHERE id=? AND user_name=? AND status='Approved'"
                    );
                    ps.setInt(1, Integer.parseInt(bookingId));
                    ps.setString(2, username);
                    int deleted = ps.executeUpdate();
                    result.put("success", deleted > 0);
                    if (deleted == 0) result.put("error", "Cannot cancel this booking");
                }

            } else if ("confirm".equals(action)) {
                String resId = req.getParameter("id");
                if(resId == null || resId.isEmpty()) {
                    result.put("success", false);
                    result.put("error", "missing_reservation_id");
                } else {
                    // 1. Get reservation details
                    PreparedStatement ps1 = con.prepareStatement(
                        "SELECT event_title, seats, total FROM reservations WHERE id=? AND user_id=? AND status='PENDING'"
                    );
                    ps1.setInt(1, Integer.parseInt(resId));
                    ps1.setString(2, username);
                    ResultSet rs = ps1.executeQuery();

                    if(rs.next()){
                        String event_name = rs.getString("event_title");
                        String seats = rs.getString("seats");
                        double amount = rs.getDouble("total");

                        // 2. Insert into bookings
                        PreparedStatement ps2 = con.prepareStatement(
                            "INSERT INTO bookings(event_name,seats,user_name,amount,status,booking_date) VALUES(?,?,?,?,'Approved',NOW())"
                        );
                        ps2.setString(1, event_name);
                        ps2.setString(2, seats);
                        ps2.setString(3, username);
                        ps2.setDouble(4, amount);
                        ps2.executeUpdate();

                        // 3. Update reservation status
                        PreparedStatement ps3 = con.prepareStatement(
                            "UPDATE reservations SET status='CONFIRMED' WHERE id=?"
                        );
                        ps3.setInt(1, Integer.parseInt(resId));
                        ps3.executeUpdate();

                        result.put("success", true);
                    } else {
                        result.put("success", false);
                        result.put("error", "reservation_not_found");
                    }
                }

            } else {
                result.put("success", false);
                result.put("error", "invalid_action");
            }

        } catch (Exception e) {
            e.printStackTrace();
            result.put("success", false);
            result.put("error", e.getMessage());
        }

        out.print(result.toString());
    }
}
