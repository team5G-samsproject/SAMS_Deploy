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

@WebServlet("/AdminServlet")
public class AdminServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        response.setContentType("application/json");
        String action = request.getParameter("action");
        JSONObject json = new JSONObject();

        try (Connection con = DBConnection.getConnection();
             PrintWriter out = response.getWriter()) {

            if (con == null) {
                json.put("error", "Database connection failed!");
                out.print(json.toString());
                return;
            }

            // ✅ 1. Dashboard Statistics (for Reports)
            if ("getStats".equals(action)) {
                Statement st = con.createStatement();

                // Total revenue and bookings
                ResultSet rs1 = st.executeQuery(
                    "SELECT IFNULL(SUM(amount),0) AS total_revenue, COUNT(*) AS total_bookings FROM bookings"
                        
                        
                );
                if (rs1.next()) {
                    json.put("revenue", rs1.getDouble("total_revenue"));
                    json.put("bookings", rs1.getInt("total_bookings"));
                }

                // Approved and Pending events (based on 'Active' status)
                ResultSet rs2 = st.executeQuery(
                    "SELECT " +
                    "SUM(LOWER(status)='active') AS approved, " +
                    "SUM(LOWER(status)!='active') AS pending " +
                    "FROM events"
                );
                if (rs2.next()) {
                    json.put("approved", rs2.getInt("approved"));
                    json.put("pending", rs2.getInt("pending"));
                }

                out.print(json.toString());
            }

            // ✅ 2. Get all administrators (Managers, Salespersons, Admin)
            else if ("getAdmins".equals(action)) {
                JSONArray arr = new JSONArray();
                PreparedStatement ps = con.prepareStatement("SELECT * FROM administrators ORDER BY created_at DESC");
                ResultSet rs = ps.executeQuery();

                while (rs.next()) {
                    JSONObject o = new JSONObject();
                    o.put("admin_id", rs.getInt("admin_id"));
                    o.put("name", rs.getString("name"));
                    o.put("email", rs.getString("email"));
                    o.put("role", rs.getString("role"));
                    o.put("created_at", rs.getString("created_at"));
                    arr.put(o);
                }
                out.print(arr.toString());
            }

            // ✅ 3. Delete an administrator
            else if ("deleteAdmin".equals(action)) {
                int id = Integer.parseInt(request.getParameter("id"));
                PreparedStatement ps = con.prepareStatement("DELETE FROM administrators WHERE admin_id=?");
                ps.setInt(1, id);
                int rows = ps.executeUpdate();
                json.put("success", rows > 0);
                out.print(json.toString());
            }

            else {
                json.put("error", "Invalid action!");
                out.print(json.toString());
            }

        } catch (Exception e) {
            e.printStackTrace();
            response.getWriter().print("{\"error\":\"" + e.getMessage() + "\"}");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        response.setContentType("application/json");
        String action = request.getParameter("action");
        JSONObject json = new JSONObject();

        try (Connection con = DBConnection.getConnection();
             PrintWriter out = response.getWriter()) {

            if (con == null) {
                json.put("error", "Database connection failed!");
                out.print(json.toString());
                return;
            }

            // ✅ 4. Add a new administrator (Manager or Salesperson)
            if ("addAdmin".equals(action)) {
                String name = request.getParameter("name");
                String email = request.getParameter("email");
                String password = request.getParameter("password");
                String role = request.getParameter("role");

                // Check for duplicate email
                PreparedStatement check = con.prepareStatement("SELECT email FROM administrators WHERE email=?");
                check.setString(1, email);
                ResultSet rs = check.executeQuery();
                if (rs.next()) {
                    json.put("success", false);
                    json.put("error", "Email already exists!");
                    out.print(json.toString());
                    return;
                }

                PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO administrators (name, email, password, role) VALUES (?, ?, ?, ?)"
                );
                ps.setString(1, name);
                ps.setString(2, email);
                ps.setString(3, password);
                ps.setString(4, role);

                int rows = ps.executeUpdate();
                json.put("success", rows > 0);
                out.print(json.toString());
            } else {
                json.put("error", "Invalid POST action!");
                out.print(json.toString());
            }

        } catch (Exception e) {
            e.printStackTrace();
            response.getWriter().print("{\"error\":\"" + e.getMessage() + "\"}");
        }
    }
}
