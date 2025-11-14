package com.myproject.servlet;

import com.myproject.db.DBConnection;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.sql.*;

@WebServlet("/SignupServlet")
public class SignupServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("text/html;charset=UTF-8");

        // Get form data
        String role = request.getParameter("signup-role");
        String username = request.getParameter("username");
        String email = request.getParameter("signup-email");
        String password = request.getParameter("signup-password");

        // ✅ Only students can sign up
        if (!"student".equalsIgnoreCase(role)) {
            response.sendRedirect("login_signup.html?error=invalidrole");
            return;
        }

        // ✅ Basic validation
        if (username == null || email == null || password == null ||
                username.isEmpty() || email.isEmpty() || password.isEmpty()) {
            response.sendRedirect("login_signup.html?error=emptyfields");
            return;
        }

        try (Connection con = DBConnection.getConnection()) {
            if (con == null) {
                response.sendRedirect("login_signup.html?error=dbconnection");
                return;
            }

            // ✅ Check for existing user
            String checkSql = "SELECT * FROM users WHERE email=? OR username=?";
            PreparedStatement checkStmt = con.prepareStatement(checkSql);
            checkStmt.setString(1, email);
            checkStmt.setString(2, username);
            ResultSet rs = checkStmt.executeQuery();

            if (rs.next()) {
                // User already exists
                response.sendRedirect("login_signup.html?error=userexists");
                return;
            }

            // ✅ Insert new student into users table
            String insertSql = "INSERT INTO users (role, username, email, password) VALUES (?, ?, ?, ?)";
            PreparedStatement insertStmt = con.prepareStatement(insertSql);
            insertStmt.setString(1, "student");
            insertStmt.setString(2, username);
            insertStmt.setString(3, email);
            insertStmt.setString(4, password);  // (you can later hash password for better security)

            int rows = insertStmt.executeUpdate();

            if (rows > 0) {
                response.sendRedirect("login_signup.html?signup=success");
            } else {
                response.sendRedirect("login_signup.html?signup=failure");
            }

        } catch (Exception e) {
            e.printStackTrace();
            response.sendRedirect("login_signup.html?signup=error");
        }
    }
}
