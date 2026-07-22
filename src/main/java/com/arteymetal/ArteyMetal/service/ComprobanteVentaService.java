package com.arteymetal.ArteyMetal.service;

import com.arteymetal.ArteyMetal.entity.ComprobanteVenta;
import com.arteymetal.ArteyMetal.entity.Venta;
import com.arteymetal.ArteyMetal.repository.ComprobanteVentaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Map;

@Service
public class ComprobanteVentaService {

    @Autowired private ComprobanteVentaRepository comprobanteRepository;

    @Transactional
    public ComprobanteVenta emitir(Venta venta, Map<String, String> datos) {
        if (venta.getComprobante() != null) {
            return venta.getComprobante();
        }

        String tipoComprobante = datos.getOrDefault("tipo_comprobante", "boleta");
        String serie = "factura".equals(tipoComprobante) ? "F001" : "B001";
        long correlativoNum = comprobanteRepository.count() + 1;
        String correlativo = String.format("%08d", correlativoNum);
        String codigo = tipoComprobante.substring(0, 1).toUpperCase() + "-" + serie + "-" + correlativo;

        ComprobanteVenta comprobante = ComprobanteVenta.builder()
            .venta(venta)
            .tipoComprobante(tipoComprobante)
            .serie(serie)
            .correlativo(correlativo)
            .codigo(codigo)
            .documentoCliente(datos.get("documento_cliente"))
            .nombreCliente(datos.get("nombre_cliente"))
            .direccionCliente(datos.get("direccion_cliente"))
            .build();

        return comprobanteRepository.save(comprobante);
    }
}
