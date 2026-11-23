package com.pritesh.qa.users_service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class UserAuthIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @LocalServerPort
    int port;

    private final HttpClient http = HttpClient.newHttpClient();

    private HttpResponse<String> post(String path, String email, String password) throws Exception {
        String json = String.format("{\"email\":\"%s\",\"password\":\"%s\"}", email, password);
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        return http.send(request, HttpResponse.BodyHandlers.ofString());
    }

    // Each test uses its own email, so tests don't depend on order or shared state
    private String newEmail() {
        return "user-" + UUID.randomUUID() + "@example.com";
    }

    @Test
    void registerReturns201() throws Exception {
        String email = newEmail();
        HttpResponse<String> response = post("/api/users/register", email, "password123");

        assertEquals(201, response.statusCode());
        assertTrue(response.body().contains("\"email\":\"" + email + "\""));
    }

    @Test
    void duplicateRegistrationReturns409() throws Exception {
        String email = newEmail();
        post("/api/users/register", email, "password123");

        HttpResponse<String> second = post("/api/users/register", email, "password123");
        assertEquals(409, second.statusCode());
    }

    @Test
    void loginWithCorrectPasswordReturnsToken() throws Exception {
        String email = newEmail();
        post("/api/users/register", email, "password123");

        HttpResponse<String> login = post("/api/users/login", email, "password123");
        assertEquals(200, login.statusCode());
        assertTrue(login.body().contains("\"token\""));
    }

    @Test
    void loginWithWrongPasswordReturns401() throws Exception {
        String email = newEmail();
        post("/api/users/register", email, "password123");

        HttpResponse<String> login = post("/api/users/login", email, "wrongpassword");
        assertEquals(401, login.statusCode());
    }

    @Test
    void registerRejectsInvalidEmail() throws Exception {
        HttpResponse<String> response = post("/api/users/register", "not-an-email", "password123");
        assertEquals(400, response.statusCode());
    }
}