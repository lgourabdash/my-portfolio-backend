package com.lgourabdash.portfolio;


import com.lgourabdash.portfolio.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {
}
