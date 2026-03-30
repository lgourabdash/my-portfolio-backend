package com.lgourabdash.portfolio;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PortfolioSectionRepository extends JpaRepository<PortfolioSection, Long> {

    Optional<PortfolioSection> findBySectionKey(String sectionKey);
}
