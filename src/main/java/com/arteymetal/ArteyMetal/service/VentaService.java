package com.arteymetal.ArteyMetal.service;

import com.arteymetal.ArteyMetal.entity.*;
import com.arteymetal.ArteyMetal.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Service
public class VentaService {

    @Autowired private VentaRepository ventaRepository;
    @Autowired private VentaDetalleRepository ventaDetalleRepository;
    @Autowired private ProductoRepository productoRepository;
    @Autowired private CajaAperturaRepository cajaAperturaRepository;
    @Autowired private PedidoRepository pedidoRepository;

    public List<Venta> listarTodas() {
        return ventaRepository.findAllByOrderByFechaVentaDesc();
    }

    public List<Venta> listarPorCaja(Long cajaAperturaId) {
        return ventaRepository.findByCajaAperturaId(cajaAperturaId);
    }

    public Optional<Venta> findById(Long id) {
        return ventaRepository.findById(id);
    }

    @Transactional
    public Venta crearVentaDirecta(List<VentaDetalle> detalles, String clienteNombre, Long cajaAperturaId, Usuario usuario) {
        BigDecimal total = BigDecimal.ZERO;
        for (VentaDetalle d : detalles) {
            total = total.add(d.getSubtotal());
        }

        Venta venta = Venta.builder()
            .codigo(generarCodigo())
            .tipoVenta("stock")
            .clienteNombre(clienteNombre)
            .fechaVenta(LocalDate.now())
            .montoTotal(total)
            .montoCobrado(total)
            .estadoPago("pagado_completo")
            .usuario(usuario)
            .build();

        if (cajaAperturaId != null) {
            CajaApertura caja = cajaAperturaRepository.findById(cajaAperturaId).orElse(null);
            venta.setCajaApertura(caja);
        }

        venta = ventaRepository.save(venta);

        for (VentaDetalle d : detalles) {
            d.setVenta(venta);
            ventaDetalleRepository.save(d);

            if (d.getProducto() != null) {
                Producto p = d.getProducto();
                p.setStockTienda(p.getStockTienda() - d.getCantidad());
                p.calcularStockActual();
                productoRepository.save(p);
            }
        }

        return venta;
    }

    @Transactional
    public Venta cerrarPedido(Long pedidoId, Long cajaAperturaId, Usuario usuario) {
        Pedido pedido = pedidoRepository.findById(pedidoId)
            .orElseThrow(() -> new RuntimeException("Pedido no encontrado"));

        Venta venta = Venta.builder()
            .codigo(generarCodigo())
            .tipoVenta("pedido")
            .pedido(pedido)
            .clienteNombre(pedido.getNombreCliente())
            .fechaVenta(LocalDate.now())
            .montoTotal(pedido.getMontoTotal())
            .montoCobrado(pedido.getMontoSaldo())
            .estadoPago("pagado_completo")
            .usuario(usuario)
            .build();

        if (cajaAperturaId != null) {
            CajaApertura caja = cajaAperturaRepository.findById(cajaAperturaId).orElse(null);
            venta.setCajaApertura(caja);
        }

        venta = ventaRepository.save(venta);

        pedido.setEstado("entregado");
        pedidoRepository.save(pedido);

        return venta;
    }

    public BigDecimal totalVentasPorCaja(Long cajaAperturaId) {
        BigDecimal total = ventaRepository.sumTotalByCajaAperturaId(cajaAperturaId);
        return total != null ? total : BigDecimal.ZERO;
    }

    private String generarCodigo() {
        long count = ventaRepository.count() + 1;
        return "VEN-" + String.format("%06d", count);
    }
}
