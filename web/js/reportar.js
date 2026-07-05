document.addEventListener("DOMContentLoaded", function() {
    
    const selectProvincia = document.getElementById("selectProvincia");
    const selectLocalidad = document.getElementById("selectLocalidad");
    
    const modal = document.getElementById("modalConfirmacion");
    const btnModalCancelar = document.getElementById("btnModalCancelar");
    const btnModalConfirmar = document.getElementById("btnModalConfirmar");
    const formReportar = document.getElementById("formReportar");
    
    const inputFoto = document.getElementById("inputFoto");
    const listaArchivosContenedor = document.getElementById("listaArchivosContenedor");
    
    const inputFecha = document.querySelector('input[name="fecha_evento"]');
    const btnBuscarCoordenadas = document.getElementById("btnBuscarCoordenadas");
    const statusUbicacion = document.getElementById("statusUbicacion");
    const inputDireccion = document.getElementById("inputDireccion");

    let imagenesSeleccionadas = [];
    
    // VARIABLES DEL MAPA INTERACTIVO
    let map;
    let marker;

    // INICIALIZACIÓN DEL MAPA INTERACTIVO (Centrado por defecto en Argentina)
    function inicializarMapa() {
        map = L.map('map').setView([-34.6037, -58.3816], 4); // Buenos Aires por defecto al abrir

        // Capa de diseño del mapa (OpenStreetMap estándar)
        L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
            maxZoom: 19,
            attribution: '© OpenStreetMap contributors'
        }).addTo(map);

        // Evento de click en cualquier parte del mapa para fijar coordenadas manualmente
        map.on('click', function(e) {
            const lat = e.latlng.lat;
            const lon = e.latlng.lng;
            actualizarMarcadorYCoordenadas(lat, lon, true);
        });
    }
    
    inicializarMapa();

    // Función auxiliar para mover el marcador y rellenar los inputs ocultos
    function actualizarMarcadorYCoordenadas(lat, lon, buscarDireccionInversa = false) {
        document.querySelector('input[name="latitud"]').value = lat;
        document.querySelector('input[name="longitud"]').value = lon;

        if (marker) {
            marker.setLatLng([lat, lon]);
        } else {
            // Crear el marcador si no existía, configurado para poder ser arrastrado
            marker = L.marker([lat, lon], { draggable: true }).addTo(map);
            
            // Si el usuario arrastra el marcador manualmente por el mapa
            marker.on('dragend', function(event) {
                const position = marker.getLatLng();
                actualizarMarcadorYCoordenadas(position.lat, position.lng, true);
            });
        }
        
        map.setView([lat, lon], 16); // Hace zoom al punto fijado
        statusUbicacion.innerHTML = `<i class="fa-solid fa-circle-check"></i> Coordenadas fijadas (Lat: ${parseFloat(lat).toFixed(4)}, Lon: ${parseFloat(lon).toFixed(4)})`;
        statusUbicacion.style.color = "#28a745";

        // Si el usuario clickeó el mapa directamente, buscamos qué dirección de calle corresponde a ese punto (Geocoding Inverso)
        if (buscarDireccionInversa) {
            fetch(`https://nominatim.openstreetmap.org/reverse?format=json&lat=${lat}&lon=${lon}`)
            .then(res => res.json())
            .then(data => {
                if (data && data.display_name) {
                    // Tomamos el nombre amigable (ej: "Calle Falsa 123")
                    const partes = data.display_name.split(",");
                    if (partes.length > 0 && inputDireccion) {
                        inputDireccion.value = partes[0].trim() + (partes[1] ? ", " + partes[1].trim() : "");
                    }
                }
            }).catch(err => console.error("Error en geocoding inverso: ", err));
        }
    }

    // 1. VALIDACIÓN DINÁMICA DE FECHA MÁXIMA (HOY)
    if (inputFecha) {
        const hoy = new Date();
        const yyyy = hoy.getFullYear();
        const mm = String(hoy.getMonth() + 1).padStart(2, '0');
        const dd = String(hoy.getDate()).padStart(2, '0');
        const fechaMaxima = `${yyyy}-${mm}-${dd}`;
        inputFecha.setAttribute("max", fechaMaxima);
    }

    // 2. CONTROL DE ACCESO INTERNO
    fetch("verificar-sesion")
    .then(response => response.json())
    .then(data => {
        if (!data.logueado) {
            window.location.href = "login.html?error=restricted";
        } else {
            cargarProvincias();
        }
    })
    .catch(error => console.error("Error verificando sesión:", error));

    function cargarProvincias() {
        fetch("ubicacion?accion=provincias")
        .then(response => response.json())
        .then(provincias => {
            if(selectProvincia) {
                selectProvincia.innerHTML = '<option value="" disabled selected>Seleccioná Provincia</option>';
                provincias.forEach(prov => {
                    const option = document.createElement("option");
                    option.value = prov.id;
                    option.text = prov.nombre;
                    selectProvincia.appendChild(option);
                });
            }
        })
        .catch(error => console.error("Error cargando provincias:", error));
    }

    if (selectProvincia) {
        selectProvincia.addEventListener("change", function() {
            const provinciaId = this.value;
            selectLocalidad.innerHTML = '<option value="" disabled selected>Cargando localidades...</option>';
            selectLocalidad.disabled = true;

            fetch(`ubicacion?accion=localidades&provincia_id=${provinciaId}`)
            .then(response => response.json())
            .then(localidades => {
                selectLocalidad.innerHTML = '<option value="" disabled selected>Seleccioná Localidad</option>';
                if (localidades.length > 0) {
                    localidades.forEach(loc => {
                        const option = document.createElement("option");
                        option.value = loc.id;
                        option.text = loc.nombre;
                        selectLocalidad.appendChild(option);
                    });
                    selectLocalidad.disabled = false;
                } else {
                    selectLocalidad.innerHTML = '<option value="" disabled>No hay localidades disponibles</option>';
                }
            })
            .catch(error => console.error("Error cargando localidades:", error));
        });
    }

    // 3. GEOLOCALIZACIÓN AL PRESIONAR EL BOTÓN "UBICAR"
    if (btnBuscarCoordenadas) {
        btnBuscarCoordenadas.addEventListener("click", function() {
            const direccionTxt = inputDireccion.value.trim();

            if (!selectProvincia.value || !selectLocalidad.value || !direccionTxt) {
                alert("Por favor, completá Provincia, Localidad y Dirección antes de ubicar en el mapa.");
                return;
            }

            const provincia = selectProvincia.options[selectProvincia.selectedIndex].text;
            const localidad = selectLocalidad.options[selectLocalidad.selectedIndex].text;

            statusUbicacion.innerText = "⏳ Buscando en el mapa...";
            statusUbicacion.style.color = "#0d47a1";

            const queryUbicacion = `${direccionTxt}, ${localidad}, ${provincia}, Argentina`;
            
            fetch(`https://nominatim.openstreetmap.org/search?format=json&q=${encodeURIComponent(queryUbicacion)}&limit=1`)
            .then(response => response.json())
            .then(data => {
                if (data && data.length > 0) {
                    const resultado = data[0];
                    // Movemos el marcador interactivo de Leaflet al punto encontrado
                    actualizarMarcadorYCoordenadas(resultado.lat, resultado.lon, false);
                } else {
                    statusUbicacion.innerHTML = "❌ No se encontró la dirección exacta. Podés marcarla haciendo click directo en el mapa.";
                    statusUbicacion.style.color = "#dc3545";
                    document.querySelector('input[name="latitud"]').value = "";
                    document.querySelector('input[name="longitud"]').value = "";
                    if(marker) map.removeLayer(marker);
                    marker = null;
                }
            })
            .catch(error => {
                console.error("Error en Geocoding:", error);
                statusUbicacion.innerText = "⚠️ Error al conectar con el servicio de mapas.";
                statusUbicacion.style.color = "#dc3545";
            });
        });
    }

    function actualizarListaVisual() {
        if (!listaArchivosContenedor) return;
        listaArchivosContenedor.innerHTML = "";

        imagenesSeleccionadas.forEach((archivo, index) => {
            const itemArchivo = document.createElement("div");
            itemArchivo.style.display = "flex";
            itemArchivo.style.alignItems = "center";
            itemArchivo.style.gap = "10px";
            itemArchivo.style.padding = "8px 12px";
            itemArchivo.style.background = "#f1f3f5";
            itemArchivo.style.borderRadius = "6px";
            itemArchivo.style.fontSize = "14px";
            itemArchivo.style.color = "#495057";
            itemArchivo.style.border = "1px solid #dee2e6";

            const esPrincipal = index === 0;
            const etiquetaPrincipal = esPrincipal ? `<span class="badge-principal">Principal</span>` : "";

            itemArchivo.innerHTML = `
                <i class="fa-solid fa-file-image" style="color: #e91e63; font-size: 16px;"></i>
                <span style="word-break: break-all; font-weight: 500;">${archivo.name}</span>
                <span style="color: #868e96; font-size: 12px;">(${(archivo.size / 1024).toFixed(1)} KB)</span>
                ${etiquetaPrincipal}
                <button type="button" class="btn-borrar-foto" style="margin-left: auto;" data-index="${index}">
                    <i class="fa-solid fa-trash-can"></i>
                </button>
            `;
            listaArchivosContenedor.appendChild(itemArchivo);
        });

        const botonesBorrar = listaArchivosContenedor.querySelectorAll(".btn-borrar-foto");
        botonesBorrar.forEach(btn => {
            btn.addEventListener("click", function() {
                const idx = parseInt(this.getAttribute("data-index"));
                imagenesSeleccionadas.splice(idx, 1); 
                actualizarListaVisual();             
            });
        });
    }

    if (inputFoto) {
        inputFoto.addEventListener("change", function() {
            const archivosNuevos = this.files;
            const contenedorMsg = document.getElementById("contenedorMensaje");

            if (archivosNuevos && archivosNuevos.length > 0) {
                if (imagenesSeleccionadas.length + archivosNuevos.length > 5) {
                    if (contenedorMsg) {
                        contenedorMsg.innerHTML = `<div class="mensaje mensaje-error" style="background: #ffebee; color: #c62828; padding: 15px; border-radius: 8px; margin-bottom: 20px; font-weight: bold;">❌ Límite excedido. Máximo de hasta 5 imágenes.</div>`;
                    }
                    this.value = ""; 
                    return;
                }

                for (let i = 0; i < archivosNuevos.length; i++) {
                    const archivo = archivosNuevos[i];
                    if (!archivo.type.startsWith("image/")) {
                        if (contenedorMsg) {
                            contenedorMsg.innerHTML = `<div class="mensaje mensaje-error" style="background: #ffebee; color: #c62828; padding: 15px; border-radius: 8px; margin-bottom: 20px; font-weight: bold;">❌ Tipo de archivo inválido. Subí solo imágenes.</div>`;
                        }
                        this.value = ""; 
                        return;
                    }
                    imagenesSeleccionadas.push(archivo);
                }

                if (contenedorMsg) contenedorMsg.innerHTML = "";
                actualizarListaVisual();
                this.value = ""; 
            }
        });
    }

    if (formReportar) {
        formReportar.addEventListener("submit", function(e) {
            e.preventDefault(); 
            const contenedorMsg = document.getElementById("contenedorMensaje");
            const lat = document.querySelector('input[name="latitud"]').value;
            
            if (!lat) {
                if (contenedorMsg) {
                    contenedorMsg.innerHTML = `<div class="mensaje mensaje-error" style="background: #ffebee; color: #c62828; padding: 15px; border-radius: 8px; margin-bottom: 20px; font-weight: bold;">⚠️ Debés ubicar la mascota en el mapa antes de publicar el reporte.</div>`;
                    window.scrollTo({ top: contenedorMsg.offsetTop - 100, behavior: 'smooth' });
                }
                return;
            }
            
            if (imagenesSeleccionadas.length === 0) {
                if (contenedorMsg) {
                    contenedorMsg.innerHTML = `<div class="mensaje mensaje-error" style="background: #ffebee; color: #c62828; padding: 15px; border-radius: 8px; margin-bottom: 20px; font-weight: bold;">⚠️ Seleccioná al menos 1 foto.</div>`;
                }
                return; 
            }
            
            if (contenedorMsg) contenedorMsg.innerHTML = "";
            if (modal) modal.classList.add("active");
        });
    }

    if (btnModalCancelar) {
        btnModalCancelar.addEventListener("click", function() {
            if (modal) modal.classList.remove("active");
        });
    }

    if (btnModalConfirmar) {
        btnModalConfirmar.addEventListener("click", function() {
            if (modal) modal.classList.remove("active");

            const contenedorMsg = document.getElementById("contenedorMensaje");
            if (contenedorMsg) {
                contenedorMsg.innerHTML = `<div class="mensaje" style="background: #e3f2fd; color: #0d47a1; padding: 15px; border-radius: 8px; margin-bottom: 20px; font-weight: bold;">⏳ Publicando reporte...</div>`;
            }

            const formData = new FormData(formReportar);
            formData.delete("foto_mascota");

            imagenesSeleccionadas.forEach((archivo, index) => {
                formData.append("foto_mascota", archivo);
                const esPrincipal = (index === 0) ? "1" : "0";
                formData.append("foto_principal", esPrincipal);
            });

            fetch("reportar-mascota", {
                method: "POST",
                body: formData
            })
            .then(res => res.text())
            .then(resultado => {
                if (resultado.trim() === "OK") {
                    sessionStorage.setItem("mensajeExitoIndex", "✅ ¡Mascota reportada con éxito!");
                    window.location.href = "index.html";
                } else {
                    if (contenedorMsg) {
                        contenedorMsg.innerHTML = `<div class="mensaje mensaje-error" style="background: #ffebee; color: #c62828; padding: 15px; border-radius: 8px; margin-bottom: 20px; font-weight: bold;">❌ Error: ${resultado}</div>`;
                    }
                }
            })
            .catch(error => {
                console.error("Error:", error);
                if (contenedorMsg) {
                    contenedorMsg.innerHTML = `<div class="mensaje mensaje-error" style="background: #ffebee; color: #c62828; padding: 15px; border-radius: 8px; margin-bottom: 20px; font-weight: bold;">❌ Error de conexión.</div>`;
                }
            });
        });
    }
});