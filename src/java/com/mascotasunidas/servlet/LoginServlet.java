package com.mascotasunidas.servlet;

import com.mascotasunidas.conexion.ConexionBD;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.mindrot.jbcrypt.BCrypt;

@WebServlet("/login")
public class LoginServlet extends HttpServlet {

    @Override
    protected void doPost(
            HttpServletRequest request,
            HttpServletResponse response)
            throws ServletException, IOException {

        // Configuramos la respuesta para AJAX y UTF-8
        response.setContentType("text/plain");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        String email = request.getParameter("email");
        String password = request.getParameter("password");

        try {
            // 1. Conexión a la base de datos
            Connection cn = ConexionBD.conectar();
            if (cn == null) {
                out.print("Error al conectar con la base de datos.");
                return;
            }

            // 2. Buscar al usuario por email
            String sql = "SELECT id, nombre, password_hash FROM usuarios WHERE email = ?";
            PreparedStatement ps = cn.prepareStatement(sql);
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                String hashBD = rs.getString("password_hash");

                // 3. Verificar si la contraseña coincide con el Hash
                if (BCrypt.checkpw(password, hashBD)) {
                    
                    HttpSession session = request.getSession();
                    session.setAttribute("idUsuario", rs.getInt("id"));
                    session.setAttribute("nombreUsuario", rs.getString("nombre"));
                    session.setAttribute("emailUsuario", email);

                    // LÓGICA DE RECORDARME
                    String recordarme = request.getParameter("recordarme");
                    jakarta.servlet.http.Cookie cookieEmail = new jakarta.servlet.http.Cookie("recordarEmail", java.net.URLEncoder.encode(email, "UTF-8"));

                    if ("true".equals(recordarme)) {
                        cookieEmail.setMaxAge(60 * 60 * 24 * 15); // Duración: 15 días
                    } else {
                        cookieEmail.setMaxAge(0); // Eliminar cookie si no se tildó
                    }
                    cookieEmail.setPath(request.getContextPath());
                    response.addCookie(cookieEmail);

                    out.print("OK");
                } else {
                    // Contraseña incorrecta
                    out.print("La contraseña ingresada es incorrecta.");
                }
            } else {
                // El correo electrónico no existe en la BD
                out.print("El correo electrónico no está registrado.");
            }

            // Cerrar recursos
            rs.close();
            ps.close();
            cn.close();

        } catch (Exception e) {
            e.printStackTrace();
            out.print("Ocurrió un error inesperado en el servidor.");
        }
    }
}