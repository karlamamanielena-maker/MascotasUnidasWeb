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
import java.sql.ResultSet;
import java.util.Properties;
import java.util.Random;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.mindrot.jbcrypt.BCrypt;

@WebServlet("/recuperar-password")
public class RecuperarPasswordServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        response.setContentType("text/plain");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();
        
        String email = request.getParameter("email");

        if (email == null || email.trim().isEmpty()) {
            out.print("El correo electrónico es obligatorio.");
            return;
        }

        String sqlCheck = "SELECT id, nombre FROM usuarios WHERE email = ? AND estado = 1";
        String sqlUpdate = "UPDATE usuarios SET password_hash = ?, fecha_modificacion = CURRENT_TIMESTAMP WHERE id = ?";

        try (Connection conn = ConexionBD.conectar()) {
            if (conn == null) {
                out.print("Error de conexión con la base de datos.");
                return;
            }

            int usuarioId = 0;
            String nombreUsuario = "";

            // 1. Verificar si el usuario existe
            try (PreparedStatement psCheck = conn.prepareStatement(sqlCheck)) {
                psCheck.setString(1, email.trim());
                try (ResultSet rs = psCheck.executeQuery()) {
                    if (rs.next()) {
                        usuarioId = rs.getInt("id");
                        nombreUsuario = rs.getString("nombre");
                    }
                }
            }

            if (usuarioId == 0) {
                out.print("El correo electrónico ingresado no pertenece a ningún usuario activo.");
                return;
            }

            // 2. Generar clave alfanumérica provisoria de 8 caracteres
            String caracteres = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
            StringBuilder sb = new StringBuilder();
            Random rnd = new Random();
            while (sb.length() < 8) { 
                int index = (int) (rnd.nextFloat() * caracteres.length());
                sb.append(caracteres.charAt(index));
            }
            String passwordProvisorio = sb.toString();

            // 3. Hashear la nueva contraseña con BCrypt
            String nuevoHash = BCrypt.hashpw(passwordProvisorio, BCrypt.gensalt());

            // 4. Actualizar la base de datos
            try (PreparedStatement psUpdate = conn.prepareStatement(sqlUpdate)) {
                psUpdate.setString(1, nuevoHash);
                psUpdate.setInt(2, usuarioId);
                
                int rows = psUpdate.executeUpdate();
                if (rows > 0) {
                    // 5. ENVIAR EL CORREO ELECTRÓNICO (Llamada al método seguro)
                    boolean correoEnviado = enviarCorreoProvisorio(email.trim(), nombreUsuario, passwordProvisorio);
                    
                    if (correoEnviado) {
                        out.print("OK"); // Solo responde OK, protegiendo la clave
                    } else {
                        out.print("La contraseña se restableció en el sistema, pero ocurrió un fallo al enviar el email.");
                    }
                } else {
                    out.print("No se pudo actualizar las credenciales del usuario.");
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            out.print("Error en el servidor: " + e.getMessage());
        }
        out.flush();
    }

    /**
     * Método interno para despachar el correo electrónico usando Jakarta Mail
     */
    private boolean enviarCorreoProvisorio(String destinatario, String nombre, String claveProvisoria) {
        // Datos de configuración del servidor SMTP (Ejemplo usando Gmail)
        final String usuarioSMTP = "contacto@mascotasunidas.com"; // <-- Cambia por tu email de Mascotas Unidas
        final String passwordSMTP = "tu-clave-de-aplicacion"; // <-- Cambia por tu clave de aplicación de Gmail
        
        Properties prop = new Properties();
        prop.put("mail.smtp.host", "smtp.gmail.com");
        prop.put("mail.smtp.port", "587");
        prop.put("mail.smtp.auth", "true");
        prop.put("mail.smtp.starttls.enable", "true"); // TLS obligatorio por seguridad
        
        Session session = Session.getInstance(prop, new jakarta.mail.Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(usuarioSMTP, passwordSMTP);
            }
        });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(usuarioSMTP, "Mascotas Unidas"));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(destinatario));
            message.setSubject("Recuperación de Contraseña - Mascotas Unidas");
            
            // Cuerpo del correo con formato HTML estético y amigable
            String cuerpoHTML = "<h2>Hola, " + nombre + "!</h2>"
                    + "<p>Recibimos una solicitud para restablecer la contraseña de tu cuenta en <strong>Mascotas Unidas</strong>.</p>"
                    + "<p>Generamos una clave temporal de acceso segura para vos:</p>"
                    + "<div style='background:#f3f4f6; padding:15px; border-radius:6px; font-size:20px; font-weight:bold; letter-spacing:2px; text-align:center; color:#e91e63; margin:20px 0;'>"
                    + claveProvisoria
                    + "</div>"
                    + "<p>Por favor, ingresá a la app con esta contraseña y recordá cambiarla desde tu panel de usuario a la brevedad por cuestiones de seguridad.</p>"
                    + "<br><p>Si vos no solicitaste este cambio, podés ignorar este correo.</p>"
                    + "<hr><p style='font-size:12px; color:#777;'>Este es un correo automático, por favor no lo respondas.</p>";
            
            message.setContent(cuerpoHTML, "text/html; charset=utf-8");

            Transport.send(message);
            return true;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}