package com.arteymetal.ArteyMetal.service;

import com.arteymetal.ArteyMetal.entity.*;
import com.arteymetal.ArteyMetal.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class CajaService {

    @Autowired private CajaRepository cajaRepository;
    @Autowired private CajaAperturaRepository cajaAperturaRepository;
    @Autowired private VentaRepository ventaRepository;
    @Autowired private UsuarioRepository usuarioRepository;

    public List<Caja> listarCajas() {
        return cajaRepository.findAll();
    }

    public List<CajaApertura> aperturasAbiertas(Long usuarioId) {
        return cajaAperturaRepository.findByUsuarioIdAndEstado(usuarioId, "abierta");
    }

    public Optional<CajaApertura> findById(Long id) {
        return cajaAperturaRepository.findById(id);
    }

    @Transactional
    public CajaApertura abrirCaja(Long cajaId, Long usuarioId, BigDecimal montoInicial, String nombre) {
        Caja caja = cajaRepository.findById(cajaId)
            .orElseThrow(() -> new RuntimeException("Caja no encontrada"));

        Usuario usuario = usuarioRepository.findById(usuarioId)
            .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        CajaApertura apertura = CajaApertura.builder()
            .caja(caja)
            .usuario(usuario)
            .nombre(nombre != null ? nombre : caja.getNombre())
            .fechaApertura(LocalDateTime.now())
            .montoInicial(montoInicial)
            .totalVentas(BigDecimal.ZERO)
            .estado("abierta")
            .build();

        return cajaAperturaRepository.save(apertura);
    }

    @Transactional
    public void cerrarCaja(Long aperturaId, BigDecimal montoFinal) {
        CajaApertura apertura = cajaAperturaRepository.findById(aperturaId)
            .orElseThrow(() -> new RuntimeException("Apertura no encontrada"));

        BigDecimal totalVentas = ventaRepository.sumTotalByCajaAperturaId(aperturaId);

        apertura.setFechaCierre(LocalDateTime.now());
        apertura.setMontoFinal(montoFinal);
        apertura.setTotalVentas(totalVentas);
        apertura.setEstado("cerrada");
        cajaAperturaRepository.save(apertura);
    }

    @Transactional
    public void guardarCaja(Caja caja) {
        cajaRepository.save(caja);
    }

    @Transactional
    public void eliminarCaja(Long id) {
        cajaRepository.deleteById(id);
    }
}
