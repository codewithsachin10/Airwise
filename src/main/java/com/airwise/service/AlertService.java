package com.airwise.service;

import com.airwise.entity.Alert;
import com.airwise.entity.AlertSeverity;
import com.airwise.repository.AlertRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class AlertService {

    private final AlertRepository alertRepository;

    public AlertService(AlertRepository alertRepository) {
        this.alertRepository = alertRepository;
    }

    public List<Alert> getActiveAlerts() {
        return alertRepository.findByActiveTrueOrderByCreatedAtDesc();
    }

    public List<Alert> getRecentActiveAlerts() {
        return alertRepository.findTop5ByActiveTrueOrderByCreatedAtDesc();
    }

    public long countActiveAlerts() {
        return alertRepository.countByActiveTrue();
    }

    public long countCriticalAlerts() {
        return alertRepository.countByActiveTrueAndSeverity(AlertSeverity.CRITICAL);
    }

    @Transactional
    public void dismissAlert(Long id) {
        Optional<Alert> opt = alertRepository.findById(id);
        if (opt.isPresent()) {
            Alert alert = opt.get();
            alert.setActive(false);
            alertRepository.save(alert);
        }
    }
}
