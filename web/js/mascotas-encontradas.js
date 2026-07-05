document.addEventListener("DOMContentLoaded", function () {
    cargarMascotasEncontradas();

    document.getElementById("btn-buscar").addEventListener("click", aplicarFiltrosUnificados);
    document.getElementById("btn-aplicar-filtros").addEventListener("click", aplicarFiltrosUnificados);
    document.getElementById("btn-limpiar-filtros").addEventListener("click", limpiarTodosLosCampos);
});

function cargarMascotasEncontradas(queryString = "") {
    const contenedor = document.getElementById("contenedorEncontradas");
    contenedor.innerHTML = `<p style="color:#666; width:100%; text-align:center;"><i class="fa-solid fa-spinner fa-spin"></i> Filtrando reportes...</p>`;

    fetch(`encontradas-data${queryString}`)
        .then(res => {
            if (!res.ok) throw new Error("Error en la consulta");
            return res.json();
        })
        .then(data => {
            if (queryString === "") {
                poblarSelectoresFijos(data.ubicaciones, data.razas);
            }

            contenedor.innerHTML = "";
            if (data.mascotas && data.mascotas.length > 0) {
                data.mascotas.forEach(mascota => {
                    const article = document.createElement("article");
                    article.className = "pet-card";
                    
                    // Conservamos la maquetación exacta en 3 partes de tu CSS para Encontradas
                    article.innerHTML = `
                        <div class="pet-image">
                            <img src="ver-foto?id=${mascota.id}" alt="${mascota.especie}" onerror="this.onerror=null; this.src='images/sin-foto.jpg';">
                            <span class="status">Encontrada</span>
                        </div>
                        <div class="pet-info">
                            <h2>${mascota.especie} • ${mascota.raza}(${mascota.tamano})</h2>
                            
                            <p><i class="fa-regular fa-calendar"></i> <strong>Fecha encontrada:</strong> ${mascota.fecha}</p>
                            <p><i class="fa-solid fa-earth-americas"></i> <strong>Zona:</strong> ${mascota.ubicacion}</p>
                            
                            <p><i class="fa-solid fa-location-dot"></i> <strong>Dirección / Ref:</strong> ${mascota.direccion || 'No especificada'}</p>
                            
                            <p style="color:#28a745; font-weight:600; margin-top:12px;"><i class="fa-solid fa-phone"></i> Retener / Contacto: ${mascota.telefono}</p>
                        </div>
                        <div class="pet-action">
                            <a href="detalle-perdida.html?id=${mascota.id}&tipo=ENCONTRADA" 
                               style="display:inline-block; padding:12px 25px; background:#e91e63; color:white; text-decoration:none; border-radius:8px; font-weight:bold;">
                               Ver detalles
                            </a>
                        </div>
                    `;
                    contenedor.appendChild(article);
                });
            } else {
                contenedor.innerHTML = `<p style="color:#666; width:100%; text-align:center; padding: 30px;">No se encontraron mascotas en este momento.</p>`;
            }
        })
        .catch(err => {
            console.error(err);
            contenedor.innerHTML = `<p style="color:#721c24; width:100%; text-align:center;">Error al sincronizar con el servidor.</p>`;
        });
}

function poblarSelectoresFijos(ubicaciones, razas) {
    const selectZona = document.getElementById("filtro-zona");
    const selectRaza = document.getElementById("filtro-raza");

    ubicaciones.forEach(u => {
        const opt = document.createElement("option");
        opt.value = u;
        opt.innerText = u;
        selectZona.appendChild(opt);
    });

    razas.forEach(r => {
        const opt = document.createElement("option");
        opt.value = r;
        opt.innerText = r;
        selectRaza.appendChild(opt);
    });
}

function aplicarFiltrosUnificados() {
    const ubicacion = document.getElementById("filtro-zona").value;
    const raza = document.getElementById("filtro-raza").value;
    const fecha = document.getElementById("filtro-fecha").value;
    const especie = document.getElementById("filtro-especie").value;
    const tamano = document.getElementById("filtro-tamano").value;
    const sexo = document.getElementById("filtro-sexo").value;

    const params = new URLSearchParams();
    if (ubicacion) params.append("ubicacion", ubicacion);
    if (raza) params.append("raza", raza);
    if (fecha) params.append("fecha", fecha);
    if (especie) params.append("especie", especie);
    if (tamano) params.append("tamano", tamano);
    if (sexo) params.append("sexo", sexo);

    cargarMascotasEncontradas(`?${params.toString()}`);
}

function limpiarTodosLosCampos() {
    document.getElementById("filtro-zona").value = "";
    document.getElementById("filtro-raza").value = "";
    document.getElementById("filtro-fecha").value = "";
    document.getElementById("filtro-especie").value = "TODOS";
    document.getElementById("filtro-tamano").value = "";
    document.getElementById("filtro-sexo").value = "";
    cargarMascotasEncontradas();
}