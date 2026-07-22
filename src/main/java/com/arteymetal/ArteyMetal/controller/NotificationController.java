package com.arteymetal.ArteyMetal.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.arteymetal.ArteyMetal.entity.Notification;
import com.arteymetal.ArteyMetal.entity.Usuario;
import com.arteymetal.ArteyMetal.repository.NotificationRepository;

@Controller
@RequestMapping("/notificaciones")
public class NotificationController {

    private final NotificationRepository notificationRepository;

    public NotificationController(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @GetMapping
    public String index(@AuthenticationPrincipal Usuario usuario, Model model) {
        List<Notification> notificaciones = notificationRepository.findByUserIdOrderByCreatedAtDesc(usuario.getId());
        long noLeidas = notificationRepository.countByUserIdAndIsReadFalse(usuario.getId());

        model.addAttribute("notificaciones", notificaciones);
        model.addAttribute("noLeidas", noLeidas);

        return "notifications/index";
    }

    @GetMapping("/no-leidas")
    public ResponseEntity<Map<String, Object>> unread(@AuthenticationPrincipal Usuario usuario) {
        List<Notification> notificaciones = notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(usuario.getId());
        long count = notificationRepository.countByUserIdAndIsReadFalse(usuario.getId());

        List<Map<String, Object>> lista = new ArrayList<>();
        int limit = Math.min(notificaciones.size(), 50);
        for (int i = 0; i < limit; i++) {
            Notification n = notificaciones.get(i);
            Map<String, Object> item = new HashMap<>();
            item.put("id", n.getId());
            item.put("title", n.getTitle());
            item.put("body", n.getBody());
            item.put("actionUrl", n.getActionUrl());
            item.put("isRead", n.getIsRead());
            item.put("createdAt", n.getCreatedAt());
            lista.add(item);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("ok", true);
        Map<String, Object> data = new HashMap<>();
        data.put("count", count);
        data.put("notifications", lista);
        response.put("data", data);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/leer")
    public String markAsRead(@PathVariable Long id, @AuthenticationPrincipal Usuario usuario, RedirectAttributes redirectAttributes) {
        notificationRepository.findById(id).ifPresent(notification -> {
            if (notification.getUserId().equals(usuario.getId())) {
                notification.setIsRead(true);
                notificationRepository.save(notification);
            }
        });

        redirectAttributes.addFlashAttribute("success", "Notificación marcada como leída.");
        return "redirect:/notificaciones";
    }

    @PostMapping("/leer-todas")
    public String markAllAsRead(@AuthenticationPrincipal Usuario usuario, RedirectAttributes redirectAttributes) {
        List<Notification> noLeidas = notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(usuario.getId());
        for (Notification n : noLeidas) {
            n.setIsRead(true);
        }
        notificationRepository.saveAll(noLeidas);

        redirectAttributes.addFlashAttribute("success", "Todas las notificaciones marcadas como leídas.");
        return "redirect:/notificaciones";
    }
}
