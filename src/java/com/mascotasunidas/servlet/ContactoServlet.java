package com.mascotasunidas.servlet;

import com.mascotasunidas.conexion.ConexionBD;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;

@WebServlet("/contacto-enviar")
public class ContactoServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        response.setContentType("text/plain");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();
        
        // Captura de datos enviados desde el formulario
        String nombreCompleto = request.getParameter("nombre");
        String email = request.getParameter("email");
        String asunto = request.getParameter("asunto");
        String telefono = request.getParameter("telefono");
        String mensaje = request.getParameter("mensaje");

        // Validación básica en servidor
        if (nombreCompleto == null || nombreCompleto.trim().isEmpty() ||
            email == null || email.trim().isEmpty() ||
            asunto == null || asunto.trim().isEmpty() ||
            mensaje == null || mensaje.trim().isEmpty()) {
            
            out.print("Faltan completar campos obligatorios.");
            return;
        }

        String sqlInsert = "INSERT INTO mensajes_contacto (nombre_completo, email, asunto, telefono, mensaje) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = ConexionBD.conectar();
             PreparedStatement ps = conn.prepareStatement(sqlInsert)) {
            
            ps.setString(1, nombreCompleto.trim());
            ps.setString(2, email.trim());
            ps.setString(3, asunto.trim());
            
            if (telefono == null || telefono.trim().isEmpty()) {
                ps.setNull(4, java.sql.Types.VARCHAR);
            } else {
                ps.setString(4, telefono.trim());
            }
            
            ps.setString(5, mensaje.trim());
            
            int filasInsertadas = ps.executeUpdate();
            if (filasInsertadas > 0) {
                out.print("OK");
            } else {
                out.print("No se pudo registrar el mensaje en el sistema.");
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            out.print("Error en base de datos: " + e.getMessage());
        }
        out.flush();
    }
}