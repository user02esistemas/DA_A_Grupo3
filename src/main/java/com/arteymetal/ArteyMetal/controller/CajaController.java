package com.arteymetal.ArteyMetal.controller;

import com.arteymetal.ArteyMetal.entity.*;
import com.arteymetal.ArteyMetal.repository.*;
import com.arteymetal.ArteyMetal.service.CajaService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Controller
@RequestMapping("/caja")
public class CajaController {

    @Autowired private CajaService cajaService;
    @Autowired private CajaRepository cajaRepository;
    @Autowired private CajaAperturaRepository cajaAperturaRepository;
    @Autowired private VentaRepository ventaRepository;

    @GetMapping
    public String index(Model model,
                        @RequestParam(defaultValue = "") String q,
                        @RequestParam(name = "estado", defaultValue = "") String filtroEstado,
                        @RequestParam(defaultValue = "") String cq,
                        @RequestParam(name = "cq_estado", defaultValue = "") String filtroCajaEstado,
                        @RequestParam(defaultValue = "0") int page) {
        Pageable pageable = PageRequest.of(page, 12);
        Page<CajaApertura> aperturas = cajaAperturaRepository.searchWithFilters(q, filtroEstado, pageable);
        List<Caja> cajas = cajaRepository.findAll().stream()
                .filter(c -> cq.isBlank() || c.getNombre().toLowerCase().contains(cq.toLowerCase()))
                .toList();

        List<CajaApertura> aperturasRecientes = cajaAperturaRepository.findAllByOrderByIdDesc(PageRequest.of(0, 500)).toList();
        Map<Long, CajaApertura> aperturaActiva = new LinkedHashMap<>();
        Map<Long, BigDecimal> ultimoMontoFinal = new LinkedHashMap<>();
        for (CajaApertura ap : aperturasRecientes) {
            Long cajaId = ap.getCaja().getId();
            if ("abierta".equals(ap.getEstado())) aperturaActiva.putIfAbsent(cajaId, ap);
            if ("cerrada".equals(ap.getEstado()) && ap.getMontoFinal() != null) ultimoMontoFinal.putIfAbsent(cajaId, ap.getMontoFinal());
        }

        if (!filtroCajaEstado.isBlank()) {
            cajas = cajas.stream().filter(c -> "abierta".equals(filtroCajaEstado)
                    ? aperturaActiva.containsKey(c.getId())
                    : !aperturaActiva.containsKey(c.getId())).toList();
        }

        Map<Long, Map<String, Object>> resumenAperturas = new LinkedHashMap<>();
        for (CajaApertura ap : aperturas) {
            List<Venta> ventas = ventaRepository.findByCajaAperturaId(ap.getId());
            BigDecimal efectivo = ventas.stream().map(Venta::getMontoEfectivo).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal digital = ventas.stream().map(Venta::getMontoDigital).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal vuelto = ventas.stream().map(Venta::getVuelto).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal efectivoFinal = ap.getMontoInicial().add(efectivo).subtract(vuelto);
            Map<String, Object> resumen = new LinkedHashMap<>();
            resumen.put("efectivo", efectivo);
            resumen.put("digital", digital);
            resumen.put("vuelto", vuelto);
            resumen.put("efectivoFinal", efectivoFinal);
            resumen.put("totalFinal", efectivoFinal.add(digital));
            resumen.put("cantidadVentas", ventas.size());
            resumenAperturas.put(ap.getId(), resumen);
        }

        model.addAttribute("cajas", cajas);
        model.addAttribute("aperturas", aperturas);
        model.addAttribute("busqueda", q);
        model.addAttribute("filtroEstado", filtroEstado);
        model.addAttribute("busquedaCaja", cq);
        model.addAttribute("filtroCajaEstado", filtroCajaEstado);
        model.addAttribute("aperturaActiva", aperturaActiva);
        model.addAttribute("ultimoMontoFinal", ultimoMontoFinal);
        model.addAttribute("resumenAperturas", resumenAperturas);
        return "caja/index";
    }

    @PostMapping("/{id}/actualizar")
    public String actualizar(@PathVariable Long id,
                             @RequestParam String nombre,
                             @RequestParam(defaultValue = "false") boolean activa,
                             RedirectAttributes flash) {
        Caja caja = cajaRepository.findById(id).orElseThrow(() -> new RuntimeException("Caja no encontrada"));
        caja.setNombre(nombre.trim());
        caja.setActiva(activa);
        cajaRepository.save(caja);
        flash.addFlashAttribute("exito", "Caja actualizada correctamente");
        return "redirect:/caja";
    }

    @PostMapping("/{id}/eliminar")
    public String eliminar(@PathVariable Long id, RedirectAttributes flash) {
        if (cajaAperturaRepository.existsByCajaId(id)) {
            flash.addFlashAttribute("error", "No se puede eliminar una caja que tiene registros de apertura");
            return "redirect:/caja";
        }
        cajaRepository.deleteById(id);
        flash.addFlashAttribute("exito", "Caja eliminada correctamente");
        return "redirect:/caja";
    }

    @PostMapping("/abrir")
    public String abrir(@RequestParam Long caja_id,
                        @RequestParam BigDecimal monto_inicial,
                        @RequestParam(required = false) String observaciones,
                        @AuthenticationPrincipal Usuario usuario,
                        RedirectAttributes flash,
                        HttpSession session) {
        if (caja_id == null) {
            flash.addFlashAttribute("error", "Debe seleccionar una caja");
            return "redirect:/caja";
        }

        if (monto_inicial == null || monto_inicial.compareTo(BigDecimal.ZERO) < 0) {
            flash.addFlashAttribute("error", "El monto inicial debe ser un número mayor o igual a 0");
            return "redirect:/caja";
        }

        Optional<Caja> cajaOpt = cajaRepository.findById(caja_id);
        if (cajaOpt.isEmpty()) {
            flash.addFlashAttribute("error", "La caja seleccionada no existe");
            return "redirect:/caja";
        }

        long aperturasAbiertas = cajaAperturaRepository.countByCajaIdAndEstado(caja_id, "abierta");
        if (aperturasAbiertas > 0) {
            flash.addFlashAttribute("error", "Esta caja ya tiene una apertura activa");
            return "redirect:/caja";
        }

        Caja caja = cajaOpt.get();
        CajaApertura apertura = CajaApertura.builder()
                .usuario(usuario)
                .caja(caja)
                .nombre(caja.getNombre())
                .fechaApertura(LocalDateTime.now())
                .montoInicial(monto_inicial)
                .totalVentas(BigDecimal.ZERO)
                .estado("abierta")
                .observaciones(observaciones)
                .build();
        cajaAperturaRepository.save(apertura);
        session.setAttribute("caja_apertura_id", apertura.getId());

        flash.addFlashAttribute("exito", "Caja abierta correctamente");
        return "redirect:/caja";
    }

    @PostMapping("/{id}/cerrar")
    public String cerrar(@PathVariable Long id,
                         @RequestParam BigDecimal monto_final,
                         @RequestParam(required = false) String observaciones,
                         RedirectAttributes flash,
                         HttpSession session) {
        Optional<CajaApertura> aperturaOpt = cajaAperturaRepository.findById(id);
        if (aperturaOpt.isEmpty()) {
            flash.addFlashAttribute("error", "Apertura no encontrada");
            return "redirect:/caja";
        }

        CajaApertura apertura = aperturaOpt.get();
        if (!"abierta".equals(apertura.getEstado())) {
            flash.addFlashAttribute("error", "Esta caja ya fue cerrada");
            return "redirect:/caja";
        }

        Page<Venta> ventasPage = ventaRepository.findByCajaAperturaId(id, PageRequest.of(0, 10000));
        List<Venta> ventas = ventasPage.getContent();

        BigDecimal totalEfectivo = BigDecimal.ZERO;
        BigDecimal totalDigital = BigDecimal.ZERO;
        BigDecimal totalVuelto = BigDecimal.ZERO;

        for (Venta v : ventas) {
            if (v.getMontoEfectivo() != null) totalEfectivo = totalEfectivo.add(v.getMontoEfectivo());
            if (v.getMontoDigital() != null) totalDigital = totalDigital.add(v.getMontoDigital());
            if (v.getVuelto() != null) totalVuelto = totalVuelto.add(v.getVuelto());
        }

        BigDecimal totalVentas = totalEfectivo.add(totalDigital).subtract(totalVuelto);

        apertura.setFechaCierre(LocalDateTime.now());
        apertura.setMontoFinal(monto_final);
        apertura.setTotalVentas(totalVentas);
        apertura.setEstado("cerrada");
        if (observaciones != null && !observaciones.isBlank()) apertura.setObservaciones(observaciones);
        cajaAperturaRepository.save(apertura);

        Long aperturaSesion = (Long) session.getAttribute("caja_apertura_id");
        if (aperturaSesion != null && aperturaSesion.equals(id)) {
            session.removeAttribute("caja_apertura_id");
        }

        flash.addFlashAttribute("exito", "Caja cerrada correctamente. Total ventas: S/ " + totalVentas);
        return "redirect:/caja";
    }

    @GetMapping("/{id}")
    public String show(@PathVariable Long id, Model model) {
        CajaApertura apertura = cajaAperturaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Apertura no encontrada"));

        Page<Venta> ventasPage = ventaRepository.findByCajaAperturaId(id, PageRequest.of(0, 10000));
        List<Venta> ventas = ventasPage.getContent();

        BigDecimal totalEfectivoVentas = BigDecimal.ZERO;
        BigDecimal totalDigitalVentas = BigDecimal.ZERO;
        BigDecimal totalVuelto = BigDecimal.ZERO;

        for (Venta v : ventas) {
            if (v.getMontoEfectivo() != null) totalEfectivoVentas = totalEfectivoVentas.add(v.getMontoEfectivo());
            if (v.getMontoDigital() != null) totalDigitalVentas = totalDigitalVentas.add(v.getMontoDigital());
            if (v.getVuelto() != null) totalVuelto = totalVuelto.add(v.getVuelto());
        }

        model.addAttribute("apertura", apertura);
        model.addAttribute("ventas", ventas);
        model.addAttribute("totalEfectivoVentas", totalEfectivoVentas);
        model.addAttribute("totalDigitalVentas", totalDigitalVentas);
        model.addAttribute("totalVuelto", totalVuelto);
        model.addAttribute("cantidadVentas", ventas.size());
        return "caja/show";
    }
}
