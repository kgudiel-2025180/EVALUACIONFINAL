package com.Evalucion.Kenny_EVAFINAL.enums;

/**
 * Roles del sistema. Se persisten como cadena en la columna {@code usuarios.rol}
 * y se validan con un CHECK en base de datos (ver db/schema.sql).
 */
public enum Rol {

    ADMIN,
    BIBLIOTECARIO,
    LECTOR;

    /**
     * Prefijo de autoridad que Spring Security espera para {@code hasRole(...)}.
     */
    public String getAuthority() {
        return "ROLE_" + name();
    }
}
