package com.example.bank_statement_analyzer.repository;

import com.example.bank_statement_analyzer.model.UploadSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface UploadSessionRepository extends JpaRepository<UploadSession, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<UploadSession> findByPublicTokenHash(String publicTokenHash);

    @Modifying
    @Query("update UploadSession s set s.workbook = null, s.status = 'EXPIRED' where s.expiresAt < :now and s.redeemedAt is null")
    int expireAndClear(Instant now);
}
