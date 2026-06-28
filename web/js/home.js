document.addEventListener("DOMContentLoaded", function () {

    

    // 1. INICIALIZAR EL MAPA DE LEAFLET

    const map = L.map('mapaHome').setView([-38.4161, -63.6167], 4);



    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {

        maxZoom: 18,

        attribution: '© OpenStreetMap contributors'

    }).addTo(map);



    // 2. FETCH PARA TRAER LOS DATOS REALES

    fetch("home-data")

    .then(res => {

        if(!res.ok) throw new Error("Error obteniendo los datos dinámicos");

        return res.json();

    })

    .then(data => {

        // ASIGNAR CONTADORES

        document.getElementById("cant-recuperadas").innerText = data.totalRecuperadas || 0;

        document.getElementById("cant-perdidas").innerText = data.totalPerdidas || 0;

        document.getElementById("cant-encontradas").innerText = data.totalEncontradas || 0;

        document.getElementById("cant-usuarios").innerText = data.totalUsuarios || 0;



        // RENDERIZAR LAS 3 ÚLTIMAS MASCOTAS

        const contenedor = document.getElementById("contenedorRecientes");

        contenedor.innerHTML = "";



        if (data.recientes && data.recientes.length > 0) {

            data.recientes.forEach(mascota => {

                const card = document.createElement("div");

                card.className = "card";

                

                // MEJORA: Se añade onerror para evitar que se rompa visualmente si la foto falta

                card.innerHTML = `

                    <img src="ver-foto?id=${mascota.id}" 

                         alt="${mascota.nombre || 'Mascota'}" 

                         onerror="this.onerror=null; this.src='images/sin-foto.jpg';">

                    <h3>${mascota.nombre || 'Sin nombre'}</h3>

                    <p>${mascota.especie} - ${mascota.raza || 'Mestizo'}</p>

                    <p><i class="fa-solid fa-location-dot"></i> ${mascota.direccion}</p>

                    <button onclick="window.location.href='detalle-perdida.html?id=${mascota.id}&tipo=PERDIDA'">Ver detalles</button>
                `;

                contenedor.appendChild(card);

            });

        } else {

            contenedor.innerHTML = `<p style="color:#666; width:100%; text-align:center;">No hay reportes cargados recientemente.</p>`;

        }



        // CARGAR MARCADORES DE MAPA

        if (data.mapaReportes && data.mapaReportes.length > 0) {

            data.mapaReportes.forEach(rep => {

                if (rep.latitud && rep.longitud) {

                    const colorTexto = rep.tipo === "PERDIDA" ? "red" : "green";

                    

                    const marker = L.marker([rep.latitud, rep.longitud]).addTo(map);

                    marker.bindPopup(`

                        <strong style="color:${colorTexto};">${rep.tipo}</strong><br>

                        <strong>${rep.nombre || 'Mascota'}</strong><br>

                        ${rep.especie}<br>

                        <a href="detalle-perdida.html?id=${rep.id}">Ver detalles</a>

                    `);

                }

            });

        }

    })

    .catch(err => console.error("Error cargando componentes de Home:", err));

});