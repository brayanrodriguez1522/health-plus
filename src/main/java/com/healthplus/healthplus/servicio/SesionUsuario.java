package com.healthplus.healthplus.servicio;

import com.healthplus.healthplus.modelo.Usuario;
import jakarta.servlet.http.HttpSession;

/**
 * Un solo lugar para "iniciar sesión" y "cerrar sesión".
 *
 * La sesión (HttpSession) es una especie de "bolsillo" que el servidor guarda
 * para cada navegador. Ahí guardamos el id y el nombre del usuario que entró.
 * Todos los controladores preguntan session.getAttribute("idUsuario"):
 * si es null, nadie ha iniciado sesión y se manda al login.
 */
public final class SesionUsuario {

    public static final String ID = "idUsuario";
    public static final String NOMBRE = "nombre";

    private SesionUsuario() {
    }

    public static void iniciar(HttpSession session, Usuario u) {
        session.setAttribute(ID, u.getIdUsuario());
        session.setAttribute(NOMBRE, u.getNombre());
    }

    public static Long id(HttpSession session) {

        return (Long) session.getAttribute(ID);
    }
}
