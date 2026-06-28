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
import java.util.ArrayList;
import java.util.List;

@WebServlet("/encontradas-data")
public class MascotasEncontradasServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();
        
        String ubicacionCompuesta = request.getParameter("ubicacion");
        String raza = request.getParameter("raza");
        String fecha = request.getParameter("fecha");
        String especie = request.getParameter("especie");
        String tamano = request.getParameter("tamano");
        String sexo = request.getParameter("sexo");

        JsonObjectBuilder responseBuilder = Json.createObjectBuilder();

        StringBuilder sql = new StringBuilder(
            "SELECT m.id, m.nombre, m.especie, m.raza, m.tamano, m.fecha_evento, m.direccion, l.nombre AS ciudad, p.nombre AS provincia, u.telefono " +
            "FROM mascotas m, usuarios u, provincias p, localidades l " +
            "WHERE m.usuario_id = u.id " +
            "AND m.estado_publicacion = 'ACTIVA' " +
            "AND m.localidad_id = l.id " +
            "AND l.provincia_id = p.id " +
            "AND m.tipo_publicacion = 'ENCONTRADA' "
        );

        List<Object> parametros = new ArrayList<>();

        if (ubicacionCompuesta != null && !ubicacionCompuesta.trim().isEmpty()) {
            String[] partes = ubicacionCompuesta.split(" - ");
            sql.append(" AND l.nombre = ?");
            parametros.add(partes[0].trim());
        }
        if (raza != null && !raza.trim().isEmpty()) {
            sql.append(" AND m.raza = ?");
            parametros.add(raza);
        }
        if (fecha != null && !fecha.trim().isEmpty()) {
            sql.append(" AND m.fecha_evento = ?");
            parametros.add(fecha);
        }
        if (especie != null && !especie.trim().isEmpty() && !especie.equalsIgnoreCase("TODOS")) {
            sql.append(" AND m.especie = ?");
            parametros.add(especie);
        }
        if (tamano != null && !tamano.trim().isEmpty()) {
            sql.append(" AND m.tamano = ?");
            parametros.add(tamano);
        }
        if (sexo != null && !sexo.trim().isEmpty()) {
            sql.append(" AND m.sexo = ?");
            parametros.add(sexo);
        }

        sql.append(" ORDER BY m.id DESC");

        try (Connection conn = ConexionBD.conectar()) {
            
            JsonArrayBuilder listaEncontradas = Json.createArrayBuilder();
            try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
                for (int i = 0; i < parametros.size(); i++) {
                    ps.setObject(i + 1, parametros.get(i));
                }
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String ubicacionTarjeta = rs.getString("ciudad") + ", " + rs.getString("provincia");
                        listaEncontradas.add(Json.createObjectBuilder()
                            .add("id", rs.getInt("id"))
                            .add("nombre", rs.getString("nombre") != null ? rs.getString("nombre") : "Sin nombre")
                            .add("especie", "Especie: "+rs.getString("especie"))
                            .add("raza", rs.getString("raza") != null ? rs.getString("raza") : "Mestizo")
                            .add("tamano", rs.getString("tamano") != null ? rs.getString("tamano") : "No especifica")
                            .add("ubicacion", ubicacionTarjeta)
                            .add("direccion", rs.getString("direccion") != null ? rs.getString("direccion") : "")
                            .add("fecha", rs.getString("fecha_evento") != null ? rs.getString("fecha_evento") : "")
                            .add("telefono", rs.getString("telefono") != null ? rs.getString("telefono") : "No disponible")
                        );
                    }
                }
            }
            responseBuilder.add("mascotas", listaEncontradas);

            // Obtener Ubicaciones Combinadas
            JsonArrayBuilder listaUbicaciones = Json.createArrayBuilder();
            String sqlUbicaciones = "SELECT p.nombre AS provincia, l.nombre AS ciudad FROM provincias p, localidades l WHERE l.provincia_id = p.id ORDER BY l.nombre ASC";
            try (PreparedStatement ps = conn.prepareStatement(sqlUbicaciones); ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    listaUbicaciones.add(rs.getString("ciudad") + " - " + rs.getString("provincia"));
                }
            }
            responseBuilder.add("ubicaciones", listaUbicaciones);

            // Razas Dinámicas Encontradas
            JsonArrayBuilder listaRazas = Json.createArrayBuilder();
            String sqlRazas = "SELECT DISTINCT raza FROM mascotas WHERE tipo_publicacion = 'ENCONTRADA' AND estado_publicacion = 'ACTIVA' AND raza IS NOT NULL";
            try (PreparedStatement ps = conn.prepareStatement(sqlRazas); ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    listaRazas.add(rs.getString("raza"));
                }
            }
            responseBuilder.add("razas", listaRazas);

            out.print(responseBuilder.build().toString());

        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"error\": \"Error al procesar: " + e.getMessage() + "\"}");
        }
        out.flush();
    }
}