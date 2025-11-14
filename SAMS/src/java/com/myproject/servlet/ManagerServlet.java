package com.myproject.servlet;

import com.myproject.db.DBConnection;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;
import org.json.JSONArray;
import org.json.JSONObject;

@WebServlet("/ManagerServlet")
public class ManagerServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        res.setContentType("application/json");
        PrintWriter out = res.getWriter();

        try (Connection conn = DBConnection.getConnection()) {
            String sql = "SELECT * FROM events ORDER BY event_date ASC";
            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();
            JSONArray arr = new JSONArray();

            while (rs.next()) {
                JSONObject obj = new JSONObject();
                obj.put("id", rs.getInt("id"));
                obj.put("title", rs.getString("title"));
                obj.put("date", rs.getDate("event_date").toString());
                obj.put("description", rs.getString("description"));
                obj.put("status", rs.getString("status"));
                obj.put("bookings", rs.getInt("bookings"));
                arr.put(obj);
            }

            // Return all events as JSON array
            out.print(arr.toString());

        } catch (Exception e) {
            e.printStackTrace();
            out.print("{\"error\":\"Failed to fetch events.\"}");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        res.setContentType("application/json");
        PrintWriter out = res.getWriter();
        String action = req.getParameter("action");

        try (Connection conn = DBConnection.getConnection()) {

            if ("create".equals(action)) {
                String title = req.getParameter("title");
                String date = req.getParameter("date");
                String desc = req.getParameter("description");

                String sql = "INSERT INTO events(title,event_date,description,status,bookings) VALUES(?,?,?,?,0)";
                PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
                ps.setString(1, title);
                ps.setDate(2, Date.valueOf(date));
                ps.setString(3, desc);
                ps.setString(4, "ACTIVE"); // default status
                int i = ps.executeUpdate();

                if (i > 0) {
                    ResultSet keys = ps.getGeneratedKeys();
                    int newId = 0;
                    if (keys.next()) newId = keys.getInt(1);

                    JSONObject obj = new JSONObject();
                    obj.put("status", "success");
                    obj.put("id", newId);
                    obj.put("title", title);
                    obj.put("date", date);
                    obj.put("description", desc);
                    obj.put("statusText", "ACTIVE");
                    obj.put("bookings", 0);
                    out.print(obj.toString());
                } else out.print("{\"status\":\"fail\"}");

            } else if ("update".equals(action)) {
                int id = Integer.parseInt(req.getParameter("id"));
                String title = req.getParameter("title");
                String date = req.getParameter("date");
                String desc = req.getParameter("description");

                String sql = "UPDATE events SET title=?, event_date=?, description=? WHERE id=?";
                PreparedStatement ps = conn.prepareStatement(sql);
                ps.setString(1, title);
                ps.setDate(2, Date.valueOf(date));
                ps.setString(3, desc);
                ps.setInt(4, id);

                if (ps.executeUpdate() > 0) {
                    JSONObject obj = new JSONObject();
                    obj.put("status", "success");
                    obj.put("id", id);
                    obj.put("title", title);
                    obj.put("date", date);
                    obj.put("description", desc);
                    out.print(obj.toString());
                } else {
                    out.print("{\"status\":\"fail\"}");
                }

            } else if ("delete".equals(action)) {
                int id = Integer.parseInt(req.getParameter("id"));
                String sql = "DELETE FROM events WHERE id=?";
                PreparedStatement ps = conn.prepareStatement(sql);
                ps.setInt(1, id);

                if (ps.executeUpdate() > 0) {
                    JSONObject obj = new JSONObject();
                    obj.put("status", "success");
                    obj.put("id", id);
                    out.print(obj.toString());
                } else {
                    out.print("{\"status\":\"fail\"}");
                }

            } else {
                out.print("{\"status\":\"invalid_action\"}");
            }

        } catch (Exception e) {
            e.printStackTrace();
            out.print("{\"status\":\"error\"}");
        }
    }
}
