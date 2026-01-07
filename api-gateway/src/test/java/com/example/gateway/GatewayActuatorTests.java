package com.example.gateway;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;
import reactor.netty.DisposableServer;
import reactor.netty.http.server.HttpServer;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("test")
class GatewayActuatorTests {
  private static final AtomicReference<String> lastAuthorization = new AtomicReference<>();
  private static final AtomicInteger requestCount = new AtomicInteger();
  private static final DisposableServer mockServer = HttpServer.create()
      .port(0)
      .handle((request, response) -> {
        lastAuthorization.set(request.requestHeaders().get("Authorization"));
        requestCount.incrementAndGet();
        return response.status(200).sendString(Mono.just("ok"));
      })
      .bindNow();

  @Autowired
  private WebTestClient webTestClient;

  @DynamicPropertySource
  static void registerProperties(DynamicPropertyRegistry registry) {
    registry.add("eureka.client.enabled", () -> "false");
  }

  @TestConfiguration(proxyBeanMethods = false)
  static class TestRoutesConfiguration {
    @Bean
    RouteLocator testRouteLocator(RouteLocatorBuilder builder) {
      return builder.routes()
          .route("auth-proxy-test", route -> route.path("/api/echo/**")
              .uri("http://localhost:" + mockServer.port()))
          .build();
    }
  }

  @BeforeEach
  void reset() {
    lastAuthorization.set(null);
    requestCount.set(0);
  }

  @AfterAll
  static void shutdown() {
    mockServer.disposeNow();
  }

  @Test
  void actuatorRoutesAreExposed() {
    webTestClient.get()
        .uri("/actuator/gateway/routes")
        .exchange()
        .expectStatus().isOk();
  }

  @Test
  void authorizationHeaderIsForwarded() {
    webTestClient.get()
        .uri("/api/echo/test")
        .header("Authorization", "Bearer test-token")
        .exchange()
        .expectStatus().isOk();

    assertThat(lastAuthorization.get()).isEqualTo("Bearer test-token");
    assertThat(requestCount.get()).isEqualTo(1);
  }
}
