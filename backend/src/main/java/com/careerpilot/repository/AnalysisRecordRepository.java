package com.careerpilot.repository;

import com.careerpilot.model.AnalysisRecord;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnalysisRecordRepository extends JpaRepository<AnalysisRecord, Long> {

    List<AnalysisRecord> findTop10ByOrderByCreatedAtDesc();

    List<AnalysisRecord> findTop10ByUserIdOrderByCreatedAtDesc(Long userId);

    List<AnalysisRecord> findTop10ByUserIdIsNullOrderByCreatedAtDesc();
}
