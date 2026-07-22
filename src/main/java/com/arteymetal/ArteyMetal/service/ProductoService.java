package com.arteymetal.ArteyMetal.service;

import com.arteymetal.ArteyMetal.entity.*;
import com.arteymetal.ArteyMetal.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

@Service
public class ProductoService {

    @Autowired private ProductoRepository productoRepository;
    @Autowired private CategoriaProductoRepository categoriaRepository;
    @Autowired private MovimientoAlmacenRepository movimientoRepository;

    public List<Producto> listarTodos() {
        return productoRepository.findAll();
    }

    public List<Producto> listarActivos() {
        return productoRepository.findByActivoTrue();
    }

    public Optional<Producto> findById(Long id) {
        return productoRepository.findById(id);
    }

    @Transactional
    public Producto guardar(Producto producto) {
        if (producto.getStockTienda() == null) producto.setStockTienda(0);
        if (producto.getStockAlmacen() == null) producto.setStockAlmacen(0);
        producto.calcularStockActual();
        return productoRepository.save(producto);
    }

    @Transactional
    public Producto actualizar(Long id, Producto datos) {
        Producto producto = productoRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Producto no encontrado"));
        producto.setCodigo(datos.getCodigo());
        producto.setNombre(datos.getNombre());
        producto.setCategoria(datos.getCategoria());
        producto.setDescripcion(datos.getDescripcion());
        producto.setPrecioReferencia(datos.getPrecioReferencia());
        producto.setStockTienda(datos.getStockTienda());
        producto.setStockAlmacen(datos.getStockAlmacen());
        producto.setActivo(datos.getActivo());
        producto.calcularStockActual();
        return productoRepository.save(producto);
    }

    @Transactional
    public void eliminar(Long id) {
        productoRepository.deleteById(id);
    }

    @Transactional
    public void entradaStock(Long productoId, int cantidad, String concepto, Usuario usuario) {
        Producto producto = productoRepository.findById(productoId)
            .orElseThrow(() -> new RuntimeException("Producto no encontrado"));
        producto.setStockAlmacen(producto.getStockAlmacen() + cantidad);
        producto.calcularStockActual();
        productoRepository.save(producto);

        MovimientoAlmacen movimiento = MovimientoAlmacen.builder()
            .producto(producto)
            .tipo("entrada")
            .cantidad(cantidad)
            .stockResultante(producto.getStockAlmacen())
            .concepto(concepto)
            .usuario(usuario)
            .build();
        movimientoRepository.save(movimiento);
    }

    @Transactional
    public void salidaStock(Long productoId, int cantidad, String concepto, Usuario usuario) {
        Producto producto = productoRepository.findById(productoId)
            .orElseThrow(() -> new RuntimeException("Producto no encontrado"));
        if (producto.getStockTienda() < cantidad) {
            throw new RuntimeException("Stock insuficiente en tienda");
        }
        producto.setStockTienda(producto.getStockTienda() - cantidad);
        producto.calcularStockActual();
        productoRepository.save(producto);

        MovimientoAlmacen movimiento = MovimientoAlmacen.builder()
            .producto(producto)
            .tipo("salida")
            .cantidad(cantidad)
            .stockResultante(producto.getStockTienda())
            .concepto(concepto)
            .usuario(usuario)
            .build();
        movimientoRepository.save(movimiento);
    }

    public List<CategoriaProducto> listarCategorias() {
        return categoriaRepository.findByActivoTrue();
    }
}
