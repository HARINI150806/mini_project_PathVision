package com.pathvision.repository;

import com.pathvision.entity.StudentProfile;
import com.pathvision.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StudentProfileRepository extends JpaRepository<StudentProfile, Long> {
    Optional<StudentProfile> findByUser(User user);
    boolean existsByUser(User user);
    Optional<StudentProfile> findByUserId(Long userId);
}
