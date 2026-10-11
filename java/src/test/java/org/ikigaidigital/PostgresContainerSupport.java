package org.ikigaidigital;

import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

public abstract class PostgresContainerSupport {

  // Started once per JVM and shared by every integration test; Testcontainers removes it at exit.
  @ServiceConnection
  protected static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>(DockerImageName.parse("postgres:17-alpine"));

  static {
    POSTGRES.start();
  }
}
