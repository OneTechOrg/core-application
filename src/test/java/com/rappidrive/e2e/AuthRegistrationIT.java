package com.rappidrive.e2e;

import com.rappidrive.infrastructure.test.IntegrationTestBase;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import com.rappidrive.application.ports.output.SmsOtpPort;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@DisplayName("Auth Registration Integration Tests")
class AuthRegistrationIT extends IntegrationTestBase {

    @LocalServerPort
    private int port;

    @MockitoBean
    private SmsOtpPort smsOtpPort;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        RestAssured.basePath = "";
    }

    @Test
    @DisplayName("OTP Send: Should return 200 when phone is valid")
    void shouldSendOtpSuccessfully() {
        String phone = "+5511987654321";
        
        given()
            .contentType(ContentType.JSON)
            .body("{ \"phone\": \"" + phone + "\" }")
            .when()
            .post("/api/v1/auth/otp/send")
            .then()
            .statusCode(200)
            .body("expiresIn", equalTo(600));
    }

    @Test
    @DisplayName("OTP Send: Should return 400 when phone is invalid")
    void shouldReturn400ForInvalidPhone() {
        String invalidPhone = "123";
        
        given()
            .contentType(ContentType.JSON)
            .body("{ \"phone\": \"" + invalidPhone + "\" }")
            .when()
            .post("/api/v1/auth/otp/send")
            .then()
            .statusCode(400);
    }
}
