package com.arteymetal.ArteyMetal.controller;

import com.arteymetal.ArteyMetal.entity.Rol;
import com.arteymetal.ArteyMetal.entity.Usuario;
import com.arteymetal.ArteyMetal.repository.RolRepository;
import com.arteymetal.ArteyMetal.repository.UsuarioRepository;
import com.arteymetal.ArteyMetal.service.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/usuarios")
public class UsuarioController {

    @Autowired private UsuarioService usuarioService;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private RolRepository rolRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @GetMapping
    public String index(@RequestParam(required = false) String q,
                        @RequestParam(required = false) Long rolId,
                        @RequestParam(required = false) Boolean activo,
                        Model model) {
        Pageable pageable = PageRequest.of(0, 12);
        Page<Usuario> usuarios = usuarioRepository.searchWithFilters(q, rolId, activo, pageable);
        model.addAttribute("usuarios", usuarios);
        model.addAttribute("busqueda", q);
        model.addAttribute("filtroRol", rolId);
        model.addAttribute("filtroActivo", activo);
        model.addAttribute("roles", rolRepository.findAllByOrderByNombre());
        return "usuarios/index";
    }

    @GetMapping("/create")
    public String create(Model model) {
        model.addAttribute("usuario", new Usuario());
        model.addAttribute("roles", rolRepository.findAllByOrderByNombre());
        return "usuarios/create";
    }

    @PostMapping
    public String store(@ModelAttribute Usuario usuario,
                        @RequestParam String password_confirmation,
                        RedirectAttributes flash, Model model) {
        List<String> errores = new ArrayList<>();

        if (usuario.getName() == null || usuario.getName().trim().isEmpty()) {
            errores.add("El nombre es requerido.");
        }

        if (usuario.getEmail() == null || usuario.getEmail().trim().isEmpty()) {
            errores.add("El email es requerido.");
        } else if (usuarioRepository.existsByEmail(usuario.getEmail().trim())) {
            errores.add("El email ya esta registrado.");
        }

        if (usuario.getPassword() == null || usuario.getPassword().isEmpty()) {
            errores.add("La contrasena es requerida.");
        } else if (usuario.getPassword().length() < 6) {
            errores.add("La contrasena debe tener al menos 6 caracteres.");
        }

        if (!usuario.getPassword().equals(password_confirmation)) {
            errores.add("Las contrasenas no coinciden.");
        }

        if (usuario.getRol() == null || usuario.getRol().getId() == null) {
            errores.add("El rol es requerido.");
        } else if (!rolRepository.existsById(usuario.getRol().getId())) {
            errores.add("El rol seleccionado no es valido.");
        }

        if (!errores.isEmpty()) {
            model.addAttribute("errores", errores);
            model.addAttribute("usuario", usuario);
            model.addAttribute("roles", rolRepository.findAllByOrderByNombre());
            return "usuarios/create";
        }

        rolRepository.findById(usuario.getRol().getId()).ifPresent(usuario::setRol);
        usuario.setPassword(passwordEncoder.encode(usuario.getPassword()));
        if (usuario.getActivo() == null) {
            usuario.setActivo(true);
        }

        usuarioRepository.save(usuario);
        flash.addFlashAttribute("exito", "Usuario registrado correctamente.");
        return "redirect:/usuarios";
    }

    @GetMapping("/{id}/edit")
    public String edit(@PathVariable Long id, Model model) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        model.addAttribute("usuario", usuario);
        model.addAttribute("roles", rolRepository.findAllByOrderByNombre());
        return "usuarios/edit";
    }

    @PutMapping("/{id}/update")
    public String update(@PathVariable Long id,
                         @ModelAttribute Usuario usuario,
                         @RequestParam(required = false) String password_confirmation,
                         RedirectAttributes flash, Model model) {
        List<String> errores = new ArrayList<>();

        if (usuario.getName() == null || usuario.getName().trim().isEmpty()) {
            errores.add("El nombre es requerido.");
        }

        if (usuario.getEmail() == null || usuario.getEmail().trim().isEmpty()) {
            errores.add("El email es requerido.");
        } else {
            var existente = usuarioRepository.findByEmail(usuario.getEmail().trim());
            if (existente.isPresent() && !existente.get().getId().equals(id)) {
                errores.add("El email ya esta registrado.");
            }
        }

        if (usuario.getPassword() != null && !usuario.getPassword().isEmpty()) {
            if (usuario.getPassword().length() < 6) {
                errores.add("La contrasena debe tener al menos 6 caracteres.");
            }
            if (password_confirmation == null || !usuario.getPassword().equals(password_confirmation)) {
                errores.add("Las contrasenas no coinciden.");
            }
        }

        if (usuario.getRol() == null || usuario.getRol().getId() == null) {
            errores.add("El rol es requerido.");
        } else if (!rolRepository.existsById(usuario.getRol().getId())) {
            errores.add("El rol seleccionado no es valido.");
        }

        if (!errores.isEmpty()) {
            model.addAttribute("errores", errores);
            usuario.setId(id);
            model.addAttribute("usuario", usuario);
            model.addAttribute("roles", rolRepository.findAllByOrderByNombre());
            return "usuarios/edit";
        }

        Usuario existente = usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        existente.setName(usuario.getName());
        existente.setEmail(usuario.getEmail());

        if (usuario.getRol() != null && usuario.getRol().getId() != null) {
            rolRepository.findById(usuario.getRol().getId()).ifPresent(existente::setRol);
        }

        if (usuario.getPassword() != null && !usuario.getPassword().isEmpty()) {
            existente.setPassword(passwordEncoder.encode(usuario.getPassword()));
        }

        usuarioRepository.save(existente);
        flash.addFlashAttribute("exito", "Usuario actualizado correctamente.");
        return "redirect:/usuarios";
    }

    @PatchMapping("/{id}/toggle-activo")
    public String toggleActivo(@PathVariable Long id,
                               @AuthenticationPrincipal Usuario usuarioActual,
                               RedirectAttributes flash) {
        if (usuarioActual.getId().equals(id)) {
            flash.addFlashAttribute("error", "No puedes desactivar tu propio usuario.");
            return "redirect:/usuarios";
        }
        usuarioService.toggleActivo(id);
        flash.addFlashAttribute("exito", "Estado del usuario actualizado correctamente.");
        return "redirect:/usuarios";
    }
}
