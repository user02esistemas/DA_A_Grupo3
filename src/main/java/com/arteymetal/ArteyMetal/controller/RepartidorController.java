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
@RequestMapping("/repartidor")
public class RepartidorController {

    @Autowired
    private PedidoRepository pedidoRepository;

    @Autowired
    private PedidoProductoRepository pedidoProductoRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private MovimientoAlmacenRepository movimientoAlmacenRepository;

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

            List<String> estados = Arrays.asList("listo_entrega", "en_transporte");
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
        model.addAttribute("titulo", "Repartos");

        return "repartidor/index";
    }

    @GetMapping("/{id}")
    public String show(@PathVariable Long id, Model model, @AuthenticationPrincipal Usuario usuario, RedirectAttributes flash) {

        Optional<Pedido> optionalPedido = pedidoRepository.findById(id);
        if (optionalPedido.isEmpty()) {
            flash.addFlashAttribute("error", "Pedido no encontrado");
            return "redirect:/repartidor";
        }

        Pedido pedido = optionalPedido.get();
        String estado = pedido.getEstado();
        if (estado == null || (!estado.equals("listo_entrega") && !estado.equals("en_transporte"))) {
            flash.addFlashAttribute("error", "Estado del pedido no válido para repartidor");
            return "redirect:/repartidor";
        }

        model.addAttribute("pedido", pedido);
        model.addAttribute("titulo", "Reparto - " + pedido.getCodigo());

        return "repartidor/show";
    }

    @PostMapping("/{id}/recoger")
    public String recoger(
            @PathVariable Long id,
            @RequestParam Map<String, String> params,
            @AuthenticationPrincipal Usuario usuario,
            RedirectAttributes flash) {

        String nombreRol = usuario.getRol() != null ? usuario.getRol().getNombre() : "";
        if (!nombreRol.equals("repartidor") && !nombreRol.equals("administrador")) {
            flash.addFlashAttribute("error", "No tiene permisos para realizar esta acción");
            return "redirect:/repartidor";
        }

        Optional<Pedido> optionalPedido = pedidoRepository.findById(id);
        if (optionalPedido.isEmpty()) {
            flash.addFlashAttribute("error", "Pedido no encontrado");
            return "redirect:/repartidor";
        }

        Pedido pedido = optionalPedido.get();

        if (!"listo_entrega".equals(pedido.getEstado())) {
            flash.addFlashAttribute("error", "El pedido no se encuentra en estado 'listo para entrega'");
            return "redirect:/repartidor/" + id;
        }

        List<PedidoProducto> productos = pedidoProductoRepository.findByPedidoId(pedido.getId());

        for (PedidoProducto pp : productos) {
            String cantidadKey = "productos[" + pp.getId() + "].cantidad_recoge";
            String cantidadStr = params.get(cantidadKey);

            if (cantidadStr != null && !cantidadStr.isEmpty()) {
                try {
                    int cantidadRecoge = Integer.parseInt(cantidadStr);
                    int cantidadMaxima = pp.getCantidad();
                    int cantidadFinal = Math.min(cantidadRecoge, cantidadMaxima);
                    pp.setCantidadRecoge(cantidadFinal);
                    pedidoProductoRepository.save(pp);
                } catch (NumberFormatException e) {
                    flash.addFlashAttribute("error", "Cantidad inválida para el producto");
                    return "redirect:/repartidor/" + id;
                }
            }
        }

        pedido.setEstado("en_transporte");
        pedidoRepository.save(pedido);

        flash.addFlashAttribute("success", "Pedido recogido correctamente. En transporte.");
        return "redirect:/repartidor/" + id;
    }

    @PostMapping("/{id}/entregar-almacen")
    public String entregarAlmacen(@PathVariable Long id, @AuthenticationPrincipal Usuario usuario, RedirectAttributes flash) {

        String nombreRol = usuario.getRol() != null ? usuario.getRol().getNombre() : "";
        if (!nombreRol.equals("repartidor") && !nombreRol.equals("administrador")) {
            flash.addFlashAttribute("error", "No tiene permisos para realizar esta acción");
            return "redirect:/repartidor";
        }

        Optional<Pedido> optionalPedido = pedidoRepository.findById(id);
        if (optionalPedido.isEmpty()) {
            flash.addFlashAttribute("error", "Pedido no encontrado");
            return "redirect:/repartidor";
        }

        Pedido pedido = optionalPedido.get();

        if (!"en_transporte".equals(pedido.getEstado())) {
            flash.addFlashAttribute("error", "El pedido no se encuentra en estado 'en transporte'");
            return "redirect:/repartidor/" + id;
        }

        pedido.setEstado("en_almacen");
        pedidoRepository.save(pedido);

        List<Usuario> usuarios = usuarioRepository.findAll();

        for (Usuario u : usuarios) {
            if (u.getRol() != null) {
                String rolNombre = u.getRol().getNombre();
                if (rolNombre.equals("almacenero")) {
                    notificationService.crear(u.getId(), "pedido_almacen", "Pedido en almacén", "El pedido #" + pedido.getCodigo() + " ha llegado al almacén.", "/pedidos/" + pedido.getId());
                }
            }
        }

        if (pedido.getUsuario() != null) {
            notificationService.crear(pedido.getUsuario().getId(), "pedido_almacen", "Pedido entregado", "Su pedido #" + pedido.getCodigo() + " ha sido entregado al almacén.", "/pedidos/" + pedido.getId());
        }

        flash.addFlashAttribute("success", "Pedido entregado al almacén correctamente");
        return "redirect:/repartidor/" + id;
    }
}
