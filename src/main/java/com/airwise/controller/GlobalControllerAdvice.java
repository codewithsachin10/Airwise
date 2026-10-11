package com.airwise.controller;

import com.airwise.entity.Alert;
import com.airwise.entity.User;
import com.airwise.repository.AlertRepository;
import com.airwise.repository.NotificationRepository;
import com.airwise.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.List;

@ControllerAdvice
public class GlobalControllerAdvice {

    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;
    private final AlertRepository alertRepository;

    public GlobalControllerAdvice(UserRepository userRepository,
                                  NotificationRepository notificationRepository,
                                  AlertRepository alertRepository) {
        this.userRepository = userRepository;
        this.notificationRepository = notificationRepository;
        this.alertRepository = alertRepository;
    }

    @ModelAttribute("currentUser")
    public User currentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            return null;
        }
        return userRepository.findByUsername(authentication.getName()).orElse(null);
    }

    @ModelAttribute("unreadNotifCount")
    public long unreadNotifCount(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            return 0;
        }
        return userRepository.findByUsername(authentication.getName())
                .map(notificationRepository::countByUserAndReadFalse)
                .orElse(0L);
    }

    @ModelAttribute("globalActiveAlerts")
    public List<Alert> globalActiveAlerts() {
        return alertRepository.findTop5ByActiveTrueOrderByCreatedAtDesc();
    }
}
