let listaFotosIds = []; // IDs de fotos guardadas en la base de datos
let fotosAEliminar = []; // IDs de fotos que el usuario decide remover
let imagenesSeleccionadas = []; // Archivos binarios de fotos NUEVAS
let indiceActual = 0;
let mascotaId_global = null;
let nombreMascota_global = "";

// Variables del Mapa Interactivo Leaflet
let map;
let marker;

document.addEventListener("DOMContentLoaded", function () {
    const urlParams = new URLSearchParams(window.location.search);
    mascotaId_global = urlParams.get("id");
    let tipoPublicacion = urlParams.get("tipo") || "PERDIDA"; 

    if (!mascotaId_global) {
        mostrarMensajePantalla("❌ ID de publicación no válido.", "error");
        setTimeout(() => window.location.href = "mis-publicaciones.html", 2000);
        return;
    }

    // 1. OBTENER INFORMACIÓN COMPLETA DE LA MASCOTA DESDE EL BACKEND
    fetch(`mascota-detalle?id=${mascotaId_global}&tipo=${tipoPublicacion}`)
    .then(res => {
        if (res.status === 401) { window.location.href = "login.html"; return; }
        if (!res.ok) throw new Error("No tienes acceso a este reporte.");
        return res.json();
    })
    .then(mascota => {
        listaFotosIds = mascota.fotos || [];
        nombreMascota_global = mascota.nombre;
        
        const modalPetName = document.getElementById("modal-pet-name");
        if(modalPetName) modalPetName.innerText = `"${mascota.nombre}"`;

        // Llenar campos informativos primarios
        document.getElementById("edit-nombre").value = mascota.nombre;
        document.getElementById("edit-especie").value = mascota.especie.toUpperCase();
        document.getElementById("edit-raza").value = mascota.raza;
        document.getElementById("edit-sexo").value = mascota.sexo.toUpperCase();
        document.getElementById("edit-edad").value = mascota.edad;
        document.getElementById("edit-color").value = mascota.color;
        document.getElementById("edit-tamano").value = mascota.tamano.toUpperCase();
        document.getElementById("inputDireccion").value = mascota.direccion;
        document.getElementById("edit-descripcion").value = mascota.descripcion;
        if(mascota.fecha_evento) document.getElementById("edit-fecha").value = mascota.fecha_evento;

        // Configurar coordenadas actuales en variables
        document.getElementById("inputLatitud").value = mascota.latitud;
        document.getElementById("inputLongitud").value = mascota.longitud;

        // Configurar etiqueta visual flotante
        const estadoLabel = document.getElementById("estado-etiqueta");
        if(estadoLabel) {
            estadoLabel.innerText = mascota.tipo;
            estadoLabel.style.background = mascota.tipo === "ENCONTRADA" ? "#28a745" : "#e91e63";
        }

        // Inicializar mapas 
        inicializarMapa(mascota.latitud, mascota.longitud);
        
        // PRECARGA GEOGRÁFICA CORREGIDA (Pasamos provincia_id y localidad_id que vienen del Servlet)
        cargarProvinciasYLocalidadActual(mascota.provincia_id, mascota.localidad_id);
        actualizarSliderVisual();
    })
    .catch(err => {
        console.error(err);
        mostrarMensajePantalla("❌ Error al validar el acceso al reporte o datos inexistentes.", "error");
    });

    // 2. CONFIGURACIÓN DEL INPUT DE NUEVAS FOTOS
    const inputFoto = document.getElementById("inputFoto");
    if (inputFoto) {
        inputFoto.addEventListener("change", function() {
            const archivosNuevos = this.files;
            if (archivosNuevos && archivosNuevos.length > 0) {
                let fotosVivasCount = (listaFotosIds.length - fotosAEliminar.length) + imagenesSeleccionadas.length;
                if (fotosVivasCount + archivosNuevos.length > 5) {
                    mostrarMensajePantalla("⚠️ Límite excedido. El reporte puede tener un máximo de 5 imágenes en total.", "error");
                    this.value = ""; return;
                }
                for (let i = 0; i < archivosNuevos.length; i++) {
                    if (!archivosNuevos[i].type.startsWith("image/")) {
                        mostrarMensajePantalla("❌ Tipo de archivo inválido. Sube solo imágenes.", "error");
                        this.value = ""; return;
                    }
                    imagenesSeleccionadas.push(archivosNuevos[i]);
                }
                limpiarMensajePantalla();
                actualizarListaVisualNuevas();
                this.value = "";
            }
        });
    }

    // INTERCEPTAR EL SUBMIT PARA MOSTRAR EL MODAL DE CONFIRMACIÓN
    const formEditar = document.getElementById("form-editar-publicacion");
    if(formEditar) {
        formEditar.addEventListener("submit", function(e) {
            e.preventDefault();
            
            const lat = document.getElementById("inputLatitud").value;
            if (!lat) {
                mostrarMensajePantalla("⚠️ Debes fijar una ubicación válida en el mapa antes de guardar.", "error");
                return;
            }

            let fotosVivasCount = (listaFotosIds.length - fotosAEliminar.length) + imagenesSeleccionadas.length;
            if (fotosVivasCount === 0) {
                mostrarMensajePantalla("⚠️ El reporte no puede quedarse sin fotos. Agrega una nueva antes de remover todas.", "error");
                return;
            }

            limpiarMensajePantalla();
            abrirModalGuardar();
        });
    }
});

/* ==========================================================================
   FUNCIÓN AUXILIAR PARA MENSAJES ESTÉTICOS EN PANTALLA (REEMPLAZA AL ALERT)
========================================================================== */
function mostrarMensajePantalla(texto, tipo) {
    const contenedorMsg = document.getElementById("contenedorMensaje");
    if (!contenedorMsg) return;
    
    const claseMsg = tipo === "error" ? "mensaje-error" : "mensaje-exito";
    contenedorMsg.innerHTML = `<div class="mensaje ${claseMsg}">${texto}</div>`;
    window.scrollTo({ top: contenedorMsg.offsetTop - 100, behavior: 'smooth' });
}

function limpiarMensajePantalla() {
    const contenedorMsg = document.getElementById("contenedorMensaje");
    if (contenedorMsg) contenedorMsg.innerHTML = "";
}

/* ==========================================================================
   LÓGICA DEL MAPA INTERACTIVO (LEAFLET + NOMINATIM)
========================================================================== */
function inicializarMapa(lat, lon) {
    let defaultLat = lat ? lat : -34.6037;
    let defaultLon = lon ? lon : -58.3816;
    let zoomInicial = lat ? 15 : 4;

    map = L.map('map').setView([defaultLat, defaultLon], zoomInicial);

    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
        maxZoom: 19,
        attribution: '© OpenStreetMap contributors'
    }).addTo(map);

    if(lat && lon) {
        actualizarMarcadorYCoordenadas(lat, lon, false);
    }

    map.on('click', function(e) {
        actualizarMarcadorYCoordenadas(e.latlng.lat, e.latlng.lng, true);
    });
}

function actualizarMarcadorYCoordenadas(lat, lon, buscarDireccionInversa = false) {
    document.getElementById("inputLatitud").value = lat;
    document.getElementById("inputLongitud").value = lon;

    if (marker) {
        marker.setLatLng([lat, lon]);
    } else {
        marker = L.marker([lat, lon], { draggable: true }).addTo(map);
        marker.on('dragend', function(event) {
            const position = marker.getLatLng();
            actualizarMarcadorYCoordenadas(position.lat, position.lng, true);
        });
    }
    
    map.setView([lat, lon], 16);
    const statusUbicacion = document.getElementById("statusUbicacion");
    statusUbicacion.innerHTML = `<i class="fa-solid fa-circle-check"></i> Ubicación establecida (Lat: ${parseFloat(lat).toFixed(4)}, Lon: ${parseFloat(lon).toFixed(4)})`;
    statusUbicacion.style.color = "#28a745";

    if (buscarDireccionInversa) {
        fetch(`https://nominatim.openstreetmap.org/reverse?format=json&lat=${lat}&lon=${lon}`)
        .then(res => res.json())
        .then(data => {
            if (data && data.display_name) {
                const partes = data.display_name.split(",");
                if (partes.length > 0) {
                    document.getElementById("inputDireccion").value = partes[0].trim() + (partes[1] ? ", " + partes[1].trim() : "");
                }
            }
        }).catch(err => console.error("Error en geocoding inverso: ", err));
    }
}

// Botón "Ubicar en Mapa" valida estéticamente en pantalla sin alerts nativos
const btnBuscarCoordenadas = document.getElementById("btnBuscarCoordenadas");
if(btnBuscarCoordenadas) {
    btnBuscarCoordenadas.addEventListener("click", function() {
        const provSelect = document.getElementById("selectProvincia");
        const locSelect = document.getElementById("selectLocalidad");
        const direccionTxt = document.getElementById("inputDireccion").value.trim();

        if (!provSelect.value || !locSelect.value || !direccionTxt) {
            mostrarMensajePantalla("⚠️ Por favor, completa Provincia, Localidad y Dirección antes de ubicar en el mapa.", "error");
            return;
        }

        limpiarMensajePantalla();
        const provincia = provSelect.options[provSelect.selectedIndex].text;
        const localidad = locSelect.options[locSelect.selectedIndex].text;
        const queryUbicacion = `${direccionTxt}, ${localidad}, ${provincia}, Argentina`;

        document.getElementById("statusUbicacion").innerText = "⏳ Buscando ubicación...";
        document.getElementById("statusUbicacion").style.color = "#0d47a1";
        
        fetch(`https://nominatim.openstreetmap.org/search?format=json&q=${encodeURIComponent(queryUbicacion)}&limit=1`)
        .then(res => res.json())
        .then(data => {
            if (data && data.length > 0) {
                actualizarMarcadorYCoordenadas(data[0].lat, data[0].lon, false);
            } else {
                document.getElementById("statusUbicacion").innerHTML = "❌ Dirección no encontrada con precisión. Coloca el marcador manualmente haciendo click directo en el mapa.";
                document.getElementById("statusUbicacion").style.color = "#dc3545";
            }
        });
    });
}

/* ==========================================================================
   GESTIÓN GEOGRÁFICA SINCRONIZADA POR ENTRADA DE PROVINCIA DESDE EL FRONT
========================================================================== */
function cargarProvinciasYLocalidadActual(provinciaIdTarget, localidadIdTarget) {
    const selectProvincia = document.getElementById("selectProvincia");
    const selectLocalidad = document.getElementById("selectLocalidad");

    fetch("ubicacion?accion=provincias")
    .then(res => res.json())
    .then(provincias => {
        selectProvincia.innerHTML = '<option value="" disabled selected>Seleccioná Provincia</option>';
        provincias.forEach(prov => {
            const option = document.createElement("option");
            option.value = prov.id; 
            option.text = prov.nombre;
            selectProvincia.appendChild(option);
        });

        if (provinciaIdTarget && provinciaIdTarget !== "undefined") {
            selectProvincia.value = provinciaIdTarget;
            dispararCargaLocalidades(provinciaIdTarget, localidadIdTarget);
        }
    })
    .catch(e => console.error("Error cargando provincias:", e));

    selectProvincia.addEventListener("change", function() {
        dispararCargaLocalidades(this.value, null);
    });

    selectLocalidad.addEventListener("change", function() {
        document.getElementById("inputLocalidadIdHidden").value = this.value;
    });
}

function dispararCargaLocalidades(provinciaId, seleccionarLocalidadId) {
    const selectLocalidad = document.getElementById("selectLocalidad");
    selectLocalidad.innerHTML = '<option value="" disabled selected>Cargando localidades...</option>';
    selectLocalidad.disabled = true;

    fetch(`ubicacion?accion=localidades&provincia_id=${provinciaId}`)
    .then(res => res.json())
    .then(localidades => {
        selectLocalidad.innerHTML = '<option value="" disabled selected>Seleccioná Localidad</option>';
        localidades.forEach(loc => {
            const option = document.createElement("option");
            option.value = loc.id; 
            option.text = loc.nombre;
            selectLocalidad.appendChild(option);
        });
        selectLocalidad.disabled = false;
        
        if (seleccionarLocalidadId) {
            selectLocalidad.value = seleccionarLocalidadId;
            document.getElementById("inputLocalidadIdHidden").value = seleccionarLocalidadId;
        }
    })
    .catch(e => console.error("Error cargando localidades:", e));
}

/* ==========================================================================
   GESTIÓN DE IMÁGENES (SLIDER CON MODAL DE ELIMINACIÓN)
========================================================================== */
function actualizarSliderVisual() {
    const img = document.getElementById("img-principal");
    const btnEliminar = document.getElementById("btn-eliminar-foto-actual");
    
    let fotosDisponibles = listaFotosIds.filter(id => !fotosAEliminar.includes(id));

    if (fotosDisponibles.length > 0) {
        if (indiceActual >= fotosDisponibles.length) indiceActual = 0;
        if (indiceActual < 0) indiceActual = fotosDisponibles.length - 1;
        img.src = `ver-foto?foto_id=${fotosDisponibles[indiceActual]}`;
        if(btnEliminar) btnEliminar.style.display = "block";
    } else {
        img.src = "images/sin-foto.png"; // Cambiado a .png según indicación
        if(btnEliminar) btnEliminar.style.display = "none";
    }
}

function cambiarFoto(direccion) {
    let fotosDisponibles = listaFotosIds.filter(id => !fotosAEliminar.includes(id));
    if (fotosDisponibles.length === 0) return;
    indiceActual += direccion;
    actualizarSliderVisual();
}

function marcarEliminacionFotoActual() {
    let fotosDisponibles = listaFotosIds.filter(id => !fotosAEliminar.includes(id));
    if (fotosDisponibles.length === 0) return;
    abrirModalEliminarFoto();
}

function confirmarEliminacionFotoProceso() {
    cerrarModalEliminarFoto();
    let fotosDisponibles = listaFotosIds.filter(id => !fotosAEliminar.includes(id));
    let idParaBorrar = fotosDisponibles[indiceActual];
    fotosAEliminar.push(idParaBorrar); 
    
    mostrarMensajePantalla("📸 Foto removida de la vista previa. Se aplicará de forma definitiva al presionar 'Guardar Cambios'.", "exito");
    indiceActual = 0;
    actualizarSliderVisual();
}

function actualizarListaVisualNuevas() {
    const contenedor = document.getElementById("listaArchivosContenedor");
    if (!contenedor) return;
    contenedor.innerHTML = "";

    imagenesSeleccionadas.forEach((archivo, index) => {
        const item = document.createElement("div");
        item.style = "display:flex; align-items:center; gap:10px; padding:8px; background:#f1f3f5; border-radius:6px; font-size:13px; border:1px solid #dee2e6;";
        item.innerHTML = `
            <i class="fa-solid fa-file-image" style="color: #28a745;"></i>
            <span style="font-weight:500;">${archivo.name} (Nueva)</span>
            <button type="button" class="btn-borrar-foto" style="margin-left:auto;" onclick="removerFotoNuevaSeleccionada(${index})">
                <i class="fa-solid fa-trash-can"></i>
            </button>
        `;
        contenedor.appendChild(item);
    });
}

function removerFotoNuevaSeleccionada(idx) {
    imagenesSeleccionadas.splice(idx, 1);
    actualizarListaVisualNuevas();
}

/* ==========================================================================
   MODALES Y PROCESAMIENTO AJAX FINAL (SUBMIT A SERVLETS)
========================================================================== */
function abrirModalGuardar() { document.getElementById("modalGuardar").classList.add("active"); }
function cerrarModalGuardar() { document.getElementById("modalGuardar").classList.remove("active"); }

function abrirModalBaja() { document.getElementById("modalBaja").classList.add("active"); }
function cerrarModalBaja() { document.getElementById("modalBaja").classList.remove("active"); }

function abrirModalEliminarFoto() { document.getElementById("modalEliminarFoto").classList.add("active"); }
function cerrarModalEliminarFoto() { document.getElementById("modalEliminarFoto").classList.remove("active"); }

function abrirModalResuelto() { document.getElementById("modalResuelto").classList.add("active"); }
function cerrarModalResuelto() { document.getElementById("modalResuelto").classList.remove("active"); }

function confirmarGuardarProceso() {
    cerrarModalGuardar();
    
    // Mostramos mensaje de espera estético en pantalla
    const contenedorMsg = document.getElementById("contenedorMensaje");
    if (contenedorMsg) {
        contenedorMsg.innerHTML = `<div class="mensaje" style="background:#e3f2fd; color:#0d47a1; padding:15px; border-radius:8px; font-weight:bold;">⏳ Actualizando publicación en base de datos...</div>`;
    }

    const form = document.getElementById("form-editar-publicacion");
    const formData = new FormData(form);

    formData.append("accion", "MODIFICAR");
    formData.append("mascota_id", mascotaId_global);
    formData.append("fotos_a_eliminar", fotosAEliminar.join(","));

    imagenesSeleccionadas.forEach(archivo => {
        formData.append("foto_mascota_nueva", archivo);
    });

    fetch("editar-publicacion-data", { 
        method: "POST",
        body: formData
    })
    .then(res => res.text())
    .then(resultado => {
        if (resultado.trim() === "OK") {
            sessionStorage.setItem("mensajeExitoIndex", "✅ ¡Publicación actualizada con éxito!");
            window.location.href = "mis-publicaciones.html";
        } else {
            mostrarMensajePantalla(`❌ Error: ${resultado}`, "error");
        }
    })
    .catch(err => {
        console.error(err);
        mostrarMensajePantalla("❌ Error de red al intentar procesar la actualización.", "error");
    });
}

function confirmarResueltoProceso() {
    cerrarModalResuelto();
    fetch("mis-publicaciones-data", {
        method: "POST",
        headers: { "Content-Type": "application/x-www-form-urlencoded" },
        body: `id=${mascotaId_global}&nuevo_estado=RESUELTA`
    })
    .then(res => { if(!res.ok) throw new Error(); return res.json(); })
    .then(data => {
        if (data.success) {
            sessionStorage.setItem("mensajeExitoIndex", `🎉 ¡Excelente noticia! Finalizamos el reporte de "${nombreMascota_global}". Mascota Reunida con sus dueños.`);
            window.location.href = "mis-publicaciones.html";
        }
    }).catch(err => mostrarMensajePantalla("❌ No se pudo actualizar el estado a resuelto.", "error"));
}

function confirmarBajaProceso() {
    cerrarModalBaja();
    fetch("mis-publicaciones-data", {
        method: "POST",
        headers: { "Content-Type": "application/x-www-form-urlencoded" },
        body: `id=${mascotaId_global}&nuevo_estado=CANCELADA`
    })
    .then(res => { if(!res.ok) throw new Error(); return res.json(); })
    .then(data => {
        if (data.success) {
            sessionStorage.setItem("mensajeExitoIndex", `❌ La publicación de "${nombreMascota_global}" fue dada de baja.`);
            window.location.href = "mis-publicaciones.html";
        }
    }).catch(err => mostrarMensajePantalla("❌ No se pudo completar la baja de la publicación.", "error"));
}