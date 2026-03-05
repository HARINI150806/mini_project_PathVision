package com.pathvision.repository;

import com.pathvision.entity.CollegeCutoff;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CollegeCutoffRepository extends JpaRepository<CollegeCutoff, Long> {
    Optional<CollegeCutoff> findByCollegeIdAndCommunityIgnoreCase(Long collegeId, String community);
    List<CollegeCutoff> findByCommunityIgnoreCase(String community);
}
