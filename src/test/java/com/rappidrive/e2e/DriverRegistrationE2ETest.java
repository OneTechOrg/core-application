package com.rappidrive.e2e;

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

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * End-to-End tests for Driver Registration API.
 * 
 * This test class validates the complete driver registration flow from HTTP request
 * to database persistence using RestAssured for API testing.
 * 
 * Prerequisites:
 *  - Docker containers must be running: docker-compose up -d
 *  - PostgreSQL 16 + PostGIS 3.4 available at localhost:5432
 *  - Application running on random port via @SpringBootTest
 * 
 * Scenarios tested:
 * 1. Happy path: successful driver creation
 * 2. Validation errors: invalid input data
 * 3. Business rules: duplicate CPF/email handling
 * 4. GET operations: retrieve by ID, 404 cases
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("e2e")
@DisplayName("Driver Registration E2E Tests")
class DriverRegistrationE2ETest {

    @LocalServerPort
    private int port;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private UUID tenantId;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        RestAssured.port = port;
        RestAssured.basePath = "";
        
        // Clean up any existing test data first
        jdbcTemplate.update("DELETE FROM driver_approvals");
        jdbcTemplate.update("DELETE FROM drivers");
        jdbcTemplate.update("DELETE FROM tenants");
        
        // Create tenant for the test
        jdbcTemplate.update(
            "INSERT INTO tenants (id, name, slug, active) VALUES (?, ?, ?, ?)",
            tenantId, "Test Tenant", "test-tenant-" + tenantId.toString().substring(0, 8), true
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
    @DisplayName("Happy Path: Create driver with valid data")
    void shouldCreateDriverSuccessfully() {
        // Given: Valid driver creation payload
        var createDriverPayload = new CreateDriverPayload(
            tenantId.toString(),
            "João Silva dos Santos",
            "joao.silva@example.com",
            "52998224725",  // Valid CPF
            "+5511987654321",
            new DriverLicensePayload(
                "96580714537",  // Valid license number
                "B",
                "2020-01-01",
                "2030-01-01",
                true
            ),
            java.util.List.of("https://docs.com/cnh.jpg")
        );

        // When & Then: POST request should return 201 with driver data
        var response = given()
            .contentType(ContentType.JSON)
            .body(createDriverPayload)
            .when()
            .post("/api/v1/drivers");
        
        if (response.statusCode() != 201) {
            System.out.println("UNEXPECTED STATUS: " + response.statusCode());
            System.out.println("RESPONSE BODY: " + response.body().asString());
        }
        
        response.then()
            .statusCode(201)
            .body("id", notNullValue())
            .body("fullName", equalTo("João Silva dos Santos"))
            .body("email", equalTo("joao.silva@example.com"))
            .body("cpf", equalTo("52998224725"))
            .body("phone", equalTo("+5511987654321"))
            .body("status", equalTo("PENDING_APPROVAL"));
    }

    @Test
    @DisplayName("Validation Error: Invalid CPF format")
    void shouldRejectInvalidCPF() {
        // Given: Payload with invalid CPF (less than 11 digits)
        var createDriverPayload = new CreateDriverPayload(
            tenantId.toString(),
            "João Silva",
            "joao@example.com",
            "123",  // Invalid: only 3 digits
            "+5511987654321",
            new DriverLicensePayload(
                "96580714537",  // Valid license number
                "B",
                "2020-01-01",
                "2030-01-01",
                true
            ),
            java.util.List.of("https://docs.com/cnh.jpg")
        );

        // When & Then: POST request should return 400
        given()
            .contentType(ContentType.JSON)
            .body(createDriverPayload)
            .when()
            .post("/api/v1/drivers")
            .then()
            .statusCode(400)
            .body("validationErrors", notNullValue());
    }

    @Test
    @DisplayName("Validation Error: Invalid email format")
    void shouldRejectInvalidEmail() {
        // Given: Payload with invalid email
        var createDriverPayload = new CreateDriverPayload(
            tenantId.toString(),
            "João Silva",
            "invalid-email",  // Invalid: not an email
            "52998224725",  // Valid CPF
            "+5511987654321",
            new DriverLicensePayload(
                "96580714537",  // Valid license number
                "B",
                "2020-01-01",
                "2030-01-01",
                true
            ),
            java.util.List.of("https://docs.com/cnh.jpg")
        );

        // When & Then: POST request should return 400
        given()
            .contentType(ContentType.JSON)
            .body(createDriverPayload)
            .when()
            .post("/api/v1/drivers")
            .then()
            .statusCode(400)
            .body("validationErrors", notNullValue());
    }

    @Test
    @DisplayName("Business Rule: Duplicate CPF rejection")
    void shouldRejectDuplicateCPF() {
        String sharedCPF = "11144477735";  // Valid CPF

        // Given: First driver successfully created
        var firstPayload = new CreateDriverPayload(
            tenantId.toString(),
            "Driver One",
            "driver.one@example.com",
            sharedCPF,
            "+5511987654321",
            new DriverLicensePayload(
                "96580714537",  // Valid CNH number
                "B",
                "2020-01-01",
                "2030-01-01",
                true
            ),
            java.util.List.of("https://docs.com/cnh.jpg")
        );

        var firstResponse = given()
            .contentType(ContentType.JSON)
            .body(firstPayload)
            .when()
            .post("/api/v1/drivers");
        
        if (firstResponse.statusCode() != 201) {
            System.out.println("FIRST DRIVER FAILED - STATUS: " + firstResponse.statusCode());
            System.out.println("FIRST DRIVER RESPONSE: " + firstResponse.body().asString());
        }
        
        firstResponse.then().statusCode(201);

        // When: Try to create second driver with same CPF
        var secondPayload = new CreateDriverPayload(
            tenantId.toString(),
            "Driver Two",
            "driver.two@example.com",
            sharedCPF,  // Same CPF
            "+5511987654322",
            new DriverLicensePayload(
                "96580714537",  // Valid CNH number
                "B",
                "2020-01-01",
                "2030-01-01",
                true
            ),
            java.util.List.of("https://docs.com/cnh.jpg")
        );

        // Then: Should return HTTP 400 (duplicate detected)
        given()
            .contentType(ContentType.JSON)
            .body(secondPayload)
            .when()
            .post("/api/v1/drivers")
            .then()
            .statusCode(400)
            .body("message", containsString("already exists"));
    }

    @Test
    @DisplayName("Business Rule: Duplicate Email rejection")
    void shouldRejectDuplicateEmail() {
        String sharedEmail = "shared@example.com";

        // Given: First driver successfully created
        var firstPayload = new CreateDriverPayload(
            tenantId.toString(),
            "Driver One",
            sharedEmail,
            "52998224725",  // Valid CPF (same as happy path test)
            "+5511987654321",
            new DriverLicensePayload(
                "96580714537",  // Valid CNH number
                "B",
                "2020-01-01",
                "2030-01-01",
                true
            ),
            java.util.List.of("https://docs.com/cnh.jpg")
        );

        var firstResponse = given()
            .contentType(ContentType.JSON)
            .body(firstPayload)
            .when()
            .post("/api/v1/drivers");
        
        if (firstResponse.statusCode() != 201) {
            System.out.println("DUPLICATE EMAIL - FIRST DRIVER FAILED: " + firstResponse.statusCode());
            System.out.println("RESPONSE: " + firstResponse.body().asString());
        }
        
        firstResponse.then().statusCode(201);

        // When: Try to create second driver with same email
        var secondPayload = new CreateDriverPayload(
            tenantId.toString(),
            "Driver Two",
            sharedEmail,  // Same email
            "11144477735",  // Different valid CPF
            "+5511987654322",
            new DriverLicensePayload(
                "96580714537",  // Valid CNH number
                "B",
                "2020-01-01",
                "2030-01-01",
                true
            ),
            java.util.List.of("https://docs.com/cnh.jpg")
        );

        // Then: Should return HTTP 409 Conflict
        var secondResponse = given()
            .contentType(ContentType.JSON)
            .body(secondPayload)
            .when()
            .post("/api/v1/drivers");
        
        if (secondResponse.statusCode() != 400) {
            System.out.println("DUPLICATE EMAIL - SECOND DRIVER STATUS: " + secondResponse.statusCode());
            System.out.println("RESPONSE: " + secondResponse.body().asString());
        }
        
        secondResponse.then()
            .statusCode(400)
            .body("message", containsString("already exists"));
    }

    @Test
    @DisplayName("GET endpoint: Retrieve created driver by ID")
    void shouldRetrieveDriverById() {
        // Given: Create a driver first
        var createPayload = new CreateDriverPayload(
            tenantId.toString(),
            "Retrieve Test Driver",
            "retrieve@example.com",
            "52998224725",  // Valid CPF (same as happy path)
            "+5511987654321",
            new DriverLicensePayload(
                "96580714537",  // Valid CNH number
                "B",
                "2020-01-01",
                "2030-01-01",
                true
            ),
            java.util.List.of("https://docs.com/cnh.jpg")
        );

        var createResponse = given()
            .contentType(ContentType.JSON)
            .body(createPayload)
            .when()
            .post("/api/v1/drivers");
        
        if (createResponse.statusCode() != 201) {
            System.out.println("RETRIEVE BY ID - CREATE FAILED: " + createResponse.statusCode());
            System.out.println("RESPONSE: " + createResponse.body().asString());
        }
        
        String driverId = createResponse.then()
            .statusCode(201)
            .extract()
            .path("id");

        // When: GET driver by ID
        // Then: Should return 200 with driver data
        given()
            .when()
            .get("/api/v1/drivers/{id}", driverId)
            .then()
            .statusCode(200)
            .body("id", equalTo(driverId))
            .body("fullName", equalTo("Retrieve Test Driver"))
            .body("email", equalTo("retrieve@example.com"))
            .body("status", equalTo("PENDING_APPROVAL"));
    }

    @Test
    @DisplayName("GET endpoint: Non-existent driver returns 404")
    void shouldReturn404ForNonExistentDriver() {
        // When: GET non-existent driver
        // Then: Should return 404
        given()
            .when()
            .get("/api/v1/drivers/{id}", UUID.randomUUID())
            .then()
            .statusCode(404);
    }

    @Test
    @DisplayName("Missing required field: Name is required")
    void shouldRejectMissingName() {
        // Given: Payload without fullName
        String payload = "{\n" +
            "  \"tenantId\": \"" + tenantId + "\",\n" +
            "  \"email\": \"test@example.com\",\n" +
            "  \"cpf\": \"76831029005\",\n" +
            "  \"phone\": \"+5511987654321\",\n" +
            "  \"driverLicense\": {\n" +
            "    \"number\": \"96580714537\",\n" +
            "    \"category\": \"B\",\n" +
            "    \"issueDate\": \"2020-01-01\",\n" +
            "    \"expiryDate\": \"2030-01-01\",\n" +
            "    \"isDefinitive\": true\n" +
            "  }\n" +
            "}";

        // When & Then: POST request should return 400
        given()
            .contentType(ContentType.JSON)
            .body(payload)
            .when()
            .post("/api/v1/drivers")
            .then()
            .statusCode(400)
            .body("validationErrors", notNullValue());
    }

    // Helper DTOs for JSON serialization
    public static class CreateDriverPayload {
        public String tenantId;
        public String fullName;
        public String email;
        public String cpf;
        public String phone;
        public DriverLicensePayload driverLicense;
        public java.util.List<String> documentUrls;

        public CreateDriverPayload(String tenantId, String fullName, String email,
                                  String cpf, String phone, DriverLicensePayload driverLicense,
                                  java.util.List<String> documentUrls) {
            this.tenantId = tenantId;
            this.fullName = fullName;
            this.email = email;
            this.cpf = cpf;
            this.phone = phone;
            this.driverLicense = driverLicense;
            this.documentUrls = documentUrls;
        }
    }

    public static class DriverLicensePayload {
        public String number;
        public String category;
        public String issueDate;
        public String expiryDate;
        public Boolean isDefinitive;

        public DriverLicensePayload(String number, String category, String issueDate,
                                   String expiryDate, Boolean isDefinitive) {
            this.number = number;
            this.category = category;
            this.issueDate = issueDate;
            this.expiryDate = expiryDate;
            this.isDefinitive = isDefinitive;
        }
    }
}
