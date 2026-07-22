package com.arteymetal.ArteyMetal.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.arteymetal.ArteyMetal.entity.Usuario;

@Controller
public class AuthController {

    @GetMapping("/login")
    public String login() {
        return "auth/login";
    }

    @GetMapping("/")
    public String root() {
        return "redirect:/login";
    }

    @GetMapping("/register")
    public String register() {
        return "auth/register";
    }

    @PostMapping("/register")
    public String registerStore(
            @RequestParam String name,
            @RequestParam String email,
            @RequestParam String password,
            @RequestParam String password_confirmation,
            RedirectAttributes ra) {
        if (!password.equals(password_confirmation)) {
            ra.addFlashAttribute("error", "Las contrasenas no coinciden.");
            return "redirect:/register";
        }
        ra.addFlashAttribute("exito", "Registro exitoso. Ahora puedes iniciar sesion.");
        return "redirect:/login";
    }

    @GetMapping("/forgot-password")
    public String forgotPassword() {
        return "auth/forgot-password";
    }

    @PostMapping("/forgot-password")
    public String forgotPasswordStore(
            @RequestParam String email,
            RedirectAttributes ra) {
        ra.addFlashAttribute("exito", "Si el correo existe, se ha enviado un codigo de recuperacion.");
        return "redirect:/forgot-password";
    }

    @GetMapping("/reset-password-code")
    public String resetPasswordCode() {
        return "auth/reset-password-code";
    }

    @PostMapping("/reset-password-code")
    public String resetPasswordCodeStore(
            @RequestParam String email,
            @RequestParam String code,
            @RequestParam String password,
            @RequestParam String password_confirmation,
            RedirectAttributes ra) {
        if (!password.equals(password_confirmation)) {
            ra.addFlashAttribute("error", "Las contrasenas no coinciden.");
            return "redirect:/reset-password-code";
        }
        ra.addFlashAttribute("exito", "Contrasena restablecida correctamente.");
        return "redirect:/login";
    }

    @GetMapping("/confirm-password")
    public String confirmPassword() {
        return "auth/confirm-password";
    }

    @PostMapping("/confirm-password")
    public String confirmPasswordStore(
            @RequestParam String password,
            RedirectAttributes ra) {
        ra.addFlashAttribute("exito", "Contrasena confirmada.");
        return "redirect:/dashboard";
    }

    @GetMapping("/verify-email")
    public String verifyEmail() {
        return "auth/verify-email";
    }

    @PostMapping("/verify-email")
    public String verifyEmailStore(RedirectAttributes ra) {
        ra.addFlashAttribute("exito", "Correo de verificacion reenviado.");
        return "redirect:/verify-email";
    }
}
