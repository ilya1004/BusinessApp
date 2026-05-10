package oll.business.repository;

import oll.business.model.AppLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AppLogRepository extends JpaRepository<AppLog, Long> {

    Page<AppLog> findByOrderByTimestampDesc(Pageable pageable);

    Page<AppLog> findByLevelOrderByTimestampDesc(String level, Pageable pageable);

    Page<AppLog> findBySourceOrderByTimestampDesc(String source, Pageable pageable);

    @Query("SELECT l FROM AppLog l WHERE l.timestamp BETWEEN :start AND :end ORDER BY l.timestamp DESC")
    Page<AppLog> findByTimestampBetween(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            Pageable pageable);

    @Query("SELECT l FROM AppLog l WHERE l.level = :level AND l.timestamp BETWEEN :start AND :end ORDER BY l.timestamp DESC")
    Page<AppLog> findByLevelAndTimestampBetween(
            @Param("level") String level,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            Pageable pageable);

    @Query("SELECT l FROM AppLog l WHERE l.userId = :userId ORDER BY l.timestamp DESC")
    Page<AppLog> findByUserIdOrderByTimestampDesc(@Param("userId") Long userId, Pageable pageable);

    List<AppLog> findTop100ByOrderByTimestampDesc();

    void deleteByTimestampBefore(LocalDateTime before);
}