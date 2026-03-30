package com.lgourabdash.portfolio;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PortfolioAttachmentRepository extends JpaRepository<PortfolioAttachment, Long> {

    List<PortfolioAttachment> findByUserProfileIdOrderByIdDesc(Long userProfileId);

    List<PortfolioAttachment> findByUserProfileIdAndKindOrderByIdDesc(Long userProfileId, String kind);

    List<PortfolioAttachment> findByUserProfileIdAndKind(Long userProfileId, String kind);

    Optional<PortfolioAttachment> findFirstByUserProfileIdAndKindAndIsActiveTrueOrderByIdDesc(
            Long userProfileId, String kind);

    long countByUserProfileIdAndKind(Long userProfileId, String kind);

    boolean existsByUserProfileIdAndKindAndIsActiveTrue(Long userProfileId, String kind);

    Optional<PortfolioAttachment> findByIdAndUserProfileId(Long id, Long userProfileId);
}
