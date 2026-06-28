document.addEventListener("DOMContentLoaded", function () {
    cargarMascotasPerdidas();

    document.getElementById("btn-buscar").addEventListener("click", aplicarFiltrosUnificados);
    document.getElementById("btn-aplicar-filtros").addEventListener("click", aplicarFiltrosUnificados);
    document.getElementById("btn-limpiar-filtros").addEventListener("click", limpiarTodosLosCampos);
});

function cargarMascotasPerdidas(queryString = "") {
    const contenedor = document.getElementById("contenedorPerdidas");
    contenedor.innerHTML = `<p style="color:#666; width:100%; text-align:center; grid-column: 1/-1;"><i class="fa-solid fa-spinner fa-spin"></i> Filtrando reportes...</p>`;

    fetch(`perdidas-data${queryString}`)
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
                    const card = document.createElement("div");
                    card.className = "pet-card";
                    card.innerHTML = `
                        <div class="pet-image">
                            <img src="ver-foto?id=${mascota.id}" alt="${mascota.nombre}" onerror="this.onerror=null; this.src='images/sin-foto.jpg';">
                            <span class="status">Perdida</span>
                        </div>
                        <div class="pet-body">
                            <h3>${mascota.nombre}</h3>
                            <p><strong>${mascota.especie}</strong> • ${mascota.raza}(${mascota.tamano})</p>
                            <p><i class="fa-solid fa-location-dot"></i> ${mascota.ubicacion}</p>
                            <p><i class="fa-regular fa-calendar"></i> ${mascota.fecha}</p>
                            <p style="color:#e91e63; font-weight:600;"><i class="fa-solid fa-phone"></i> ${mascota.telefono}</p>
                            <a href="detalle-perdida.html?id=${mascota.id}&tipo=PERDIDA" class="btn-detalle">Ver detalles</a>
                        </div>
                    `;
                    contenedor.appendChild(card);
                });
            } else {
                contenedor.innerHTML = `<p style="color:#666; width:100%; text-align:center; grid-column: 1/-1; padding: 30px;">No se encontraron reportes con los criterios seleccionados.</p>`;
            }
        })
        .catch(err => {
            console.error(err);
            contenedor.innerHTML = `<p style="color:#721c24; width:100%; text-align:center; grid-column: 1/-1;">Error al sincronizar datos.</p>`;
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

    cargarMascotasPerdidas(`?${params.toString()}`);
}

function limpiarTodosLosCampos() {
    document.getElementById("filtro-zona").value = "";
    document.getElementById("filtro-raza").value = "";
    document.getElementById("filtro-fecha").value = "";
    document.getElementById("filtro-especie").value = "TODOS";
    document.getElementById("filtro-tamano").value = "";
    document.getElementById("filtro-sexo").value = "";
    cargarMascotasPerdidas();
}