package com.lgourabdash.portfolio;

import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminAccessAuditRepository extends JpaRepository<AdminAccessAudit, Long> {

    List<AdminAccessAudit> findByFirebaseEmailOrderByCreatedAtDesc(
            String firebaseEmail, Pageable pageable);

    List<AdminAccessAudit> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
