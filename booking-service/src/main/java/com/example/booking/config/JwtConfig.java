package com.example.booking.config;

import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

@Configuration
public class JwtConfig {
  @Value("${security.jwt.secret}")
  private String secret;

  @Bean
  public SecretKey jwtSecretKey() {
    return new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
  }

  @Bean
  public JwtDecoder jwtDecoder(SecretKey jwtSecretKey) {
    return NimbusJwtDecoder.withSecretKey(jwtSecretKey)
        .macAlgorithm(MacAlgorithm.HS256)
        .build();
  }

  @Bean
  public JwtEncoder jwtEncoder(SecretKey jwtSecretKey) {
    return new NimbusJwtEncoder(new ImmutableSecret<SecurityContext>(jwtSecretKey.getEncoded()));
  }
}
