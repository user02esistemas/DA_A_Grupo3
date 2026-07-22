package com.arteymetal.ArteyMetal.controller;

import com.arteymetal.ArteyMetal.entity.*;
import com.arteymetal.ArteyMetal.repository.*;
import com.arteymetal.ArteyMetal.service.ComprobanteVentaService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@Controller
@RequestMapping("/ventas")
public class VentaController {

    @Autowired private VentaRepository ventaRepository;
    @Autowired private VentaDetalleRepository ventaDetalleRepository;
    @Autowired private ProductoRepository productoRepository;
    @Autowired private CajaAperturaRepository cajaAperturaRepository;
    @Autowired private ComprobanteVentaService comprobanteVentaService;
    @Autowired private HttpSession session;

    @GetMapping
    public String index(Model model,
                        @RequestParam(defaultValue = "") String q,
                        @RequestParam(defaultValue = "") String tipo,
                        @RequestParam(defaultValue = "todas") String scope,
                        @RequestParam(defaultValue = "0") int page,
                        @AuthenticationPrincipal Usuario usuario) {

        Long cajaAperturaId = (Long) session.getAttribute("caja_apertura_id");
        CajaApertura caja = null;
        List<CajaApertura> cajasAbiertas = cajaAperturaRepository
                .findByUsuarioIdAndEstadoOrderByFechaAperturaDesc(usuario.getId(), "abierta");

        if (cajaAperturaId != null) {
            caja = cajaAperturaRepository.findById(cajaAperturaId).orElse(null);
            if (caja == null || !"abierta".equals(caja.getEstado())) {
                session.removeAttribute("caja_apertura_id");
                caja = null;
            }
        }

        Pageable pageable = PageRequest.of(page, 12);
        Page<Venta> ventas;
        if (caja != null) {
            if ("mi_caja".equals(scope)) {
                ventas = ventaRepository.searchWithFiltersByCaja(cajaAperturaId, q, tipo, pageable);
            } else {
                ventas = ventaRepository.searchWithFilters(q, tipo, pageable);
            }
        } else {
            if (!q.isEmpty() || !tipo.isEmpty()) {
                ventas = ventaRepository.searchWithFilters(q, tipo, pageable);
            } else {
                ventas = ventaRepository.findAllByOrderByFechaVentaDesc(pageable);
            }
            scope = "todas";
        }

        model.addAttribute("ventas", ventas);
        model.addAttribute("busqueda", q);
        model.addAttribute("tipo", tipo);
        model.addAttribute("caja", caja);
        model.addAttribute("cajasAbiertas", cajasAbiertas);
        model.addAttribute("sinCaja", caja == null);
        model.addAttribute("scope", scope);
        return "ventas/index";
    }

    @GetMapping("/create")
    public String create(Model model, RedirectAttributes flash) {
        Long cajaAperturaId = (Long) session.getAttribute("caja_apertura_id");
        if (cajaAperturaId == null) {
            flash.addFlashAttribute("error", "Debe seleccionar una caja abierta para registrar ventas");
            return "redirect:/ventas";
        }

        CajaApertura caja = cajaAperturaRepository.findById(cajaAperturaId).orElse(null);
        if (caja == null || !"abierta".equals(caja.getEstado())) {
            session.removeAttribute("caja_apertura_id");
            flash.addFlashAttribute("error", "La caja seleccionada no esta abierta");
            return "redirect:/ventas";
        }

        List<Producto> productos = productoRepository
                .findByActivoTrue(PageRequest.of(0, 10000))
                .getContent();
        model.addAttribute("productos", productos);
        return "ventas/create";
    }

    @PostMapping
    @Transactional
    public String store(@RequestParam(required = false) String cliente_nombre,
                        @RequestParam(required = false) String observaciones,
                        @RequestParam(defaultValue = "boleta") String tipo_comprobante,
                        @RequestParam(required = false) String documento_cliente,
                        @RequestParam(required = false) String direccion_cliente,
                        @RequestParam(defaultValue = "efectivo") String forma_pago,
                        @RequestParam(required = false) BigDecimal monto_recibido,
                        @RequestParam("producto_id[]") Long[] productoIds,
                        @RequestParam("cantidad[]") Integer[] cantidades,
                        @AuthenticationPrincipal Usuario usuario,
                        RedirectAttributes flash) {

        Long cajaAperturaId = (Long) session.getAttribute("caja_apertura_id");
        if (cajaAperturaId == null) {
            flash.addFlashAttribute("error", "Debe seleccionar una caja abierta");
            return "redirect:/ventas";
        }

        CajaApertura caja = cajaAperturaRepository.findById(cajaAperturaId).orElse(null);
        if (caja == null || !"abierta".equals(caja.getEstado())) {
            session.removeAttribute("caja_apertura_id");
            flash.addFlashAttribute("error", "La caja seleccionada no esta abierta");
            return "redirect:/ventas";
        }

        List<String> formasPagoValidas = Arrays.asList(
                "efectivo", "yape", "plin", "tarjeta", "transferencia", "mixto");
        if (!formasPagoValidas.contains(forma_pago)) {
            flash.addFlashAttribute("error", "Forma de pago no valida");
            return "redirect:/ventas/create";
        }

        List<String> comprobantesValidos = Arrays.asList("boleta", "factura");
        if (!comprobantesValidos.contains(tipo_comprobante)) {
            flash.addFlashAttribute("error", "Tipo de comprobante no valido");
            return "redirect:/ventas/create";
        }

        if (productoIds == null || productoIds.length == 0) {
            flash.addFlashAttribute("error", "Debe agregar al menos un producto");
            return "redirect:/ventas/create";
        }

        if (cantidades == null || cantidades.length == 0) {
            flash.addFlashAttribute("error", "Debe indicar la cantidad de al menos un producto");
            return "redirect:/ventas/create";
        }

        if (productoIds.length != cantidades.length) {
            flash.addFlashAttribute("error", "Los productos y cantidades no coinciden");
            return "redirect:/ventas/create";
        }

        for (int i = 0; i < cantidades.length; i++) {
            if (cantidades[i] == null || cantidades[i] <= 0) {
                flash.addFlashAttribute("error", "Las cantidades deben ser mayores a 0");
                return "redirect:/ventas/create";
            }
        }

        if ("boleta".equals(tipo_comprobante)
                && (documento_cliente == null || documento_cliente.trim().isEmpty())) {
            documento_cliente = "99999999";
        }

        if ("factura".equals(tipo_comprobante)) {
            if (documento_cliente == null || documento_cliente.trim().isEmpty()) {
                flash.addFlashAttribute("error", "La factura requiere un numero de RUC");
                return "redirect:/ventas/create";
            }
            if (documento_cliente.trim().length() != 11) {
                flash.addFlashAttribute("error", "El RUC debe tener 11 digitos");
                return "redirect:/ventas/create";
            }
            if (cliente_nombre == null || cliente_nombre.trim().isEmpty()) {
                flash.addFlashAttribute("error", "La factura requiere una razon social");
                return "redirect:/ventas/create";
            }
        }

        String codigo = "VEN-" + String.format("%06d", ventaRepository.count() + 1);
        BigDecimal total = BigDecimal.ZERO;
        List<VentaDetalle> detalles = new ArrayList<>();

        for (int i = 0; i < productoIds.length; i++) {
            Producto producto = productoRepository.findById(productoIds[i])
                    .orElseThrow(() -> new RuntimeException("Producto no encontrado"));

            if (producto.getStockTienda() < cantidades[i]) {
                flash.addFlashAttribute("error",
                        "Stock insuficiente para: " + producto.getNombre()
                                + " (disponible: " + producto.getStockTienda() + ")");
                return "redirect:/ventas/create";
            }

            BigDecimal subtotal = producto.getPrecioReferencia()
                    .multiply(new BigDecimal(cantidades[i]));
            total = total.add(subtotal);

            VentaDetalle detalle = VentaDetalle.builder()
                    .producto(producto)
                    .productoNombre(producto.getNombre())
                    .cantidad(cantidades[i])
                    .precioUnitario(producto.getPrecioReferencia())
                    .subtotal(subtotal)
                    .build();

            detalles.add(detalle);
        }

        BigDecimal vuelto = BigDecimal.ZERO;
        BigDecimal montoEfectivo = BigDecimal.ZERO;
        BigDecimal montoDigital = BigDecimal.ZERO;

        if ("efectivo".equals(forma_pago)) {
            if (monto_recibido == null) {
                flash.addFlashAttribute("error",
                        "Debe indicar el monto recibido para pago en efectivo");
                return "redirect:/ventas/create";
            }
            if (monto_recibido.compareTo(total) < 0) {
                flash.addFlashAttribute("error",
                        "El monto recibido es menor al total de S/ " + total);
                return "redirect:/ventas/create";
            }
            montoEfectivo = total;
            vuelto = monto_recibido.subtract(total);
        } else if ("mixto".equals(forma_pago)) {
            if (monto_recibido == null) {
                monto_recibido = BigDecimal.ZERO;
            }
            montoEfectivo = monto_recibido;
            montoDigital = total.subtract(montoEfectivo);
            if (montoDigital.compareTo(BigDecimal.ZERO) < 0) {
                montoDigital = BigDecimal.ZERO;
                vuelto = montoEfectivo.subtract(total);
            }
        } else {
            montoDigital = total;
        }

        Venta venta = Venta.builder()
                .codigo(codigo)
                .tipoVenta("stock")
                .clienteNombre(cliente_nombre != null ? cliente_nombre.trim() : null)
                .fechaVenta(LocalDate.now())
                .montoTotal(total)
                .montoCobrado(total)
                .estadoPago("pagado_completo")
                .observaciones(observaciones)
                .usuario(usuario)
                .cajaApertura(caja)
                .metodoPago(forma_pago)
                .montoEfectivo(montoEfectivo)
                .montoDigital(montoDigital)
                .vuelto(vuelto)
                .detalles(detalles)
                .build();

        venta = ventaRepository.save(venta);

        for (VentaDetalle detalle : detalles) {
            detalle.setVenta(venta);
            ventaDetalleRepository.save(detalle);

            Producto p = detalle.getProducto();
            p.setStockTienda(p.getStockTienda() - detalle.getCantidad());
            p.calcularStockActual();
            productoRepository.save(p);
        }

        Map<String, String> comprobanteDatos = new HashMap<>();
        comprobanteDatos.put("tipo_comprobante", tipo_comprobante);
        comprobanteDatos.put("documento_cliente",
                documento_cliente != null ? documento_cliente.trim() : null);
        comprobanteDatos.put("nombre_cliente",
                cliente_nombre != null ? cliente_nombre.trim() : null);
        comprobanteDatos.put("direccion_cliente",
                direccion_cliente != null ? direccion_cliente.trim() : null);

        comprobanteVentaService.emitir(venta, comprobanteDatos);

        flash.addFlashAttribute("exito",
                "Venta " + codigo + " registrada correctamente. Vuelto: S/ " + vuelto);
        return "redirect:/ventas";
    }

    @GetMapping("/{id}")
    public String show(@PathVariable Long id, Model model) {
        Venta venta = ventaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Venta no encontrada"));

        List<VentaDetalle> detalles = ventaDetalleRepository.findByVentaId(id);

        model.addAttribute("venta", venta);
        model.addAttribute("detalles", detalles);
        model.addAttribute("comprobante", venta.getComprobante());
        model.addAttribute("pedido", venta.getPedido());
        return "ventas/show";
    }

    @PostMapping("/seleccionar-caja/{id}")
    public String seleccionarCaja(@PathVariable Long id,
                                  @AuthenticationPrincipal Usuario usuario,
                                  RedirectAttributes flash) {
        CajaApertura caja = cajaAperturaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Apertura de caja no encontrada"));

        if (!caja.getUsuario().getId().equals(usuario.getId())) {
            flash.addFlashAttribute("error",
                    "Esta caja no pertenece al usuario actual");
            return "redirect:/ventas";
        }

        if (!"abierta".equals(caja.getEstado())) {
            flash.addFlashAttribute("error", "Esta caja no esta abierta");
            return "redirect:/ventas";
        }

        session.setAttribute("caja_apertura_id", caja.getId());
        flash.addFlashAttribute("exito",
                "Caja " + caja.getNombre() + " seleccionada correctamente");
        return "redirect:/ventas";
    }

    @PostMapping("/cambiar-caja")
    public String cambiarCaja(RedirectAttributes flash) {
        session.removeAttribute("caja_apertura_id");
        flash.addFlashAttribute("exito", "Caja cambiada correctamente. Seleccione una nueva caja");
        return "redirect:/ventas";
    }

    @PostMapping("/{id}/emitir-comprobante")
    public String emitirComprobante(@PathVariable Long id, RedirectAttributes flash) {
        Venta venta = ventaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Venta no encontrada"));

        if (venta.getComprobante() != null) {
            flash.addFlashAttribute("info",
                    "Esta venta ya tiene un comprobante emitido: "
                            + venta.getComprobante().getCodigo());
            return "redirect:/ventas/" + id;
        }

        Map<String, String> comprobanteDatos = new HashMap<>();
        comprobanteDatos.put("tipo_comprobante", "boleta");

        if (venta.getPedido() != null) {
            Pedido pedido = venta.getPedido();
            if (pedido.getDocumentoCliente() != null
                    && !pedido.getDocumentoCliente().trim().isEmpty()) {
                comprobanteDatos.put("documento_cliente",
                        pedido.getDocumentoCliente().trim());
            }
            if (pedido.getNombreCliente() != null
                    && !pedido.getNombreCliente().trim().isEmpty()) {
                comprobanteDatos.put("nombre_cliente",
                        pedido.getNombreCliente().trim());
            }
            if (pedido.getDireccionEntrega() != null
                    && !pedido.getDireccionEntrega().trim().isEmpty()) {
                comprobanteDatos.put("direccion_cliente",
                        pedido.getDireccionEntrega().trim());
            }
        }

        if (venta.getClienteNombre() != null
                && !venta.getClienteNombre().trim().isEmpty()) {
            comprobanteDatos.put("nombre_cliente",
                    venta.getClienteNombre().trim());
        }

        comprobanteVentaService.emitir(venta, comprobanteDatos);

        flash.addFlashAttribute("exito", "Comprobante emitido correctamente");
        return "redirect:/ventas/" + id;
    }
}
