package com.example.bank_statement_analyzer.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "upload_sessions")
public class UploadSession {
    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "public_token_hash", nullable = false, unique = true, length = 64)
    private String publicTokenHash;

    @Column(name = "transaction_count", nullable = false)
    private Integer transactionCount;

    @Column(name = "price_per_transaction_paise", nullable = false)
    private Integer pricePerTransactionPaise;

    @Column(name = "total_amount_paise", nullable = false)
    private Long totalAmountPaise;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false, length = 32)
    private String status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @JdbcTypeCode(SqlTypes.VARBINARY)
    @Column(name = "workbook")
    private byte[] workbook;

    @Column(name = "redeemed_at")
    private Instant redeemedAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getPublicTokenHash() { return publicTokenHash; }
    public void setPublicTokenHash(String publicTokenHash) { this.publicTokenHash = publicTokenHash; }
    public Integer getTransactionCount() { return transactionCount; }
    public void setTransactionCount(Integer transactionCount) { this.transactionCount = transactionCount; }
    public Integer getPricePerTransactionPaise() { return pricePerTransactionPaise; }
    public void setPricePerTransactionPaise(Integer pricePerTransactionPaise) { this.pricePerTransactionPaise = pricePerTransactionPaise; }
    public Long getTotalAmountPaise() { return totalAmountPaise; }
    public void setTotalAmountPaise(Long totalAmountPaise) { this.totalAmountPaise = totalAmountPaise; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
    public byte[] getWorkbook() { return workbook; }
    public void setWorkbook(byte[] workbook) { this.workbook = workbook; }
    public Instant getRedeemedAt() { return redeemedAt; }
    public void setRedeemedAt(Instant redeemedAt) { this.redeemedAt = redeemedAt; }
}
