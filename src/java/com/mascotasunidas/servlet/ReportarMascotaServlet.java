package com.mascotasunidas.servlet;

import com.mascotasunidas.conexion.ConexionBD;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.Collection;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.Part;

@WebServlet("/reportar-mascota")
@MultipartConfig(
    fileSizeThreshold = 1024 * 1024 * 2,  // 2MB
    maxFileSize = 1024 * 1024 * 10,       // 10MB máximo por foto
    maxRequestSize = 1024 * 1024 * 50     // 50MB máximo total
)
public class ReportarMascotaServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        response.setContentType("text/plain");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();
        
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("idUsuario") == null) {
            out.print("Sesión inválida o expirada. Por favor, ingrese nuevamente.");
            return;
        }
        
        int usuarioId = (int) session.getAttribute("idUsuario");
        
        String tipoPublicacion = request.getParameter("tipo_publicacion");
        String nombre = request.getParameter("nombre");
        String especie = request.getParameter("especie");
        String raza = request.getParameter("raza");
        String sexo = request.getParameter("sexo");
        String edad = request.getParameter("edad");
        String color = request.getParameter("color");
        String tamano = request.getParameter("tamano");
        String descripcion = request.getParameter("descripcion");
        String fechaEvento = request.getParameter("fecha_evento");
        String direccion = request.getParameter("direccion");
        String localidadIdStr = request.getParameter("localidad_id");
        String latitud = request.getParameter("latitud");
        String longitud = request.getParameter("longitud");
        
        // --- VALIDACIÓN DE SEGURIDAD PARA LA FECHA EN SERVIDOR ---
        if (fechaEvento != null && !fechaEvento.isEmpty()) {
            try {
                LocalDate fechaEv = LocalDate.parse(fechaEvento);
                LocalDate hoy = LocalDate.now();
                if (fechaEv.isAfter(hoy)) {
                    out.print("La fecha del evento no puede ser posterior a la fecha actual del sistema.");
                    return;
                }
            } catch (Exception e) {
                out.print("Formato de fecha inválido.");
                return;
            }
        }
        
        String[] fotoPrincipalFlags = request.getParameterValues("foto_principal");
        Collection<Part> parts = request.getParts();
        
        boolean tieneArchivos = false;
        for (Part part : parts) {
            if (part.getName().equals("foto_mascota") && part.getSize() > 0) {
                tieneArchivos = true;
                break;
            }
        }
        
        if (!tieneArchivos) {
            out.print("Se requiere al menos una foto de la mascota.");
            return;
        }

        Connection cn = null;
        PreparedStatement psMascota = null;
        PreparedStatement psFoto = null;
        ResultSet rs = null;

        try {
            cn = ConexionBD.conectar();
            if (cn == null) {
                out.print("No se pudo establecer conexión con el motor de base de datos.");
                return;
            }
            
            cn.setAutoCommit(false);
            
            String sqlMascota = "INSERT INTO mascotas (usuario_id, localidad_id, tipo_publicacion, nombre, especie, raza, sexo, edad, color, tamano, descripcion, fecha_evento, direccion, latitud, longitud) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            psMascota = cn.prepareStatement(sqlMascota, Statement.RETURN_GENERATED_KEYS);
            
            psMascota.setInt(1, usuarioId);
            psMascota.setInt(2, Integer.parseInt(localidadIdStr));
            psMascota.setString(3, tipoPublicacion);
            psMascota.setString(4, (nombre == null || nombre.trim().isEmpty()) ? "Sin nombre" : nombre);
            psMascota.setString(5, especie);
            psMascota.setString(6, raza);
            psMascota.setString(7, sexo);
            psMascota.setString(8, edad);
            psMascota.setString(9, color);
            psMascota.setString(10, tamano);
            psMascota.setString(11, descripcion);
            psMascota.setString(12, fechaEvento);
            psMascota.setString(13, direccion);
            
            if (latitud == null || latitud.isEmpty()) psMascota.setNull(14, java.sql.Types.DECIMAL);
            else psMascota.setBigDecimal(14, new java.math.BigDecimal(latitud));
            
            if (longitud == null || longitud.isEmpty()) psMascota.setNull(15, java.sql.Types.DECIMAL);
            else psMascota.setBigDecimal(15, new java.math.BigDecimal(longitud));
            
            psMascota.executeUpdate();
            
            rs = psMascota.getGeneratedKeys();
            int mascotaId = 0;
            if (rs.next()) {
                mascotaId = rs.getInt(1);
            }
            
            String sqlFoto = "INSERT INTO fotos_mascotas (mascota_id, nombre_archivo, extencion_archivo, archivo_blob, foto_principal) VALUES (?, ?, ?, ?, ?)";
            psFoto = cn.prepareStatement(sqlFoto);
            
            int indiceFoto = 0;
            for (Part part : parts) {
                if (part.getName().equals("foto_mascota") && part.getSize() > 0) {
                    
                    String nombreOriginal = part.getSubmittedFileName();
                    String extension = nombreOriginal.substring(nombreOriginal.lastIndexOf(".")).toLowerCase();
                    
                    if (!extension.equals(".jpg") && !extension.equals(".jpeg") && !extension.equals(".png") && !extension.equals(".webp")) {
                        cn.rollback();
                        out.print("Formato inválido detectado en '" + nombreOriginal + "'. Solo se admiten imágenes JPG, JPEG, PNG o WEBP.");
                        return;
                    }
                    
                    String nuevoNombreArchivo = "mascota_" + mascotaId + "_" + System.currentTimeMillis() + "_" + indiceFoto + extension;
                    
                    int esPrincipalValue = 0; 
                    if (fotoPrincipalFlags != null && indiceFoto < fotoPrincipalFlags.length) {
                        if ("1".equals(fotoPrincipalFlags[indiceFoto])) {
                            esPrincipalValue = 1;
                        }
                    }
                    
                    psFoto.setInt(1, mascotaId);
                    psFoto.setString(2, nuevoNombreArchivo);
                    psFoto.setString(3, extension.replace(".", "")); 
                    psFoto.setBinaryStream(4, part.getInputStream(), (int) part.getSize());
                    psFoto.setInt(5, esPrincipalValue);
                    
                    psFoto.executeUpdate();
                    indiceFoto++;
                }
            }
            
            cn.commit();
            out.print("OK");
            
        } catch (Exception e) {
            try { if (cn != null) cn.rollback(); } catch (Exception ex) {}
            e.printStackTrace();
            out.print("Fallo en la persistencia de datos: " + e.getMessage());
        } finally {
            try { if (rs != null) rs.close(); if (psMascota != null) psMascota.close(); if (psFoto != null) psFoto.close(); if (cn != null) cn.close(); } catch(Exception ex) {}
        }
    }
}