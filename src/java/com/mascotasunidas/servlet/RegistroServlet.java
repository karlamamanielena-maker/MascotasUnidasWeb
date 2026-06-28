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

import org.mindrot.jbcrypt.BCrypt;

@WebServlet("/registro")
public class RegistroServlet extends HttpServlet {

    @Override
    protected void doPost(
            HttpServletRequest request,
            HttpServletResponse response)
            throws ServletException, IOException {

        // Configuramos la respuesta para trabajar con texto plano y codificación UTF-8 (evita problemas con tildes o la 'ñ')
        response.setContentType("text/plain");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        String nombre = request.getParameter("nombre");
        String apellido = request.getParameter("apellido");
        String email = request.getParameter("email");
        String telefono = request.getParameter("telefono");
        String password = request.getParameter("password");
        String confirmarPassword = request.getParameter("confirmarPassword");

        try {

            // 1. Validar contraseñas
            if (!password.equals(confirmarPassword)) {
                out.print("Las contraseñas ingresadas no coinciden.");
                return;
            }

            // 2. Conectar a la base de datos
            Connection cn = ConexionBD.conectar();

            if (cn == null) {
                out.print("Error de conexión con la base de datos. Intente más tarde.");
                return;
            }

            // 3. Validar si el email ya existe
            String sqlValidar = "SELECT id FROM usuarios WHERE email = ?";
            PreparedStatement psValidar = cn.prepareStatement(sqlValidar);
            psValidar.setString(1, email);
            ResultSet rs = psValidar.executeQuery();

            if (rs.next()) {
                out.print("El correo electrónico ya se encuentra registrado.");
                
                // Cerramos recursos antes de salir
                rs.close();
                psValidar.close();
                cn.close();
                return;
            }

            // 4. Generar hash seguro de la contraseña
            String hash = BCrypt.hashpw(password, BCrypt.gensalt());

            // 5. Insertar el nuevo usuario
            String sql = "INSERT INTO usuarios " +
                         "(nombre, apellido, email, telefono, password_hash) " +
                         "VALUES (?, ?, ?, ?, ?)";

            PreparedStatement ps = cn.prepareStatement(sql);
            ps.setString(1, nombre);
            ps.setString(2, apellido);
            ps.setString(3, email);
            ps.setString(4, telefono);
            ps.setString(5, hash);

            ps.executeUpdate();

            // 6. Cerrar de forma ordenada todos los recursos abiertos
            ps.close();
            rs.close();
            psValidar.close();
            cn.close();

            // Si todo salió bien, respondemos estrictamente con un exitoso "OK"
            out.print("OK");

        } catch (Exception e) {
            e.printStackTrace();
            // Si salta una excepción inesperada en el bloque try, enviamos este mensaje
            out.print("Ocurrió un error inesperado al procesar el registro.");
        }
    }
}