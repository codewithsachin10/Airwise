package com.airwise.repository;

import com.airwise.entity.Alert;
import com.airwise.entity.AlertSeverity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AlertRepository extends JpaRepository<Alert, Long> {
    List<Alert> findByActiveTrueOrderByCreatedAtDesc();
    List<Alert> findTop5ByActiveTrueOrderByCreatedAtDesc();
    List<Alert> findByActiveTrueAndSeverity(AlertSeverity severity);
    long countByActiveTrue();
    long countByActiveTrueAndSeverity(AlertSeverity severity);
}
