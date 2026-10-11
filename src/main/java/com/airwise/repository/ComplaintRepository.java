package com.airwise.repository;

import com.airwise.entity.Complaint;
import com.airwise.entity.ComplaintPriority;
import com.airwise.entity.ComplaintStatus;
import com.airwise.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ComplaintRepository extends JpaRepository<Complaint, Long> {

    Optional<Complaint> findByReferenceNo(String referenceNo);

    List<Complaint> findByUserOrderByCreatedAtDesc(User user);

    List<Complaint> findAllByOrderByCreatedAtDesc();

    List<Complaint> findTop10ByOrderByCreatedAtDesc();

    long countByStatus(ComplaintStatus status);

    long countByPriority(ComplaintPriority priority);

    long countByCategory(String category);

    @Query("SELECT c.category, COUNT(c) FROM Complaint c GROUP BY c.category")
    List<Object[]> countGroupByCategory();

    @Query("SELECT c.status, COUNT(c) FROM Complaint c GROUP BY c.status")
    List<Object[]> countGroupByStatus();

    @Query("SELECT c.assignedDepartment, COUNT(c) FROM Complaint c GROUP BY c.assignedDepartment")
    List<Object[]> countGroupByDepartment();
}
