package com.example.bank_statement_analyzer.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "download_records")
public class DownloadRecord {
    @Id
    @GeneratedValue
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "upload_session_id", nullable = false, unique = true)
    private UploadSession uploadSession;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payment_id", nullable = false)
    private Payment payment;

    @Column(name = "download_status", nullable = false, length = 32)
    private String downloadStatus;

    @Column(name = "redemption_status", nullable = false, length = 32)
    private String redemptionStatus;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "redeemed_at")
    private Instant redeemedAt;

    public UUID getId() { return id; }
    public UploadSession getUploadSession() { return uploadSession; }
    public void setUploadSession(UploadSession uploadSession) { this.uploadSession = uploadSession; }
    public Payment getPayment() { return payment; }
    public void setPayment(Payment payment) { this.payment = payment; }
    public String getDownloadStatus() { return downloadStatus; }
    public void setDownloadStatus(String downloadStatus) { this.downloadStatus = downloadStatus; }
    public String getRedemptionStatus() { return redemptionStatus; }
    public void setRedemptionStatus(String redemptionStatus) { this.redemptionStatus = redemptionStatus; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getRedeemedAt() { return redeemedAt; }
    public void setRedeemedAt(Instant redeemedAt) { this.redeemedAt = redeemedAt; }
}
