package com.arteymetal.ArteyMetal.controller;

import com.arteymetal.ArteyMetal.entity.PasswordResetCode;
import com.arteymetal.ArteyMetal.entity.Usuario;
import com.arteymetal.ArteyMetal.repository.PasswordResetCodeRepository;
import com.arteymetal.ArteyMetal.repository.UsuarioRepository;
import com.arteymetal.ArteyMetal.service.EmailService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.concurrent.ThreadLocalRandom;

@Controller
public class AuthController {

    private final UsuarioRepository usuarioRepository;
    private final PasswordResetCodeRepository passwordResetCodeRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    public AuthController(UsuarioRepository usuarioRepository,
                          PasswordResetCodeRepository passwordResetCodeRepository,
                          PasswordEncoder passwordEncoder,
                          EmailService emailService) {
        this.usuarioRepository = usuarioRepository;
        this.passwordResetCodeRepository = passwordResetCodeRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

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
            RedirectAttributes ra,
            Model model) {
        if (email == null || email.trim().isEmpty()) {
            model.addAttribute("error_email", "El correo electronico es obligatorio.");
            return "auth/forgot-password";
        }

        var usuario = usuarioRepository.findByEmail(email.trim());

        if (usuario.isEmpty() || usuario.get().getActivo() == null || !usuario.get().getActivo()) {
            ra.addFlashAttribute("exito", "Si el correo existe, se ha enviado un codigo de recuperacion.");
            return "redirect:/forgot-password";
        }

        String code = String.format("%06d", ThreadLocalRandom.current().nextInt(0, 1000000));

        passwordResetCodeRepository.deleteByEmail(email.trim());

        PasswordResetCode resetCode = new PasswordResetCode();
        resetCode.setEmail(email.trim());
        resetCode.setCode(code);
        resetCode.setExpiresAt(LocalDateTime.now().plusMinutes(15));
        passwordResetCodeRepository.save(resetCode);

        try {
            emailService.sendPasswordResetCode(email.trim(), code);
        } catch (Exception e) {
            // Still redirect even if email fails (dev mode with no SMTP)
        }

        return "redirect:/reset-password-code?email=" + email.trim();
    }

    @GetMapping("/reset-password-code")
    public String resetPasswordCode(@RequestParam(required = false) String email, Model model) {
        model.addAttribute("old_email", email != null ? email : "");
        return "auth/reset-password-code";
    }

    @PostMapping("/reset-password-code")
    public String resetPasswordCodeStore(
            @RequestParam String email,
            @RequestParam String code,
            @RequestParam String password,
            @RequestParam String password_confirmation,
            RedirectAttributes ra,
            Model model) {
        if (email == null || email.trim().isEmpty()) {
            ra.addFlashAttribute("error", "El correo electronico es obligatorio.");
            return "redirect:/reset-password-code";
        }

        if (!password.equals(password_confirmation)) {
            ra.addFlashAttribute("error", "Las contrasenas no coinciden.");
            model.addAttribute("old_email", email);
            return "auth/reset-password-code";
        }

        if (password.length() < 6) {
            ra.addFlashAttribute("error", "La contrasena debe tener al menos 6 caracteres.");
            model.addAttribute("old_email", email);
            return "auth/reset-password-code";
        }

        var resetCode = passwordResetCodeRepository.findTopByEmailAndCodeAndExpiresAtAfter(
                email.trim(), code, LocalDateTime.now());

        if (resetCode == null) {
            ra.addFlashAttribute("error", "El codigo es invalido o ya expiro.");
            model.addAttribute("old_email", email);
            return "auth/reset-password-code";
        }

        var usuario = usuarioRepository.findByEmail(email.trim());
        if (usuario.isEmpty() || usuario.get().getActivo() == null || !usuario.get().getActivo()) {
            ra.addFlashAttribute("error", "No se encontro una cuenta activa con ese correo.");
            return "redirect:/forgot-password";
        }

        Usuario user = usuario.get();
        user.setPassword(passwordEncoder.encode(password));
        usuarioRepository.save(user);

        passwordResetCodeRepository.deleteByEmail(email.trim());

        ra.addFlashAttribute("exito", "Tu contrasena ha sido restablecida. Inicia sesion.");
        return "redirect:/login";
    }

    @GetMapping("/confirm-password")
    public String confirmPassword() {
        return "auth/confirm-password";
    }

    @PostMapping("/confirm-password")
    public String confirmPasswordStore(
            @RequestParam String password,
            @AuthenticationPrincipal Usuario usuario,
            RedirectAttributes ra) {
        if (usuario == null) {
            return "redirect:/login";
        }
        if (!passwordEncoder.matches(password, usuario.getPassword())) {
            ra.addFlashAttribute("error", "La contrasena ingresada es incorrecta.");
            return "redirect:/confirm-password";
        }
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

    @PutMapping("/password/update")
    public String passwordUpdate(
            @RequestParam("current_password") String currentPassword,
            @RequestParam("password") String newPassword,
            @RequestParam(value = "password_confirmation", required = false) String passwordConfirmation,
            @AuthenticationPrincipal Usuario usuario,
            RedirectAttributes ra) {
        if (usuario == null) {
            return "redirect:/login";
        }

        if (!passwordEncoder.matches(currentPassword, usuario.getPassword())) {
            ra.addFlashAttribute("error", "La contrasena actual es incorrecta.");
            return "redirect:/perfil";
        }

        if (newPassword == null || newPassword.length() < 6) {
            ra.addFlashAttribute("error", "La nueva contrasena debe tener al menos 6 caracteres.");
            return "redirect:/perfil";
        }

        if (!newPassword.equals(passwordConfirmation)) {
            ra.addFlashAttribute("error", "Las contrasenas no coinciden.");
            return "redirect:/perfil";
        }

        usuario.setPassword(passwordEncoder.encode(newPassword));
        usuarioRepository.save(usuario);

        ra.addFlashAttribute("success", "Contrasena actualizada correctamente.");
        return "redirect:/perfil";
    }
}
