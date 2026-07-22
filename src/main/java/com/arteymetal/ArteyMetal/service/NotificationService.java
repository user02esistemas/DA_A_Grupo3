package com.arteymetal.ArteyMetal.service;

import com.arteymetal.ArteyMetal.entity.Notification;
import com.arteymetal.ArteyMetal.repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class NotificationService {

    @Autowired private NotificationRepository notificationRepository;

    @Transactional
    public Notification crear(Long userId, String type, String title, String body, String actionUrl) {
        Notification notif = Notification.builder()
            .userId(userId)
            .type(type)
            .title(title)
            .body(body)
            .actionUrl(actionUrl)
            .isRead(false)
            .build();
        return notificationRepository.save(notif);
    }

    public List<Notification> listarPorUsuario(Long userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public long contarNoLeidas(Long userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    public List<Notification> noLeidas(Long userId) {
        return notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId);
    }

    @Transactional
    public void marcarLeida(Long id, Long userId) {
        notificationRepository.findById(id).ifPresent(n -> {
            if (n.getUserId().equals(userId)) {
                n.setIsRead(true);
                n.setReadAt(LocalDateTime.now());
                notificationRepository.save(n);
            }
        });
    }

    @Transactional
    public void marcarTodasLeidas(Long userId) {
        List<Notification> noLeidas = notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId);
        for (Notification n : noLeidas) {
            n.setIsRead(true);
            n.setReadAt(LocalDateTime.now());
            notificationRepository.save(n);
        }
    }
}
