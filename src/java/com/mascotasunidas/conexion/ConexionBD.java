package com.mascotasunidas.conexion;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class ConexionBD {

    // URL limpia sin credenciales embebidas
    // Se añade trustServerCertificate=true para evitar el error de validación SSL en la nube
    private static final String URL = 
        "jdbc:mysql://sv-mascotasunidasweb-karlamamanielena-2405.a.aivencloud.com:22891/defaultdb?sslMode=REQUIRED&trustServerCertificate=true";
        
    private static final String USER = "avnadmin";

    // NOTA: Asegúrate de reemplazar esto por tu contraseña real
    private static final String PASSWORD = "AVNS_e7aq0f667G0p2_fQzhI";

    public static Connection conectar() {
        try {
            System.out.println("Intentando conectar a la base de datos...");

            // Carga del driver
            Class.forName("com.mysql.cj.jdbc.Driver");

            // Conexión usando parámetros separados (forma correcta y segura)
            Connection cn = DriverManager.getConnection(URL, USER, PASSWORD);

            System.out.println("Conectado correctamente a la base de datos.");
            return cn;

        } catch (ClassNotFoundException e) {
            System.out.println("Error: Driver MySQL no encontrado.");
            e.printStackTrace();
            return null;
        } catch (SQLException e) {
            System.out.println("Error de conexión SQL:");
            e.printStackTrace(); // Esto imprimirá el error real en los logs de Render
            return null;
        } catch (Exception e) {
            System.out.println("Error inesperado:");
            e.printStackTrace();
            return null;
        }
    }
}