package com.example.bank_statement_analyzer.repository;

import com.example.bank_statement_analyzer.model.DownloadRecord;
import com.example.bank_statement_analyzer.model.UploadSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;

import java.util.Optional;
import java.util.UUID;

public interface DownloadRecordRepository extends JpaRepository<DownloadRecord, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<DownloadRecord> findByUploadSession(UploadSession uploadSession);
}
