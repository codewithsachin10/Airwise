package com.airwise.repository;

import com.airwise.entity.TaxiOperator;
import com.airwise.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TaxiOperatorRepository extends JpaRepository<TaxiOperator, Long> {
    Optional<TaxiOperator> findByUser(User user);
    Optional<TaxiOperator> findByUserId(Long userId);
}
