package com.pathvision.repository;

import com.pathvision.entity.CollegeFeeDetail;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CollegeFeeDetailRepository extends JpaRepository<CollegeFeeDetail, Long> {
    List<CollegeFeeDetail> findByCollegeIdOrderByDisplayOrderAscIdAsc(Long collegeId);
    void deleteByCollegeId(Long collegeId);
}
