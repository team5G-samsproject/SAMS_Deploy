package com.myproject.servlet;

import com.myproject.db.DBConnection;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import org.json.JSONObject;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;

@WebServlet("/StudentServlet")
public class StudentServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        PrintWriter out = response.getWriter();
        JSONObject json = new JSONObject();

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("username") == null) {
            json.put("success", false);
            json.put("error", "User not logged in");
            out.print(json.toString());
            return;
        }

        String username = (String) session.getAttribute("username");
        String action = request.getParameter("action");

        try (Connection con = DBConnection.getConnection()) {
            if ("getProfile".equals(action)) {
                PreparedStatement ps = con.prepareStatement(
                        "SELECT user_id, username, email, department, year FROM users WHERE username = ?");
                ps.setString(1, username);
                ResultSet rs = ps.executeQuery();

                if (rs.next()) {
                    json.put("success", true);
                    json.put("user_id", rs.getInt("user_id"));
                    json.put("username", rs.getString("username"));
                    json.put("email", rs.getString("email"));
                    json.put("department", rs.getString("department"));
                    json.put("year", rs.getString("year"));
                } else {
                    json.put("success", false);
                    json.put("error", "User not found");
                }
            } else {
                json.put("success", false);
                json.put("error", "Invalid action");
            }
        } catch (Exception e) {
            e.printStackTrace();
            json.put("success", false);
            json.put("error", e.getMessage());
        }

        out.print(json.toString());
        out.flush();
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        PrintWriter out = response.getWriter();
        JSONObject json = new JSONObject();

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("username") == null) {
            json.put("success", false);
            json.put("error", "User not logged in");
            out.print(json.toString());
            return;
        }

        String action = request.getParameter("action");

        try (Connection con = DBConnection.getConnection()) {
            if ("updateProfile".equals(action)) {
                int user_id = Integer.parseInt(request.getParameter("user_id"));
                String username = request.getParameter("username");
                String email = request.getParameter("email");
                String department = request.getParameter("department");
                String year = request.getParameter("year");

                PreparedStatement ps = con.prepareStatement(
                        "UPDATE users SET username=?, email=?, department=?, year=? WHERE user_id=?");
                ps.setString(1, username);
                ps.setString(2, email);
                ps.setString(3, department);
                ps.setString(4, year);
                ps.setInt(5, user_id);

                int updated = ps.executeUpdate();

                if (updated > 0) {
                    // update session username
                    session.setAttribute("username", username);

                    json.put("success", true);
                } else {
                    json.put("success", false);
                    json.put("error", "Update failed or user not found");
                }
            } else {
                json.put("success", false);
                json.put("error", "Invalid action");
            }

        } catch (Exception e) {
            e.printStackTrace();
            json.put("success", false);
            json.put("error", e.getMessage());
        }

        out.print(json.toString());
        out.flush();
    }
}
