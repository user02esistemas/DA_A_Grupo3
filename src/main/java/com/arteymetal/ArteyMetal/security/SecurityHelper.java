package com.arteymetal.ArteyMetal.security;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import com.arteymetal.ArteyMetal.entity.Usuario;

@Component("securityHelper")
public class SecurityHelper {

    public static Usuario getUsuarioActual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Usuario) {
            return (Usuario) auth.getPrincipal();
        }
        return null;
    }

    public static boolean tienePermiso(String permiso) {
        Usuario usuario = getUsuarioActual();
        if (usuario == null) return false;
        return usuario.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals(permiso));
    }

    public static void verificarPermiso(String permiso) {
        if (!tienePermiso(permiso)) {
            throw new AccessDeniedException("No tiene permiso: " + permiso);
        }
    }
}
