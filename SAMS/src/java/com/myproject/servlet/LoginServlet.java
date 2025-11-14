package com.myproject.servlet;

import com.myproject.db.DBConnection;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;

@WebServlet("/LoginServlet")
public class LoginServlet extends HttpServlet {
    
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = response.getWriter();

        String role = request.getParameter("login-role");
        String emailOrUsername = request.getParameter("login-email");
        String password = request.getParameter("login-password");

        try (Connection conn = DBConnection.getConnection()) {

            if (conn == null) {
                out.println("<script>alert('⚠️ Database connection failed!');location='login_signup.html';</script>");
                return;
            }

            String sql;

            // ✅ Choose correct table based on role
            if ("student".equalsIgnoreCase(role)) {
                sql = "SELECT user_id AS id, username, role FROM users WHERE role=? AND (email=? OR username=?) AND password=?";
            } else {
                sql = "SELECT admin_id AS id, name AS username, role FROM administrators WHERE role=? AND (email=? OR name=?) AND password=?";
            }

            PreparedStatement pst = conn.prepareStatement(sql);
            pst.setString(1, role);
            pst.setString(2, emailOrUsername);
            pst.setString(3, emailOrUsername);
            pst.setString(4, password);

            ResultSet rs = pst.executeQuery();

            if (rs.next()) {
                int userId = rs.getInt("id");
                String username = rs.getString("username");
                String userRole = rs.getString("role");

                // ✅ Create session
                HttpSession session = request.getSession();
                session.setAttribute("user_id", userId);
                session.setAttribute("username", username);
                session.setAttribute("role", userRole);
                session.setMaxInactiveInterval(30 * 60); // 30 minutes

                // ✅ Redirect based on role
                switch (userRole.toLowerCase()) {
                    case "student":
                        response.sendRedirect("student_dashboard.html");
                        break;
                    case "salesperson":
                        response.sendRedirect("salesperson_dashboard.html");
                        break;
                    case "manager":
                        response.sendRedirect("manager_dashboard.html");
                        break;
                    case "admin":
                        response.sendRedirect("admin_dashboard.html");
                        break;
                    default:
                        out.println("<script>alert('Unknown role!');location='login_signup.html';</script>");
                }

            } else {
                out.println("<script>alert('❌ Invalid credentials or role!');location='login_signup.html';</script>");
            }

        } catch (Exception e) {
            e.printStackTrace(out);
            out.println("<script>alert('⚠️ Database error!');location='login_signup.html';</script>");
        }
    }
}
