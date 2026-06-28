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

@WebServlet("/perdidas-data")
public class MascotasPerdidasServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();
        
        // Capturar parámetros del frontend
        String ubicacionCompuesta = request.getParameter("ubicacion"); // "Ciudad - Provincia"
        String raza = request.getParameter("raza");
        String fecha = request.getParameter("fecha");
        String especie = request.getParameter("especie");
        String tamano = request.getParameter("tamano");
        String sexo = request.getParameter("sexo");

        JsonObjectBuilder responseBuilder = Json.createObjectBuilder();

        // Query principal adaptada para el listado general usando tus JOINs establecidos
        StringBuilder sql = new StringBuilder(
            "SELECT m.id, m.nombre, m.especie, m.raza, m.tamano, m.fecha_evento, l.nombre AS ciudad, p.nombre AS provincia, u.telefono " +
            "FROM mascotas m, usuarios u, provincias p, localidades l " +
            "WHERE m.usuario_id = u.id " +
            "AND m.estado_publicacion = 'ACTIVA' " +
            "AND m.localidad_id = l.id " +
            "AND l.provincia_id = p.id " +
            "AND m.tipo_publicacion = 'PERDIDA' "
        );

        List<Object> parametros = new ArrayList<>();

        // Filtros dinámicos
        if (ubicacionCompuesta != null && !ubicacionCompuesta.trim().isEmpty()) {
            // Separamos por el guion "Ciudad - Provincia" para buscar la localidad exacta
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
            
            // 1. Ejecutar consulta de mascotas perdidas
            JsonArrayBuilder listaPerdidas = Json.createArrayBuilder();
            try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
                for (int i = 0; i < parametros.size(); i++) {
                    ps.setObject(i + 1, parametros.get(i));
                }
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String ubicacionTarjeta = rs.getString("ciudad") + ", " + rs.getString("provincia");
                        listaPerdidas.add(Json.createObjectBuilder()
                            .add("id", rs.getInt("id"))
                            .add("nombre", rs.getString("nombre") != null ? rs.getString("nombre") : "Sin nombre")
                            .add("especie", rs.getString("especie"))
                            .add("raza", rs.getString("raza") != null ? rs.getString("raza") : "Mestizo")
                            .add("tamano", rs.getString("tamano") != null ? rs.getString("tamano") : "No especifica")
                            .add("ubicacion", ubicacionTarjeta)
                            .add("fecha", rs.getString("fecha_evento") != null ? rs.getString("fecha_evento") : "")
                            .add("telefono", rs.getString("telefono") != null ? rs.getString("telefono") : "No disponible")
                        );
                    }
                }
            }
            responseBuilder.add("mascotas", listaPerdidas);

            // 2. Obtener combinaciones dinámicas desde tu Query de Ubicación
            JsonArrayBuilder listaUbicaciones = Json.createArrayBuilder();
            String sqlUbicaciones = "SELECT p.nombre AS provincia, l.nombre AS ciudad FROM provincias p, localidades l WHERE l.provincia_id = p.id ORDER BY l.nombre ASC";
            try (PreparedStatement ps = conn.prepareStatement(sqlUbicaciones); ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String compuesto = rs.getString("ciudad") + " - " + rs.getString("provincia");
                    listaUbicaciones.add(compuesto);
                }
            }
            responseBuilder.add("ubicaciones", listaUbicaciones);

            // 3. Razas dinámicas existentes para el buscador
            JsonArrayBuilder listaRazas = Json.createArrayBuilder();
            String sqlRazas = "SELECT DISTINCT raza FROM mascotas WHERE tipo_publicacion = 'PERDIDA' AND estado_publicacion = 'ACTIVA' AND raza IS NOT NULL";
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
            out.print("{\"error\": \"Error en los JOINs: " + e.getMessage() + "\"}");
        }
        out.flush();
    }
}