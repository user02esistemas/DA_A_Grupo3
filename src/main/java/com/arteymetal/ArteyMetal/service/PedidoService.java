package com.arteymetal.ArteyMetal.service;

import com.arteymetal.ArteyMetal.entity.*;
import com.arteymetal.ArteyMetal.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Service
public class PedidoService {

    @Autowired private PedidoRepository pedidoRepository;
    @Autowired private ClienteRepository clienteRepository;
    @Autowired private PedidoProductoRepository pedidoProductoRepository;
    @Autowired private VentaRepository ventaRepository;

    public List<Pedido> listarTodos() {
        return pedidoRepository.findAllByOrderByIdDesc();
    }

    public List<Pedido> listarPorEstado(String estado) {
        return pedidoRepository.findByEstadoOrderByIdDesc(estado);
    }

    public Optional<Pedido> findById(Long id) {
        return pedidoRepository.findById(id);
    }

    public Optional<Pedido> findByCodigo(String codigo) {
        return pedidoRepository.findByCodigo(codigo);
    }

    @Transactional
    public Pedido guardar(Pedido pedido) {
        if (pedido.getCodigo() == null || pedido.getCodigo().isEmpty()) {
            pedido.setCodigo(generarCodigo());
        }
        if (pedido.getEstado() == null) pedido.setEstado("registrado");
        if (pedido.getEstadoPersonalizacion() == null) pedido.setEstadoPersonalizacion("sin_iniciar");
        if (pedido.getEstadoPago() == null) pedido.setEstadoPago("pendiente_adelanto");
        if (pedido.getCantidad() == null) pedido.setCantidad(1);

        if (pedido.getCliente() != null && pedido.getCliente().getId() != null) {
            pedido.setCliente(clienteRepository.findById(pedido.getCliente().getId()).orElse(null));
        }

        return pedidoRepository.save(pedido);
    }

    @Transactional
    public Pedido actualizar(Long id, Pedido datos) {
        Pedido pedido = pedidoRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Pedido no encontrado"));

        pedido.setNombreCliente(datos.getNombreCliente());
        pedido.setTelefonoCliente(datos.getTelefonoCliente());
        pedido.setTipoProducto(datos.getTipoProducto());
        pedido.setDetalleTrabajo(datos.getDetalleTrabajo());
        pedido.setCantidad(datos.getCantidad());
        pedido.setMontoTotal(datos.getMontoTotal());
        pedido.setObservaciones(datos.getObservaciones());
        pedido.setTipoEntrega(datos.getTipoEntrega());
        pedido.setDireccionEntrega(datos.getDireccionEntrega());
        pedido.setDistritoEntrega(datos.getDistritoEntrega());

        if (datos.getCliente() != null && datos.getCliente().getId() != null) {
            pedido.setCliente(clienteRepository.findById(datos.getCliente().getId()).orElse(null));
        }

        return pedidoRepository.save(pedido);
    }

    @Transactional
    public void eliminar(Long id) {
        pedidoRepository.deleteById(id);
    }

    @Transactional
    public void avanzarEstado(Long id, String nuevoEstado) {
        Pedido pedido = pedidoRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Pedido no encontrado"));
        pedido.setEstado(nuevoEstado);
        pedidoRepository.save(pedido);
    }

    @Transactional
    public void confirmarPago(Long id, String tipoPago, BigDecimal monto, String medio, String referencia) {
        Pedido pedido = pedidoRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Pedido no encontrado"));

        if ("adelanto".equals(tipoPago)) {
            pedido.setMontoAdelanto(monto);
            pedido.setMedioPagoAdelanto(medio);
            pedido.setReferenciaPagoAdelanto(referencia);
            pedido.setEstadoPagoAdelanto("pagado");
            if (pedido.getMontoTotal() != null) {
                pedido.setMontoSaldo(pedido.getMontoTotal().subtract(monto));
            }
            pedido.setEstadoPago("adelanto_pagado");
        } else {
            pedido.setMontoSaldo(monto);
            pedido.setMedioPagoSaldo(medio);
            pedido.setReferenciaPagoSaldo(referencia);
            pedido.setEstadoPagoSaldo("pagado");
            pedido.setEstadoPago("pagado_completo");
        }

        pedidoRepository.save(pedido);
    }

    public long contarPorEstado(String estado) {
        return pedidoRepository.countByEstado(estado);
    }

    private String generarCodigo() {
        long count = pedidoRepository.count() + 1;
        String fecha = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        return "PED-" + fecha + "-" + String.format("%04d", count);
    }
}
