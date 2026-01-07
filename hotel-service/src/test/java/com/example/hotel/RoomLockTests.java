package com.example.hotel;

import com.example.hotel.model.Room;
import com.example.hotel.model.RoomLockStatus;
import com.example.hotel.repository.RoomLockRepository;
import com.example.hotel.repository.RoomRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class RoomLockTests {
  private static final String INTERNAL_TOKEN = "test-internal-token";

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private RoomRepository roomRepository;

  @Autowired
  private RoomLockRepository roomLockRepository;

  @Autowired
  private ObjectMapper objectMapper;

  @BeforeEach
  void setup() {
    roomLockRepository.deleteAll();
  }

  @Test
  void conflictOnOverlappingDatesReturns409() throws Exception {
    Room room = roomRepository.findAll().get(0);
    LocalDate start = LocalDate.now().plusDays(1);
    LocalDate end = start.plusDays(2);

    mockMvc.perform(post("/api/rooms/{id}/confirm-availability", room.getId())
            .header("X-Internal-Token", INTERNAL_TOKEN)
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestJson(start, end, UUID.randomUUID().toString(), 100L)))
        .andExpect(status().isOk());

    mockMvc.perform(post("/api/rooms/{id}/confirm-availability", room.getId())
            .header("X-Internal-Token", INTERNAL_TOKEN)
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestJson(start.plusDays(1), end.plusDays(1), UUID.randomUUID().toString(), 200L)))
        .andExpect(status().isConflict());
  }

  @Test
  void confirmIsIdempotentByRequestId() throws Exception {
    Room room = roomRepository.findAll().get(0);
    String requestId = UUID.randomUUID().toString();
    LocalDate start = LocalDate.now().plusDays(3);
    LocalDate end = start.plusDays(1);

    mockMvc.perform(post("/api/rooms/{id}/confirm-availability", room.getId())
            .header("X-Internal-Token", INTERNAL_TOKEN)
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestJson(start, end, requestId, 300L)))
        .andExpect(status().isOk());

    mockMvc.perform(post("/api/rooms/{id}/confirm-availability", room.getId())
            .header("X-Internal-Token", INTERNAL_TOKEN)
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestJson(start, end, requestId, 300L)))
        .andExpect(status().isOk());

    assertThat(roomLockRepository.findAll()).hasSize(1);
  }

  @Test
  void parallelBookingCreatesSingleLock() throws Exception {
    Room room = roomRepository.findAll().get(0);
    LocalDate start = LocalDate.now().plusDays(5);
    LocalDate end = start.plusDays(2);

    ExecutorService executor = Executors.newFixedThreadPool(2);
    CountDownLatch startLatch = new CountDownLatch(1);

    Callable<Integer> task1 = () -> performConfirm(room.getId(), start, end, UUID.randomUUID().toString(), 400L, startLatch);
    Callable<Integer> task2 = () -> performConfirm(room.getId(), start, end, UUID.randomUUID().toString(), 401L, startLatch);

    Future<Integer> result1 = executor.submit(task1);
    Future<Integer> result2 = executor.submit(task2);

    startLatch.countDown();

    int status1 = result1.get();
    int status2 = result2.get();

    executor.shutdown();

    assertThat(List.of(status1, status2)).containsExactlyInAnyOrder(200, 409);
    assertThat(roomLockRepository.findAll()).hasSize(1);
    assertThat(roomLockRepository.findAll().get(0).getStatus()).isEqualTo(RoomLockStatus.LOCKED);
  }

  private int performConfirm(Long roomId, LocalDate start, LocalDate end, String requestId, Long bookingId,
                             CountDownLatch startLatch) throws Exception {
    startLatch.await();
    MvcResult result = mockMvc.perform(post("/api/rooms/{id}/confirm-availability", roomId)
            .header("X-Internal-Token", INTERNAL_TOKEN)
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestJson(start, end, requestId, bookingId)))
        .andReturn();
    return result.getResponse().getStatus();
  }

  private String requestJson(LocalDate start, LocalDate end, String requestId, Long bookingId) throws Exception {
    return objectMapper.writeValueAsString(new AvailabilityRequestPayload(start, end, requestId, bookingId));
  }

  private static class AvailabilityRequestPayload {
    public LocalDate startDate;
    public LocalDate endDate;
    public String requestId;
    public Long bookingId;

    AvailabilityRequestPayload(LocalDate startDate, LocalDate endDate, String requestId, Long bookingId) {
      this.startDate = startDate;
      this.endDate = endDate;
      this.requestId = requestId;
      this.bookingId = bookingId;
    }
  }
}
