package com.arteymetal.ArteyMetal.controller;

import com.arteymetal.ArteyMetal.entity.*;
import com.arteymetal.ArteyMetal.repository.*;
import com.arteymetal.ArteyMetal.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Controller
@RequestMapping("/almacen")
public class AlmacenController {

    @Autowired private ProductoService productoService;
    @Autowired private PedidoRepository pedidoRepository;
    @Autowired private ProductoRepository productoRepository;
    @Autowired private MovimientoAlmacenRepository movimientoAlmacenRepository;
    @Autowired private PedidoProductoRepository pedidoProductoRepository;
    @Autowired private NotificationService notificationService;
    @Autowired private VentaRepository ventaRepository;

    @GetMapping
    public String index(Model model,
                        @RequestParam(defaultValue = "") String q,
                        @RequestParam(defaultValue = "") String qe,
                        @RequestParam(defaultValue = "0") int pagePendientes,
                        @RequestParam(defaultValue = "0") int pageEntrega) {
        Pageable pageablePendientes = PageRequest.of(pagePendientes, 10);
        Pageable pageableEntrega = PageRequest.of(pageEntrega, 10);

        Page<Pedido> pedidosPendientes = pedidoRepository.searchWithFilters(q, "en_almacen", "", pageablePendientes);
        Page<Pedido> pedidosEntrega = pedidoRepository.searchWithFilters(qe, "listo_recoger", "", pageableEntrega);

        Long totalStock = productoRepository.sumStockActual();

        LocalDate hoy = LocalDate.now();
        LocalDateTime inicioHoy = hoy.atStartOfDay();
        LocalDateTime finHoy = hoy.plusDays(1).atStartOfDay();
        Long entradasHoy = movimientoAlmacenRepository.sumCantidadByTipoAndFecha("entrada", inicioHoy, finHoy);
        Long salidasHoy = movimientoAlmacenRepository.sumCantidadByTipoAndFecha("salida", inicioHoy, finHoy);

        Page<MovimientoAlmacen> movimientosPage = movimientoAlmacenRepository.findAllByOrderByIdDesc(PageRequest.of(0, 15));
        List<MovimientoAlmacen> movimientos = movimientosPage.getContent();

        model.addAttribute("pedidosPendientes", pedidosPendientes);
        model.addAttribute("pedidosEntrega", pedidosEntrega);
        model.addAttribute("q", q);
        model.addAttribute("qe", qe);
        model.addAttribute("totalStock", totalStock != null ? totalStock : 0L);
        model.addAttribute("entradasHoy", entradasHoy != null ? entradasHoy : 0L);
        model.addAttribute("salidasHoy", salidasHoy != null ? salidasHoy : 0L);
        model.addAttribute("movimientos", movimientos);
        return "almacen/index";
    }

    @GetMapping("/productos")
    public String productos(Model model,
                            @RequestParam(defaultValue = "") String q,
                            @RequestParam(defaultValue = "") String categoria,
                            @RequestParam(required = false) String stock,
                            @RequestParam(defaultValue = "0") int page) {
        Pageable pageable = PageRequest.of(page, 10);

        Page<Producto> productos;
        if (stock != null && !stock.isEmpty()) {
            List<Producto> todos = productoRepository.findAll();
            List<Producto> filtrados = new ArrayList<>();
            for (Producto p : todos) {
                boolean coincideBusqueda = q.isEmpty() ||
                    p.getNombre().toLowerCase().contains(q.toLowerCase()) ||
                    (p.getCodigo() != null && p.getCodigo().toLowerCase().contains(q.toLowerCase()));
                boolean coincideCategoria = categoria.isEmpty() ||
                    (p.getCategoria() != null && p.getCategoria().equalsIgnoreCase(categoria));
                boolean coincideStock = false;
                int stockActual = p.getStockActual() != null ? p.getStockActual() : 0;
                switch (stock) {
                    case "bajo": coincideStock = stockActual > 0 && stockActual <= 5; break;
                    case "sin": coincideStock = stockActual == 0; break;
                    case "con": coincideStock = stockActual > 0; break;
                    default: coincideStock = true;
                }
                if (coincideBusqueda && coincideCategoria && coincideStock) {
                    filtrados.add(p);
                }
            }
            int start = (int) pageable.getOffset();
            int end = Math.min(start + pageable.getPageSize(), filtrados.size());
            List<Producto> pagina = start < filtrados.size() ? filtrados.subList(start, end) : Collections.emptyList();
            productos = new org.springframework.data.domain.PageImpl<>(pagina, pageable, filtrados.size());
        } else {
            productos = productoRepository.searchWithFilters(q, categoria, null, pageable);
        }

        List<Producto> todosProductos = productoRepository.findAll();

        model.addAttribute("productos", productos);
        model.addAttribute("todosProductos", todosProductos);
        model.addAttribute("busqueda", q);
        model.addAttribute("filtroCategoria", categoria);
        model.addAttribute("filtroStock", stock);
        return "almacen/productos";
    }

    @GetMapping("/movimientos")
    public String movimientos(Model model,
                              @RequestParam(defaultValue = "") String q,
                              @RequestParam(defaultValue = "") String tipo,
                              @RequestParam(defaultValue = "0") int page) {
        Pageable pageable = PageRequest.of(page, 15);

        Page<MovimientoAlmacen> movimientos;
        if (tipo != null && !tipo.isEmpty()) {
            Page<MovimientoAlmacen> porTipo = movimientoAlmacenRepository.findByTipo(tipo, PageRequest.of(0, 10000));
            List<MovimientoAlmacen> filtrados = new ArrayList<>();
            for (MovimientoAlmacen m : porTipo.getContent()) {
                if (q.isEmpty() ||
                    (m.getConcepto() != null && m.getConcepto().toLowerCase().contains(q.toLowerCase())) ||
                    (m.getProducto() != null && m.getProducto().getNombre().toLowerCase().contains(q.toLowerCase())) ||
                    (m.getProducto() != null && m.getProducto().getCodigo() != null && m.getProducto().getCodigo().toLowerCase().contains(q.toLowerCase()))) {
                    filtrados.add(m);
                }
            }
            int start = (int) pageable.getOffset();
            int end = Math.min(start + pageable.getPageSize(), filtrados.size());
            List<MovimientoAlmacen> pagina = start < filtrados.size() ? filtrados.subList(start, end) : Collections.emptyList();
            movimientos = new org.springframework.data.domain.PageImpl<>(pagina, pageable, filtrados.size());
        } else {
            movimientos = movimientoAlmacenRepository.search(q, pageable);
        }

        List<Producto> productos = productoRepository.findAll();

        model.addAttribute("movimientos", movimientos);
        model.addAttribute("productos", productos);
        model.addAttribute("busqueda", q);
        model.addAttribute("filtroTipo", tipo);
        return "almacen/movimientos";
    }

    @GetMapping("/pedidos")
    public String pedidosPendientes(Model model,
                                    @RequestParam(defaultValue = "") String busqueda,
                                    @RequestParam(defaultValue = "") String filtroEstado,
                                    @RequestParam(defaultValue = "0") int page) {
        Pageable pageable = PageRequest.of(page, 10);

        Page<Pedido> pedidos;
        if (filtroEstado != null && !filtroEstado.isEmpty()) {
            pedidos = pedidoRepository.searchWithFilters(busqueda, filtroEstado, "", pageable);
        } else {
            List<String> estados = Arrays.asList("en_almacen", "listo_recoger", "entregado");
            List<Pedido> todos = pedidoRepository.findByEstadoIn(estados);
            List<Pedido> filtrados = new ArrayList<>();
            for (Pedido p : todos) {
                if (busqueda.isEmpty() ||
                    (p.getCodigo() != null && p.getCodigo().toLowerCase().contains(busqueda.toLowerCase())) ||
                    (p.getNombreCliente() != null && p.getNombreCliente().toLowerCase().contains(busqueda.toLowerCase())) ||
                    (p.getNombreProducto() != null && p.getNombreProducto().toLowerCase().contains(busqueda.toLowerCase()))) {
                    filtrados.add(p);
                }
            }
            int start = (int) pageable.getOffset();
            int end = Math.min(start + pageable.getPageSize(), filtrados.size());
            List<Pedido> pagina = start < filtrados.size() ? filtrados.subList(start, end) : Collections.emptyList();
            pedidos = new org.springframework.data.domain.PageImpl<>(pagina, pageable, filtrados.size());
        }

        model.addAttribute("pedidos", pedidos);
        model.addAttribute("busqueda", busqueda);
        model.addAttribute("filtroEstado", filtroEstado);
        return "almacen/pedidos";
    }

    @PostMapping("/entrada")
    @Transactional
    public String storeEntrada(@RequestParam Long producto_id,
                               @RequestParam Integer cantidad,
                               @RequestParam(defaultValue = "almacen") String ubicacion,
                               @RequestParam(required = false) String concepto,
                               @RequestParam(required = false) Long pedido_id,
                               @AuthenticationPrincipal Usuario usuario,
                               RedirectAttributes flash) {
        if (producto_id == null) {
            flash.addFlashAttribute("error", "Debe seleccionar un producto");
            return "redirect:/almacen/productos";
        }

        if (cantidad == null || cantidad < 1) {
            flash.addFlashAttribute("error", "La cantidad debe ser al menos 1");
            return "redirect:/almacen/productos";
        }

        Optional<Producto> productoOpt = productoRepository.findById(producto_id);
        if (productoOpt.isEmpty()) {
            flash.addFlashAttribute("error", "Producto no encontrado");
            return "redirect:/almacen/productos";
        }

        Producto producto = productoOpt.get();

        if ("tienda".equals(ubicacion)) {
            producto.setStockTienda(producto.getStockTienda() + cantidad);
        } else {
            producto.setStockAlmacen(producto.getStockAlmacen() + cantidad);
        }
        producto.calcularStockActual();
        productoRepository.save(producto);

        Pedido pedido = null;
        String conceptoFinal = concepto != null ? concepto : "Entrada de stock - " + ubicacion;
        if (pedido_id != null) {
            pedido = pedidoRepository.findById(pedido_id).orElse(null);
            if (pedido != null) {
                conceptoFinal = (pedido.getCodigo() != null ? pedido.getCodigo() : "PED") + " - " + conceptoFinal;
            }
        }

        MovimientoAlmacen movimiento = MovimientoAlmacen.builder()
                .producto(producto)
                .tipo("entrada")
                .cantidad(cantidad)
                .stockResultante("tienda".equals(ubicacion) ? producto.getStockTienda() : producto.getStockAlmacen())
                .concepto(conceptoFinal)
                .pedido(pedido)
                .usuario(usuario)
                .build();
        movimientoAlmacenRepository.save(movimiento);

        flash.addFlashAttribute("exito", "Entrada registrada correctamente");
        return "redirect:/almacen/productos";
    }

    @PostMapping("/salida")
    @Transactional
    public String storeSalida(@RequestParam Long producto_id,
                               @RequestParam Integer cantidad,
                               @RequestParam(defaultValue = "almacen") String ubicacion,
                               @RequestParam(required = false) String concepto,
                               @RequestParam(required = false) Long pedido_id,
                               @AuthenticationPrincipal Usuario usuario,
                               RedirectAttributes flash) {
        if (producto_id == null) {
            flash.addFlashAttribute("error", "Debe seleccionar un producto");
            return "redirect:/almacen/productos";
        }

        if (cantidad == null || cantidad < 1) {
            flash.addFlashAttribute("error", "La cantidad debe ser al menos 1");
            return "redirect:/almacen/productos";
        }

        Optional<Producto> productoOpt = productoRepository.findById(producto_id);
        if (productoOpt.isEmpty()) {
            flash.addFlashAttribute("error", "Producto no encontrado");
            return "redirect:/almacen/productos";
        }

        Producto producto = productoOpt.get();
        int stockDisponible = "tienda".equals(ubicacion) ? producto.getStockTienda() : producto.getStockAlmacen();

        if (stockDisponible < cantidad) {
            flash.addFlashAttribute("error", "Stock insuficiente. Disponible: " + stockDisponible);
            return "redirect:/almacen/productos";
        }

        if ("tienda".equals(ubicacion)) {
            producto.setStockTienda(producto.getStockTienda() - cantidad);
        } else {
            producto.setStockAlmacen(producto.getStockAlmacen() - cantidad);
        }
        producto.calcularStockActual();
        productoRepository.save(producto);

        Pedido pedido = null;
        String conceptoFinal = concepto != null ? concepto : "Salida de stock - " + ubicacion;
        if (pedido_id != null) {
            pedido = pedidoRepository.findById(pedido_id).orElse(null);
            if (pedido != null) {
                conceptoFinal = (pedido.getCodigo() != null ? pedido.getCodigo() : "PED") + " - " + conceptoFinal;
            }
        }

        MovimientoAlmacen movimiento = MovimientoAlmacen.builder()
                .producto(producto)
                .tipo("salida")
                .cantidad(cantidad)
                .stockResultante("tienda".equals(ubicacion) ? producto.getStockTienda() : producto.getStockAlmacen())
                .concepto(conceptoFinal)
                .pedido(pedido)
                .usuario(usuario)
                .build();
        movimientoAlmacenRepository.save(movimiento);

        flash.addFlashAttribute("exito", "Salida registrada correctamente");
        return "redirect:/almacen/productos";
    }

    @PostMapping("/{id}/recibir")
    public String recibirPedido(@PathVariable Long id,
                                @AuthenticationPrincipal Usuario usuario,
                                RedirectAttributes flash) {
        String rolNombre = usuario.getRol() != null ? usuario.getRol().getNombre().toLowerCase() : "";
        if (!rolNombre.contains("almacenero") && !rolNombre.contains("admin")) {
            flash.addFlashAttribute("error", "No tiene permisos para realizar esta acción");
            return "redirect:/almacen";
        }

        Optional<Pedido> pedidoOpt = pedidoRepository.findById(id);
        if (pedidoOpt.isEmpty()) {
            flash.addFlashAttribute("error", "Pedido no encontrado");
            return "redirect:/almacen";
        }

        Pedido pedido = pedidoOpt.get();
        if (!"en_almacen".equals(pedido.getEstado())) {
            flash.addFlashAttribute("error", "El pedido no está en estado 'en_almacen'");
            return "redirect:/almacen";
        }

        List<PedidoProducto> pedidoProductos = pedidoProductoRepository.findByPedidoId(id);

        for (PedidoProducto pp : pedidoProductos) {
            String nombreProducto = pp.getNombre();
            Optional<Producto> productoOpt = productoRepository.findByCodigo(null);

            List<Producto> todos = productoRepository.findAll();
            Producto producto = null;
            for (Producto p : todos) {
                if (p.getNombre().equalsIgnoreCase(nombreProducto)) {
                    producto = p;
                    break;
                }
            }

            if (producto == null) {
                producto = Producto.builder()
                        .codigo("PRD-" + System.currentTimeMillis())
                        .nombre(nombreProducto)
                        .descripcion(pp.getDescripcion())
                        .precioReferencia(pp.getPrecioUnitario())
                        .stockTienda(0)
                        .stockAlmacen(0)
                        .stockActual(0)
                        .activo(true)
                        .build();
                producto.calcularStockActual();
                producto = productoRepository.save(producto);
            }

            int cantidadRecibir = pp.getCantidadRecoge() != null && pp.getCantidadRecoge() > 0
                    ? pp.getCantidadRecoge()
                    : (pp.getCantidad() != null ? pp.getCantidad() : 0);

            if (cantidadRecibir > 0) {
                producto.setStockAlmacen(producto.getStockAlmacen() + cantidadRecibir);
                producto.calcularStockActual();
                productoRepository.save(producto);

                MovimientoAlmacen movimiento = MovimientoAlmacen.builder()
                        .producto(producto)
                        .tipo("entrada")
                        .cantidad(cantidadRecibir)
                        .stockResultante(producto.getStockAlmacen())
                        .concepto("Recepción pedido " + (pedido.getCodigo() != null ? pedido.getCodigo() : "#" + pedido.getId()))
                        .pedido(pedido)
                        .usuario(usuario)
                        .build();
                movimientoAlmacenRepository.save(movimiento);
            }
        }

        pedido.setEstado("listo_recoger");
        pedidoRepository.save(pedido);

        flash.addFlashAttribute("exito", "Pedido recibido y stock actualizado correctamente");
        return "redirect:/almacen";
    }

    @PostMapping("/{id}/entregar")
    public String entregarCliente(@PathVariable Long id,
                                  @AuthenticationPrincipal Usuario usuario,
                                  RedirectAttributes flash) {
        String rolNombre = usuario.getRol() != null ? usuario.getRol().getNombre().toLowerCase() : "";
        if (!rolNombre.contains("almacenero") && !rolNombre.contains("admin")) {
            flash.addFlashAttribute("error", "No tiene permisos para realizar esta acción");
            return "redirect:/almacen";
        }

        Optional<Pedido> pedidoOpt = pedidoRepository.findById(id);
        if (pedidoOpt.isEmpty()) {
            flash.addFlashAttribute("error", "Pedido no encontrado");
            return "redirect:/almacen";
        }

        Pedido pedido = pedidoOpt.get();
        if (!"listo_recoger".equals(pedido.getEstado())) {
            flash.addFlashAttribute("error", "El pedido no está en estado 'listo_recoger'");
            return "redirect:/almacen";
        }

        if (!"pagado_completo".equals(pedido.getEstadoPago())) {
            flash.addFlashAttribute("error", "El pedido no tiene pago completo");
            return "redirect:/almacen";
        }

        List<PedidoProducto> pedidoProductos = pedidoProductoRepository.findByPedidoId(id);

        for (PedidoProducto pp : pedidoProductos) {
            String nombreProducto = pp.getNombre();
            List<Producto> todos = productoRepository.findAll();
            Producto producto = null;
            for (Producto p : todos) {
                if (p.getNombre().equalsIgnoreCase(nombreProducto)) {
                    producto = p;
                    break;
                }
            }

            if (producto != null) {
                int cantidadEntregar = pp.getCantidadRecoge() != null && pp.getCantidadRecoge() > 0
                        ? pp.getCantidadRecoge()
                        : (pp.getCantidad() != null ? pp.getCantidad() : 0);

                if (cantidadEntregar > 0 && producto.getStockAlmacen() >= cantidadEntregar) {
                    producto.setStockAlmacen(producto.getStockAlmacen() - cantidadEntregar);
                    producto.calcularStockActual();
                    productoRepository.save(producto);

                    MovimientoAlmacen movimiento = MovimientoAlmacen.builder()
                            .producto(producto)
                            .tipo("salida")
                            .cantidad(cantidadEntregar)
                            .stockResultante(producto.getStockAlmacen())
                            .concepto("Entrega al cliente pedido " + (pedido.getCodigo() != null ? pedido.getCodigo() : "#" + pedido.getId()))
                            .pedido(pedido)
                            .usuario(usuario)
                            .build();
                    movimientoAlmacenRepository.save(movimiento);
                }
            }
        }

        pedido.setEstado("entregado");
        pedidoRepository.save(pedido);

        if (pedido.getUsuario() != null) {
            notificationService.crear(
                pedido.getUsuario().getId(),
                "pedido",
                "Pedido entregado",
                "El pedido " + (pedido.getCodigo() != null ? pedido.getCodigo() : "#" + pedido.getId()) + " ha sido entregado al cliente.",
                "/pedidos/" + pedido.getId()
            );
        }

        flash.addFlashAttribute("exito", "Pedido entregado correctamente");
        return "redirect:/almacen";
    }
}
