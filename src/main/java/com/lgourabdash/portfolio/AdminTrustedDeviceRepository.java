package com.lgourabdash.portfolio;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminTrustedDeviceRepository extends JpaRepository<AdminTrustedDevice, Long> {

    List<AdminTrustedDevice> findByFirebaseEmailOrderByCreatedAtDesc(String firebaseEmail);

    List<AdminTrustedDevice> findAllByOrderByCreatedAtDesc();
}
