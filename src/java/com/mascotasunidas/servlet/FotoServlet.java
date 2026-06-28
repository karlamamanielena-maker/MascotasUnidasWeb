package com.mascotasunidas.servlet;

import com.mascotasunidas.conexion.ConexionBD;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

@WebServlet("/ver-foto")
public class FotoServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        String idFoto = request.getParameter("foto_id"); 
        String idMascota = request.getParameter("id");    

        String sql = "";
        int queryId = 0;

        try {
            if (idFoto != null) {
                sql = "SELECT archivo_blob FROM fotos_mascotas WHERE id = ?";
                queryId = Integer.parseInt(idFoto);
            } else if (idMascota != null) {
                sql = "SELECT archivo_blob FROM fotos_mascotas WHERE mascota_id = ? ORDER BY foto_principal DESC LIMIT 1";
                queryId = Integer.parseInt(idMascota);
            } else {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }

            try (Connection conn = ConexionBD.conectar();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                
                ps.setInt(1, queryId);
                
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        byte[] foto = rs.getBytes("archivo_blob");
                        if (foto != null) {
                            response.setContentType("image/jpeg");
                            try (OutputStream out = response.getOutputStream()) {
                                out.write(foto);
                            }
                            return;
                        }
                    }
                    response.sendRedirect(request.getContextPath() + "/images/sin-foto.jpg");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.sendRedirect(request.getContextPath() + "/images/sin-foto.jpg");
        }
    }
}