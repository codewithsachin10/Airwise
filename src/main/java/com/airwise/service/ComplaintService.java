package com.airwise.service;

import com.airwise.complaint.ComplaintNLPAnalyzer;
import com.airwise.complaint.ComplaintNLPAnalyzer.AnalysisOutput;
import com.airwise.entity.*;
import com.airwise.repository.AlertRepository;
import com.airwise.repository.ComplaintAnalysisRepository;
import com.airwise.repository.ComplaintRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Random;

@Service
public class ComplaintService {

    private final ComplaintRepository complaintRepository;
    private final ComplaintAnalysisRepository complaintAnalysisRepository;
    private final ComplaintNLPAnalyzer complaintNLPAnalyzer;
    private final AlertRepository alertRepository;
    private final NotificationService notificationService;
    private final Random random = new Random();

    public ComplaintService(ComplaintRepository complaintRepository,
                            ComplaintAnalysisRepository complaintAnalysisRepository,
                            ComplaintNLPAnalyzer complaintNLPAnalyzer,
                            AlertRepository alertRepository,
                            NotificationService notificationService) {
        this.complaintRepository = complaintRepository;
        this.complaintAnalysisRepository = complaintAnalysisRepository;
        this.complaintNLPAnalyzer = complaintNLPAnalyzer;
        this.alertRepository = alertRepository;
        this.notificationService = notificationService;
    }

    public List<Complaint> getAllComplaints() {
        return complaintRepository.findAllByOrderByCreatedAtDesc();
    }

    public List<Complaint> getUserComplaints(User user) {
        return complaintRepository.findByUserOrderByCreatedAtDesc(user);
    }

    public Optional<Complaint> getComplaintById(Long id) {
        return complaintRepository.findById(id);
    }

    public Optional<Complaint> getComplaintByReference(String referenceNo) {
        return complaintRepository.findByReferenceNo(referenceNo);
    }

    public Optional<ComplaintAnalysis> getAnalysisForComplaint(Complaint complaint) {
        return complaintAnalysisRepository.findByComplaint(complaint);
    }

    @Transactional
    public Complaint submitComplaint(User user, String title, String description, String userCategory,
                                     Airport airport, String terminal, String flightNumber,
                                     String bookingReference, String preferredContact, String attachmentName) {

        // Generate Reference: AW-CP-#####
        String ref = "AW-CP-" + (10000 + random.nextInt(90000));

        // 1. NLP Analysis
        AnalysisOutput analysisOutput = complaintNLPAnalyzer.analyze(title, description, userCategory);

        // 2. Create Complaint
        Complaint complaint = new Complaint();
        complaint.setReferenceNo(ref);
        complaint.setUser(user);
        complaint.setTitle(title);
        complaint.setDescription(description);
        complaint.setCategory(analysisOutput.category());
        complaint.setPriority(analysisOutput.priority());
        complaint.setStatus(ComplaintStatus.SUBMITTED);
        complaint.setAssignedDepartment(analysisOutput.recommendedDepartment());
        complaint.setAirport(airport);
        complaint.setTerminal(terminal != null && !terminal.isBlank() ? terminal : "Terminal 2");
        complaint.setFlightNumber(flightNumber);
        complaint.setBookingReference(bookingReference);
        complaint.setPreferredContact(preferredContact != null ? preferredContact : "Email");
        complaint.setAttachmentName(attachmentName);
        complaint.setCreatedAt(LocalDateTime.now());
        complaint.setUpdatedAt(LocalDateTime.now());

        Complaint savedComplaint = complaintRepository.save(complaint);

        // 3. Save Complaint Analysis record
        ComplaintAnalysis analysis = new ComplaintAnalysis();
        analysis.setComplaint(savedComplaint);
        analysis.setDetectedCategory(analysisOutput.category());
        analysis.setSentiment(analysisOutput.sentiment());
        analysis.setPriority(analysisOutput.priority());
        analysis.setExtractedKeywords(analysisOutput.extractedKeywords());
        analysis.setSummaryReason(analysisOutput.summaryReason());
        analysis.setRecommendedDepartment(analysisOutput.recommendedDepartment());
        analysis.setUrgentSafety(analysisOutput.isUrgentSafety());
        analysis.setAnalyzedAt(LocalDateTime.now());

        complaintAnalysisRepository.save(analysis);

        // 4. If Urgent / Safety-Critical, generate operational Alert
        if (analysisOutput.isUrgentSafety() || analysisOutput.priority() == ComplaintPriority.URGENT) {
            Alert alert = new Alert(
                    "URGENT SAFETY COMPLAINT: " + ref,
                    "High-priority passenger report received: '" + title + "'. Auto-assigned to: " + analysisOutput.recommendedDepartment(),
                    AlertSeverity.CRITICAL,
                    "COMPLAINT",
                    airport
            );
            alertRepository.save(alert);
        }

        // 5. In-app passenger notification
        notificationService.createNotification(
                user,
                "Complaint Registered: " + ref,
                "Your report '" + title + "' has been routed to " + analysisOutput.recommendedDepartment() +
                        ". Priority assigned: " + analysisOutput.priority(),
                "COMPLAINT",
                "/complaints/view/" + ref
        );

        return savedComplaint;
    }

    @Transactional
    public Complaint updateComplaintStatus(Long id, ComplaintStatus newStatus, String department, String resolutionNotes) {
        Complaint complaint = complaintRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Complaint not found with ID: " + id));

        complaint.setStatus(newStatus);
        if (department != null && !department.isBlank()) {
            complaint.setAssignedDepartment(department);
        }
        if (resolutionNotes != null && !resolutionNotes.isBlank()) {
            complaint.setResolutionNotes(resolutionNotes);
        }
        complaint.setUpdatedAt(LocalDateTime.now());

        Complaint saved = complaintRepository.save(complaint);

        // Notify user about status transition
        notificationService.createNotification(
                complaint.getUser(),
                "Complaint Status Updated: " + complaint.getReferenceNo(),
                "Your complaint '" + complaint.getTitle() + "' is now marked as " + newStatus +
                        (resolutionNotes != null ? ". Notes: " + resolutionNotes : ""),
                "COMPLAINT",
                "/complaints/view/" + complaint.getReferenceNo()
        );

        return saved;
    }

    public long countTotalComplaints() {
        return complaintRepository.count();
    }

    public long countOpenComplaints() {
        return complaintRepository.countByStatus(ComplaintStatus.SUBMITTED) +
               complaintRepository.countByStatus(ComplaintStatus.UNDER_REVIEW) +
               complaintRepository.countByStatus(ComplaintStatus.ASSIGNED) +
               complaintRepository.countByStatus(ComplaintStatus.IN_PROGRESS);
    }

    public long countUrgentComplaints() {
        return complaintRepository.countByPriority(ComplaintPriority.URGENT);
    }

    public List<Object[]> getCategoryBreakdown() {
        return complaintRepository.countGroupByCategory();
    }

    public List<Object[]> getStatusBreakdown() {
        return complaintRepository.countGroupByStatus();
    }

    public List<Object[]> getDepartmentBreakdown() {
        return complaintRepository.countGroupByDepartment();
    }

    public List<Object[]> getSentimentBreakdown() {
        return complaintAnalysisRepository.countGroupBySentiment();
    }
}
