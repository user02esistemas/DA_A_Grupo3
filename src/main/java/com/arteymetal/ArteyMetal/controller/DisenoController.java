package com.arteymetal.ArteyMetal.controller;

import com.arteymetal.ArteyMetal.entity.*;
import com.arteymetal.ArteyMetal.repository.*;
import com.arteymetal.ArteyMetal.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import org.springframework.transaction.annotation.Transactional;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@Controller
@RequestMapping("/diseno")
public class DisenoController {

    @Autowired
    private PedidoRepository pedidoRepository;

    @Autowired
    private PedidoProductoRepository pedidoProductoRepository;

    @Autowired
    private PedidoDisenoArchivoRepository pedidoDisenoArchivoRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    private final String UPLOAD_DIR = "uploads/diseno/";

    @GetMapping
    @Transactional(readOnly = true)
    public String index(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String filtroEstado,
            Model model,
            @AuthenticationPrincipal Usuario usuario) {

        Pageable pageable = PageRequest.of(page, 10, Sort.by("id").descending());

        Specification<Pedido> spec = (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();

            List<String> estados = Arrays.asList("en_diseno", "en_revision");
            predicates.add(root.get("estadoPersonalizacion").in(estados));

            if (filtroEstado != null && !filtroEstado.isEmpty()) {
                predicates.add(cb.equal(root.get("estadoPersonalizacion"), filtroEstado));
            }

            if (q != null && !q.isEmpty()) {
                String patron = "%" + q.toLowerCase() + "%";
                jakarta.persistence.criteria.Subquery<PedidoProducto> sub = query.subquery(PedidoProducto.class);
                var subRoot = sub.from(PedidoProducto.class);
                sub.select(subRoot);
                sub.where(
                    cb.and(
                        cb.equal(subRoot.get("pedido"), root),
                        cb.or(
                            cb.like(cb.lower(subRoot.get("producto").get("nombre")), patron)
                        )
                    )
                );

                predicates.add(cb.or(
                    cb.like(cb.lower(root.get("codigo")), patron),
                    cb.like(cb.lower(root.get("cliente").get("nombre")), patron),
                    cb.exists(sub)
                ));
            }

            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };

        Page<Pedido> pedidos = pedidoRepository.findAll(spec, pageable);

        Map<Long, Map<String, Object>> modelosMap = new LinkedHashMap<>();
        for (Pedido p : pedidos) {
            if (p.getArchivosDiseno() != null) p.getArchivosDiseno().size();
            if (p.getProductos() != null) {
                p.getProductos().size();
                for (PedidoProducto pp : p.getProductos()) {
                    if (pp.getArchivosDiseno() != null) pp.getArchivosDiseno().size();
                }
            }
            Map<String, Object> info = new LinkedHashMap<>();
            List<PedidoDisenoArchivo> archivos = p.getArchivosDiseno() != null ? p.getArchivosDiseno() : List.of();
            List<Map<String, Object>> refFiles = new ArrayList<>();
            List<Map<String, Object>> disFiles = new ArrayList<>();
            for (PedidoDisenoArchivo a : archivos) {
                Map<String, Object> fm = new LinkedHashMap<>();
                fm.put("nombre", a.getNombreOriginal() != null ? a.getNombreOriginal() : "archivo");
                fm.put("url", "/uploads/" + (a.getArchivoPath() != null ? a.getArchivoPath() : ""));
                fm.put("tipo", a.getTipo() != null ? a.getTipo() : "");
                fm.put("tamano", a.getTamanoBytes() != null ? a.getTamanoBytes() : 0L);
                if ("Reference".equalsIgnoreCase(a.getTipo())) {
                    refFiles.add(fm);
                } else {
                    disFiles.add(fm);
                }
            }
            info.put("modelosRef", refFiles);
            info.put("modelosDis", disFiles);

            List<Map<String, Object>> prods = new ArrayList<>();
            if (p.getProductos() != null) {
                for (PedidoProducto pp : p.getProductos()) {
                    Map<String, Object> pm = new LinkedHashMap<>();
                    pm.put("id", pp.getId());
                    pm.put("nombre", pp.getNombre() != null ? pp.getNombre() : "");
                    int countDiseno = 0;
                    int countRef = 0;
                    if (pp.getArchivosDiseno() != null) {
                        for (PedidoDisenoArchivo da : pp.getArchivosDiseno()) {
                            if ("Reference".equalsIgnoreCase(da.getTipo())) countRef++;
                            else countDiseno++;
                        }
                    }
                    pm.put("countDiseno", countDiseno);
                    pm.put("countRef", countRef);
                    prods.add(pm);
                }
            }
            info.put("productos", prods);
            modelosMap.put(p.getId(), info);
        }

        model.addAttribute("pedidos", pedidos);
        model.addAttribute("busqueda", q);
        model.addAttribute("filtroEstado", filtroEstado);
        model.addAttribute("titulo", "Diseños");
        model.addAttribute("modelosMap", modelosMap);

        return "diseno/index";
    }

    @GetMapping("/{id}")
    public String show(@PathVariable Long id, Model model, @AuthenticationPrincipal Usuario usuario, RedirectAttributes flash) {

        Optional<Pedido> optionalPedido = pedidoRepository.findById(id);
        if (optionalPedido.isEmpty()) {
            flash.addFlashAttribute("error", "Pedido no encontrado");
            return "redirect:/diseno";
        }

        Pedido pedido = optionalPedido.get();
        String estado = pedido.getEstadoPersonalizacion();
        if (estado == null || (!estado.equals("en_diseno") && !estado.equals("en_revision"))) {
            flash.addFlashAttribute("error", "Estado del pedido no válido para diseño");
            return "redirect:/diseno";
        }

        pedido.setProductos(pedidoProductoRepository.findByPedidoId(pedido.getId()));
        pedido.setArchivosDiseno(pedidoDisenoArchivoRepository.findByPedidoId(pedido.getId()));

        model.addAttribute("pedido", pedido);
        model.addAttribute("titulo", "Diseño - " + pedido.getCodigo());

        return "diseno/show";
    }

    @PostMapping("/{id}")
    public String update(
            @PathVariable Long id,
            @RequestParam(required = false) Long pedido_producto_id,
            @RequestParam String estado_personalizacion,
            @RequestParam("archivos_diseno[]") MultipartFile[] archivosDiseno,
            @AuthenticationPrincipal Usuario usuario,
            RedirectAttributes flash) {

        String nombreRol = usuario.getRol() != null ? usuario.getRol().getNombre() : "";
        if (!nombreRol.equals("administrador") && !nombreRol.equals("disenador")) {
            flash.addFlashAttribute("error", "No tiene permisos para realizar esta acción");
            return "redirect:/diseno";
        }

        Optional<Pedido> optionalPedido = pedidoRepository.findById(id);
        if (optionalPedido.isEmpty()) {
            flash.addFlashAttribute("error", "Pedido no encontrado");
            return "redirect:/diseno";
        }

        Pedido pedido = optionalPedido.get();

        if (pedido_producto_id == null) {
            flash.addFlashAttribute("error", "El campo pedido_producto_id es obligatorio");
            return "redirect:/diseno/" + id;
        }

        if (estado_personalizacion == null || (!estado_personalizacion.equals("en_diseno") && !estado_personalizacion.equals("en_revision"))) {
            flash.addFlashAttribute("error", "Estado de personalización no válido");
            return "redirect:/diseno/" + id;
        }

        if (archivosDiseno == null || archivosDiseno.length < 1) {
            flash.addFlashAttribute("error", "Debe subir al menos un archivo de diseño");
            return "redirect:/diseno/" + id;
        }

        pedido.setEstadoPersonalizacion(estado_personalizacion);
        pedidoRepository.save(pedido);

        Path uploadPath = Paths.get(UPLOAD_DIR);
        try {
            Files.createDirectories(uploadPath);
        } catch (IOException e) {
            flash.addFlashAttribute("error", "Error al crear directorio de uploads");
            return "redirect:/diseno/" + id;
        }

        for (MultipartFile archivo : archivosDiseno) {
            if (!archivo.isEmpty()) {
                try {
                    String nombreOriginal = archivo.getOriginalFilename();
                    String nombreUnico = UUID.randomUUID() + "_" + nombreOriginal;
                    Path destino = uploadPath.resolve(nombreUnico);
                    archivo.transferTo(destino.toFile());

                    PedidoDisenoArchivo disenoArchivo = new PedidoDisenoArchivo();
                    disenoArchivo.setPedido(pedido);
                    disenoArchivo.setNombreOriginal(nombreOriginal);
                    disenoArchivo.setArchivoPath(UPLOAD_DIR + nombreUnico);
                    disenoArchivo.setTipo(archivo.getContentType());
                    disenoArchivo.setMimeType(archivo.getContentType());
                    disenoArchivo.setTamanoBytes(archivo.getSize());
                    pedidoDisenoArchivoRepository.save(disenoArchivo);
                } catch (IOException e) {
                    flash.addFlashAttribute("error", "Error al guardar el archivo: " + archivo.getOriginalFilename());
                    return "redirect:/diseno/" + id;
                }
            }
        }

        flash.addFlashAttribute("success", "Diseño actualizado correctamente");
        return "redirect:/diseno/" + id;
    }

    @PostMapping("/{id}/eliminar-archivo/{archivoId}")
    @ResponseBody
    public ResponseEntity<?> destroyArchivo(
            @PathVariable Long id,
            @PathVariable Long archivoId,
            @RequestHeader(value = "X-Requested-With", required = false) String requestedWith,
            @AuthenticationPrincipal Usuario usuario) {

        if (usuario == null) {
            if ("XMLHttpRequest".equals(requestedWith)) {
                return ResponseEntity.status(401).body(Map.of("error", "No autenticado"));
            }
            return ResponseEntity.status(302).build();
        }

        String nombreRol = usuario.getRol() != null ? usuario.getRol().getNombre() : "";
        if (!nombreRol.equals("administrador") && !nombreRol.equals("disenador")) {
            if ("XMLHttpRequest".equals(requestedWith)) {
                return ResponseEntity.status(403).body(Map.of("error", "Sin permisos"));
            }
            return ResponseEntity.status(302).build();
        }

        Optional<PedidoDisenoArchivo> optionalArchivo = pedidoDisenoArchivoRepository.findById(archivoId);
        if (optionalArchivo.isEmpty()) {
            if ("XMLHttpRequest".equals(requestedWith)) {
                return ResponseEntity.status(404).body(Map.of("error", "Archivo no encontrado"));
            }
            return ResponseEntity.status(302).build();
        }

        PedidoDisenoArchivo archivo = optionalArchivo.get();

        Path rutaArchivo = Paths.get(archivo.getArchivoPath());
        try {
            Files.deleteIfExists(rutaArchivo);
        } catch (IOException e) {
            // silently ignore
        }

        pedidoDisenoArchivoRepository.delete(archivo);

        if ("XMLHttpRequest".equals(requestedWith)) {
            return ResponseEntity.ok(Map.of("success", true, "message", "Archivo eliminado correctamente"));
        }

        return ResponseEntity.status(302).header("Location", "/diseno/" + id).build();
    }

    @PostMapping("/{id}/notificar")
    public String notificar(@PathVariable Long id, @AuthenticationPrincipal Usuario usuario, RedirectAttributes flash) {

        String nombreRol = usuario.getRol() != null ? usuario.getRol().getNombre() : "";
        if (!nombreRol.equals("administrador") && !nombreRol.equals("disenador")) {
            flash.addFlashAttribute("error", "No tiene permisos para realizar esta acción");
            return "redirect:/diseno";
        }

        Optional<Pedido> optionalPedido = pedidoRepository.findById(id);
        if (optionalPedido.isEmpty()) {
            flash.addFlashAttribute("error", "Pedido no encontrado");
            return "redirect:/diseno";
        }

        Pedido pedido = optionalPedido.get();
        List<Usuario> usuarios = usuarioRepository.findAll();

        for (Usuario u : usuarios) {
            if (u.getRol() != null) {
                String rolNombre = u.getRol().getNombre();
                if (rolNombre.equals("administrador") || rolNombre.equals("vendedor")) {
                    notificationService.crear(u.getId(), "pedido_diseno", "Diseño actualizado", "El diseño del pedido #" + pedido.getCodigo() + " ha sido actualizado.", "/pedidos/" + pedido.getId());
                }
            }
        }

        flash.addFlashAttribute("success", "Notificaciones enviadas correctamente");
        return "redirect:/diseno/" + id;
    }
}
