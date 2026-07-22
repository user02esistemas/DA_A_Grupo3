package com.arteymetal.ArteyMetal.controller;

import com.arteymetal.ArteyMetal.entity.Pedido;
import com.arteymetal.ArteyMetal.entity.Usuario;
import com.arteymetal.ArteyMetal.entity.Venta;
import com.arteymetal.ArteyMetal.repository.ClienteRepository;
import com.arteymetal.ArteyMetal.repository.PedidoRepository;
import com.arteymetal.ArteyMetal.repository.ProductoRepository;
import com.arteymetal.ArteyMetal.repository.VentaRepository;
import com.arteymetal.ArteyMetal.service.PedidoService;
import com.arteymetal.ArteyMetal.service.ProductoService;
import com.arteymetal.ArteyMetal.service.VentaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
public class DashboardController {

    @Autowired private PedidoService pedidoService;
    @Autowired private ProductoService productoService;
    @Autowired private VentaService ventaService;
    @Autowired private PedidoRepository pedidoRepository;
    @Autowired private VentaRepository ventaRepository;
    @Autowired private ClienteRepository clienteRepository;
    @Autowired private ProductoRepository productoRepository;

    @GetMapping("/dashboard")
    public String index(Model model, @AuthenticationPrincipal Usuario usuario) {
        LocalDate hoy = LocalDate.now();
        LocalDate inicioRango = hoy.minusDays(13);

        Map<String, Object> metricas = new HashMap<>();
        metricas.put("pedidos_total", pedidoRepository.count());
        metricas.put("pedidos_produccion", pedidoRepository.countByEstado("en_produccion"));
        metricas.put("pedidos_listo_entrega", pedidoRepository.countByEstado("listo_entrega"));
        metricas.put("ventas_hoy", ventaRepository.sumMontoCobradoByFechaVentaBetween(hoy, hoy));
        metricas.put("clientes_total", clienteRepository.count());
        metricas.put("productos_total", productoRepository.count());

        List<Pedido> ultimosPedidos = pedidoRepository.findAll(Sort.by(Sort.Order.desc("id")))
                .stream().limit(5).collect(Collectors.toList());

        List<Venta> ultimasVentas = ventaRepository.findAll(Sort.by(Sort.Order.desc("id")))
                .stream().limit(5).collect(Collectors.toList());

        Map<String, Object> graficos = new HashMap<>();

        List<Venta> todasVentas = ventaRepository.findAll(Sort.by(Sort.Order.asc("fechaVenta")));
        Map<LocalDate, BigDecimal> ventasPorDia = new LinkedHashMap<>();
        for (LocalDate d = inicioRango; !d.isAfter(hoy); d = d.plusDays(1)) {
            ventasPorDia.put(d, BigDecimal.ZERO);
        }
        for (Venta v : todasVentas) {
            if (v.getFechaVenta() != null && !v.getFechaVenta().isBefore(inicioRango) && !v.getFechaVenta().isAfter(hoy)) {
                BigDecimal monto = v.getMontoCobrado() != null ? v.getMontoCobrado() : BigDecimal.ZERO;
                ventasPorDia.merge(v.getFechaVenta(), monto, BigDecimal::add);
            }
        }

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM");
        List<String> labelsVentas = new ArrayList<>();
        List<BigDecimal> dataVentas = new ArrayList<>();
        for (Map.Entry<LocalDate, BigDecimal> entry : ventasPorDia.entrySet()) {
            labelsVentas.add(entry.getKey().format(fmt));
            dataVentas.add(entry.getValue());
        }

        Map<String, Object> ventas14Dias = new HashMap<>();
        ventas14Dias.put("labels", labelsVentas);
        ventas14Dias.put("data", dataVentas);
        graficos.put("ventas_14_dias", ventas14Dias);

        String[] estados = {"registrado", "en_produccion", "listo_entrega", "entregado", "cancelado"};
        String[] labelsEstado = {"Registrado", "En produccion", "Listo entrega", "Entregado", "Cancelado"};
        List<Long> dataEstado = new ArrayList<>();
        for (String estado : estados) {
            dataEstado.add(pedidoRepository.countByEstado(estado));
        }

        Map<String, Object> pedidosEstado = new HashMap<>();
        pedidosEstado.put("labels", Arrays.asList(labelsEstado));
        pedidosEstado.put("data", dataEstado);
        graficos.put("pedidos_estado", pedidosEstado);

        model.addAttribute("metricas", metricas);
        model.addAttribute("ultimosPedidos", ultimosPedidos);
        model.addAttribute("ultimasVentas", ultimasVentas);
        model.addAttribute("graficos", graficos);
        model.addAttribute("usuario", usuario);
        return "dashboard/index";
    }

    @GetMapping("/acceso-denegado")
    public String accesoDenegado() {
        return "acceso-denegado";
    }
}
