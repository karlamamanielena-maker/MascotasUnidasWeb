document.getElementById("formRegistro").addEventListener("submit", function(e) {
    e.preventDefault(); // Detiene el envío tradicional

    const contenedorMsg = document.getElementById("contenedorMensaje");
    contenedorMsg.innerHTML = ""; // Limpiar mensajes anteriores

    // 1. Validar contraseñas en el cliente
    const pass = document.getElementById("password").value;
    const confirmPass = document.getElementById("confirmarPassword").value;

    if (pass !== confirmPass) {
        contenedorMsg.innerHTML = `<div class="mensaje mensaje-error">❌ Las contraseñas no coinciden.</div>`;
        return; 
    }

    // 2. Capturar los datos del formulario
    const formData = new URLSearchParams(new FormData(this));

    // 3. Petición asíncrona al Servlet
    fetch("registro", {
        method: "POST",
        headers: {
            "Content-Type": "application/x-www-form-urlencoded"
        },
        body: formData.toString()
    })
    .then(response => {
        if (!response.ok) {
            throw new Error("Error en la respuesta del servidor");
        }
        return response.text();
    })
    .then(resultado => {
        if (resultado.trim() === "OK") {
            contenedorMsg.innerHTML = `<div class="mensaje mensaje-exito">✅ Cuenta registrada correctamente. Redirigiendo...</div>`;
            document.getElementById("formRegistro").reset(); 
            
            setTimeout(() => {
                window.location.href = "login.html";
            }, 2000);
        } else {
            contenedorMsg.innerHTML = `<div class="mensaje mensaje-error">❌ ${resultado}</div>`;
        }
    })
    .catch(error => {
        console.error("Error:", error);
        contenedorMsg.innerHTML = `<div class="mensaje mensaje-error">❌ Error de conexión. Intente más tarde.</div>`;
    });
});