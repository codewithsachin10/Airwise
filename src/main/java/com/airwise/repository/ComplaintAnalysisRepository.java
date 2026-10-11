package com.airwise.repository;

import com.airwise.entity.Complaint;
import com.airwise.entity.ComplaintAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ComplaintAnalysisRepository extends JpaRepository<ComplaintAnalysis, Long> {
    Optional<ComplaintAnalysis> findByComplaint(Complaint complaint);
    Optional<ComplaintAnalysis> findByComplaintId(Long complaintId);

    @Query("SELECT ca.sentiment, COUNT(ca) FROM ComplaintAnalysis ca GROUP BY ca.sentiment")
    List<Object[]> countGroupBySentiment();
}
