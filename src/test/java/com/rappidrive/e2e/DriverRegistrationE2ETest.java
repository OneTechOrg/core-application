package com.rappidrive.e2e;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.rappidrive.presentation.dto.response.DriverResponse;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("e2e")
@DisplayName("Driver Registration E2E Tests")
class DriverRegistrationE2ETest {

    @LocalServerPort
    private int port;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private UUID tenantId;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        RestAssured.port = port;
        RestAssured.basePath = "";
        
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        
        // Clean up any existing test data first
        jdbcTemplate.update("DELETE FROM driver_approvals");
        jdbcTemplate.update("DELETE FROM drivers");
        jdbcTemplate.update("DELETE FROM tenants");
        
        // Create tenant for the test
        jdbcTemplate.update(
            "INSERT INTO tenants (id, name, slug, active, created_at, updated_at) VALUES (?, ?, ?, ?, NOW(), NOW())",
            tenantId, "Test Tenant", "test-tenant-" + tenantId, true
        );
    }

    @AfterEach
    void tearDown() {
        // Clean up test data after each test
        jdbcTemplate.update("DELETE FROM driver_approvals");
        jdbcTemplate.update("DELETE FROM drivers");
        jdbcTemplate.update("DELETE FROM tenants");
    }

    @Test
    @DisplayName("Should create driver successfully with valid data")
    void shouldCreateDriverSuccessfully() throws Exception {
        // Given: Valid driver creation request using a Map to avoid record serialization issues in tests
        Map<String, Object> request = Map.of(
            "tenantId", tenantId.toString(),
            "fullName", "João Silva dos Santos",
            "email", "joao.silva@example.com",
            "cpf", "52998224725",
            "phone", "+5511987654321",
            "driverLicense", Map.of(
                "number", "96580714537",
                "category", "B",
                "issueDate", "2020-01-01",
                "expiryDate", "2030-01-01",
                "isDefinitive", true
            ),
            "documentUrls", List.of("https://docs.com/cnh.jpg")
        );

        // When & Then: POST request should return 201
        var response = given()
            .contentType(ContentType.JSON)
            .body(request)
            .when()
            .post("/api/v1/drivers")
            .then()
            .statusCode(201)
            .body("id", notNullValue())
            .body("fullName", equalTo("João Silva dos Santos"))
            .extract().asString();

        DriverResponse driverResponse = objectMapper.readValue(response, DriverResponse.class);
        assertThat(driverResponse.id()).isNotNull();
    }

    @Test
    @DisplayName("Should return 400 when name is missing")
    void shouldRejectMissingName() {
        Map<String, Object> request = new java.util.HashMap<>(Map.of(
            "tenantId", tenantId.toString(),
            "email", "joao@example.com",
            "cpf", "52998224725",
            "phone", "+5511987654321",
            "driverLicense", Map.of(
                "number", "96580714537",
                "category", "B",
                "issueDate", "2020-01-01",
                "expiryDate", "2030-01-01",
                "isDefinitive", true
            ),
            "documentUrls", List.of("https://docs.com/cnh.jpg")
        ));
        // fullName is missing

        given()
            .contentType(ContentType.JSON)
            .body(request)
            .when()
            .post("/api/v1/drivers")
            .then()
            .statusCode(400);
    }

    @Test
    @DisplayName("Should return 409 when email already exists")
    void shouldRejectDuplicateEmail() {
        String sharedEmail = "shared@example.com";
        Map<String, Object> request = Map.of(
            "tenantId", tenantId.toString(),
            "fullName", "Driver One",
            "email", sharedEmail,
            "cpf", "52998224725",
            "phone", "+5511987654321",
            "driverLicense", Map.of(
                "number", "96580714537",
                "category", "B",
                "issueDate", "2020-01-01",
                "expiryDate", "2030-01-01",
                "isDefinitive", true
            ),
            "documentUrls", List.of("https://docs.com/cnh.jpg")
        );

        given()
            .contentType(ContentType.JSON)
            .body(request)
            .when()
            .post("/api/v1/drivers")
            .then()
            .statusCode(201);

        // Try duplicate
        given()
            .contentType(ContentType.JSON)
            .body(request)
            .when()
            .post("/api/v1/drivers")
            .then()
            .statusCode(409);
    }

    @Test
    @DisplayName("Should return 400 when CPF already exists")
    void shouldRejectDuplicateCPF() {
        String sharedCPF = "11144477735";
        Map<String, Object> firstRequest = Map.of(
            "tenantId", tenantId.toString(),
            "fullName", "Driver One",
            "email", "driver1@example.com",
            "cpf", sharedCPF,
            "phone", "+5511987654321",
            "driverLicense", Map.of(
                "number", "96580714537",
                "category", "B",
                "issueDate", "2020-01-01",
                "expiryDate", "2030-01-01",
                "isDefinitive", true
            ),
            "documentUrls", List.of("https://docs.com/cnh.jpg")
        );

        given()
            .contentType(ContentType.JSON)
            .body(firstRequest)
            .when()
            .post("/api/v1/drivers")
            .then()
            .statusCode(201);

        Map<String, Object> secondRequest = Map.of(
            "tenantId", tenantId.toString(),
            "fullName", "Driver Two",
            "email", "driver2@example.com",
            "cpf", sharedCPF,
            "phone", "+5511987654322",
            "driverLicense", Map.of(
                "number", "96580714537",
                "category", "B",
                "issueDate", "2020-01-01",
                "expiryDate", "2030-01-01",
                "isDefinitive", true
            ),
            "documentUrls", List.of("https://docs.com/cnh.jpg")
        );

        given()
            .contentType(ContentType.JSON)
            .body(secondRequest)
            .when()
            .post("/api/v1/drivers")
            .then()
            .statusCode(409)
            .body("message", containsString("already exists"));
    }
}
