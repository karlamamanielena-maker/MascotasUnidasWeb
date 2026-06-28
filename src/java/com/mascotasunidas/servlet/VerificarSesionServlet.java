package com.mascotasunidas.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet("/verificar-sesion")
public class VerificarSesionServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();
        
        HttpSession session = request.getSession(false); // false para que no cree una nueva si no existe
        
        if (session != null && session.getAttribute("nombreUsuario") != null) {
            String nombre = (String) session.getAttribute("nombreUsuario");
            // Devolvemos un JSON indicando que está logueado y su nombre
            out.print("{\"logueado\": true, \"nombre\": \"" + nombre + "\"}");
        } else {
            // Devolvemos que no está logueado
            out.print("{\"logueado\": false}");
        }
    }
}