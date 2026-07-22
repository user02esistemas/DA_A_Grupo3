package com.arteymetal.ArteyMetal.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalModelAdvice {

    @ModelAttribute("requestURI")
    public String requestURI(HttpServletRequest request) {
        return request.getRequestURI();
    }

    @ModelAttribute("titulo")
    public String titulo(HttpServletRequest request) {
        String uri = request.getRequestURI();
        if (uri.startsWith("/caja")) return "Caja";
        if (uri.startsWith("/ventas")) return "Ventas";
        if (uri.startsWith("/pedidos")) return "Pedidos";
        if (uri.startsWith("/productos")) return "Productos";
        if (uri.startsWith("/clientes")) return "Clientes";
        if (uri.startsWith("/diseno")) return "Diseños";
        if (uri.startsWith("/produccion")) return "Produccion";
        if (uri.startsWith("/repartidor")) return "Repartos";
        if (uri.startsWith("/almacen")) return "Almacen";
        if (uri.startsWith("/usuarios")) return "Usuarios";
        if (uri.startsWith("/roles")) return "Roles";
        if (uri.startsWith("/reportes")) return "Reportes";
        if (uri.startsWith("/notificaciones")) return "Notificaciones";
        if (uri.startsWith("/perfil")) return "Configuracion";
        return "Inicio";
    }
}
