package com.example.hotel.repository;

import com.example.hotel.model.RoomLock;
import com.example.hotel.model.RoomLockStatus;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RoomLockRepository extends JpaRepository<RoomLock, Long> {
  Optional<RoomLock> findByRequestId(String requestId);

  Optional<RoomLock> findFirstByBookingIdAndStatus(Long bookingId, RoomLockStatus status);

  @Query("select count(rl) > 0 from RoomLock rl " +
      "where rl.roomId = :roomId and rl.status = :status " +
      "and rl.startDate <= :endDate and rl.endDate >= :startDate")
  boolean existsOverlappingLock(@Param("roomId") Long roomId,
                                @Param("startDate") LocalDate startDate,
                                @Param("endDate") LocalDate endDate,
                                @Param("status") RoomLockStatus status);
}
