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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.*;

@Controller
@RequestMapping("/produccion")
public class ProduccionController {

    @Autowired
    private PedidoRepository pedidoRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @GetMapping
    public String index(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String filtroEstado,
            Model model,
            @AuthenticationPrincipal Usuario usuario) {

        Pageable pageable = PageRequest.of(page, 10, Sort.by("id").descending());

        Specification<Pedido> spec = (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();

            List<String> estados = Arrays.asList("en_produccion", "produciendo");
            predicates.add(root.get("estado").in(estados));

            if (filtroEstado != null && !filtroEstado.isEmpty()) {
                predicates.add(cb.equal(root.get("estado"), filtroEstado));
            }

            if (q != null && !q.isEmpty()) {
                String patron = "%" + q.toLowerCase() + "%";
                predicates.add(cb.or(
                    cb.like(cb.lower(root.get("codigo")), patron),
                    cb.like(cb.lower(root.get("cliente").get("nombre")), patron)
                ));
            }

            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };

        Page<Pedido> pedidos = pedidoRepository.findAll(spec, pageable);

        model.addAttribute("pedidos", pedidos);
        model.addAttribute("busqueda", q);
        model.addAttribute("filtroEstado", filtroEstado);
        model.addAttribute("titulo", "Produccion");

        return "produccion/index";
    }

    @GetMapping("/{id}")
    public String show(@PathVariable Long id, Model model, @AuthenticationPrincipal Usuario usuario, RedirectAttributes flash) {

        Optional<Pedido> optionalPedido = pedidoRepository.findById(id);
        if (optionalPedido.isEmpty()) {
            flash.addFlashAttribute("error", "Pedido no encontrado");
            return "redirect:/produccion";
        }

        Pedido pedido = optionalPedido.get();
        String estado = pedido.getEstado();
        if (estado == null || (!estado.equals("en_produccion") && !estado.equals("produciendo"))) {
            flash.addFlashAttribute("error", "Estado del pedido no válido para producción");
            return "redirect:/produccion";
        }

        model.addAttribute("pedido", pedido);
        model.addAttribute("titulo", "Produccion - " + pedido.getCodigo());

        return "produccion/show";
    }

    @PostMapping("/{id}/iniciar")
    public String iniciarProduccion(@PathVariable Long id, @AuthenticationPrincipal Usuario usuario, RedirectAttributes flash) {

        String nombreRol = usuario.getRol() != null ? usuario.getRol().getNombre() : "";
        if (!nombreRol.equals("administrador") && !nombreRol.equals("orfebre")) {
            flash.addFlashAttribute("error", "No tiene permisos para realizar esta acción");
            return "redirect:/produccion";
        }

        Optional<Pedido> optionalPedido = pedidoRepository.findById(id);
        if (optionalPedido.isEmpty()) {
            flash.addFlashAttribute("error", "Pedido no encontrado");
            return "redirect:/produccion";
        }

        Pedido pedido = optionalPedido.get();

        if (!"en_produccion".equals(pedido.getEstado())) {
            flash.addFlashAttribute("error", "El pedido no se encuentra en estado 'en producción'");
            return "redirect:/produccion/" + id;
        }

        pedido.setEstado("produciendo");
        pedidoRepository.save(pedido);

        if (pedido.getUsuario() != null) {
            notificationService.crear(pedido.getUsuario().getId(), "pedido_produccion", "Pedido en producción", "El pedido #" + pedido.getCodigo() + " ha pasado a estado 'produciendo'.", "/pedidos/" + pedido.getId());
        }

        flash.addFlashAttribute("success", "Producción iniciada correctamente");
        return "redirect:/produccion/" + id;
    }

    @PostMapping("/{id}/notificar-repartidor")
    public String notificarRepartidor(@PathVariable Long id, @AuthenticationPrincipal Usuario usuario, RedirectAttributes flash) {

        String nombreRol = usuario.getRol() != null ? usuario.getRol().getNombre() : "";
        if (!nombreRol.equals("administrador") && !nombreRol.equals("orfebre")) {
            flash.addFlashAttribute("error", "No tiene permisos para realizar esta acción");
            return "redirect:/produccion";
        }

        Optional<Pedido> optionalPedido = pedidoRepository.findById(id);
        if (optionalPedido.isEmpty()) {
            flash.addFlashAttribute("error", "Pedido no encontrado");
            return "redirect:/produccion";
        }

        Pedido pedido = optionalPedido.get();

        if (!"produciendo".equals(pedido.getEstado())) {
            flash.addFlashAttribute("error", "El pedido no se encuentra en estado 'produciendo'");
            return "redirect:/produccion/" + id;
        }

        pedido.setEstado("listo_entrega");
        pedidoRepository.save(pedido);

        List<Usuario> usuarios = usuarioRepository.findAll();

        for (Usuario u : usuarios) {
            if (u.getRol() != null) {
                String rolNombre = u.getRol().getNombre();
                if (rolNombre.equals("repartidor")) {
                    notificationService.crear(u.getId(), "pedido_listo", "Pedido listo", "El pedido #" + pedido.getCodigo() + " está listo para entrega.", "/pedidos/" + pedido.getId());
                }
            }
        }

        if (pedido.getUsuario() != null) {
            notificationService.crear(pedido.getUsuario().getId(), "pedido_listo", "Pedido listo", "Su pedido #" + pedido.getCodigo() + " está listo para entrega.", "/pedidos/" + pedido.getId());
        }

        flash.addFlashAttribute("success", "Notificación de entrega enviada correctamente");
        return "redirect:/produccion/" + id;
    }
}
