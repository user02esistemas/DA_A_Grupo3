package com.arteymetal.ArteyMetal.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.arteymetal.ArteyMetal.entity.Permiso;
import com.arteymetal.ArteyMetal.entity.Rol;
import com.arteymetal.ArteyMetal.repository.PermisoRepository;
import com.arteymetal.ArteyMetal.repository.RolRepository;
import com.arteymetal.ArteyMetal.repository.UsuarioRepository;

@Controller
@RequestMapping("/roles")
public class RolController {

    private final RolRepository rolRepository;
    private final PermisoRepository permisoRepository;
    private final UsuarioRepository usuarioRepository;

    public RolController(RolRepository rolRepository, PermisoRepository permisoRepository, UsuarioRepository usuarioRepository) {
        this.rolRepository = rolRepository;
        this.permisoRepository = permisoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @GetMapping
    public String index(@RequestParam(value = "busqueda", required = false) String busqueda, Model model) {
        Pageable pageable = PageRequest.of(0, 20);
        Page<Rol> roles;

        if (busqueda != null && !busqueda.trim().isEmpty()) {
            roles = rolRepository.findByNombreContainingIgnoreCaseOrDescripcionContainingIgnoreCase(busqueda.trim(), busqueda.trim(), pageable);
        } else {
            roles = rolRepository.findAll(pageable);
        }

        model.addAttribute("roles", roles);
        model.addAttribute("busqueda", busqueda);

        return "roles/index";
    }

    @GetMapping("/create")
    public String create(Model model) {
        model.addAttribute("permisos", permisoRepository.findAll());
        return "roles/create";
    }

    @PostMapping
    public String store(@RequestParam("nombre") String nombre,
                        @RequestParam(value = "descripcion", required = false) String descripcion,
                        @RequestParam(value = "permisos", required = false) List<Long> permisosIds,
                        RedirectAttributes redirectAttributes) {
        if (nombre == null || nombre.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "El nombre del rol es obligatorio.");
            return "redirect:/roles/create";
        }

        Rol rol = new Rol();
        rol.setNombre(nombre.trim());
        rol.setDescripcion(descripcion != null ? descripcion.trim() : null);
        rol.setActivo(true);

        if (permisosIds != null && !permisosIds.isEmpty()) {
            Set<Permiso> permisos = new HashSet<>(permisoRepository.findAllById(permisosIds));
            rol.setPermisos(permisos);
        } else {
            rol.setPermisos(new HashSet<>());
        }

        rolRepository.save(rol);

        redirectAttributes.addFlashAttribute("success", "Rol registrado correctamente.");
        return "redirect:/roles";
    }

    @GetMapping("/{id}/edit")
    public String edit(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        Rol rol = rolRepository.findById(id).orElse(null);
        if (rol == null) {
            redirectAttributes.addFlashAttribute("error", "Rol no encontrado.");
            return "redirect:/roles";
        }

        model.addAttribute("rol", rol);
        model.addAttribute("permisos", permisoRepository.findAll());
        return "roles/edit";
    }

    @PostMapping("/{id}/update")
    public String update(@PathVariable Long id,
                         @RequestParam("nombre") String nombre,
                         @RequestParam(value = "descripcion", required = false) String descripcion,
                         @RequestParam(value = "permisos", required = false) List<Long> permisosIds,
                         RedirectAttributes redirectAttributes) {
        Rol rol = rolRepository.findById(id).orElse(null);
        if (rol == null) {
            redirectAttributes.addFlashAttribute("error", "Rol no encontrado.");
            return "redirect:/roles";
        }

        if (nombre == null || nombre.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "El nombre del rol es obligatorio.");
            return "redirect:/roles/" + id + "/edit";
        }

        rol.setNombre(nombre.trim());
        rol.setDescripcion(descripcion != null ? descripcion.trim() : null);

        if (permisosIds != null && !permisosIds.isEmpty()) {
            Set<Permiso> permisos = new HashSet<>(permisoRepository.findAllById(permisosIds));
            rol.setPermisos(permisos);
        } else {
            rol.setPermisos(new HashSet<>());
        }

        rolRepository.save(rol);

        redirectAttributes.addFlashAttribute("success", "Rol actualizado correctamente.");
        return "redirect:/roles";
    }

    @PostMapping("/{id}/eliminar")
    public String destroy(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        if (!rolRepository.existsById(id)) {
            redirectAttributes.addFlashAttribute("error", "Rol no encontrado.");
            return "redirect:/roles";
        }

        if (usuarioRepository.existsByRolId(id)) {
            redirectAttributes.addFlashAttribute("error", "No se puede eliminar el rol porque tiene usuarios asignados.");
            return "redirect:/roles";
        }

        rolRepository.deleteById(id);

        redirectAttributes.addFlashAttribute("success", "Rol eliminado correctamente.");
        return "redirect:/roles";
    }

    @GetMapping("/panel-data")
    public ResponseEntity<Map<String, Object>> panelData() {
        List<Rol> roles = rolRepository.findAll();
        List<Permiso> permisos = permisoRepository.findAll();

        List<Map<String, Object>> rolesData = new ArrayList<>();
        for (Rol rol : roles) {
            Map<String, Object> rolMap = new HashMap<>();
            rolMap.put("id", rol.getId());
            rolMap.put("nombre", rol.getNombre());
            rolMap.put("descripcion", rol.getDescripcion());
            rolMap.put("activo", rol.getActivo());
            rolMap.put("usuarioCount", rol.getUsuarios() != null ? rol.getUsuarios().size() : 0);
            rolMap.put("permisoCount", rol.getPermisos() != null ? rol.getPermisos().size() : 0);
            List<Map<String, String>> permisosList = new ArrayList<>();
            if (rol.getPermisos() != null) {
                for (Permiso p : rol.getPermisos()) {
                    Map<String, String> permMap = new HashMap<>();
                    permMap.put("nombre", p.getNombre());
                    permMap.put("slug", p.getSlug());
                    permisosList.add(permMap);
                }
            }
            rolMap.put("permisos", permisosList);
            rolesData.add(rolMap);
        }

        List<Map<String, Object>> permisosData = new ArrayList<>();
        for (Permiso p : permisos) {
            Map<String, Object> permMap = new HashMap<>();
            permMap.put("id", p.getId());
            permMap.put("nombre", p.getNombre());
            permMap.put("slug", p.getSlug());
            permisosData.add(permMap);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("ok", true);
        Map<String, Object> data = new HashMap<>();
        data.put("roles", rolesData);
        data.put("permisos", permisosData);
        response.put("data", data);

        return ResponseEntity.ok(response);
    }
}
