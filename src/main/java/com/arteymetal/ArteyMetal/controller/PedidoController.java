package com.arteymetal.ArteyMetal.controller;

import com.arteymetal.ArteyMetal.entity.*;
import com.arteymetal.ArteyMetal.repository.*;
import com.arteymetal.ArteyMetal.service.*;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Controller
@RequestMapping("/pedidos")
public class PedidoController {

    @Autowired private PedidoRepository pedidoRepository;
    @Autowired private PedidoProductoRepository pedidoProductoRepository;
    @Autowired private PedidoProductoArchivoRepository pedidoProductoArchivoRepository;
    @Autowired private PedidoOrdenArchivoRepository pedidoOrdenArchivoRepository;
    @Autowired private PedidoDisenoArchivoRepository pedidoDisenoArchivoRepository;
    @Autowired private ClienteRepository clienteRepository;
    @Autowired private CategoriaProductoRepository categoriaRepository;
    @Autowired private VentaRepository ventaRepository;
    @Autowired private VentaDetalleRepository ventaDetalleRepository;
    @Autowired private CajaAperturaRepository cajaAperturaRepository;
    @Autowired private ComprobanteVentaService comprobanteService;
    @Autowired private NotificationService notificationService;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private RolRepository rolRepository;
    @Autowired private HttpSession session;

    @GetMapping
    public String index(Model model, @AuthenticationPrincipal Usuario usuario,
                        @RequestParam(required = false) String q,
                        @RequestParam(required = false) String estado,
                        @RequestParam(required = false) String estado_personalizacion,
                        @RequestParam(defaultValue = "todas") String scope,
                        @RequestParam(defaultValue = "0") int page) {
        Long cajaAperturaId = (Long) session.getAttribute("pedido_caja_apertura_id");

        if (cajaAperturaId == null) {
            return redirectToCajaSelection(model, usuario);
        }

        CajaApertura caja = cajaAperturaRepository.findById(cajaAperturaId).orElse(null);
        if (caja == null || !"abierta".equals(caja.getEstado()) || !caja.getUsuario().getId().equals(usuario.getId())) {
            session.removeAttribute("pedido_caja_apertura_id");
            return redirectToCajaSelection(model, usuario);
        }

        Pageable pageable = PageRequest.of(Math.max(0, page), 10);
        Page<Pedido> pedidos;

        if ("mis_pedidos".equals(scope)) {
            pedidos = pedidoRepository.searchWithFiltersByUsuario(usuario.getId(), q, estado, estado_personalizacion, pageable);
        } else {
            pedidos = pedidoRepository.searchWithFilters(q, estado, estado_personalizacion, pageable);
        }

        model.addAttribute("pedidos", pedidos);
        model.addAttribute("busqueda", q);
        model.addAttribute("filtroEstado", estado);
        model.addAttribute("filtroPersonalizacion", estado_personalizacion);
        model.addAttribute("caja", caja);
        model.addAttribute("scope", scope);
        return "pedidos/index";
    }

    private String redirectToCajaSelection(Model model, Usuario usuario) {
        session.removeAttribute("pedido_caja_apertura_id");
        List<CajaApertura> cajasAbiertas = cajaAperturaRepository.findByUsuarioIdAndEstadoOrderByFechaAperturaDesc(usuario.getId(), "abierta");
        model.addAttribute("pedidos", Page.empty(PageRequest.of(0, 10)));
        model.addAttribute("busqueda", "");
        model.addAttribute("filtroEstado", "");
        model.addAttribute("filtroPersonalizacion", "");
        model.addAttribute("caja", null);
        model.addAttribute("cajasAbiertas", cajasAbiertas);
        model.addAttribute("sinCaja", cajasAbiertas.isEmpty());
        model.addAttribute("scope", "todas");
        return "pedidos/index";
    }

    @GetMapping("/create")
    public String create(Model model, @AuthenticationPrincipal Usuario usuario) {
        Long cajaAperturaId = (Long) session.getAttribute("pedido_caja_apertura_id");
        if (cajaAperturaId == null || !isCajaValida(cajaAperturaId, usuario.getId())) {
            return redirectToCajaSelection(model, usuario);
        }
        model.addAttribute("pedido", new Pedido());
        model.addAttribute("clientes", clienteRepository.findAll());
        model.addAttribute("categorias", categoriaRepository.findByActivoTrueOrderByNombre());
        prepararFormulario(model, null);
        return "pedidos/create";
    }

    @PostMapping
    @Transactional
    public String store(@RequestParam Map<String, String> params,
                        @RequestParam(value = "archivos_orden", required = false) MultipartFile[] archivosOrden,
                        @RequestParam(value = "productos_archivos", required = false) MultipartFile[] productosArchivos,
                        @AuthenticationPrincipal Usuario usuario,
                        RedirectAttributes flash,
                        Model model) {
        Long cajaAperturaId = (Long) session.getAttribute("pedido_caja_apertura_id");
        if (cajaAperturaId == null || !isCajaValida(cajaAperturaId, usuario.getId())) {
            return redirectToCajaSelection(model, usuario);
        }

        Pedido pedido = new Pedido();
        pedido.setNombreCliente(params.getOrDefault("nombre_cliente", ""));
        pedido.setTelefonoCliente(params.getOrDefault("telefono_cliente", ""));
        pedido.setDocumentoCliente(params.getOrDefault("documento_cliente", ""));
        pedido.setCorreoCliente(params.getOrDefault("correo_cliente", ""));
        pedido.setTipoProducto(params.getOrDefault("tipo_producto", ""));
        pedido.setDetalleTrabajo(params.getOrDefault("detalle_trabajo", ""));
        pedido.setTipoEntrega(params.getOrDefault("tipo_entrega", "local"));
        pedido.setDireccionEntrega(params.get("direccion_entrega"));
        pedido.setReferenciaEntrega(params.get("referencia_entrega"));
        pedido.setDistritoEntrega(params.get("distrito_entrega"));
        pedido.setCodigoPostalEntrega(params.get("codigo_postal_entrega"));
        pedido.setNombreRecibe(params.get("nombre_recibe"));
        pedido.setTelefonoRecibe(params.get("telefono_recibe"));
        pedido.setFechaEntregaCompromiso(params.get("fecha_entrega_compromiso") != null ? LocalDate.parse(params.get("fecha_entrega_compromiso")) : null);
        pedido.setObservaciones(params.get("observaciones"));
        pedido.setTipoPago(params.get("tipo_pago"));
        pedido.setEstado("registrado");
        pedido.setEstadoPersonalizacion("sin_iniciar");

        BigDecimal[] montos = calcularMontosDesdeParams(params);
        pedido.setMontoTotal(montos[0]);
        pedido.setMontoAdelanto(montos[1]);
        pedido.setMontoSaldo(montos[0].subtract(montos[1]).setScale(2, RoundingMode.HALF_UP));
        pedido.setEstadoPago(montos[1].compareTo(BigDecimal.ZERO) > 0 ? "adelanto_pagado" : "pagado_completo");

        pedido = completarDatosCliente(pedido, params);
        pedido = sincronizarClientePorDocumento(pedido);
        pedido = normalizarDatosEntrega(pedido);
        pedido.setCodigo(generarCodigoPedido());
        pedido.setUsuario(usuario);

        pedido = pedidoRepository.save(pedido);

        guardarArchivosOrden(archivosOrden, pedido);
        guardarProductos(params, pedido);

        String metodoPago = params.get("metodo_pago");
        BigDecimal adelanto = pedido.getMontoAdelanto();
        if (adelanto != null && adelanto.compareTo(BigDecimal.ZERO) > 0) {
            Venta venta = Venta.builder()
                .codigo(generarCodigoVenta())
                .tipoVenta("pedido")
                .pedido(pedido)
                .clienteNombre(pedido.getNombreCliente())
                .fechaVenta(LocalDate.now())
                .montoTotal(adelanto)
                .montoCobrado(adelanto)
                .estadoPago("pagado_completo")
                .metodoPago(metodoPago)
                .montoEfectivo("efectivo".equals(metodoPago) ? adelanto : BigDecimal.ZERO)
                .montoDigital(!"efectivo".equals(metodoPago) ? adelanto : BigDecimal.ZERO)
                .observaciones("Adelanto pedido " + pedido.getCodigo())
                .usuario(usuario)
                .cajaApertura(cajaAperturaRepository.findById(cajaAperturaId).orElse(null))
                .build();
            venta = ventaRepository.save(venta);

            VentaDetalle detalle = VentaDetalle.builder()
                .venta(venta)
                .productoNombre("Adelanto pedido " + pedido.getCodigo())
                .cantidad(1)
                .precioUnitario(adelanto)
                .subtotal(adelanto)
                .build();
            ventaDetalleRepository.save(detalle);

            String documento = (pedido.getDocumentoCliente() != null) ? pedido.getDocumentoCliente().replaceAll("\\D", "") : "";
            String tipoComprobante = documento.length() == 11 ? "factura" : "boleta";
            comprobanteService.emitir(venta, Map.of(
                "tipo_comprobante", tipoComprobante,
                "documento_cliente", documento.isEmpty() ? null : documento,
                "nombre_cliente", pedido.getNombreCliente() != null ? pedido.getNombreCliente() : "Cliente"
            ));
        }

        flash.addFlashAttribute("ok", "Pedido registrado correctamente.");
        return "redirect:/pedidos";
    }

    @PostMapping("/{id}/seleccionar-caja")
    public String seleccionarCaja(@PathVariable Long id, @AuthenticationPrincipal Usuario usuario, RedirectAttributes flash) {
        CajaApertura caja = cajaAperturaRepository.findById(id).orElse(null);
        if (caja == null || !caja.getUsuario().getId().equals(usuario.getId()) || !"abierta".equals(caja.getEstado())) {
            flash.addFlashAttribute("error", "Caja no valida.");
            return "redirect:/pedidos";
        }
        session.setAttribute("pedido_caja_apertura_id", caja.getId());
        return "redirect:/pedidos";
    }

    @PostMapping("/cambiar-caja")
    public String cambiarCaja() {
        session.removeAttribute("pedido_caja_apertura_id");
        return "redirect:/pedidos";
    }

    @GetMapping("/{id}")
    public String show(@PathVariable Long id, Model model) {
        Pedido pedido = pedidoRepository.findById(id).orElseThrow(() -> new RuntimeException("Pedido no encontrado"));
        model.addAttribute("pedido", pedido);
        return "pedidos/show";
    }

    @GetMapping("/{id}/edit")
    public String edit(@PathVariable Long id, Model model) {
        Pedido pedido = pedidoRepository.findById(id).orElseThrow(() -> new RuntimeException("Pedido no encontrado"));
        model.addAttribute("pedido", pedido);
        model.addAttribute("clientes", clienteRepository.findAll());
        model.addAttribute("categorias", categoriaRepository.findByActivoTrueOrderByNombre());
        prepararFormulario(model, pedido);
        return "pedidos/edit";
    }

    private void prepararFormulario(Model model, Pedido pedido) {
        List<Map<String, Object>> productos = new ArrayList<>();
        List<List<Map<String, Object>>> archivos = new ArrayList<>();
        if (pedido != null && pedido.getProductos() != null) {
            for (PedidoProducto producto : pedido.getProductos()) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("id", producto.getId());
                item.put("nombre", producto.getNombre());
                item.put("descripcion", producto.getDescripcion());
                item.put("precio_unitario", producto.getPrecioUnitario());
                item.put("cantidad", producto.getCantidad());
                productos.add(item);
                archivos.add(new ArrayList<>());
            }
        }
        if (productos.isEmpty()) {
            productos.add(new LinkedHashMap<>(Map.of("nombre", "", "descripcion", "", "precio_unitario", "", "cantidad", 1)));
            archivos.add(new ArrayList<>());
        }
        model.addAttribute("productosIniciales", productos);
        model.addAttribute("archivosIniciales", archivos);
        model.addAttribute("comprobantesExistentes", Collections.emptyList());
        model.addAttribute("ordenArchivos", Collections.emptyList());
        model.addAttribute("metodoPagoInfo", "efectivo");
    }

    @PostMapping("/{id}/update")
    @Transactional
    public String update(@PathVariable Long id,
                         @RequestParam Map<String, String> params,
                         @RequestParam(value = "archivos_orden", required = false) MultipartFile[] archivosOrden,
                         RedirectAttributes flash) {
        Pedido pedido = pedidoRepository.findById(id).orElseThrow(() -> new RuntimeException("Pedido no encontrado"));

        pedido.setNombreCliente(params.getOrDefault("nombre_cliente", ""));
        pedido.setTelefonoCliente(params.getOrDefault("telefono_cliente", ""));
        pedido.setDocumentoCliente(params.getOrDefault("documento_cliente", ""));
        pedido.setCorreoCliente(params.getOrDefault("correo_cliente", ""));
        pedido.setTipoProducto(params.getOrDefault("tipo_producto", ""));
        pedido.setDetalleTrabajo(params.getOrDefault("detalle_trabajo", ""));
        pedido.setTipoEntrega(params.getOrDefault("tipo_entrega", "local"));
        pedido.setDireccionEntrega(params.get("direccion_entrega"));
        pedido.setReferenciaEntrega(params.get("referencia_entrega"));
        pedido.setDistritoEntrega(params.get("distrito_entrega"));
        pedido.setCodigoPostalEntrega(params.get("codigo_postal_entrega"));
        pedido.setNombreRecibe(params.get("nombre_recibe"));
        pedido.setTelefonoRecibe(params.get("telefono_recibe"));
        pedido.setFechaEntregaCompromiso(params.get("fecha_entrega_compromiso") != null ? LocalDate.parse(params.get("fecha_entrega_compromiso")) : null);
        pedido.setObservaciones(params.get("observaciones"));
        pedido.setTipoPago(params.get("tipo_pago"));

        BigDecimal[] montos = calcularMontosDesdeParams(params);
        pedido.setMontoTotal(montos[0]);
        pedido.setMontoAdelanto(montos[1]);
        pedido.setMontoSaldo(montos[0].subtract(montos[1]).setScale(2, RoundingMode.HALF_UP));

        pedido = completarDatosCliente(pedido, params);
        pedido = sincronizarClientePorDocumento(pedido);
        pedido = normalizarDatosEntrega(pedido);

        pedidoRepository.save(pedido);
        guardarArchivosOrden(archivosOrden, pedido);
        guardarProductos(params, pedido);

        flash.addFlashAttribute("ok", "Pedido actualizado correctamente.");
        return "redirect:/pedidos";
    }

    @PostMapping("/{id}/eliminar")
    public String destroy(@PathVariable Long id, RedirectAttributes flash) {
        pedidoRepository.deleteById(id);
        flash.addFlashAttribute("ok", "Pedido eliminado correctamente.");
        return "redirect:/pedidos";
    }

    @PostMapping("/{id}/personalizacion")
    public String actualizarPersonalizacion(@PathVariable Long id,
                                            @RequestParam Map<String, String> params,
                                            @RequestParam(value = "archivos_diseno", required = false) MultipartFile[] archivosDiseno,
                                            @AuthenticationPrincipal Usuario usuario,
                                            RedirectAttributes flash) {
        Pedido pedido = pedidoRepository.findById(id).orElseThrow(() -> new RuntimeException("Pedido no encontrado"));
        String rol = usuario.getRol().getNombre();

        if (params.containsKey("estado")) {
            pedido.setEstado(params.get("estado"));
        }
        if (params.containsKey("estado_personalizacion")) {
            pedido.setEstadoPersonalizacion(params.get("estado_personalizacion"));
        }
        if (params.containsKey("estado_pago")) {
            String estadoPago = params.get("estado_pago");
            pedido.setEstadoPago(estadoPago);
            BigDecimal[] pagos = calcularPago(pedido.getMontoTotal(), estadoPago);
            pedido.setMontoAdelanto(pagos[0]);
            pedido.setMontoSaldo(pagos[1]);
        }
        if (params.get("fecha_entrega_compromiso") != null && !params.get("fecha_entrega_compromiso").isEmpty()) {
            pedido.setFechaEntregaCompromiso(LocalDate.parse(params.get("fecha_entrega_compromiso")));
        }
        if (params.get("fecha_inicio_diseno") != null && !params.get("fecha_inicio_diseno").isEmpty()) {
            pedido.setFechaInicioDiseno(LocalDate.parse(params.get("fecha_inicio_diseno")));
        }
        if (params.get("fecha_aprobacion_diseno") != null && !params.get("fecha_aprobacion_diseno").isEmpty()) {
            pedido.setFechaAprobacionDiseno(LocalDate.parse(params.get("fecha_aprobacion_diseno")));
        }
        if (params.containsKey("observaciones_personalizacion")) {
            pedido.setObservacionesPersonalizacion(params.get("observaciones_personalizacion"));
        }

        pedidoRepository.save(pedido);

        if (archivosDiseno != null) {
            Long productoId = null;
            if (params.get("pedido_producto_id") != null && !params.get("pedido_producto_id").isEmpty()) {
                productoId = Long.parseLong(params.get("pedido_producto_id"));
            }
            for (MultipartFile archivo : archivosDiseno) {
                if (!archivo.isEmpty()) {
                    PedidoDisenoArchivo arch = PedidoDisenoArchivo.builder()
                        .pedido(pedido)
                        .pedidoProducto(productoId != null ? pedidoProductoRepository.findById(productoId).orElse(null) : null)
                        .tipo("diseno")
                        .archivoPath("disenos_pedido/" + archivo.getOriginalFilename())
                        .nombreOriginal(archivo.getOriginalFilename())
                        .mimeType(archivo.getContentType())
                        .tamanoBytes(archivo.getSize())
                        .build();
                    pedidoDisenoArchivoRepository.save(arch);
                }
            }
        }

        flash.addFlashAttribute("ok", "Personalizacion actualizada correctamente.");
        return "redirect:/pedidos/" + id;
    }

    @PostMapping("/{id}/transporte")
    public String marcarEnTransporte(@PathVariable Long id,
                                     @AuthenticationPrincipal Usuario usuario,
                                     RedirectAttributes flash) {
        Pedido pedido = pedidoRepository.findById(id).orElseThrow(() -> new RuntimeException("Pedido no encontrado"));
        String rol = usuario.getRol().getNombre();
        if (!"repartidor".equals(rol) && !"administrador".equals(rol)) {
            flash.addFlashAttribute("error", "Sin permiso.");
            return "redirect:/pedidos/" + id;
        }
        if (!"listo_entrega".equals(pedido.getEstado())) {
            flash.addFlashAttribute("error", "El pedido debe estar en estado listo entrega.");
            return "redirect:/pedidos/" + id;
        }
        pedido.setEstado("en_transporte");
        pedidoRepository.save(pedido);
        flash.addFlashAttribute("ok", "Pedido recogido de produccion, en transporte al almacen.");
        return "redirect:/pedidos/" + id;
    }

    @PostMapping("/{id}/almacen")
    public String marcarEnAlmacen(@PathVariable Long id,
                                  @AuthenticationPrincipal Usuario usuario,
                                  RedirectAttributes flash) {
        Pedido pedido = pedidoRepository.findById(id).orElseThrow(() -> new RuntimeException("Pedido no encontrado"));
        String rol = usuario.getRol().getNombre();
        if (!"almacenero".equals(rol) && !"administrador".equals(rol)) {
            flash.addFlashAttribute("error", "Sin permiso.");
            return "redirect:/pedidos/" + id;
        }
        if (!"en_transporte".equals(pedido.getEstado())) {
            flash.addFlashAttribute("error", "El pedido debe estar en transporte.");
            return "redirect:/pedidos/" + id;
        }
        pedido.setEstado("en_almacen");
        pedidoRepository.save(pedido);
        flash.addFlashAttribute("ok", "Pedido registrado en almacen correctamente.");
        return "redirect:/pedidos/" + id;
    }

    @PostMapping("/{id}/pago-final")
    @Transactional
    public String confirmarPagoFinal(@PathVariable Long id,
                                     @RequestParam String metodo_pago,
                                     @RequestParam(required = false) BigDecimal vuelto,
                                     @AuthenticationPrincipal Usuario usuario,
                                     RedirectAttributes flash) {
        Pedido pedido = pedidoRepository.findById(id).orElseThrow(() -> new RuntimeException("Pedido no encontrado"));
        String rol = usuario.getRol().getNombre();
        if (!"administrador".equals(rol) && !"vendedor".equals(rol)) {
            flash.addFlashAttribute("error", "Sin permiso para cobrar el saldo final.");
            return "redirect:/pedidos";
        }
        if ("pagado_completo".equals(pedido.getEstadoPago()) || pedido.getMontoSaldo() == null || pedido.getMontoSaldo().compareTo(BigDecimal.ZERO) <= 0) {
            flash.addFlashAttribute("error", "Este pedido ya fue cerrado.");
            return "redirect:/pedidos";
        }

        Long cajaAperturaId = (Long) session.getAttribute("pedido_caja_apertura_id");
        BigDecimal saldoPendiente = pedido.getMontoSaldo().setScale(2, RoundingMode.HALF_UP);

        Venta venta = Venta.builder()
            .codigo(generarCodigoVenta())
            .tipoVenta("pedido")
            .pedido(pedido)
            .clienteNombre(pedido.getNombreCliente())
            .fechaVenta(LocalDate.now())
            .montoTotal(saldoPendiente)
            .montoCobrado(saldoPendiente)
            .estadoPago("pagado_completo")
            .metodoPago(metodo_pago)
            .montoEfectivo("efectivo".equals(metodo_pago) ? saldoPendiente : BigDecimal.ZERO)
            .montoDigital(!"efectivo".equals(metodo_pago) ? saldoPendiente : BigDecimal.ZERO)
            .vuelto(vuelto)
            .observaciones("Pago final pedido " + pedido.getCodigo())
            .usuario(usuario)
            .cajaApertura(cajaAperturaId != null ? cajaAperturaRepository.findById(cajaAperturaId).orElse(null) : null)
            .build();
        venta = ventaRepository.save(venta);

        VentaDetalle detalle = VentaDetalle.builder()
            .venta(venta)
            .productoNombre("Pago final pedido " + pedido.getCodigo())
            .cantidad(1)
            .precioUnitario(saldoPendiente)
            .subtotal(saldoPendiente)
            .build();
        ventaDetalleRepository.save(detalle);

        String documento = (pedido.getDocumentoCliente() != null) ? pedido.getDocumentoCliente().replaceAll("\\D", "") : "";
        String tipoComprobante = documento.length() == 11 ? "factura" : "boleta";
        comprobanteService.emitir(venta, Map.of(
            "tipo_comprobante", tipoComprobante,
            "documento_cliente", documento.isEmpty() ? null : documento,
            "nombre_cliente", pedido.getNombreCliente() != null ? pedido.getNombreCliente() : "Cliente",
            "direccion_cliente", pedido.getDireccionEntrega() != null ? pedido.getDireccionEntrega() : ""
        ));

        pedido.setEstadoPago("pagado_completo");
        pedido.setMontoSaldo(BigDecimal.ZERO);
        pedidoRepository.save(pedido);

        flash.addFlashAttribute("ok", "Pago registrado correctamente.");
        return "redirect:/pedidos";
    }

    @PostMapping("/{id}/autorizar-recoger")
    @Transactional
    public String autorizarRecoger(@PathVariable Long id,
                                   @RequestParam(required = false) String metodo_pago,
                                   @RequestParam(required = false) BigDecimal vuelto,
                                   @AuthenticationPrincipal Usuario usuario,
                                   RedirectAttributes flash) {
        Pedido pedido = pedidoRepository.findById(id).orElseThrow(() -> new RuntimeException("Pedido no encontrado"));
        String rol = usuario.getRol().getNombre();
        if (!"administrador".equals(rol) && !"vendedor".equals(rol)) {
            flash.addFlashAttribute("error", "Sin permiso.");
            return "redirect:/pedidos";
        }
        if (!"en_almacen".equals(pedido.getEstado())) {
            flash.addFlashAttribute("error", "El pedido debe estar en almacen.");
            return "redirect:/pedidos";
        }

        if ("pagado_completo".equals(pedido.getEstadoPago()) || (pedido.getMontoSaldo() != null && pedido.getMontoSaldo().compareTo(BigDecimal.ZERO) <= 0)) {
            pedido.setEstado("listo_recoger");
            pedidoRepository.save(pedido);
            flash.addFlashAttribute("ok", "Pedido habilitado para recoger en almacen.");
            return "redirect:/pedidos";
        }

        Long cajaAperturaId = (Long) session.getAttribute("pedido_caja_apertura_id");
        BigDecimal saldoPendiente = pedido.getMontoSaldo().setScale(2, RoundingMode.HALF_UP);

        Venta venta = Venta.builder()
            .codigo(generarCodigoVenta())
            .tipoVenta("pedido")
            .pedido(pedido)
            .clienteNombre(pedido.getNombreCliente())
            .fechaVenta(LocalDate.now())
            .montoTotal(saldoPendiente)
            .montoCobrado(saldoPendiente)
            .estadoPago("pagado_completo")
            .metodoPago(metodo_pago)
            .montoEfectivo("efectivo".equals(metodo_pago) ? saldoPendiente : BigDecimal.ZERO)
            .montoDigital(!"efectivo".equals(metodo_pago) ? saldoPendiente : BigDecimal.ZERO)
            .vuelto(vuelto)
            .observaciones("Pago final + autorizar recoger " + pedido.getCodigo())
            .usuario(usuario)
            .cajaApertura(cajaAperturaId != null ? cajaAperturaRepository.findById(cajaAperturaId).orElse(null) : null)
            .build();
        venta = ventaRepository.save(venta);

        VentaDetalle detalle = VentaDetalle.builder()
            .venta(venta)
            .productoNombre("Pago final pedido " + pedido.getCodigo())
            .cantidad(1)
            .precioUnitario(saldoPendiente)
            .subtotal(saldoPendiente)
            .build();
        ventaDetalleRepository.save(detalle);

        String documento = (pedido.getDocumentoCliente() != null) ? pedido.getDocumentoCliente().replaceAll("\\D", "") : "";
        String tipoComprobante = documento.length() == 11 ? "factura" : "boleta";
        comprobanteService.emitir(venta, Map.of(
            "tipo_comprobante", tipoComprobante,
            "documento_cliente", documento.isEmpty() ? null : documento,
            "nombre_cliente", pedido.getNombreCliente() != null ? pedido.getNombreCliente() : "Cliente"
        ));

        pedido.setEstado("listo_recoger");
        pedido.setEstadoPago("pagado_completo");
        pedido.setMontoSaldo(BigDecimal.ZERO);
        pedidoRepository.save(pedido);

        flash.addFlashAttribute("ok", "Pago registrado y pedido habilitado para recoger.");
        return "redirect:/pedidos";
    }

    @PostMapping("/{id}/derivar")
    public String derivar(@PathVariable Long id,
                          @RequestParam String destino,
                          @AuthenticationPrincipal Usuario usuario,
                          RedirectAttributes flash) {
        Pedido pedido = pedidoRepository.findById(id).orElseThrow(() -> new RuntimeException("Pedido no encontrado"));
        String rol = usuario.getRol().getNombre();
        if (!"administrador".equals(rol) && !"vendedor".equals(rol)) {
            flash.addFlashAttribute("error", "Sin permiso para derivar pedidos.");
            return "redirect:/pedidos";
        }

        if ("diseno".equals(destino)) {
            if (!"sin_iniciar".equals(pedido.getEstadoPersonalizacion())) {
                flash.addFlashAttribute("error", "El pedido ya fue derivado a diseno.");
                return "redirect:/pedidos";
            }
            pedido.setEstadoPersonalizacion("en_diseno");
            pedidoRepository.save(pedido);

            List<Usuario> disenadores = usuarioRepository.findAll();
            for (Usuario u : disenadores) {
                if (u.getRol() != null && ("administrador".equals(u.getRol().getNombre()) || "disenador".equals(u.getRol().getNombre()))) {
                    notificationService.crear(u.getId(), "diseno", "Nuevo pedido para diseno",
                        "El pedido " + pedido.getCodigo() + " de " + pedido.getNombreCliente() + " ha sido derivado a diseno.",
                        "/diseno/" + pedido.getId());
                }
            }
            flash.addFlashAttribute("ok", "Pedido derivado a Diseno correctamente.");
        } else {
            if (!"registrado".equals(pedido.getEstado())) {
                flash.addFlashAttribute("error", "El pedido ya fue derivado a produccion.");
                return "redirect:/pedidos";
            }
            pedido.setEstado("en_produccion");
            pedidoRepository.save(pedido);

            List<Usuario> orfebres = usuarioRepository.findAll();
            for (Usuario u : orfebres) {
                if (u.getRol() != null && ("administrador".equals(u.getRol().getNombre()) || "orfebre".equals(u.getRol().getNombre()))) {
                    notificationService.crear(u.getId(), "produccion", "Nuevo pedido para produccion",
                        "El pedido " + pedido.getCodigo() + " de " + pedido.getNombreCliente() + " ha sido derivado a produccion.",
                        "/pedidos/" + pedido.getId());
                }
            }
            flash.addFlashAttribute("ok", "Pedido derivado a Produccion correctamente.");
        }
        return "redirect:/pedidos";
    }

    @PostMapping("/producto-archivo/{id}/eliminar")
    public ResponseEntity<Map<String, Object>> eliminarArchivoProducto(@PathVariable Long id) {
        pedidoProductoArchivoRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("ok", true));
    }

    @PostMapping("/orden-archivo/{id}/eliminar")
    public ResponseEntity<Map<String, Object>> eliminarArchivoOrden(@PathVariable Long id) {
        pedidoOrdenArchivoRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("ok", true));
    }

    @PostMapping("/{id}/comprobante-pago/eliminar")
    public ResponseEntity<Map<String, Object>> eliminarComprobantePago(@PathVariable Long id) {
        Pedido pedido = pedidoRepository.findById(id).orElse(null);
        if (pedido == null) {
            return ResponseEntity.status(404).body(Map.of("ok", false, "message", "Pedido no encontrado"));
        }
        pedido.setComprobantePagoPath(null);
        pedido.setComprobantePago(null);
        pedidoRepository.save(pedido);
        return ResponseEntity.ok(Map.of("ok", true));
    }

    @PostMapping("/{id}/llegada-tienda")
    @Transactional
    public String registrarLlegadaTienda(@PathVariable Long id,
                                         @AuthenticationPrincipal Usuario usuario,
                                         RedirectAttributes flash) {
        Pedido pedido = pedidoRepository.findById(id).orElseThrow(() -> new RuntimeException("Pedido no encontrado"));
        String rol = usuario.getRol().getNombre();
        if (!"repartidor".equals(rol) && !"administrador".equals(rol) && !"almacenero".equals(rol)) {
            flash.addFlashAttribute("error", "Sin permiso.");
            return "redirect:/pedidos/" + id;
        }
        if (!"en_almacen".equals(pedido.getEstado())) {
            flash.addFlashAttribute("error", "El pedido debe estar en almacen.");
            return "redirect:/pedidos/" + id;
        }
        pedido.setEstado("listo_recoger");
        pedidoRepository.save(pedido);
        flash.addFlashAttribute("ok", "Pedido registrado en tienda para recoger.");
        return "redirect:/pedidos/" + id;
    }

    private boolean isCajaValida(Long cajaAperturaId, Long usuarioId) {
        CajaApertura caja = cajaAperturaRepository.findById(cajaAperturaId).orElse(null);
        return caja != null && "abierta".equals(caja.getEstado()) && caja.getUsuario().getId().equals(usuarioId);
    }

    private BigDecimal[] calcularMontosDesdeParams(Map<String, String> params) {
        BigDecimal total = BigDecimal.ZERO;
        int i = 0;
        while (params.containsKey("productos[" + i + "].precio_unitario")) {
            try {
                BigDecimal precio = new BigDecimal(params.getOrDefault("productos[" + i + "].precio_unitario", "0"));
                int cantidad = Integer.parseInt(params.getOrDefault("productos[" + i + "].cantidad", "1"));
                total = total.add(precio.multiply(BigDecimal.valueOf(cantidad)));
            } catch (Exception ignored) {}
            i++;
        }
        String tipoPago = params.getOrDefault("tipo_pago", "dos_partes");
        BigDecimal adelanto = "contado".equals(tipoPago) ? total : total.multiply(new BigDecimal("0.5")).setScale(2, RoundingMode.HALF_UP);
        return new BigDecimal[]{total.setScale(2, RoundingMode.HALF_UP), adelanto};
    }

    private Pedido completarDatosCliente(Pedido pedido, Map<String, String> params) {
        String clienteId = params.get("cliente_id");
        if (clienteId != null && !clienteId.isEmpty()) {
            Cliente cliente = clienteRepository.findById(Long.parseLong(clienteId)).orElse(null);
            if (cliente != null) {
                pedido.setCliente(cliente);
                pedido.setNombreCliente(cliente.getNombreCompleto());
                if ((pedido.getTelefonoCliente() == null || pedido.getTelefonoCliente().isEmpty()) && cliente.getTelefono() != null) {
                    pedido.setTelefonoCliente(cliente.getTelefono());
                }
                if ((pedido.getDocumentoCliente() == null || pedido.getDocumentoCliente().isEmpty()) && cliente.getDocumento() != null) {
                    pedido.setDocumentoCliente(cliente.getDocumento());
                }
                if ((pedido.getCorreoCliente() == null || pedido.getCorreoCliente().isEmpty()) && cliente.getCorreo() != null) {
                    pedido.setCorreoCliente(cliente.getCorreo());
                }
            }
        }
        return pedido;
    }

    private Pedido sincronizarClientePorDocumento(Pedido pedido) {
        if (pedido.getCliente() != null) return pedido;
        String documento = (pedido.getDocumentoCliente() != null) ? pedido.getDocumentoCliente().trim() : "";
        if (documento.isEmpty()) return pedido;

        Cliente cliente = clienteRepository.findByDocumento(documento).orElse(null);
        if (cliente == null) {
            cliente = Cliente.builder()
                .nombreCompleto(pedido.getNombreCliente() != null ? pedido.getNombreCliente() : "Cliente sin nombre")
                .telefono(pedido.getTelefonoCliente())
                .correo(pedido.getCorreoCliente())
                .documento(documento)
                .direccion(pedido.getDireccionEntrega())
                .observaciones("Creado automaticamente desde pedidos.")
                .build();
            cliente = clienteRepository.save(cliente);
        }
        pedido.setCliente(cliente);
        return pedido;
    }

    private Pedido normalizarDatosEntrega(Pedido pedido) {
        if ("local".equals(pedido.getTipoEntrega())) {
            pedido.setDireccionEntrega(null);
            pedido.setReferenciaEntrega(null);
            pedido.setDistritoEntrega(null);
            pedido.setCodigoPostalEntrega(null);
            pedido.setNombreRecibe(null);
            pedido.setTelefonoRecibe(null);
            pedido.setCostoDelivery(null);
        }
        return pedido;
    }

    private BigDecimal[] calcularPago(BigDecimal montoTotal, String estadoPago) {
        if (montoTotal == null) return new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO};
        BigDecimal adelanto = montoTotal.multiply(new BigDecimal("0.5")).setScale(2, RoundingMode.HALF_UP);
        BigDecimal saldo = montoTotal.subtract(adelanto).setScale(2, RoundingMode.HALF_UP);

        if ("pendiente_adelanto".equals(estadoPago)) {
            return new BigDecimal[]{BigDecimal.ZERO, montoTotal};
        }
        if ("adelanto_pagado".equals(estadoPago)) {
            return new BigDecimal[]{adelanto, saldo};
        }
        return new BigDecimal[]{adelanto, BigDecimal.ZERO};
    }

    private void guardarArchivosOrden(MultipartFile[] archivos, Pedido pedido) {
        if (archivos == null) return;
        for (MultipartFile archivo : archivos) {
            if (!archivo.isEmpty()) {
                PedidoOrdenArchivo arch = PedidoOrdenArchivo.builder()
                    .pedido(pedido)
                    .archivoPath("ordenes_compra_pedido/" + archivo.getOriginalFilename())
                    .nombreOriginal(archivo.getOriginalFilename())
                    .mimeType(archivo.getContentType())
                    .tamanoBytes(archivo.getSize())
                    .build();
                pedidoOrdenArchivoRepository.save(arch);
            }
        }
    }

    private void guardarProductos(Map<String, String> params, Pedido pedido) {
        List<PedidoProducto> actuales = pedidoProductoRepository.findByPedidoId(pedido.getId());
        Set<Long> idsActuales = new HashSet<>();
        for (PedidoProducto pp : actuales) idsActuales.add(pp.getId());

        Set<Long> idsRecibidos = new HashSet<>();
        int i = 0;
        while (params.containsKey("productos[" + i + "].nombre")) {
            String nombre = params.getOrDefault("productos[" + i + "].nombre", "");
            if (nombre.isEmpty()) { i++; continue; }
            String desc = params.get("productos[" + i + "].descripcion");
            BigDecimal precio = new BigDecimal(params.getOrDefault("productos[" + i + "].precio_unitario", "0"));
            int cantidad = Integer.parseInt(params.getOrDefault("productos[" + i + "].cantidad", "1"));
            String idStr = params.get("productos[" + i + "].id");

            PedidoProducto pp;
            if (idStr != null && !idStr.isEmpty()) {
                Long ppId = Long.parseLong(idStr);
                pp = pedidoProductoRepository.findById(ppId).orElse(null);
                if (pp != null) {
                    pp.setNombre(nombre);
                    pp.setDescripcion(desc);
                    pp.setPrecioUnitario(precio);
                    pp.setCantidad(cantidad);
                    pp.setTotal(precio.multiply(BigDecimal.valueOf(cantidad)).setScale(2, RoundingMode.HALF_UP));
                    pp.setOrden(i);
                    pp = pedidoProductoRepository.save(pp);
                } else {
                    pp = crearPedidoProducto(pedido, nombre, desc, precio, cantidad, i);
                }
            } else {
                pp = crearPedidoProducto(pedido, nombre, desc, precio, cantidad, i);
            }
            idsRecibidos.add(pp.getId());
            i++;
        }

        for (Long id : idsActuales) {
            if (!idsRecibidos.contains(id)) {
                pedidoProductoRepository.deleteById(id);
            }
        }
    }

    private PedidoProducto crearPedidoProducto(Pedido pedido, String nombre, String desc, BigDecimal precio, int cantidad, int orden) {
        PedidoProducto pp = PedidoProducto.builder()
            .pedido(pedido)
            .nombre(nombre)
            .descripcion(desc)
            .precioUnitario(precio)
            .cantidad(cantidad)
            .total(precio.multiply(BigDecimal.valueOf(cantidad)).setScale(2, RoundingMode.HALF_UP))
            .orden(orden)
            .cantidadRecoge(0)
            .build();
        return pedidoProductoRepository.save(pp);
    }

    private String generarCodigoPedido() {
        Long maxId = pedidoRepository.maxId();
        long siguiente = (maxId != null ? maxId : 0) + 1;
        return "PED-" + String.format("%06d", siguiente);
    }

    private String generarCodigoVenta() {
        long count = ventaRepository.count() + 1;
        return "VEN-" + String.format("%06d", count);
    }
}
