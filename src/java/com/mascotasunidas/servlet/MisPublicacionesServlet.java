package com.mascotasunidas.servlet;

import com.mascotasunidas.conexion.ConexionBD;
import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObjectBuilder;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

@WebServlet("/mis-publicaciones-data")
public class MisPublicacionesServlet extends HttpServlet {

    // 1. OBTENER PUBLICACIONES DEL USUARIO LOGUEADO (Tu lógica original intacta)
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();
        
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("idUsuario") == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            out.print("{\"error\": \"No autorizado. Inicie sesión.\"}");
            return;
        }

        int usuarioId = (int) session.getAttribute("idUsuario");
        JsonObjectBuilder responseBuilder = Json.createObjectBuilder();
        JsonArrayBuilder lista = Json.createArrayBuilder();

        String sql = "SELECT id, nombre, especie, raza, tipo_publicacion, estado_publicacion, fecha_evento " +
                     "FROM mascotas WHERE usuario_id = ? AND estado_publicacion = 'ACTIVA' ORDER BY id DESC";

        try (Connection conn = ConexionBD.conectar();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setInt(1, usuarioId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(Json.createObjectBuilder()
                        .add("id", rs.getInt("id"))
                        .add("nombre", rs.getString("nombre") != null ? rs.getString("nombre") : "Sin nombre")
                        .add("especie", rs.getString("especie"))
                        .add("raza", rs.getString("raza") != null ? rs.getString("raza") : "Mestizo")
                        .add("tipo", rs.getString("tipo_publicacion"))
                        .add("estado", rs.getString("estado_publicacion"))
                        .add("fecha", rs.getString("fecha_evento") != null ? rs.getString("fecha_evento") : "")
                    );
                }
            }
            responseBuilder.add("publicaciones", lista);
            out.print(responseBuilder.build().toString());

        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"error\": \"Error en el servidor: " + e.getMessage() + "\"}");
        }
        out.flush();
    }

    // 2. PROCESAR CAMBIOS DE ESTADO DINÁMICOS (RESUELTA O CANCELADA)
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();
        
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("idUsuario") == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            out.print("{\"success\": false, \"error\": \"Sesión expirada.\"}");
            return;
        }

        String idMascotaStr = request.getParameter("id");
        String nuevoEstado = request.getParameter("nuevo_estado"); // Captura 'RESUELTA' o 'CANCELADA'
        int usuarioId = (int) session.getAttribute("idUsuario");

        if (idMascotaStr == null || idMascotaStr.trim().isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print("{\"success\": false, \"error\": \"ID inválido.\"}");
            return;
        }

        // Si por alguna razón el estado viene vacío de respaldo asignamos CANCELADA
        if (nuevoEstado == null || nuevoEstado.trim().isEmpty()) {
            nuevoEstado = "CANCELADA";
        }

        // Filtramos por m.id y m.usuario_id para que nadie pueda alterar registros ajenos
        String sqlUpdate = "UPDATE mascotas SET estado_publicacion = ? WHERE id = ? AND usuario_id = ?";

        try (Connection conn = ConexionBD.conectar();
             PreparedStatement ps = conn.prepareStatement(sqlUpdate)) {
            
            ps.setString(1, nuevoEstado.toUpperCase());
            ps.setInt(2, Integer.parseInt(idMascotaStr));
            ps.setInt(3, usuarioId);
            
            int filasAfectadas = ps.executeUpdate();
            if (filasAfectadas > 0) {
                out.print("{\"success\": true}");
            } else {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                out.print("{\"success\": false, \"error\": \"No tienes permisos para modificar este reporte.\"}");
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"success\": false, \"error\": \"Error en la base de datos: " + e.getMessage() + "\"}");
        }
        out.flush();
    }
}