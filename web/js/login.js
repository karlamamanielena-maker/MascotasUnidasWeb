document.addEventListener("DOMContentLoaded", function() {
    // Buscar si existe la cookie de correo recordado
    const cookies = document.cookie.split(';');
    const cookieRecordar = cookies.find(c => c.trim().startsWith("recordarEmail="));
    
    if (cookieRecordar) {
        const emailGuardado = cookieRecordar.split('=')[1];
        const inputEmail = document.querySelector('input[name="email"]');
        const checkboxRecordar = document.querySelector('.remember input[type="checkbox"]');
        
        if (inputEmail) inputEmail.value = decodeURIComponent(emailGuardado);
        if (checkboxRecordar) checkboxRecordar.checked = true;
    }
});

document.getElementById("formLogin").addEventListener("submit", function(e) {
    e.preventDefault(); 

    const contenedorMsg = document.getElementById("contenedorMensaje");
    contenedorMsg.innerHTML = ""; 

    const formData = new FormData(this);
    
    // Capturar si el checkbox de recordar está marcado
    const checkboxRecordar = document.querySelector('.remember input[type="checkbox"]');
    if (checkboxRecordar) {
        formData.append("recordarme", checkboxRecordar.checked ? "true" : "false");
    }

    const formDataUrl = new URLSearchParams(formData);

    fetch("login", {
        method: "POST",
        headers: {
            "Content-Type": "application/x-www-form-urlencoded"
        },
        body: formDataUrl.toString()
    })
    .then(response => {
        if (!response.ok) throw new Error("Error en el servidor");
        return response.text();
    })
    .then(resultado => {
        if (resultado.trim() === "OK") {
            contenedorMsg.innerHTML = `<div class="mensaje mensaje-exito">✅ ¡Ingreso exitoso! Redirigiendo...</div>`;
            document.getElementById("formLogin").reset();
            
            setTimeout(() => {
                window.location.href = "index.html";
            }, 1500);
        } else {
            contenedorMsg.innerHTML = `<div class="mensaje mensaje-error">❌ ${resultado}</div>`;
        }
    })
    .catch(error => {
        console.error("Error:", error);
        contenedorMsg.innerHTML = `<div class="mensaje mensaje-error">❌ Error de conexión. Intente más tarde.</div>`;
    });
});

/* ==========================================================================
   GESTIÓN DEL MODAL DE RECUPERACIÓN DE CONTRASEÑA
========================================================================== */
function abrirModalRecuperar() {
    document.getElementById("emailRecuperacion").value = "";
    document.getElementById("modalRecuperar").classList.add("active");
}

function cerrarModalRecuperar() {
    document.getElementById("modalRecuperar").classList.remove("active");
}

function procesarRecuperacion() {
    const emailInput = document.getElementById("emailRecuperacion").value.trim();
    const contenedorMsg = document.getElementById("contenedorMensaje");

    if (!emailInput) {
        alert("Por favor, ingresá un correo electrónico válido.");
        return;
    }

    cerrarModalRecuperar();
    if (contenedorMsg) {
        contenedorMsg.innerHTML = `<div class="mensaje" style="background:#e3f2fd; color:#0d47a1; padding:15px; border-radius:8px; font-weight:bold;">⏳ Procesando solicitud y enviando correo...</div>`;
    }

    fetch("recuperar-password", {
        method: "POST",
        headers: { "Content-Type": "application/x-www-form-urlencoded" },
        body: `email=${encodeURIComponent(emailInput)}`
    })
    .then(res => {
        if (!res.ok) throw new Error("Error en el proceso del servidor.");
        return res.text();
    })
    .then(resultado => {
        if (resultado.trim() === "OK") {
            // CORRECCIÓN PROTEGIDA: Cartel puramente informativo sin mostrar contraseñas
            if (contenedorMsg) {
                contenedorMsg.innerHTML = `
                    <div class="mensaje mensaje-exito" style="padding:15px; border-radius:8px;">
                        <strong>✅ ¡Solicitud procesada con éxito!</strong><br>
                        Enviamos un correo electrónico a <u>${emailInput}</u> con tu nueva contraseña provisoria de acceso. 
                        Por favor, revisá tu bandeja de entrada (y la carpeta de SPAM o correo no deseado).
                    </div>`;
            }
        } else {
            if (contenedorMsg) {
                contenedorMsg.innerHTML = `<div class="mensaje mensaje-error">❌ ${resultado}</div>`;
            }
        }
    })
    .catch(err => {
        console.error(err);
        if (contenedorMsg) {
            contenedorMsg.innerHTML = `<div class="mensaje mensaje-error">❌ Error al conectar con el servidor de recuperación.</div>`;
        }
    });
}