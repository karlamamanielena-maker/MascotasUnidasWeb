document.addEventListener("DOMContentLoaded", function() {
    
    // 1. CAPTURAR MENSAJES DE ÉXITO PENDIENTES DE REDIRECCIÓN
    const mensajePendiente = sessionStorage.getItem("mensajeExitoIndex");
    
    if (mensajePendiente) {
        let contenedorMsgGlobal = document.getElementById("contenedorMensaje");
        
        if (!contenedorMsgGlobal) {
            contenedorMsgGlobal = document.createElement("div");
            contenedorMsgGlobal.id = "contenedorMensaje";
            contenedorMsgGlobal.style.padding = "0 5%"; 
            contenedorMsgGlobal.style.marginTop = "20px";
            
            const header = document.querySelector("header");
            if (header) {
                header.insertAdjacentElement("afterend", contenedorMsgGlobal);
            } else {
                document.body.insertBefore(contenedorMsgGlobal, document.body.firstChild);
            }
        }
        
        contenedorMsgGlobal.innerHTML = `
            <div class="mensaje mensaje-exito">
                ${mensajePendiente}
            </div>
        `;
        
        sessionStorage.removeItem("mensajeExitoIndex");
    }
    
    // 2. CONTROL DINÁMICO DEL NAVBAR (USUARIO CONECTADO / INVITADO)
    const authContainer = document.getElementById("auth-container");
    
    if (authContainer) {
        fetch("verificar-sesion")
        .then(response => {
            if (!response.ok) throw new Error("Error al verificar sesión");
            return response.json();
        })
        .then(data => {
            // Evaluamos si el backend devolvió una sesión activa válida
            if (data.logueado) {
                console.log("Usuario logueado, inyectando menú...");
                
                authContainer.innerHTML = `
                    <div class="user-dropdown" id="userDropdown">
                        <button class="dropdown-toggle" id="dropdownBtn">
                            <i class="fa-solid fa-user"></i> Hola, ${data.nombre} <i class="fa-solid fa-chevron-down" style="font-size: 12px;"></i>
                        </button>
                        <div class="dropdown-menu" id="dropdownMenu">
                            <a href="mis-publicaciones.html"><i class="fa-solid fa-folder-open"></i> Ver mis publicaciones</a>
                            <a href="#" id="btn-logout" style="border-top: 1px solid #eee;"><i class="fa-solid fa-right-from-bracket"></i> Cerrar sesión</a>
                        </div>
                    </div>
                `;
                
                // CONTROL DEL MENÚ DESPLEGABLE (Muestra/Oculta al hacer clic)
                const dropdownBtn = document.getElementById("dropdownBtn");
                const dropdownMenu = document.getElementById("dropdownMenu");
                
                if (dropdownBtn && dropdownMenu) {
                    dropdownBtn.addEventListener("click", (e) => {
                        e.stopPropagation(); // Evita que el clic se propague al documento
                        const isVisible = dropdownMenu.style.display === "block";
                        dropdownMenu.style.display = isVisible ? "none" : "block";
                    });
                    
                    // Si el usuario hace clic fuera del menú, este se cierra automáticamente
                    document.addEventListener("click", () => {
                        dropdownMenu.style.display = "none";
                    });
                }
                
                // MANEJO ASÍNCRONO DEL LOGOUT
                const btnLogout = document.getElementById("btn-logout");
                if (btnLogout) {
                    btnLogout.addEventListener("click", function(e) {
                        e.preventDefault();
                        fetch("logout")
                        .then(() => {
                            window.location.reload(); // Recarga la página para volver al estado invitado
                        })
                        .catch(err => console.error("Error al cerrar sesión:", err));
                    });
                }
            }
        })
        .catch(error => console.error("Error verificando sesión:", error));
    }
});