package com.mascotasunidas.servlet;

import com.mascotasunidas.conexion.ConexionBD;
import jakarta.json.*;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.sql.*;

@WebServlet("/api/mascotas-mapa")
public class ObtenerMascotasMapaServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        JsonArrayBuilder mapaBuilder = Json.createArrayBuilder();
        String sql = "SELECT id, latitud, longitud, tipo_publicacion, nombre FROM mascotas WHERE estado_publicacion = 'ACTIVA' AND latitud IS NOT NULL AND longitud IS NOT NULL";

        try (Connection conn = ConexionBD.conectar();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                mapaBuilder.add(Json.createObjectBuilder()
                    .add("id", rs.getInt("id"))
                    .add("lat", rs.getDouble("latitud"))
                    .add("lng", rs.getDouble("longitud"))
                    .add("tipo", rs.getString("tipo_publicacion"))
                    .add("nombre", rs.getString("nombre") != null ? rs.getString("nombre") : "Mascota"));
            }
            response.getWriter().write(mapaBuilder.build().toString());

        } catch (SQLException e) {
            response.setStatus(500);
            response.getWriter().write("{\"error\": \"Error de BD\"}");
        }
    }
}