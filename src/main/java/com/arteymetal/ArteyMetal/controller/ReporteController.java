package com.arteymetal.ArteyMetal.controller;

import com.arteymetal.ArteyMetal.entity.*;
import com.arteymetal.ArteyMetal.repository.*;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Controller
@RequestMapping("/reportes")
public class ReporteController {

    @Autowired private VentaRepository ventaRepository;
    @Autowired private PedidoRepository pedidoRepository;
    @Autowired private ProductoRepository productoRepository;

    @GetMapping
    public String index(Model model,
                        @RequestParam(required = false) String ventas_fecha_inicio,
                        @RequestParam(required = false) String ventas_fecha_fin,
                        @RequestParam(required = false) String ventas_tipo,
                        @RequestParam(required = false) String pedidos_fecha_inicio,
                        @RequestParam(required = false) String pedidos_fecha_fin,
                        @RequestParam(required = false) String pedidos_estado,
                        @RequestParam(required = false) String pedidos_tipo_entrega,
                        @RequestParam(required = false, defaultValue = "10") Integer umbral) {

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDate hoy = LocalDate.now();

        Map<String, String> filtrosVentas = new LinkedHashMap<>();
        filtrosVentas.put("fecha_inicio", ventas_fecha_inicio != null ? ventas_fecha_inicio : "");
        filtrosVentas.put("fecha_fin", ventas_fecha_fin != null ? ventas_fecha_fin : "");
        filtrosVentas.put("tipo", ventas_tipo != null ? ventas_tipo : "");

        Map<String, String> filtrosPedidos = new LinkedHashMap<>();
        filtrosPedidos.put("fecha_inicio", pedidos_fecha_inicio != null ? pedidos_fecha_inicio : "");
        filtrosPedidos.put("fecha_fin", pedidos_fecha_fin != null ? pedidos_fecha_fin : "");
        filtrosPedidos.put("estado", pedidos_estado != null ? pedidos_estado : "");
        filtrosPedidos.put("tipo_entrega", pedidos_tipo_entrega != null ? pedidos_tipo_entrega : "");

        List<Venta> todasVentas = ventaRepository.findAll();
        List<Venta> ventasFiltradas = new ArrayList<>();
        for (Venta v : todasVentas) {
            boolean pasa = true;
            if (ventas_fecha_inicio != null && !ventas_fecha_inicio.isEmpty()) {
                LocalDate inicio = LocalDate.parse(ventas_fecha_inicio, fmt);
                if (v.getFechaVenta().isBefore(inicio)) pasa = false;
            }
            if (ventas_fecha_fin != null && !ventas_fecha_fin.isEmpty()) {
                LocalDate fin = LocalDate.parse(ventas_fecha_fin, fmt);
                if (v.getFechaVenta().isAfter(fin)) pasa = false;
            }
            if (ventas_tipo != null && !ventas_tipo.isEmpty()) {
                if (!ventas_tipo.equals(v.getTipoVenta())) pasa = false;
            }
            if (pasa) ventasFiltradas.add(v);
        }
        Collections.sort(ventasFiltradas, (a, b) -> Long.compare(b.getId() != null ? b.getId() : 0, a.getId() != null ? a.getId() : 0));

        BigDecimal totalVendido = BigDecimal.ZERO;
        BigDecimal totalCobrado = BigDecimal.ZERO;
        for (Venta v : ventasFiltradas) {
            if (v.getMontoTotal() != null) totalVendido = totalVendido.add(v.getMontoTotal());
            if (v.getMontoCobrado() != null) totalCobrado = totalCobrado.add(v.getMontoCobrado());
        }
        int cantidadVentas = ventasFiltradas.size();
        BigDecimal ticketPromedio = cantidadVentas > 0
                ? totalVendido.divide(BigDecimal.valueOf(cantidadVentas), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        Map<String, Object> kpisVentas = new LinkedHashMap<>();
        kpisVentas.put("total_vendido", totalVendido);
        kpisVentas.put("total_cobrado", totalCobrado);
        kpisVentas.put("ticket_promedio", ticketPromedio);
        kpisVentas.put("cantidad", cantidadVentas);

        List<Venta> ultimasVentas = ventasFiltradas.size() > 20 ? ventasFiltradas.subList(0, 20) : ventasFiltradas;

        List<Pedido> todosPedidos = pedidoRepository.findAll();
        List<Pedido> pedidosFiltrados = new ArrayList<>();
        for (Pedido p : todosPedidos) {
            boolean pasa = true;
            if (pedidos_fecha_inicio != null && !pedidos_fecha_inicio.isEmpty()) {
                LocalDate inicio = LocalDate.parse(pedidos_fecha_inicio, fmt);
                if (p.getFechaEntregaCompromiso() != null && p.getFechaEntregaCompromiso().isBefore(inicio)) pasa = false;
            }
            if (pedidos_fecha_fin != null && !pedidos_fecha_fin.isEmpty()) {
                LocalDate fin = LocalDate.parse(pedidos_fecha_fin, fmt);
                if (p.getFechaEntregaCompromiso() != null && p.getFechaEntregaCompromiso().isAfter(fin)) pasa = false;
            }
            if (pedidos_estado != null && !pedidos_estado.isEmpty()) {
                if (!pedidos_estado.equals(p.getEstado())) pasa = false;
            }
            if (pedidos_tipo_entrega != null && !pedidos_tipo_entrega.isEmpty()) {
                if (!pedidos_tipo_entrega.equals(p.getTipoEntrega())) pasa = false;
            }
            if (pasa) pedidosFiltrados.add(p);
        }
        Collections.sort(pedidosFiltrados, (a, b) -> Long.compare(b.getId() != null ? b.getId() : 0, a.getId() != null ? a.getId() : 0));

        long totalPedidos = pedidosFiltrados.size();
        long registrados = pedidosFiltrados.stream().filter(p -> "registrado".equals(p.getEstado())).count();
        long enProduccion = pedidosFiltrados.stream().filter(p -> "en_produccion".equals(p.getEstado())).count();
        long listoEntrega = pedidosFiltrados.stream().filter(p -> "listo_entrega".equals(p.getEstado())).count();
        long entregados = pedidosFiltrados.stream().filter(p -> "entregado".equals(p.getEstado())).count();
        long atrasados = pedidosFiltrados.stream().filter(p ->
                p.getFechaEntregaCompromiso() != null
                && p.getFechaEntregaCompromiso().isBefore(hoy)
                && !"entregado".equals(p.getEstado())
                && !"cancelado".equals(p.getEstado())
        ).count();

        Map<String, Object> kpisPedidos = new LinkedHashMap<>();
        kpisPedidos.put("total", totalPedidos);
        kpisPedidos.put("registrados", registrados);
        kpisPedidos.put("en_produccion", enProduccion);
        kpisPedidos.put("listo_entrega", listoEntrega);
        kpisPedidos.put("entregados", entregados);
        kpisPedidos.put("atrasados", atrasados);

        List<Pedido> saldosPendientes = new ArrayList<>();
        for (Pedido p : todosPedidos) {
            boolean tieneSaldo = (p.getMontoSaldo() != null && p.getMontoSaldo().compareTo(BigDecimal.ZERO) > 0);
            boolean noPagado = !"pagado_completo".equals(p.getEstadoPago());
            if (tieneSaldo || noPagado) {
                if (!"cancelado".equals(p.getEstado()) && !"entregado".equals(p.getEstado())) {
                    saldosPendientes.add(p);
                }
            }
        }
        BigDecimal totalPendiente = BigDecimal.ZERO;
        for (Pedido p : saldosPendientes) {
            if (p.getMontoSaldo() != null) totalPendiente = totalPendiente.add(p.getMontoSaldo());
        }

        Map<String, Object> kpisSaldos = new LinkedHashMap<>();
        kpisSaldos.put("count", saldosPendientes.size());
        kpisSaldos.put("total_pendiente", totalPendiente);

        List<Producto> todosProductos = productoRepository.findAll();
        int umbralVal = umbral != null ? umbral : 10;
        List<Producto> stockBajo = new ArrayList<>();
        int unidadesEnRiesgo = 0;
        for (Producto p : todosProductos) {
            int stock = p.getStockActual() != null ? p.getStockActual() : 0;
            if (stock <= umbralVal && p.getActivo()) {
                stockBajo.add(p);
                unidadesEnRiesgo += stock;
            }
        }

        Map<String, Object> kpisStock = new LinkedHashMap<>();
        kpisStock.put("count", stockBajo.size());
        kpisStock.put("unidades_riesgo", unidadesEnRiesgo);

        Map<String, Object> graficos = new LinkedHashMap<>();

        Map<String, Long> ventasPorDia = new LinkedHashMap<>();
        LocalDate fechaInicioGrafico = hoy.minusDays(29);
        for (int i = 0; i < 30; i++) {
            LocalDate fecha = fechaInicioGrafico.plusDays(i);
            ventasPorDia.put(fecha.format(fmt), 0L);
        }
        for (Venta v : todasVentas) {
            if (v.getFechaVenta() != null && v.getFechaVenta().isAfter(fechaInicioGrafico.minusDays(1))) {
                String clave = v.getFechaVenta().format(fmt);
                ventasPorDia.merge(clave, 1L, Long::sum);
            }
        }
        graficos.put("ventas_por_dia", ventasPorDia);

        Map<String, Long> ventasPorTipo = new LinkedHashMap<>();
        for (Venta v : todasVentas) {
            String tipo = v.getTipoVenta() != null ? v.getTipoVenta() : "Sin tipo";
            ventasPorTipo.merge(tipo, 1L, Long::sum);
        }
        graficos.put("ventas_por_tipo", ventasPorTipo);

        Map<String, Long> pedidosPorEstado = new LinkedHashMap<>();
        for (Pedido p : todosPedidos) {
            String estado = p.getEstado() != null ? p.getEstado() : "Sin estado";
            pedidosPorEstado.merge(estado, 1L, Long::sum);
        }
        graficos.put("pedidos_por_estado", pedidosPorEstado);

        Map<String, Long> pedidosPorEntrega = new LinkedHashMap<>();
        for (Pedido p : todosPedidos) {
            String tipo = p.getTipoEntrega() != null ? p.getTipoEntrega() : "Sin tipo";
            pedidosPorEntrega.merge(tipo, 1L, Long::sum);
        }
        graficos.put("pedidos_por_entrega", pedidosPorEntrega);

        List<Producto> stockCriticoTop10 = stockBajo.stream()
                .sorted(Comparator.comparingInt(p -> p.getStockActual() != null ? p.getStockActual() : 0))
                .limit(10)
                .collect(Collectors.toList());
        Map<String, Integer> stockCriticoMap = new LinkedHashMap<>();
        for (Producto p : stockCriticoTop10) {
            stockCriticoMap.put(p.getNombre(), p.getStockActual() != null ? p.getStockActual() : 0);
        }
        graficos.put("stock_critico_top10", stockCriticoMap);

        model.addAttribute("filtrosVentas", filtrosVentas);
        model.addAttribute("kpisVentas", kpisVentas);
        model.addAttribute("ventas", ultimasVentas);
        model.addAttribute("filtrosPedidos", filtrosPedidos);
        model.addAttribute("kpisPedidos", kpisPedidos);
        model.addAttribute("pedidos", pedidosFiltrados);
        model.addAttribute("saldosPendientes", saldosPendientes);
        model.addAttribute("kpisSaldos", kpisSaldos);
        model.addAttribute("stockBajo", stockBajo);
        model.addAttribute("kpisStock", kpisStock);
        model.addAttribute("graficos", graficos);

        return "reportes/index";
    }

    @GetMapping("/ventas/csv")
    public void exportarVentasCsv(
            @RequestParam(required = false) String ventas_fecha_inicio,
            @RequestParam(required = false) String ventas_fecha_fin,
            @RequestParam(required = false) String ventas_tipo,
            HttpServletResponse response) throws Exception {

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        List<Venta> ventasFiltradas = filtrarVentas(ventas_fecha_inicio, ventas_fecha_fin, ventas_tipo, fmt);

        String nombre = "reporte_ventas_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=" + nombre + ".csv");

        PrintWriter out = response.getWriter();
        out.write("\uFEFF");
        out.println("sep=;");
        out.println("Codigo;Fecha;Tipo;Cliente;Monto total;Monto cobrado;Estado pago");
        for (Venta v : ventasFiltradas) {
            out.println(String.join(";",
                v.getCodigo() != null ? v.getCodigo() : "",
                v.getFechaVenta() != null ? v.getFechaVenta().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "",
                "pedido".equals(v.getTipoVenta()) ? "Cierre pedido" : "Venta stock",
                v.getClienteNombre() != null ? v.getClienteNombre() : "-",
                v.getMontoTotal() != null ? v.getMontoTotal().setScale(2, RoundingMode.HALF_UP).toString() : "0.00",
                v.getMontoCobrado() != null ? v.getMontoCobrado().setScale(2, RoundingMode.HALF_UP).toString() : "0.00",
                v.getEstadoPago() != null ? v.getEstadoPago().replace("_", " ") : ""
            ));
        }
        out.flush();
    }

    @GetMapping("/ventas/excel")
    public void exportarVentasExcel(
            @RequestParam(required = false) String ventas_fecha_inicio,
            @RequestParam(required = false) String ventas_fecha_fin,
            @RequestParam(required = false) String ventas_tipo,
            HttpServletResponse response) throws Exception {

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        List<Venta> ventasFiltradas = filtrarVentas(ventas_fecha_inicio, ventas_fecha_fin, ventas_tipo, fmt);

        String nombre = "reporte_ventas_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String[][] datos = new String[ventasFiltradas.size() + 1][7];
        datos[0] = new String[]{"Codigo", "Fecha", "Tipo", "Cliente", "Monto total", "Monto cobrado", "Estado pago"};
        for (int i = 0; i < ventasFiltradas.size(); i++) {
            Venta v = ventasFiltradas.get(i);
            datos[i + 1] = new String[]{
                v.getCodigo() != null ? v.getCodigo() : "",
                v.getFechaVenta() != null ? v.getFechaVenta().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "",
                "pedido".equals(v.getTipoVenta()) ? "Cierre pedido" : "Venta stock",
                v.getClienteNombre() != null ? v.getClienteNombre() : "-",
                v.getMontoTotal() != null ? v.getMontoTotal().setScale(2, RoundingMode.HALF_UP).toString() : "0.00",
                v.getMontoCobrado() != null ? v.getMontoCobrado().setScale(2, RoundingMode.HALF_UP).toString() : "0.00",
                v.getEstadoPago() != null ? v.getEstadoPago().replace("_", " ") : ""
            };
        }
        descargarXlsx(response, nombre, "Ventas", datos);
    }

    @GetMapping("/pedidos/csv")
    public void exportarPedidosCsv(
            @RequestParam(required = false) String pedidos_fecha_inicio,
            @RequestParam(required = false) String pedidos_fecha_fin,
            @RequestParam(required = false) String pedidos_estado,
            @RequestParam(required = false) String pedidos_tipo_entrega,
            HttpServletResponse response) throws Exception {

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        List<Pedido> pedidosFiltrados = filtrarPedidos(pedidos_fecha_inicio, pedidos_fecha_fin, pedidos_estado, pedidos_tipo_entrega, fmt);

        String nombre = "reporte_pedidos_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=" + nombre + ".csv");

        PrintWriter out = response.getWriter();
        out.write("\uFEFF");
        out.println("sep=;");
        out.println("Codigo;Cliente;Estado;Tipo entrega;Fecha compromiso;Total");
        for (Pedido p : pedidosFiltrados) {
            out.println(String.join(";",
                p.getCodigo() != null ? p.getCodigo() : "",
                p.getNombreCliente() != null ? p.getNombreCliente() : "",
                p.getEstado() != null ? p.getEstado().replace("_", " ") : "",
                p.getTipoEntrega() != null ? capitalize(p.getTipoEntrega()) : "Local",
                p.getFechaEntregaCompromiso() != null ? p.getFechaEntregaCompromiso().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "-",
                p.getMontoTotal() != null ? p.getMontoTotal().setScale(2, RoundingMode.HALF_UP).toString() : ""
            ));
        }
        out.flush();
    }

    @GetMapping("/pedidos/excel")
    public void exportarPedidosExcel(
            @RequestParam(required = false) String pedidos_fecha_inicio,
            @RequestParam(required = false) String pedidos_fecha_fin,
            @RequestParam(required = false) String pedidos_estado,
            @RequestParam(required = false) String pedidos_tipo_entrega,
            HttpServletResponse response) throws Exception {

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        List<Pedido> pedidosFiltrados = filtrarPedidos(pedidos_fecha_inicio, pedidos_fecha_fin, pedidos_estado, pedidos_tipo_entrega, fmt);

        String nombre = "reporte_pedidos_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String[][] datos = new String[pedidosFiltrados.size() + 1][6];
        datos[0] = new String[]{"Codigo", "Cliente", "Estado", "Tipo entrega", "Fecha compromiso", "Total"};
        for (int i = 0; i < pedidosFiltrados.size(); i++) {
            Pedido p = pedidosFiltrados.get(i);
            datos[i + 1] = new String[]{
                p.getCodigo() != null ? p.getCodigo() : "",
                p.getNombreCliente() != null ? p.getNombreCliente() : "",
                p.getEstado() != null ? p.getEstado().replace("_", " ") : "",
                p.getTipoEntrega() != null ? capitalize(p.getTipoEntrega()) : "Local",
                p.getFechaEntregaCompromiso() != null ? p.getFechaEntregaCompromiso().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "-",
                p.getMontoTotal() != null ? p.getMontoTotal().setScale(2, RoundingMode.HALF_UP).toString() : ""
            };
        }
        descargarXlsx(response, nombre, "Pedidos", datos);
    }

    @GetMapping("/saldos/csv")
    public void exportarSaldosCsv(
            @RequestParam(required = false) String pedidos_fecha_inicio,
            @RequestParam(required = false) String pedidos_fecha_fin,
            HttpServletResponse response) throws Exception {

        List<Pedido> saldos = filtrarSaldos(pedidos_fecha_inicio, pedidos_fecha_fin);

        String nombre = "reporte_saldos_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=" + nombre + ".csv");

        PrintWriter out = response.getWriter();
        out.write("\uFEFF");
        out.println("sep=;");
        out.println("Codigo;Cliente;Cancelado;Saldo pendiente;Porcentaje cancelado");
        for (Pedido p : saldos) {
            BigDecimal total = p.getMontoTotal() != null ? p.getMontoTotal() : BigDecimal.ZERO;
            BigDecimal adelanto = p.getMontoAdelanto() != null ? p.getMontoAdelanto() : BigDecimal.ZERO;
            BigDecimal saldo = p.getMontoSaldo() != null ? p.getMontoSaldo() : BigDecimal.ZERO;
            double porcentaje = total.compareTo(BigDecimal.ZERO) > 0
                ? Math.round(((total.doubleValue() - saldo.doubleValue()) / total.doubleValue()) * 10000.0) / 100.0
                : 0;
            out.println(String.join(";",
                p.getCodigo() != null ? p.getCodigo() : "",
                p.getNombreCliente() != null ? p.getNombreCliente() : "",
                adelanto.setScale(2, RoundingMode.HALF_UP).toString(),
                saldo.setScale(2, RoundingMode.HALF_UP).toString(),
                String.valueOf(porcentaje) + "%"
            ));
        }
        out.flush();
    }

    @GetMapping("/saldos/excel")
    public void exportarSaldosExcel(
            @RequestParam(required = false) String pedidos_fecha_inicio,
            @RequestParam(required = false) String pedidos_fecha_fin,
            HttpServletResponse response) throws Exception {

        List<Pedido> saldos = filtrarSaldos(pedidos_fecha_inicio, pedidos_fecha_fin);

        String nombre = "reporte_saldos_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String[][] datos = new String[saldos.size() + 1][5];
        datos[0] = new String[]{"Codigo", "Cliente", "Cancelado", "Saldo pendiente", "Porcentaje cancelado"};
        for (int i = 0; i < saldos.size(); i++) {
            Pedido p = saldos.get(i);
            BigDecimal total = p.getMontoTotal() != null ? p.getMontoTotal() : BigDecimal.ZERO;
            BigDecimal adelanto = p.getMontoAdelanto() != null ? p.getMontoAdelanto() : BigDecimal.ZERO;
            BigDecimal saldo = p.getMontoSaldo() != null ? p.getMontoSaldo() : BigDecimal.ZERO;
            double porcentaje = total.compareTo(BigDecimal.ZERO) > 0
                ? Math.round(((total.doubleValue() - saldo.doubleValue()) / total.doubleValue()) * 10000.0) / 100.0
                : 0;
            datos[i + 1] = new String[]{
                p.getCodigo() != null ? p.getCodigo() : "",
                p.getNombreCliente() != null ? p.getNombreCliente() : "",
                adelanto.setScale(2, RoundingMode.HALF_UP).toString(),
                saldo.setScale(2, RoundingMode.HALF_UP).toString(),
                String.valueOf(porcentaje) + "%"
            };
        }
        descargarXlsx(response, nombre, "Saldos", datos);
    }

    @GetMapping("/stock/csv")
    public void exportarStockCsv(
            @RequestParam(required = false, defaultValue = "10") Integer umbral,
            HttpServletResponse response) throws Exception {

        int umbralVal = umbral != null ? umbral : 10;
        List<Producto> stock = filtrarStock(umbralVal);

        String nombre = "reporte_stock_bajo_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=" + nombre + ".csv");

        PrintWriter out = response.getWriter();
        out.write("\uFEFF");
        out.println("sep=;");
        out.println("Codigo;Producto;Categoria;Stock actual");
        for (Producto p : stock) {
            out.println(String.join(";",
                p.getCodigo() != null ? p.getCodigo() : "",
                p.getNombre() != null ? p.getNombre() : "",
                p.getCategoria() != null ? p.getCategoria() : "",
                p.getStockActual() != null ? String.valueOf(p.getStockActual()) : "0"
            ));
        }
        out.flush();
    }

    @GetMapping("/stock/excel")
    public void exportarStockExcel(
            @RequestParam(required = false, defaultValue = "10") Integer umbral,
            HttpServletResponse response) throws Exception {

        int umbralVal = umbral != null ? umbral : 10;
        List<Producto> stock = filtrarStock(umbralVal);

        String nombre = "reporte_stock_bajo_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String[][] datos = new String[stock.size() + 1][4];
        datos[0] = new String[]{"Codigo", "Producto", "Categoria", "Stock actual"};
        for (int i = 0; i < stock.size(); i++) {
            Producto p = stock.get(i);
            datos[i + 1] = new String[]{
                p.getCodigo() != null ? p.getCodigo() : "",
                p.getNombre() != null ? p.getNombre() : "",
                p.getCategoria() != null ? p.getCategoria() : "",
                p.getStockActual() != null ? String.valueOf(p.getStockActual()) : "0"
            };
        }
        descargarXlsx(response, nombre, "Stock", datos);
    }

    private List<Venta> filtrarVentas(String fechaInicio, String fechaFin, String tipo, DateTimeFormatter fmt) {
        List<Venta> todas = ventaRepository.findAll();
        List<Venta> filtradas = new ArrayList<>();
        for (Venta v : todas) {
            boolean pasa = true;
            if (fechaInicio != null && !fechaInicio.isEmpty()) {
                LocalDate inicio = LocalDate.parse(fechaInicio, fmt);
                if (v.getFechaVenta().isBefore(inicio)) pasa = false;
            }
            if (fechaFin != null && !fechaFin.isEmpty()) {
                LocalDate fin = LocalDate.parse(fechaFin, fmt);
                if (v.getFechaVenta().isAfter(fin)) pasa = false;
            }
            if (tipo != null && !tipo.isEmpty()) {
                if (!tipo.equals(v.getTipoVenta())) pasa = false;
            }
            if (pasa) filtradas.add(v);
        }
        Collections.sort(filtradas, (a, b) -> Long.compare(b.getId() != null ? b.getId() : 0, a.getId() != null ? a.getId() : 0));
        return filtradas;
    }

    private List<Pedido> filtrarPedidos(String fechaInicio, String fechaFin, String estado, String tipoEntrega, DateTimeFormatter fmt) {
        List<Pedido> todos = pedidoRepository.findAll();
        List<Pedido> filtrados = new ArrayList<>();
        for (Pedido p : todos) {
            boolean pasa = true;
            if (fechaInicio != null && !fechaInicio.isEmpty()) {
                LocalDate inicio = LocalDate.parse(fechaInicio, fmt);
                if (p.getFechaEntregaCompromiso() != null && p.getFechaEntregaCompromiso().isBefore(inicio)) pasa = false;
            }
            if (fechaFin != null && !fechaFin.isEmpty()) {
                LocalDate fin = LocalDate.parse(fechaFin, fmt);
                if (p.getFechaEntregaCompromiso() != null && p.getFechaEntregaCompromiso().isAfter(fin)) pasa = false;
            }
            if (estado != null && !estado.isEmpty()) {
                if (!estado.equals(p.getEstado())) pasa = false;
            }
            if (tipoEntrega != null && !tipoEntrega.isEmpty()) {
                if (!tipoEntrega.equals(p.getTipoEntrega())) pasa = false;
            }
            if (pasa) filtrados.add(p);
        }
        Collections.sort(filtrados, (a, b) -> Long.compare(b.getId() != null ? b.getId() : 0, a.getId() != null ? a.getId() : 0));
        return filtrados;
    }

    private List<Pedido> filtrarSaldos(String fechaInicio, String fechaFin) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        List<Pedido> todos = pedidoRepository.findAll();
        List<Pedido> filtrados = new ArrayList<>();
        for (Pedido p : todos) {
            boolean tieneSaldo = (p.getMontoSaldo() != null && p.getMontoSaldo().compareTo(BigDecimal.ZERO) > 0);
            boolean noPagado = !"pagado_completo".equals(p.getEstadoPago());
            if (!tieneSaldo && !noPagado) continue;
            if ("cancelado".equals(p.getEstado()) || "entregado".equals(p.getEstado())) continue;
            if (fechaInicio != null && !fechaInicio.isEmpty()) {
                LocalDate inicio = LocalDate.parse(fechaInicio, fmt);
                if (p.getCreatedAt() != null && p.getCreatedAt().toLocalDate().isBefore(inicio)) continue;
            }
            if (fechaFin != null && !fechaFin.isEmpty()) {
                LocalDate fin = LocalDate.parse(fechaFin, fmt);
                if (p.getCreatedAt() != null && p.getCreatedAt().toLocalDate().isAfter(fin)) continue;
            }
            filtrados.add(p);
        }
        Collections.sort(filtrados, (a, b) -> Long.compare(b.getId() != null ? b.getId() : 0, a.getId() != null ? a.getId() : 0));
        return filtrados;
    }

    private List<Producto> filtrarStock(int umbral) {
        List<Producto> todos = productoRepository.findAll();
        List<Producto> filtrados = new ArrayList<>();
        for (Producto p : todos) {
            int stock = p.getStockActual() != null ? p.getStockActual() : 0;
            if (stock <= umbral && p.getActivo()) {
                filtrados.add(p);
            }
        }
        filtrados.sort(Comparator.comparingInt(p -> p.getStockActual() != null ? p.getStockActual() : 0));
        return filtrados;
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }

    private String excelColumnName(int column) {
        StringBuilder name = new StringBuilder();
        while (column > 0) {
            int mod = (column - 1) % 26;
            name.insert(0, (char) (65 + mod));
            column = (column - 1) / 26;
        }
        return name.length() > 0 ? name.toString() : "A";
    }

    private String xmlEscape(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&apos;");
    }

    private void descargarXlsx(HttpServletResponse response, String nombreBase, String hoja, String[][] datos) throws Exception {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=" + nombreBase + ".xlsx");
        response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
        response.setHeader("Pragma", "no-cache");

        String sheetName = hoja.replaceAll("[\\[\\]*\\\\?:]", "");
        if (sheetName.length() > 31) sheetName = sheetName.substring(0, 31);

        int columnCount = datos[0].length;
        int rowCount = datos.length;
        String lastColumn = excelColumnName(columnCount);

        StringBuilder worksheetRows = new StringBuilder();
        for (int r = 0; r < rowCount; r++) {
            worksheetRows.append("<row r=\"").append(r + 1).append("\">");
            for (int c = 0; c < columnCount; c++) {
                String cellRef = excelColumnName(c + 1) + (r + 1);
                String valor = (c < datos[r].length && datos[r][c] != null) ? datos[r][c] : "";
                if (r == 0) {
                    worksheetRows.append("<c r=\"").append(cellRef).append("\" s=\"1\" t=\"inlineStr\"><is><t xml:space=\"preserve\">").append(xmlEscape(valor)).append("</t></is></c>");
                } else {
                    worksheetRows.append("<c r=\"").append(cellRef).append("\" t=\"inlineStr\"><is><t xml:space=\"preserve\">").append(xmlEscape(valor)).append("</t></is></c>");
                }
            }
            worksheetRows.append("</row>");
        }

        String created = LocalDateTime.now().toString();

        String contentTypes = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
            + "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">"
            + "<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>"
            + "<Default Extension=\"xml\" ContentType=\"application/xml\"/>"
            + "<Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/>"
            + "<Override PartName=\"/xl/worksheets/sheet1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>"
            + "<Override PartName=\"/xl/styles.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml\"/>"
            + "<Override PartName=\"/docProps/core.xml\" ContentType=\"application/vnd.openxmlformats-package.core-properties+xml\"/>"
            + "<Override PartName=\"/docProps/app.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.extended-properties+xml\"/>"
            + "</Types>";

        String rels = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
            + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"
            + "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/>"
            + "<Relationship Id=\"rId2\" Type=\"http://schemas.openxmlformats.org/package/2006/relationships/metadata/core-properties\" Target=\"docProps/core.xml\"/>"
            + "<Relationship Id=\"rId3\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/extended-properties\" Target=\"docProps/app.xml\"/>"
            + "</Relationships>";

        String workbook = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
            + "<workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\">"
            + "<sheets><sheet name=\"" + xmlEscape(sheetName) + "\" sheetId=\"1\" r:id=\"rId1\"/></sheets>"
            + "</workbook>";

        String workbookRels = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
            + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"
            + "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet1.xml\"/>"
            + "<Relationship Id=\"rId2\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles\" Target=\"styles.xml\"/>"
            + "</Relationships>";

        String styles = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
            + "<styleSheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">"
            + "<fonts count=\"2\"><font><sz val=\"10\"/><name val=\"Calibri\"/></font><font><b/><sz val=\"10\"/><name val=\"Calibri\"/></font></fonts>"
            + "<fills count=\"2\"><fill><patternFill patternType=\"none\"/></fill><fill><patternFill patternType=\"solid\"><fgColor rgb=\"FFF5ECCE\"/><bgColor indexed=\"64\"/></patternFill></fill></fills>"
            + "<borders count=\"1\"><border><left/><right/><top/><bottom/><diagonal/></border></borders>"
            + "<cellStyleXfs count=\"1\"><xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\"/></cellStyleXfs>"
            + "<cellXfs count=\"2\"><xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\" xfId=\"0\"/><xf numFmtId=\"0\" fontId=\"1\" fillId=\"1\" borderId=\"0\" xfId=\"0\" applyFont=\"1\" applyFill=\"1\"/></cellXfs>"
            + "<cellStyles count=\"1\"><cellStyle name=\"Normal\" xfId=\"0\" builtinId=\"0\"/></cellStyles>"
            + "</styleSheet>";

        String worksheet = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
            + "<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">"
            + "<dimension ref=\"A1:" + lastColumn + rowCount + "\"/>"
            + "<sheetData>" + worksheetRows + "</sheetData>"
            + "<autoFilter ref=\"A1:" + lastColumn + "1\"/>"
            + "</worksheet>";

        String core = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
            + "<cp:coreProperties xmlns:cp=\"http://schemas.openxmlformats.org/package/2006/metadata/core-properties\" xmlns:dc=\"http://purl.org/dc/elements/1.1/\" xmlns:dcterms=\"http://purl.org/dc/terms/\" xmlns:dcmitype=\"http://purl.org/dc/dcmitype/\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">"
            + "<dc:creator>Arte y Metal</dc:creator>"
            + "<cp:lastModifiedBy>Arte y Metal</cp:lastModifiedBy>"
            + "<dcterms:created xsi:type=\"dcterms:W3CDTF\">" + created + "</dcterms:created>"
            + "<dcterms:modified xsi:type=\"dcterms:W3CDTF\">" + created + "</dcterms:modified>"
            + "</cp:coreProperties>";

        String app = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
            + "<Properties xmlns=\"http://schemas.openxmlformats.org/officeDocument/2006/extended-properties\" xmlns:vt=\"http://schemas.openxmlformats.org/officeDocument/2006/docPropsVTypes\">"
            + "<Application>Arte y Metal</Application>"
            + "</Properties>";

        ZipOutputStream zip = new ZipOutputStream(response.getOutputStream());
        zip.putNextEntry(new ZipEntry("[Content_Types].xml"));
        zip.write(contentTypes.getBytes("UTF-8"));
        zip.closeEntry();

        zip.putNextEntry(new ZipEntry("_rels/.rels"));
        zip.write(rels.getBytes("UTF-8"));
        zip.closeEntry();

        zip.putNextEntry(new ZipEntry("xl/workbook.xml"));
        zip.write(workbook.getBytes("UTF-8"));
        zip.closeEntry();

        zip.putNextEntry(new ZipEntry("xl/_rels/workbook.xml.rels"));
        zip.write(workbookRels.getBytes("UTF-8"));
        zip.closeEntry();

        zip.putNextEntry(new ZipEntry("xl/styles.xml"));
        zip.write(styles.getBytes("UTF-8"));
        zip.closeEntry();

        zip.putNextEntry(new ZipEntry("xl/worksheets/sheet1.xml"));
        zip.write(worksheet.getBytes("UTF-8"));
        zip.closeEntry();

        zip.putNextEntry(new ZipEntry("docProps/core.xml"));
        zip.write(core.getBytes("UTF-8"));
        zip.closeEntry();

        zip.putNextEntry(new ZipEntry("docProps/app.xml"));
        zip.write(app.getBytes("UTF-8"));
        zip.closeEntry();

        zip.close();
    }
}
