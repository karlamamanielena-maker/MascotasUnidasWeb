document.addEventListener("DOMContentLoaded", function () {
    const map = L.map('map').setView([-26.1849, -58.1731], 13);
    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', { attribution: '© OpenStreetMap' }).addTo(map);

    let marcadores = []; // Guardaremos todos los marcadores aquí

    fetch("api/mascotas-mapa")
    .then(res => res.json())
    .then(data => {
        data.forEach(m => {
            // Asignar color: rojo para Perdida, azul para Encontrada
            const color = (m.tipo === "PERDIDA") ? "red" : "blue";
            const iconHtml = `<i class="fa-solid fa-location-dot" style="color:${color}; font-size:24px;"></i>`;
            
            const marker = L.marker([m.lat, m.lng], {
                icon: L.divIcon({ html: iconHtml, className: 'custom-icon' })
            }).addTo(map);

            marker.bindPopup(`<b>${m.nombre}</b><br>${m.tipo}<br><a href="detalle-perdida.html?id=${m.id}">Ver detalles</a>`);
            
            // Guardamos el marcador junto con su tipo
            marcadores.push({ marker: marker, tipo: m.tipo });
        });
    });

    // Función de filtrado
    window.filtrarMapa = function() {
        const mostrarPerdidas = document.getElementById("filtro-perdidas").checked;
        const mostrarEncontradas = document.getElementById("filtro-encontradas").checked;

        marcadores.forEach(obj => {
            const esPerdida = (obj.tipo === "PERDIDA");
            if ((esPerdida && mostrarPerdidas) || (!esPerdida && mostrarEncontradas)) {
                obj.marker.addTo(map);
            } else {
                map.removeLayer(obj.marker);
            }
        });
    };
});