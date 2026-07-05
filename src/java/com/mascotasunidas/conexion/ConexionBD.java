package com.mascotasunidas.conexion;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class ConexionBD {

    // URL configurada para ser compatible con Aiven.io desde entornos en la nube
    // Se fuerza TLSv1.2/1.3 y se confía en el certificado para evitar errores de SSL
    private static final String URL = 
        "jdbc:mysql://sv-mascotasunidasweb-karlamamanielena-2405.a.aivencloud.com:22891/defaultdb?sslMode=REQUIRED&trustServerCertificate=true&enabledTLSProtocols=TLSv1.2,TLSv1.3";
        
    private static final String USER = "avnadmin";
    private static final String PASSWORD = "AVNS_e7aq0f667G0p2_fQzhI"; // Asegúrate de poner la tuya

    public static Connection conectar() {
        try {
            System.out.println("Intentando conectar a Aiven.io...");

            Class.forName("com.mysql.cj.jdbc.Driver");

            Connection cn = DriverManager.getConnection(URL, USER, PASSWORD);

            System.out.println("Conectado exitosamente.");
            return cn;

        } catch (ClassNotFoundException e) {
            System.err.println("Error: No se encontró el Driver de MySQL.");
            e.printStackTrace();
            return null;
        } catch (SQLException e) {
            // Log detallado para diagnóstico en Render
            System.err.println("--- ERROR DE CONEXIÓN SQL ---");
            System.err.println("Mensaje: " + e.getMessage());
            System.err.println("SQLState: " + e.getSQLState());
            System.err.println("Código de error: " + e.getErrorCode());
            e.printStackTrace();
            return null;
        } catch (Exception e) {
            System.err.println("Error inesperado en la conexión: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
}