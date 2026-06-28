package com.mascotasunidas.servlet;

import com.mascotasunidas.conexion.ConexionBD;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet("/ubicacion")
public class UbicacionServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();
        
        String accion = request.getParameter("accion");
        Connection cn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            cn = ConexionBD.conectar();
            if (cn == null) {
                out.print("[]");
                return;
            }

            // CASO A: Pedir todas las provincias
            if ("provincias".equals(accion)) {
                String sql = "SELECT id, nombre FROM provincias ORDER BY nombre ASC";
                ps = cn.prepareStatement(sql);
                rs = ps.executeQuery();
                
                StringBuilder json = new StringBuilder("[");
                while (rs.next()) {
                    json.append(String.format("{\"id\": %d, \"nombre\": \"%s\"},", 
                            rs.getInt("id"), rs.getString("nombre")));
                }
                if (json.length() > 1) json.setLength(json.length() - 1); // Quitar última coma
                json.append("]");
                
                out.print(json.toString());
            } 
            
            // CASO B: Pedir localidades filtradas por provincia_id
            else if ("localidades".equals(accion)) {
                String provinciaId = request.getParameter("provincia_id");
                String sql = "SELECT id, nombre FROM localidades WHERE provincia_id = ? ORDER BY nombre ASC";
                ps = cn.prepareStatement(sql);
                ps.setString(1, provinciaId);
                rs = ps.executeQuery();
                
                StringBuilder json = new StringBuilder("[");
                while (rs.next()) {
                    json.append(String.format("{\"id\": %d, \"nombre\": \"%s\"},", 
                            rs.getInt("id"), rs.getString("nombre")));
                }
                if (json.length() > 1) json.setLength(json.length() - 1);
                json.append("]");
                
                out.print(json.toString());
            }

        } catch (Exception e) {
            e.printStackTrace();
            out.print("[]");
        } finally {
            try { if (rs != null) rs.close(); if (ps != null) ps.close(); if (cn != null) cn.close(); } catch(Exception ex) {}
        }
    }
}