package com.mascotasunidas.conexion;

import java.sql.Connection;
import java.sql.DriverManager;

public class ConexionBD {

    private static final String URL =
        "jdbc:mysql://localhost:3306/mascotas";

    private static final String USER =
        "root";

    private static final String PASSWORD =
        "admin";

    public static Connection conectar() {

        try {

            System.out.println("Intentando conectar...");

            Class.forName("com.mysql.cj.jdbc.Driver");

            Connection cn =
                DriverManager.getConnection(
                    URL,
                    USER,
                    PASSWORD
                );

            System.out.println("Conectado correctamente");

            return cn;

        } catch (Exception e) {

            System.out.println("Error de conexión:");

            e.printStackTrace();

            return null;
        }
    }
}