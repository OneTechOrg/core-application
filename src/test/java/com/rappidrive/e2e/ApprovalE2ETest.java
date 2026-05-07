package com.rappidrive.e2e;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("e2e")
@DisplayName("Driver Approval E2E Tests")
class ApprovalE2ETest {

    @LocalServerPort
    private int port;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private UUID tenantId;
    private UUID adminId;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        adminId = UUID.randomUUID();
        RestAssured.port = port;
        RestAssured.basePath = "";

        jdbcTemplate.update("DELETE FROM driver_approvals");
        jdbcTemplate.update("DELETE FROM admin_users");
        jdbcTemplate.update("DELETE FROM drivers");
        jdbcTemplate.update("DELETE FROM tenants");

        jdbcTemplate.update(
            "INSERT INTO tenants (id, name, slug, active) VALUES (?, ?, ?, ?)",
            tenantId, "Test Tenant", "tenant-" + tenantId.toString().substring(0, 8), true
        );

        jdbcTemplate.update(
            "INSERT INTO admin_users (id, tenant_id, email, role, full_name, created_at) VALUES (?, ?, ?, ?, ?, now())",
            adminId, tenantId, "admin@e2e.com", "SUPER_ADMIN", "Alice Admin"
        );
    }

    @AfterEach
    void tearDown() {
        jdbcTemplate.update("DELETE FROM driver_approvals");
        jdbcTemplate.update("DELETE FROM admin_users");
        jdbcTemplate.update("DELETE FROM drivers");
        jdbcTemplate.update("DELETE FROM tenants");
    }

    @Test
    @DisplayName("Happy path: submit, list, approve and activate driver")
    void shouldSubmitListAndApprove() {
        String driverId = createDriver();

        // List pending approvals (one should have been created automatically)
        var listResponse = given()
            .when()
            .get("/api/v1/admin/approvals/pending?adminId={adminId}&page=0&size=10", adminId)
            .then()
            .statusCode(200)
            .body("approvals.size()", equalTo(1))
            .body("approvals[0].driverId", equalTo(driverId))
            .extract();

        String approvalId = listResponse.path("approvals[0].approvalId");

        // Approve
        ApprovePayload approvePayload = new ApprovePayload(adminId);
        given()
            .contentType(ContentType.JSON)
            .body(approvePayload)
            .when()
            .post("/api/v1/admin/approvals/{approvalId}/approve", approvalId)
            .then()
            .statusCode(200)
            .body("status", equalTo("APPROVED"))
            .body("driverId", equalTo(driverId));

        // Driver should now be ACTIVE
        given()
            .when()
            .get("/api/v1/drivers/{id}", driverId)
            .then()
            .statusCode(200)
            .body("status", equalTo("ACTIVE"));
    }

    private String createDriver() {
        CreateDriverPayload payload = new CreateDriverPayload(
            tenantId.toString(),
            "Ana Driver",
            "ana.driver@example.com",
            "52998224725",
            "+5511987654321",
            new DriverLicensePayload("96580714537", "B", "2020-01-01", "2030-01-01", true),
            List.of("https://docs.com/cnh.jpg")
        );

        var response = given()
            .contentType(ContentType.JSON)
            .body(payload)
            .when()
            .post("/api/v1/drivers");

        if (response.statusCode() != 201) {
            throw new AssertionError("Driver creation failed: status=" + response.statusCode() + ", body=" + response.asString());
        }

        return response.then().extract().path("id");
    }

    // Payload records reused locally for brevity
    private record SubmitApprovalPayload(String driverId, List<String> documentUrls) {}
    private record ApprovePayload(UUID adminId) {}
    private record CreateDriverPayload(
        String tenantId,
        String fullName,
        String email,
        String cpf,
        String phone,
        DriverLicensePayload driverLicense,
        List<String> documentUrls
    ) {}

    private record DriverLicensePayload(
        String number,
        String category,
        String issueDate,
        String expiryDate,
        boolean isDefinitive
    ) {}
}
