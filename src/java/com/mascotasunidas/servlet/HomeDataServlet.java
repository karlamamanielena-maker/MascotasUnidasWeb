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
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@WebServlet("/home-data")
public class HomeDataServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();
        
        JsonObjectBuilder responseBuilder = Json.createObjectBuilder();

        // Usamos tu clase centralizada de conexión
        try (Connection conn = ConexionBD.conectar()) {
            if (conn == null) {
                throw new SQLException("No se pudo establecer la conexión a la base de datos.");
            }

            // 1. CONTEO: Mascotas Recuperadas
            String sqlRecuperadas = "SELECT COUNT(*) FROM mascotas WHERE estado_publicacion = 'RESUELTA'";
            try (PreparedStatement ps = conn.prepareStatement(sqlRecuperadas);
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) responseBuilder.add("totalRecuperadas", rs.getInt(1));
            }

            // 2. CONTEO: Mascotas Perdidas Activas
            String sqlPerdidas = "SELECT COUNT(*) FROM mascotas WHERE tipo_publicacion = 'PERDIDA' AND estado_publicacion = 'ACTIVA'";
            try (PreparedStatement ps = conn.prepareStatement(sqlPerdidas);
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) responseBuilder.add("totalPerdidas", rs.getInt(1));
            }

            // 3. CONTEO: Mascotas Encontradas Activas
            String sqlEncontradas = "SELECT COUNT(*) FROM mascotas WHERE tipo_publicacion = 'ENCONTRADA' AND estado_publicacion = 'ACTIVA'";
            try (PreparedStatement ps = conn.prepareStatement(sqlEncontradas);
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) responseBuilder.add("totalEncontradas", rs.getInt(1));
            }

            // 4. CONTEO: Total de Usuarios
            String sqlUsuarios = "SELECT COUNT(*) FROM usuarios";
            try (PreparedStatement ps = conn.prepareStatement(sqlUsuarios);
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) responseBuilder.add("totalUsuarios", rs.getInt(1));
            }

            // 5. LISTA: 3 últimas mascotas (solo las 'ACTIVA')
            JsonArrayBuilder recientesBuilder = Json.createArrayBuilder();
            String sqlRecientes = "SELECT id, nombre, especie, raza, direccion FROM mascotas WHERE estado_publicacion = 'ACTIVA' and tipo_publicacion = 'PERDIDA' ORDER BY id DESC LIMIT 3";
            try (PreparedStatement ps = conn.prepareStatement(sqlRecientes);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    JsonObjectBuilder mascotaBuilder = Json.createObjectBuilder();
                    mascotaBuilder.add("id", rs.getInt("id"))
                                  .add("nombre", rs.getString("nombre") != null ? rs.getString("nombre") : "")
                                  .add("especie", rs.getString("especie"))
                                  .add("raza", rs.getString("raza") != null ? rs.getString("raza") : "Mestizo")
                                  .add("direccion", rs.getString("direccion"));
                    recientesBuilder.add(mascotaBuilder);
                }
            }
            responseBuilder.add("recientes", recientesBuilder);

            // 6. LISTA: Mapa (solo las 'ACTIVA')
            JsonArrayBuilder mapaBuilder = Json.createArrayBuilder();
            String sqlMapa = "SELECT id, latitud, longitud, tipo_publicacion, nombre, especie FROM mascotas WHERE estado_publicacion = 'ACTIVA' AND latitud IS NOT NULL AND longitud IS NOT NULL";
            try (PreparedStatement ps = conn.prepareStatement(sqlMapa);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    JsonObjectBuilder puntoBuilder = Json.createObjectBuilder();
                    puntoBuilder.add("id", rs.getInt("id"))
                                .add("latitud", rs.getDouble("latitud"))
                                .add("longitud", rs.getDouble("longitud"))
                                .add("tipo", rs.getString("tipo_publicacion"))
                                .add("nombre", rs.getString("nombre") != null ? rs.getString("nombre") : "")
                                .add("especie", rs.getString("especie"));
                    mapaBuilder.add(puntoBuilder);
                }
            }
            responseBuilder.add("mapaReportes", mapaBuilder);

            // Construir y enviar respuesta
            out.print(responseBuilder.build().toString());

        } catch (SQLException e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"error\": \"Error al procesar los datos de inicio\"}");
        }
        out.flush();
    }
}