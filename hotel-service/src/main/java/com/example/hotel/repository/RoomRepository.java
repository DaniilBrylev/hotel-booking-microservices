package com.example.hotel.repository;

import com.example.hotel.model.Room;
import com.example.hotel.model.RoomLockStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface RoomRepository extends JpaRepository<Room, Long> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select r from Room r where r.id = :id")
  Optional<Room> findByIdForUpdate(@Param("id") Long id);

  List<Room> findByHotelId(Long hotelId);

  @Query("select r from Room r where r.available = true and r.id not in (" +
      "select rl.roomId from RoomLock rl where rl.status = :status " +
      "and rl.startDate <= :endDate and rl.endDate >= :startDate) " +
      "order by r.timesBooked asc, r.id asc")
  List<Room> findRecommendations(@Param("startDate") LocalDate startDate,
                                 @Param("endDate") LocalDate endDate,
                                 @Param("status") RoomLockStatus status);
}
