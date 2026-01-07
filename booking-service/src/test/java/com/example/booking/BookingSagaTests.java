package com.example.booking;

import com.example.booking.dto.BookingResponse;
import com.example.booking.repository.BookingRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class BookingSagaTests {
  private static final WireMockServer wireMockServer =
      new WireMockServer(WireMockConfiguration.options().dynamicPort());

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private BookingRepository bookingRepository;

  @DynamicPropertySource
  static void registerProperties(DynamicPropertyRegistry registry) {
    if (!wireMockServer.isRunning()) {
      wireMockServer.start();
    }
    registry.add("booking.hotel.base-url", () -> "http://localhost:" + wireMockServer.port());
    registry.add("booking.hotel.internal-token", () -> "test-internal-token");
  }

  @AfterAll
  static void stopServer() {
    wireMockServer.stop();
  }

  @BeforeEach
  void setup() {
    wireMockServer.resetAll();
    bookingRepository.deleteAll();
  }

  @Test
  void successfulBookingTransitionsToConfirmed() throws Exception {
    wireMockServer.stubFor(com.github.tomakehurst.wiremock.client.WireMock.post(urlEqualTo("/api/rooms/101/confirm-availability"))
        .willReturn(aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody("{\"roomId\":101,\"bookingId\":1,\"status\":\"LOCKED\"}")));

    String token = authenticateUser();
    String requestId = UUID.randomUUID().toString();

    MvcResult result = mockMvc.perform(post("/booking")
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(bookingJson(requestId, LocalDate.now().plusDays(1), LocalDate.now().plusDays(2), 101L, false)))
        .andExpect(status().isOk())
        .andReturn();

    BookingResponse response = objectMapper.readValue(result.getResponse().getContentAsString(), BookingResponse.class);
    assertThat(response.getStatus()).isEqualTo("CONFIRMED");

    wireMockServer.verify(1, postRequestedFor(urlEqualTo("/api/rooms/101/confirm-availability")));
  }

  @Test
  void timeoutAndRetryTriggersCancelAndRelease() throws Exception {
    wireMockServer.stubFor(com.github.tomakehurst.wiremock.client.WireMock.post(urlEqualTo("/api/rooms/101/confirm-availability"))
        .willReturn(aResponse()
            .withFixedDelay(1500)
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody("{\"roomId\":101,\"bookingId\":1,\"status\":\"LOCKED\"}")));

    wireMockServer.stubFor(com.github.tomakehurst.wiremock.client.WireMock.post(urlEqualTo("/api/rooms/101/release"))
        .willReturn(aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody("{\"roomId\":101,\"bookingId\":1,\"status\":\"RELEASED\"}")));

    String token = authenticateUser();
    String requestId = UUID.randomUUID().toString();

    mockMvc.perform(post("/booking")
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(bookingJson(requestId, LocalDate.now().plusDays(3), LocalDate.now().plusDays(4), 101L, false)))
        .andExpect(status().isBadGateway());

    BookingResponse response = bookingRepository.findByRequestId(requestId)
        .map(booking -> new BookingResponse(
            booking.getId(),
            booking.getUserId(),
            booking.getRoomId(),
            booking.getStartDate(),
            booking.getEndDate(),
            booking.getStatus().name(),
            booking.getCreatedAt()))
        .orElseThrow();
    assertThat(response.getStatus()).isEqualTo("CANCELLED");

    wireMockServer.verify(3, postRequestedFor(urlEqualTo("/api/rooms/101/confirm-availability")));
    wireMockServer.verify(1, postRequestedFor(urlEqualTo("/api/rooms/101/release")));
  }

  @Test
  void autoSelectWithoutRoomsReturnsConflictAndDoesNotCreateBooking() throws Exception {
    wireMockServer.stubFor(com.github.tomakehurst.wiremock.client.WireMock.get(urlEqualTo(
            "/api/rooms/recommend?startDate=2030-01-10&endDate=2030-01-12"))
        .willReturn(aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody("[]")));

    String token = authenticateUser();
    String requestId = UUID.randomUUID().toString();

    mockMvc.perform(post("/booking")
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(bookingJson(requestId, LocalDate.of(2030, 1, 10), LocalDate.of(2030, 1, 12), null, true)))
        .andExpect(status().isConflict());

    assertThat(bookingRepository.findByRequestId(requestId)).isEmpty();
    wireMockServer.verify(1, getRequestedFor(urlEqualTo("/api/rooms/recommend?startDate=2030-01-10&endDate=2030-01-12")));
  }

  private String authenticateUser() throws Exception {
    MvcResult result = mockMvc.perform(post("/user/auth")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"username\":\"user\",\"password\":\"userpass\"}"))
        .andExpect(status().isOk())
        .andReturn();

    JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());
    return node.get("token").asText();
  }

  private String bookingJson(String requestId, LocalDate start, LocalDate end, Long roomId, boolean autoSelect) throws Exception {
    return objectMapper.writeValueAsString(new BookingPayload(requestId, start, end, roomId, autoSelect));
  }

  private static class BookingPayload {
    public String requestId;
    public LocalDate startDate;
    public LocalDate endDate;
    public Long roomId;
    public Boolean autoSelect;

    BookingPayload(String requestId, LocalDate startDate, LocalDate endDate, Long roomId, Boolean autoSelect) {
      this.requestId = requestId;
      this.startDate = startDate;
      this.endDate = endDate;
      this.roomId = roomId;
      this.autoSelect = autoSelect;
    }
  }
}
