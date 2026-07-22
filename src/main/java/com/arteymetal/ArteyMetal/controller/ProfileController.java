package com.arteymetal.ArteyMetal.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.arteymetal.ArteyMetal.entity.Usuario;
import com.arteymetal.ArteyMetal.repository.UsuarioRepository;

@Controller
@RequestMapping("/perfil")
public class ProfileController {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public ProfileController(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping
    public String edit(@AuthenticationPrincipal Usuario usuario, Model model) {
        model.addAttribute("usuario", usuario);
        return "profile/edit";
    }

    @PostMapping
    public String update(@AuthenticationPrincipal Usuario usuario,
                         @RequestParam("name") String name,
                         @RequestParam("email") String email,
                         RedirectAttributes redirectAttributes) {
        if (name == null || name.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "El nombre es obligatorio.");
            return "redirect:/perfil";
        }

        if (email == null || email.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "El correo electrónico es obligatorio.");
            return "redirect:/perfil";
        }

        usuario.setName(name.trim());
        usuario.setEmail(email.trim());
        usuarioRepository.save(usuario);

        redirectAttributes.addFlashAttribute("success", "Perfil actualizado correctamente.");
        return "redirect:/perfil";
    }

    @PostMapping("/eliminar")
    public String destroy(@AuthenticationPrincipal Usuario usuario,
                          @RequestParam("password") String password,
                          RedirectAttributes redirectAttributes) {
        if (!passwordEncoder.matches(password, usuario.getPassword())) {
            redirectAttributes.addFlashAttribute("error", "La contraseña ingresada es incorrecta.");
            return "redirect:/perfil";
        }

        usuarioRepository.delete(usuario);
        SecurityContextHolder.clearContext();

        return "redirect:/login";
    }
}
