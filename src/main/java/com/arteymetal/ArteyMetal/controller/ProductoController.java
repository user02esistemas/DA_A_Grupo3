package com.arteymetal.ArteyMetal.controller;

import com.arteymetal.ArteyMetal.entity.CategoriaProducto;
import com.arteymetal.ArteyMetal.entity.Producto;
import com.arteymetal.ArteyMetal.entity.ProductoImagen;
import com.arteymetal.ArteyMetal.repository.CategoriaProductoRepository;
import com.arteymetal.ArteyMetal.repository.ProductoImagenRepository;
import com.arteymetal.ArteyMetal.repository.ProductoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/productos")
public class ProductoController {

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private CategoriaProductoRepository categoriaProductoRepository;

    @Autowired
    private ProductoImagenRepository productoImagenRepository;

    private static final String UPLOAD_DIR = "uploads/productos/";

    @GetMapping
    public String index(@RequestParam(value = "q", required = false) String busqueda,
                        @RequestParam(value = "categoria", required = false) String categoria,
                        @RequestParam(value = "activo", required = false) String filtroActivo,
                        @RequestParam(value = "page", defaultValue = "0") int page,
                        Model model, HttpSession session) {

        Pageable pageable = PageRequest.of(page, 10, Sort.by("id").descending());

        Specification<Producto> spec = (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();

            if (busqueda != null && !busqueda.trim().isEmpty()) {
                String busquedaLower = "%" + busqueda.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("codigo")), busquedaLower),
                        cb.like(cb.lower(root.get("nombre")), busquedaLower),
                        cb.like(cb.lower(root.get("descripcion")), busquedaLower)
                ));
            }

            if (categoria != null && !categoria.trim().isEmpty()) {
                predicates.add(cb.equal(root.get("categoria"), categoria.trim()));
            }

            if (filtroActivo != null && !filtroActivo.trim().isEmpty()) {
                Boolean activo = Boolean.parseBoolean(filtroActivo);
                predicates.add(cb.equal(root.get("activo"), activo));
            }

            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };

        Page<Producto> productos = productoRepository.findAll(spec, pageable);

        List<CategoriaProducto> categorias = categoriaProductoRepository.findByActivoTrueOrderByNombre();

        Map<String, String> categoriasMap = new LinkedHashMap<>();
        for (CategoriaProducto cat : categorias) {
            categoriasMap.put(cat.getSlug(), cat.getNombre());
        }

        model.addAttribute("productos", productos);
        model.addAttribute("busqueda", busqueda);
        model.addAttribute("categoria", categoria);
        model.addAttribute("filtroActivo", filtroActivo);
        model.addAttribute("categorias", categorias);
        model.addAttribute("categoriasMap", categoriasMap);

        return "productos/index";
    }

    @GetMapping("/create")
    public String create(Model model) {
        model.addAttribute("producto", new Producto());
        model.addAttribute("categorias", categoriaProductoRepository.findByActivoTrueOrderByNombre());
        return "productos/create";
    }

    @PostMapping
    @Transactional
    public String store(@Valid @ModelAttribute Producto producto,
                        BindingResult result,
                        @RequestParam(value = "imagenes", required = false) MultipartFile[] imagenes,
                        Model model,
                        RedirectAttributes flash) {

        if (producto.getNombre() != null && producto.getNombre().trim().length() > 150) {
            result.rejectValue("nombre", "error.producto", "El nombre no puede exceder 150 caracteres");
        }

        if (producto.getCategoria() != null && producto.getCategoria().trim().length() > 60) {
            result.rejectValue("categoria", "error.producto", "La categoría no puede exceder 60 caracteres");
        }

        if (producto.getPrecioReferencia() != null && producto.getPrecioReferencia().compareTo(BigDecimal.ZERO) < 0) {
            result.rejectValue("precioReferencia", "error.producto", "El precio de referencia no puede ser negativo");
        }

        if (producto.getStockTienda() < 0) {
            result.rejectValue("stockTienda", "error.producto", "El stock de tienda no puede ser negativo");
        }

        if (producto.getStockAlmacen() < 0) {
            result.rejectValue("stockAlmacen", "error.producto", "El stock de almacén no puede ser negativo");
        }

        if (result.hasErrors()) {
            model.addAttribute("categorias", categoriaProductoRepository.findByActivoTrueOrderByNombre());
            return "productos/create";
        }

        String codigo = generarCodigo();
        producto.setCodigo(codigo);
        producto.setStockActual(producto.getStockTienda() + producto.getStockAlmacen());
        productoRepository.save(producto);

        if (imagenes != null && imagenes.length > 0) {
            for (MultipartFile imagen : imagenes) {
                if (!imagen.isEmpty()) {
                    guardarImagen(imagen, producto);
                }
            }
        }

        flash.addFlashAttribute("success", "Producto creado exitosamente");
        return "redirect:/productos";
    }

    @GetMapping("/{id}")
    public String show(@PathVariable Long id, Model model) {
        Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado"));

        List<ProductoImagen> imagenes = productoImagenRepository.findByProductoId(id);
        model.addAttribute("producto", producto);
        model.addAttribute("imagenes", imagenes);
        return "productos/show";
    }

    @GetMapping("/{id}/edit")
    public String edit(@PathVariable Long id, Model model) {
        Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado"));

        List<CategoriaProducto> categoriasActivas = categoriaProductoRepository.findByActivoTrueOrderByNombre();
        List<CategoriaProducto> categorias = new ArrayList<>(categoriasActivas);

        boolean categoriaActualActiva = categoriasActivas.stream()
                .anyMatch(c -> c.getSlug().equals(producto.getCategoria()));

        if (!categoriaActualActiva && producto.getCategoria() != null) {
            CategoriaProducto catActual = new CategoriaProducto();
            catActual.setSlug(producto.getCategoria());
            catActual.setNombre(producto.getCategoria());
            catActual.setActivo(false);
            categorias.add(0, catActual);
        }

        List<ProductoImagen> imagenes = productoImagenRepository.findByProductoId(id);
        model.addAttribute("producto", producto);
        model.addAttribute("categorias", categorias);
        model.addAttribute("imagenes", imagenes);
        return "productos/edit";
    }

    @PostMapping("/{id}/update")
    @Transactional
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute Producto producto,
                         BindingResult result,
                         @RequestParam(value = "imagenes", required = false) MultipartFile[] imagenes,
                         Model model,
                         RedirectAttributes flash) {

        Producto productoExistente = productoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado"));

        if (producto.getNombre() != null && producto.getNombre().trim().length() > 150) {
            result.rejectValue("nombre", "error.producto", "El nombre no puede exceder 150 caracteres");
        }

        if (producto.getCategoria() != null && producto.getCategoria().trim().length() > 60) {
            result.rejectValue("categoria", "error.producto", "La categoría no puede exceder 60 caracteres");
        }

        if (producto.getPrecioReferencia() != null && producto.getPrecioReferencia().compareTo(BigDecimal.ZERO) < 0) {
            result.rejectValue("precioReferencia", "error.producto", "El precio de referencia no puede ser negativo");
        }

        if (producto.getStockTienda() < 0) {
            result.rejectValue("stockTienda", "error.producto", "El stock de tienda no puede ser negativo");
        }

        if (producto.getStockAlmacen() < 0) {
            result.rejectValue("stockAlmacen", "error.producto", "El stock de almacén no puede ser negativo");
        }

        if (result.hasErrors()) {
            List<CategoriaProducto> categorias = categoriaProductoRepository.findByActivoTrueOrderByNombre();
            List<ProductoImagen> imagenesExistentes = productoImagenRepository.findByProductoId(id);
            model.addAttribute("categorias", categorias);
            model.addAttribute("imagenes", imagenesExistentes);
            return "productos/edit";
        }

        productoExistente.setNombre(producto.getNombre());
        productoExistente.setCategoria(producto.getCategoria());
        productoExistente.setDescripcion(producto.getDescripcion());
        productoExistente.setPrecioReferencia(producto.getPrecioReferencia());
        productoExistente.setStockTienda(producto.getStockTienda());
        productoExistente.setStockAlmacen(producto.getStockAlmacen());
        productoExistente.setActivo(producto.getActivo());
        productoExistente.setStockActual(producto.getStockTienda() + producto.getStockAlmacen());
        productoRepository.save(productoExistente);

        if (imagenes != null && imagenes.length > 0) {
            for (MultipartFile imagen : imagenes) {
                if (!imagen.isEmpty()) {
                    guardarImagen(imagen, productoExistente);
                }
            }
        }

        flash.addFlashAttribute("success", "Producto actualizado exitosamente");
        return "redirect:/productos";
    }

    @PostMapping("/{id}/eliminar")
    @Transactional
    public String destroy(@PathVariable Long id, RedirectAttributes flash) {
        Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado"));

        List<ProductoImagen> imagenes = productoImagenRepository.findByProductoId(id);
        for (ProductoImagen imagen : imagenes) {
            eliminarArchivo(imagen.getArchivoPath());
            productoImagenRepository.delete(imagen);
        }

        productoRepository.delete(producto);
        flash.addFlashAttribute("success", "Producto eliminado exitosamente");
        return "redirect:/productos";
    }

    @PostMapping("/{id}/imagen/{imagenId}/eliminar")
    @ResponseBody
    @Transactional
    public ResponseEntity<Map<String, Object>> destroyImagen(@PathVariable Long id,
                                                             @PathVariable Long imagenId) {
        Map<String, Object> response = new HashMap<>();

        try {
            Optional<ProductoImagen> imagenOpt = productoImagenRepository.findById(imagenId);
            if (imagenOpt.isEmpty()) {
                response.put("success", false);
                response.put("message", "Imagen no encontrada");
                return ResponseEntity.ok(response);
            }

            ProductoImagen imagen = imagenOpt.get();
            eliminarArchivo(imagen.getArchivoPath());
            productoImagenRepository.delete(imagen);

            response.put("success", true);
            response.put("message", "Imagen eliminada exitosamente");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error al eliminar la imagen");
            return ResponseEntity.ok(response);
        }
    }

    @GetMapping("/categorias-json")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> categoriasJson() {
        List<CategoriaProducto> categorias = categoriaProductoRepository.findByActivoTrueOrderByNombre();
        List<Map<String, Object>> resultado = new ArrayList<>();

        for (CategoriaProducto cat : categorias) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", cat.getId());
            item.put("slug", cat.getSlug());
            item.put("nombre", cat.getNombre());
            item.put("activo", cat.getActivo());
            resultado.add(item);
        }

        return ResponseEntity.ok(resultado);
    }

    @PostMapping("/categoria")
    @ResponseBody
    @Transactional
    public ResponseEntity<Map<String, Object>> categoriaStore(
            @RequestParam("nombre") String nombre) {

        Map<String, Object> response = new HashMap<>();

        if (nombre == null || nombre.trim().isEmpty()) {
            response.put("success", false);
            response.put("message", "El nombre es requerido");
            return ResponseEntity.ok(response);
        }

        String slug = generarSlug(nombre.trim());

        if (categoriaProductoRepository.existsBySlug(slug)) {
            int contador = 2;
            String slugBase = slug;
            while (categoriaProductoRepository.existsBySlug(slug)) {
                slug = slugBase + "_" + contador;
                contador++;
            }
        }

        CategoriaProducto categoria = new CategoriaProducto();
        categoria.setNombre(nombre.trim());
        categoria.setSlug(slug);
        categoria.setActivo(true);
        categoriaProductoRepository.save(categoria);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", categoria.getId());
        data.put("slug", categoria.getSlug());
        data.put("nombre", categoria.getNombre());
        data.put("activo", categoria.getActivo());

        response.put("success", true);
        response.put("message", "Categoría creada exitosamente");
        response.put("categoria", data);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/categoria/{id}/actualizar")
    @ResponseBody
    @Transactional
    public ResponseEntity<Map<String, Object>> categoriaUpdate(
            @PathVariable Long id,
            @RequestParam("nombre") String nombre) {

        Map<String, Object> response = new HashMap<>();

        Optional<CategoriaProducto> categoriaOpt = categoriaProductoRepository.findById(id);
        if (categoriaOpt.isEmpty()) {
            response.put("success", false);
            response.put("message", "Categoría no encontrada");
            return ResponseEntity.ok(response);
        }

        if (nombre == null || nombre.trim().isEmpty()) {
            response.put("success", false);
            response.put("message", "El nombre es requerido");
            return ResponseEntity.ok(response);
        }

        CategoriaProducto categoria = categoriaOpt.get();
        String slugAnterior = categoria.getSlug();
        String nuevoSlug = generarSlug(nombre.trim());

        if (!slugAnterior.equals(nuevoSlug)) {
            if (categoriaProductoRepository.existsBySlug(nuevoSlug)) {
                int contador = 2;
                String slugBase = nuevoSlug;
                while (categoriaProductoRepository.existsBySlug(nuevoSlug)) {
                    nuevoSlug = slugBase + "_" + contador;
                    contador++;
                }
            }

            List<Producto> productos = productoRepository.findByCategoria(slugAnterior);
            for (Producto producto : productos) {
                producto.setCategoria(nuevoSlug);
                productoRepository.save(producto);
            }
        }

        categoria.setNombre(nombre.trim());
        categoria.setSlug(nuevoSlug);
        categoriaProductoRepository.save(categoria);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", categoria.getId());
        data.put("slug", categoria.getSlug());
        data.put("nombre", categoria.getNombre());
        data.put("activo", categoria.getActivo());

        response.put("success", true);
        response.put("message", "Categoría actualizada exitosamente");
        response.put("categoria", data);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/categoria/{id}/toggle")
    @ResponseBody
    @Transactional
    public ResponseEntity<Map<String, Object>> categoriaToggle(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();

        Optional<CategoriaProducto> categoriaOpt = categoriaProductoRepository.findById(id);
        if (categoriaOpt.isEmpty()) {
            response.put("success", false);
            response.put("message", "Categoría no encontrada");
            return ResponseEntity.ok(response);
        }

        CategoriaProducto categoria = categoriaOpt.get();
        categoria.setActivo(!categoria.getActivo());
        categoriaProductoRepository.save(categoria);

        response.put("success", true);
        response.put("message", "Estado de la categoría actualizado");
        response.put("activo", categoria.getActivo());
        return ResponseEntity.ok(response);
    }

    private String generarCodigo() {
        long total = productoRepository.count();
        String codigo = String.format("PROD-%04d", total + 1);

        while (productoRepository.existsByCodigo(codigo)) {
            total++;
            codigo = String.format("PROD-%04d", total + 1);
        }

        return codigo;
    }

    private String generarSlug(String nombre) {
        String slug = nombre.toLowerCase().trim().replaceAll("[^a-z0-9]+", "_").replaceAll("^_|_$", "");
        if (slug.isEmpty()) slug = "categoria";
        return slug;
    }

    private void guardarImagen(MultipartFile archivo, Producto producto) {
        try {
            String uploadDir = System.getProperty("user.dir") + File.separator + UPLOAD_DIR;
            File directorio = new File(uploadDir);
            if (!directorio.exists()) {
                directorio.mkdirs();
            }

            String nombreArchivo = UUID.randomUUID().toString() + "_" + archivo.getOriginalFilename();
            Path rutaArchivo = Paths.get(uploadDir + nombreArchivo);
            Files.write(rutaArchivo, archivo.getBytes());

            ProductoImagen imagen = new ProductoImagen();
            imagen.setProducto(producto);
            imagen.setArchivoPath(UPLOAD_DIR + nombreArchivo);
            imagen.setNombreOriginal(archivo.getOriginalFilename());
            imagen.setMimeType(archivo.getContentType());
            imagen.setTamanoBytes(archivo.getSize());
            productoImagenRepository.save(imagen);
        } catch (IOException e) {
            throw new RuntimeException("Error al guardar la imagen", e);
        }
    }

    private void eliminarArchivo(String rutaArchivo) {
        try {
            String rutaCompleta = System.getProperty("user.dir") + File.separator + rutaArchivo;
            Path path = Paths.get(rutaCompleta);
            if (Files.exists(path)) {
                Files.delete(path);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
