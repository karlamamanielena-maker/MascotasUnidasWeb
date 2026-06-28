package com.mascotasunidas.servlet;

import com.mascotasunidas.conexion.ConexionBD;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.Part;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Collection;

@WebServlet("/editar-publicacion-data")
@MultipartConfig(
    fileSizeThreshold = 1024 * 1024 * 2,  // 2MB
    maxFileSize = 1024 * 1024 * 10,       // 10MB máximo por foto
    maxRequestSize = 1024 * 1024 * 50     // 50MB máximo total
)
public class EditarPublicacionServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        response.setContentType("text/plain");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();
        
        // 1. VERIFICACIÓN DE SEGURIDAD DE SESIÓN
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("idUsuario") == null) {
            out.print("Sesión inválida o expirada. Por favor, reingrese al sistema.");
            return;
        }
        
        int usuarioId = (int) session.getAttribute("idUsuario");
        
        // 2. CAPTURA DE PARÁMETROS FORMULARIO
        String mascotaIdStr = request.getParameter("mascota_id");
        String nombre = request.getParameter("nombre");
        String especie = request.getParameter("especie");
        String raza = request.getParameter("raza");
        String sexo = request.getParameter("sexo");
        String edad = request.getParameter("edad");
        String color = request.getParameter("color");
        String tamano = request.getParameter("tamano");
        String fechaEvento = request.getParameter("fecha_evento");
        String direccion = request.getParameter("direccion");
        String localidadIdStr = request.getParameter("localidad_id");
        String latitud = request.getParameter("latitud");
        String longitud = request.getParameter("longitud");
        String descripcion = request.getParameter("descripcion");
        
        // Parámetro de control para las fotos viejas que se descartaron en el Front
        String fotosAEliminarStr = request.getParameter("fotos_a_eliminar");

        if (mascotaIdStr == null || mascotaIdStr.trim().isEmpty()) {
            out.print("Error: ID de mascota ausente.");
            return;
        }
        
        int mascotaId = Integer.parseInt(mascotaIdStr);

        Connection cn = null;
        PreparedStatement psMascota = null;
        PreparedStatement psEliminarFotos = null;
        PreparedStatement psFotoNueva = null;

        try {
            cn = ConexionBD.conectar();
            cn.setAutoCommit(false); // Iniciamos transacción atómica de protección

            // 3. CONSULTA SQL DE ACTUALIZACIÓN (Filtrando por usuario_id por estricta seguridad)
            String sqlUpdateMascota = "UPDATE mascotas SET localidad_id = ?, nombre = ?, especie = ?, raza = ?, "
                                    + "sexo = ?, edad = ?, color = ?, tamano = ?, descripcion = ?, fecha_evento = ?, "
                                    + "direccion = ?, latitud = ?, longitud = ?, fecha_modificacion = CURRENT_TIMESTAMP "
                                    + "WHERE id = ? AND usuario_id = ?";
            
            psMascota = cn.prepareStatement(sqlUpdateMascota);
            psMascota.setInt(1, Integer.parseInt(localidadIdStr));
            psMascota.setString(2, (nombre == null || nombre.trim().isEmpty()) ? "Sin nombre" : nombre);
            psMascota.setString(3, especie);
            psMascota.setString(4, raza);
            psMascota.setString(5, sexo);
            psMascota.setString(6, edad);
            psMascota.setString(7, color);
            psMascota.setString(8, tamano);
            psMascota.setString(9, descripcion);
            psMascota.setString(10, fechaEvento);
            psMascota.setString(11, direccion);
            
            if (latitud == null || latitud.isEmpty()) psMascota.setNull(12, java.sql.Types.DECIMAL);
            else psMascota.setBigDecimal(12, new java.math.BigDecimal(latitud));
            
            if (longitud == null || longitud.isEmpty()) psMascota.setNull(13, java.sql.Types.DECIMAL);
            else psMascota.setBigDecimal(13, new java.math.BigDecimal(longitud));
            
            psMascota.setInt(14, mascotaId);
            psMascota.setInt(15, usuarioId);
            
            int filasAfectadas = psMascota.executeUpdate();
            
            if (filasAfectadas == 0) {
                cn.rollback();
                out.print("No tienes autorización para modificar este reporte o el registro no existe.");
                return;
            }

            // 4. PROCESAR ELIMINACIÓN DE FOTOS DESCARTADAS (Si hay IDs válidos)
            if (fotosAEliminarStr != null && !fotosAEliminarStr.trim().isEmpty()) {
                // Sanitizamos los IDs para prevenir inyecciones y creamos el query dinámico seguro
                String[] idsUnicos = fotosAEliminarStr.split(",");
                StringBuilder sb = new StringBuilder("DELETE FROM fotos_mascotas WHERE mascota_id = ? AND id IN (");
                for (int i = 0; i < idsUnicos.length; i++) {
                    sb.append(i == 0 ? "?" : ",?");
                }
                sb.append(")");

                psEliminarFotos = cn.prepareStatement(sb.toString());
                psEliminarFotos.setInt(1, mascotaId);
                for (int i = 0; i < idsUnicos.length; i++) {
                    psEliminarFotos.setInt(i + 2, Integer.parseInt(idsUnicos[i].trim()));
                }
                psEliminarFotos.executeUpdate();
            }

            // 5. PROCESAR SUBIDA DE IMÁGENES NUEVAS (Reutilizando tu lógica exacta de reportar)
            Collection<Part> parts = request.getParts();
            String sqlInsFoto = "INSERT INTO fotos_mascotas (mascota_id, nombre_archivo, extencion_archivo, archivo_blob, foto_principal) VALUES (?, ?, ?, ?, 0)";
            psFotoNueva = cn.prepareStatement(sqlInsFoto);
            
            int indiceFoto = 0;
            for (Part part : parts) {
                if (part.getName().equals("foto_mascota_nueva") && part.getSize() > 0) {
                    String nombreOriginal = part.getSubmittedFileName();
                    String extension = nombreOriginal.substring(nombreOriginal.lastIndexOf(".")).toLowerCase();
                    
                    if (!extension.equals(".jpg") && !extension.equals(".jpeg") && !extension.equals(".png") && !extension.equals(".webp")) {
                        cn.rollback();
                        out.print("Formato inválido en '" + nombreOriginal + "'. Solo imágenes JPG, JPEG, PNG o WEBP.");
                        return;
                    }
                    
                    String nuevoNombreArchivo = "mascota_" + mascotaId + "_" + System.currentTimeMillis() + "_new_" + indiceFoto + extension;
                    
                    psFotoNueva.setInt(1, mascotaId);
                    psFotoNueva.setString(2, nuevoNombreArchivo);
                    psFotoNueva.setString(3, extension.replace(".", "")); 
                    psFotoNueva.setBinaryStream(4, part.getInputStream(), (int) part.getSize());
                    
                    psFotoNueva.executeUpdate();
                    indiceFoto++;
                }
            }
            
            // Confirmamos de forma definitiva todos los cambios en cascada de la base de datos
            cn.commit();
            out.print("OK");
            
        } catch (Exception e) {
            try { if (cn != null) cn.rollback(); } catch (Exception ex) {}
            e.printStackTrace();
            out.print("Fallo en la persistencia de cambios: " + e.getMessage());
        } finally {
            try { 
                if (psMascota != null) psMascota.close(); 
                if (psEliminarFotos != null) psEliminarFotos.close(); 
                if (psFotoNueva != null) psFotoNueva.close(); 
                if (cn != null) cn.close(); 
            } catch(Exception ex) {}
        }
    }
}