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

@WebServlet("/mascota-detalle")
public class MascotaDetalleServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        String idParam = request.getParameter("id");
        String tipoParam = request.getParameter("tipo");

        if (idParam == null || idParam.trim().isEmpty() || tipoParam == null || tipoParam.trim().isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print("{\"error\": \"ID y Tipo de publicación son requeridos.\"}");
            return;
        }
        String sqlMascota = "SELECT m.tipo_publicacion, m.nombre, m.especie, m.raza, m.sexo, m.edad, m.color, "
                          + "m.tamano, m.descripcion, m.fecha_evento, m.direccion, m.latitud, m.longitud, u.telefono, "
                          + "l.nombre provincia, p.nombre localidad, "
                          + "m.localidad_id, l.provincia_id "
                          + "FROM mascotas m, usuarios u, provincias p, localidades l  "
                          + "WHERE m.usuario_id = u.id "
                          + "AND m.estado_publicacion = 'ACTIVA' "
                          + "AND m.localidad_id = l.id "
                          + "AND l.provincia_id = p.id "
                          + "AND m.tipo_publicacion = ? "
                          + "AND m.id = ?";

        try (Connection conn = ConexionBD.conectar()) {
            JsonObjectBuilder mascotaJson = Json.createObjectBuilder();
            boolean existe = false;

            try (PreparedStatement ps = conn.prepareStatement(sqlMascota)) {
                ps.setString(1, tipoParam.toUpperCase());
                ps.setInt(2, Integer.parseInt(idParam));

                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        existe = true;
                        mascotaJson.add("tipo", rs.getString("tipo_publicacion"))
                                   .add("nombre", rs.getString("nombre") != null ? rs.getString("nombre") : "Sin nombre")
                                   .add("especie", rs.getString("especie"))
                                   .add("raza", rs.getString("raza") != null ? rs.getString("raza") : "Mestizo")
                                   .add("sexo", rs.getString("sexo") != null ? rs.getString("sexo") : "No especifica")
                                   .add("edad", rs.getString("edad") != null ? rs.getString("edad") : "No especifica")
                                   .add("color", rs.getString("color") != null ? rs.getString("color") : "No especifica")
                                   .add("tamano", rs.getString("tamano") != null ? rs.getString("tamano") : "No especifica")
                                   .add("descripcion", rs.getString("descripcion") != null ? rs.getString("descripcion") : "Sin descripcion")
                                   .add("fecha_evento", rs.getString("fecha_evento") != null ? rs.getString("fecha_evento") : "")
                                   .add("ciudad", rs.getString("provincia")+" - "+rs.getString("localidad"))
                                   .add("direccion", rs.getString("direccion"))
                                   .add("latitud", rs.getDouble("latitud"))
                                   .add("longitud", rs.getDouble("longitud"))
                                   .add("telefono", rs.getString("telefono") != null ? rs.getString("telefono") : "")
                                   .add("localidad_id", rs.getInt("localidad_id"))
                                   .add("provincia_id", rs.getInt("provincia_id"));
                    }
                }
            }

            if (!existe) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                out.print("{\"error\": \"Mascota no encontrada con esos parámetros.\"}");
                return;
            }

            // Adjuntar las fotos asociadas para el slider (Tu lógica original intacta)
            JsonArrayBuilder fotosArray = Json.createArrayBuilder();
            String sqlFotos = "SELECT id FROM fotos_mascotas WHERE mascota_id = ? ORDER BY foto_principal DESC, id ASC";
            try (PreparedStatement ps = conn.prepareStatement(sqlFotos)) {
                ps.setInt(1, Integer.parseInt(idParam));
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        fotosArray.add(rs.getInt("id"));
                    }
                }
            }
            mascotaJson.add("fotos", fotosArray);

            out.print(mascotaJson.build().toString());

        } catch (SQLException | NumberFormatException e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"error\": \"Error en base de datos: " + e.getMessage() + "\"}");
        }
        out.flush();
    }
}