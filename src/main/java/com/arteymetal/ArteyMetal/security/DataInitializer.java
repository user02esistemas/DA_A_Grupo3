package com.arteymetal.ArteyMetal.security;

import com.arteymetal.ArteyMetal.entity.*;
import com.arteymetal.ArteyMetal.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import java.util.*;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired private RolRepository rolRepository;
    @Autowired private PermisoRepository permisoRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private CajaRepository cajaRepository;
    @Autowired private CategoriaProductoRepository categoriaRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (usuarioRepository.count() > 0) return;

        // Crear permisos
        Map<String, Permiso> permisos = new HashMap<>();
        String[][] permisosData = {
            {"dashboard.ver", "Ver Dashboard"},
            {"pedidos.ver", "Ver Pedidos"},
            {"pedidos.gestionar", "Gestionar Pedidos"},
            {"clientes.ver", "Ver Clientes"},
            {"clientes.gestionar", "Gestionar Clientes"},
            {"productos.ver", "Ver Productos"},
            {"productos.gestionar", "Gestionar Productos"},
            {"ventas.ver", "Ver Ventas"},
            {"ventas.gestionar", "Gestionar Ventas"},
            {"caja.ver", "Ver Caja"},
            {"caja.gestionar", "Gestionar Caja"},
            {"almacen.ver", "Ver Almacen"},
            {"almacen.gestionar", "Gestionar Almacen"},
            {"usuarios.ver", "Ver Usuarios"},
            {"usuarios.gestionar", "Gestionar Usuarios"},
            {"roles.ver", "Ver Roles"},
            {"roles.gestionar", "Gestionar Roles"},
            {"reportes.ver", "Ver Reportes"},
            {"diseno.ver", "Ver Diseno"},
            {"diseno.gestionar", "Gestionar Diseno"},
            {"produccion.ver", "Ver Produccion"},
            {"produccion.gestionar", "Gestionar Produccion"},
            {"repartidor.ver", "Ver Repartidor"},
            {"repartidor.gestionar", "Gestionar Repartidor"},
            {"configuracion.ver", "Ver Configuracion"}
        };
        for (String[] p : permisosData) {
            Permiso permiso = Permiso.builder().slug(p[0]).nombre(p[1]).build();
            permisos.put(p[0], permisoRepository.save(permiso));
        }

        // Crear roles con permisos
        Rol admin = rolRepository.save(Rol.builder().nombre("administrador").descripcion("Acceso total").activo(true).build());
        Rol vendedor = rolRepository.save(Rol.builder().nombre("vendedor").descripcion("Gestion comercial").activo(true).build());
        Rol disenador = rolRepository.save(Rol.builder().nombre("disenador").descripcion("Crea disenos").activo(true).build());
        Rol orfebre = rolRepository.save(Rol.builder().nombre("orfebre").descripcion("Fabrica piezas").activo(true).build());
        Rol repartidor = rolRepository.save(Rol.builder().nombre("repartidor").descripcion("Transporte").activo(true).build());
        Rol almacenero = rolRepository.save(Rol.builder().nombre("almacenero").descripcion("Control de almacen").activo(true).build());

        // Asignar todos los permisos al admin
        admin.setPermisos(new HashSet<>(permisos.values()));
        rolRepository.save(admin);

        // Vendedor
        vendedor.setPermisos(new HashSet<>(Set.of(
            permisos.get("dashboard.ver"), permisos.get("pedidos.ver"),
            permisos.get("clientes.ver"), permisos.get("clientes.gestionar"),
            permisos.get("productos.ver"), permisos.get("ventas.ver"),
            permisos.get("ventas.gestionar"), permisos.get("caja.ver"),
            permisos.get("caja.gestionar"), permisos.get("reportes.ver")
        )));
        rolRepository.save(vendedor);

        // Disenador
        disenador.setPermisos(new HashSet<>(Set.of(
            permisos.get("dashboard.ver"), permisos.get("pedidos.ver"),
            permisos.get("pedidos.gestionar"), permisos.get("clientes.ver"),
            permisos.get("productos.ver"), permisos.get("reportes.ver"),
            permisos.get("diseno.ver"), permisos.get("diseno.gestionar")
        )));
        rolRepository.save(disenador);

        // Orfebre
        orfebre.setPermisos(new HashSet<>(Set.of(
            permisos.get("dashboard.ver"), permisos.get("pedidos.ver"),
            permisos.get("pedidos.gestionar"), permisos.get("clientes.ver"),
            permisos.get("productos.ver"), permisos.get("reportes.ver"),
            permisos.get("produccion.ver"), permisos.get("produccion.gestionar")
        )));
        rolRepository.save(orfebre);

        // Repartidor
        repartidor.setPermisos(new HashSet<>(Set.of(
            permisos.get("dashboard.ver"), permisos.get("pedidos.ver"),
            permisos.get("clientes.ver"),
            permisos.get("repartidor.ver"), permisos.get("repartidor.gestionar")
        )));
        rolRepository.save(repartidor);

        // Almacenero
        almacenero.setPermisos(new HashSet<>(Set.of(
            permisos.get("dashboard.ver"), permisos.get("clientes.ver"),
            permisos.get("productos.ver"), permisos.get("productos.gestionar"),
            permisos.get("reportes.ver"), permisos.get("almacen.ver"),
            permisos.get("almacen.gestionar")
        )));
        rolRepository.save(almacenero);

        // Crear usuarios (mismos que el seeder original de Laravel)
        usuarioRepository.save(Usuario.builder()
            .name("bvasquezkeysije").email("bvasquezkeysije@gmail.com")
            .password(passwordEncoder.encode("76636255"))
            .rol(admin).activo(true).build());
        usuarioRepository.save(Usuario.builder()
            .name("pfernandezadeli").email("pfernandezadeli@gmail.com")
            .password(passwordEncoder.encode("77684878"))
            .rol(admin).activo(true).build());
        usuarioRepository.save(Usuario.builder()
            .name("ventas").email("ventas@gmail.com")
            .password(passwordEncoder.encode("ventas123"))
            .rol(vendedor).activo(true).build());
        usuarioRepository.save(Usuario.builder()
            .name("produccion").email("produccion@gmail.com")
            .password(passwordEncoder.encode("produccion123"))
            .rol(orfebre).activo(true).build());
        usuarioRepository.save(Usuario.builder()
            .name("almacen").email("almacen@gmail.com")
            .password(passwordEncoder.encode("almacen123"))
            .rol(almacenero).activo(true).build());
        usuarioRepository.save(Usuario.builder()
            .name("disenador").email("disenador@gmail.com")
            .password(passwordEncoder.encode("disenador123"))
            .rol(disenador).activo(true).build());
        usuarioRepository.save(Usuario.builder()
            .name("repartidor").email("repartidor@gmail.com")
            .password(passwordEncoder.encode("repartidor123"))
            .rol(repartidor).activo(true).build());

        // Crear cajas
        cajaRepository.save(Caja.builder().nombre("Caja 1").activa(true).build());
        cajaRepository.save(Caja.builder().nombre("Caja 2").activa(true).build());
        cajaRepository.save(Caja.builder().nombre("Caja 3").activa(true).build());

        // Crear categorias
        categoriaRepository.save(CategoriaProducto.builder().slug("anillos").nombre("Anillos").activo(true).build());
        categoriaRepository.save(CategoriaProducto.builder().slug("collares").nombre("Collares").activo(true).build());
        categoriaRepository.save(CategoriaProducto.builder().slug("pulseras").nombre("Pulseras").activo(true).build());
        categoriaRepository.save(CategoriaProducto.builder().slug("aros").nombre("Aros").activo(true).build());
        categoriaRepository.save(CategoriaProducto.builder().slug("relojes").nombre("Relojes").activo(true).build());
        categoriaRepository.save(CategoriaProducto.builder().slug("accesorios").nombre("Accesorios").activo(true).build());

        System.out.println("=== Datos iniciales creados correctamente ===");
        System.out.println("Admin: bvasquezkeysije@gmail.com / 76636255");
        System.out.println("Vendedor: ventas@gmail.com / ventas123");
        System.out.println("Almacen: almacen@gmail.com / almacen123");
    }
}
