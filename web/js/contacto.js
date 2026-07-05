document.addEventListener("DOMContentLoaded", function () {
    const formContacto = document.getElementById("formContacto");

    if (formContacto) {
        formContacto.addEventListener("submit", function (e) {
            e.preventDefault(); // Detiene el refresco de página
            limpiarMensajePantalla();
            abrirModalContacto();
        });
    }
});

/* ==========================================================================
   INTERACCIÓN Y ENVÍO ASÍNCRONO DEL MODAL
========================================================================== */
function abrirModalContacto() { 
    document.getElementById("modalContacto").classList.add("active"); 
}

function cerrarModalContacto() { 
    document.getElementById("modalContacto").classList.remove("active"); 
}

function confirmarEnvioMensaje() {
    cerrarModalContacto();
    
    const contenedorMsg = document.getElementById("contenedorMensaje");
    if (contenedorMsg) {
        contenedorMsg.innerHTML = `<div class="mensaje" style="background:#e3f2fd; color:#0d47a1; padding:15px; border-radius:8px; font-weight:bold;">⏳ Procesando y enviando consulta...</div>`;
    }

    const form = document.getElementById("formContacto");
    // Serializamos de forma clásica x-www-form-urlencoded
    const datosFormulario = new URLSearchParams(new FormData(form)).toString();

    fetch("contacto-enviar", {
        method: "POST",
        headers: { "Content-Type": "application/x-www-form-urlencoded" },
        body: datosFormulario
    })
    .then(res => res.text())
    .then(resultado => {
        if (resultado.trim() === "OK") {
            // Mensaje de éxito inline en pantalla coherente con editar-publicacion
            if (contenedorMsg) {
                contenedorMsg.innerHTML = `<div class="mensaje mensaje-exito">✅ ¡Mensaje enviado con éxito! Nos comunicaremos con vos a la brevedad.</div>`;
            }
            form.reset(); // Limpia los campos del formulario
            window.scrollTo({ top: contenedorMsg.offsetTop - 100, behavior: 'smooth' });
        } else {
            mostrarMensajePantalla(`❌ Error al procesar: ${resultado}`, "error");
        }
    })
    .catch(err => {
        console.error(err);
        mostrarMensajePantalla("❌ Ocurrió un error de conexión al intentar enviar el mensaje.", "error");
    });
}

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