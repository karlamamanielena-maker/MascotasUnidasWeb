let listaFotos = []; 
let indiceActual = 0;

document.addEventListener("DOMContentLoaded", function () {
    const urlParams = new URLSearchParams(window.location.search);
    const mascotaId = urlParams.get("id");
    
    // Si venimos del index o mapa, deducimos el tipo. Por defecto, asumimos "PERDIDA" si no se envía en la URL
    let tipoPublicacion = urlParams.get("tipo") || "PERDIDA"; 

    if (!mascotaId) {
        console.error("No se especificó un ID de mascota.");
        document.querySelector(".detalle-container").innerHTML = `<p style="text-align:center; padding: 50px; font-weight: bold;">Error: Mascota no especificada.</p>`;
        return;
    }

    // Llamamos al servlet enviando ID y Tipo obligatoriamente
    fetch(`mascota-detalle?id=${mascotaId}&tipo=${tipoPublicacion}`)
    .then(res => {
        if (!res.ok) throw new Error("Reporte no encontrado o parámetros inválidos");
        return res.json();
    })
    .then(mascota => {
        listaFotos = mascota.fotos || [];
        
        // Actualizar textos básicos
        document.getElementById("breadcrumb-nombre").innerText = mascota.nombre;
        document.getElementById("titulo-nombre").innerText = mascota.nombre;
        
        // Renderizar todos los campos de tu consulta SQL incluyendo el teléfono visible
        const datosContenedor = document.querySelector(".datos");
        datosContenedor.innerHTML = `
            <p><i class="fa-solid fa-paw"></i> <strong>Especie:</strong> ${mascota.especie}</p>
            <p><i class="fa-solid fa-dna"></i> <strong>Raza:</strong> ${mascota.raza}</p>
            <p><i class="fa-solid fa-venus-mars"></i> <strong>Sexo:</strong> ${mascota.sexo}</p>
            <p><i class="fa-solid fa-cake-candles"></i> <strong>Edad:</strong> ${mascota.edad}</p>
            <p><i class="fa-solid fa-palette"></i> <strong>Color:</strong> ${mascota.color}</p>
            <p><i class="fa-solid fa-ruler"></i> <strong>Tamaño:</strong> ${mascota.tamano}</p>
            <p><i class="fa-regular fa-calendar"></i> <strong>Fecha del hecho:</strong> ${mascota.fecha_evento}</p>
            <p><i class="fa-solid fa-location-dot"></i> <strong>Ciudad:</strong> ${mascota.ciudad}</p>
            <p><i class="fa-solid fa-location-dot"></i> <strong>Dirección:</strong> ${mascota.direccion}</p>
            <p><i class="fa-solid fa-phone"></i> <strong>Teléfono de contacto:</strong> ${mascota.telefono || 'No disponible'}</p>
        `;

        // Modificar dinámicamente según sea PERDIDA o ENCONTRADA
        const estadoLabel = document.getElementById("estado-etiqueta");
        const breadcrumbSeccion = document.getElementById("breadcrumb-seccion");

        if (mascota.tipo === "ENCONTRADA") {
            estadoLabel.innerText = "Encontrada";
            estadoLabel.style.background = "#2e7d32"; 
            breadcrumbSeccion.innerText = "Mascotas Encontradas";
            breadcrumbSeccion.href = "mascotas-encontradas.html";
        } else {
            estadoLabel.innerText = "Perdida";
            estadoLabel.style.background = "#e91e63";
            breadcrumbSeccion.innerText = "Mascotas Perdidas";
            breadcrumbSeccion.href = "mascotas-perdidas.html";
        }

        // Asignar Descripción
        const descParrafo = document.getElementById("descripcion-texto");
        if(descParrafo) {
            descParrafo.innerText = mascota.descripcion || "Sin descripción adicional.";
        }

        // Configurar botón para contactar vía WhatsApp o Teléfono directo
        const btnContactar = document.querySelector(".btn-contactar");
        if (btnContactar && mascota.telefono) {
            btnContactar.onclick = () => {
                window.open(`https://wa.me/${mascota.telefono}?text=Hola,%20me%20contacto%20por%20la%20publicación%20de%20${mascota.nombre}`, '_blank');
            };
        }

        inicializarSlider();
    })
    .catch(err => {
        console.error("Error al cargar los detalles:", err);
        document.querySelector(".detalle-container").innerHTML = `<p style="text-align:center; padding: 50px;">El reporte no se pudo cargar correctamente.</p>`;
    });
});

function inicializarSlider() {
    const prevBtn = document.querySelector(".nav-btn.prev");
    const nextBtn = document.querySelector(".nav-btn.next");
    if (listaFotos.length <= 1) {
        if(prevBtn) prevBtn.style.display = "none";
        if(nextBtn) nextBtn.style.display = "none";
    }
    actualizarImagen();
}

function actualizarImagen() {
    const img = document.getElementById("img-principal");
    if (listaFotos.length > 0) {
        img.src = `ver-foto?foto_id=${listaFotos[indiceActual]}`;
    } else {
        img.src = "images/sin-foto.jpg";
    }
}

function cambiarFoto(direccion) {
    if (listaFotos.length === 0) return;
    indiceActual += direccion;
    if (indiceActual >= listaFotos.length) indiceActual = 0;
    if (indiceActual < 0) indiceActual = listaFotos.length - 1;
    actualizarImagen();
}