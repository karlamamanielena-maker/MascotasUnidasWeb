
# Descarga la imagen oficial de Tomcat 10 con Jakarta EE corriendo en Java 17
FROM tomcat:10.1-jdk17

# Elimina las aplicaciones por defecto de Tomcat para liberar memoria RAM
RUN rm -rf /usr/local/tomcat/webapps/*

# Copia tu archivo empaquetado y lo renombra como ROOT.war para que cargue en la raíz de la web
COPY dist/MascotasUnidasWeb.war /usr/local/tomcat/webapps/ROOT.war

# Expone el puerto por defecto de Tomcat
EXPOSE 8080

# Inicia el servidor al levantar la app
CMD ["catalina.sh", "run"]