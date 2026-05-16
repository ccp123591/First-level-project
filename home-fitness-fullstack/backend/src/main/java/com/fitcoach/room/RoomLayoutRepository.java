package com.fitcoach.room;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoomLayoutRepository extends JpaRepository<RoomLayoutSnapshot, Long> {

    /** 最新一份（coach 注入用）。 */
    Optional<RoomLayoutSnapshot> findTopByUserIdOrderByCapturedAtDesc(Long userId);

    Page<RoomLayoutSnapshot> findByUserIdOrderByCapturedAtDesc(Long userId, Pageable pageable);
}
