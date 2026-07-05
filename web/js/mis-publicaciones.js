document.addEventListener("DOMContentLoaded", function () {
    cargarMisPublicaciones();
});

function cargarMisPublicaciones() {
    const contenedor = document.getElementById("contenedorMisPublicaciones");

    fetch("mis-publicaciones-data")
        .then(res => {
            if (res.status === 401) {
                // Redirección si la sesión de Tomcat expiró
                window.location.href = "login.html";
                return;
            }
            if (!res.ok) throw new Error("Error cargando los reportes");
            return res.json();
        })
        .then(data => {
            contenedor.innerHTML = "";

            if (data.publicaciones && data.publicaciones.length > 0) {
                data.publicaciones.forEach(pub => {
                const article = document.createElement("article");
                article.className = "pet-card";

                const colorTipo = pub.tipo === "PERDIDA" ? "#e91e63" : "#28a745";

                article.innerHTML = `
                    <div class="pet-image">
                        <img src="ver-foto?id=${pub.id}" alt="${pub.nombre}" onerror="this.onerror=null; this.src='images/sin-foto.jpg';">
                        <span class="status" style="background:${colorTipo}">${pub.tipo}</span>
                    </div>
                    <div class="pet-info">
                        <h2>${pub.nombre}</h2>
                        <p><strong>Clasificación:</strong> ${pub.especie} • ${pub.raza}</p>
                        <p><i class="fa-regular fa-calendar"></i> <strong>Fecha del reporte:</strong> ${pub.fecha}</p>
                        <p><strong>Estado actual:</strong> <span style="font-weight:bold; color:#555;">${pub.estado}</span></p>
                    </div>
                    <div class="pet-action" style="display:flex; flex-direction:column; gap:10px; justify-content:center;">
                        <button onclick="window.location.href='editar-publicacion.html?id=${pub.id}&tipo=${pub.tipo}'" style="background:#007bff; color:white; border:none; border-radius:8px; padding:12px 25px; font-weight:bold; cursor:pointer;">
                            <i class="fa-solid fa-pen-to-square"></i> Editar Publicación
                        </button>
                    </div>
                `;                    
                contenedor.appendChild(article);
            });
            } else {
                contenedor.innerHTML = `
                    <div style="text-align:center; padding:40px; width:100%;">
                        <p style="color:#666; font-size:18px; margin-bottom:15px;">Aún no has realizado ninguna publicación.</p>
                        <a href="reportar-mascota.html" class="pink-btn" style="text-decoration:none; padding:10px 20px; display:inline-block; border-radius:8px;">Crear un reporte ahora</a>
                    </div>
                `;
            }
        })
        .catch(err => {
            console.error(err);
            contenedor.innerHTML = `<p style="text-align:center; color:#dc3545; padding:20px;">Error al cargar tus publicaciones del sistema.</p>`;
        });
}

function cancelarPublicacion(id, nombre) {
    if (confirm(`¿Estás seguro de que deseas dar de baja la publicación de "${nombre}"? Esta acción no se puede deshacer.`)) {
        
        // Petición estructurada POST para Tomcat
        fetch("mis-publicaciones-data", {
            method: "POST",
            headers: { "Content-Type": "application/x-www-form-urlencoded" },
            body: `id=${id}`
        })
        .then(res => {
            if (!res.ok) throw new Error("No se pudo procesar la baja.");
            return res.json();
        })
        .then(data => {
            if (data.success) {
                alert("La publicación fue dada de baja con éxito.");
                cargarMisPublicaciones(); // Refresca dinámicamente la grilla
            }
        })
        .catch(err => {
            alert("Error: " + err.message);
        });
    }
}

function editarPublicacion(id) {
    alert("Función para editar el reporte ID: " + id + " (Se implementará en el siguiente paso)");
}