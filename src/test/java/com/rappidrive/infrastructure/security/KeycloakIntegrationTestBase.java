package com.rappidrive.infrastructure.security;

import dasniko.testcontainers.keycloak.KeycloakContainer;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.HashMap;
import java.util.Map;

import static io.restassured.RestAssured.given;

/**
 * Classe base para testes de integração com Keycloak usando Testcontainers.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("e2e")
@Testcontainers(disabledWithoutDocker = true)
public abstract class KeycloakIntegrationTestBase {

    private static final String KEYCLOAK_VERSION = "23.0.3";
    private static final String REALM_NAME = "rappidrive-test";
    private static final String CLIENT_ID = "rappidrive-api";
    private static final String CLIENT_SECRET = "test-secret";

    @LocalServerPort
    protected int port;

    /**
     * Container Keycloak compartilhado entre todos os testes.
     * 
     * Container é reutilizado entre execuções se testcontainers.reuse.enable=true
     * estiver configurado em ~/.testcontainers.properties
     */
    @Container
    protected static final KeycloakContainer keycloak = new KeycloakContainer("quay.io/keycloak/keycloak:" + KEYCLOAK_VERSION)
        .withRealmImportFile("keycloak/rappidrive-test-realm.json");

    /**
     * Configura propriedades dinâmicas do Spring Boot para usar o Keycloak do container.
     * 
     * Sobrescreve as configurações de application-e2e.yml com URLs do container.
     */
    @DynamicPropertySource
    static void registerKeycloakProperties(DynamicPropertyRegistry registry) {
        String keycloakUrl = keycloak.getAuthServerUrl();
        String jwkSetUri = keycloakUrl + "/realms/" + REALM_NAME + "/protocol/openid-connect/certs";
        String issuerUri = keycloakUrl + "/realms/" + REALM_NAME;

        registry.add("spring.security.oauth2.resourceserver.jwt.jwk-set-uri", () -> jwkSetUri);
        registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri", () -> issuerUri);
        registry.add("keycloak.auth-server-url", () -> keycloakUrl);
        registry.add("keycloak.realm", () -> REALM_NAME);
        registry.add("keycloak.resource", () -> CLIENT_ID);
        registry.add("keycloak.credentials.secret", () -> CLIENT_SECRET);
    }

    @BeforeAll
    static void setUpKeycloak() {
        // Container já foi iniciado pelo @Container
        // Logs para debug
        System.out.println("Keycloak Auth Server URL: " + keycloak.getAuthServerUrl());
        System.out.println("Realm: " + REALM_NAME);
        System.out.println("Client: " + CLIENT_ID);
    }

    @BeforeEach
    void setUpRestAssured() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = port;
        RestAssured.basePath = "/api/v1";
    }

    /**
     * Obtém um access token válido para o usuário driver-test.
     * 
     * Usa OAuth2 Resource Owner Password Credentials Grant.
     */
    protected String getDriverToken() {
        return getAccessToken("driver-test", "driver123");
    }

    /**
     * Obtém um access token válido para o usuário passenger-test.
     */
    protected String getPassengerToken() {
        return getAccessToken("passenger-test", "passenger123");
    }

    /**
     * Obtém um access token válido para o usuário admin-test.
     */
    protected String getAdminToken() {
        return getAccessToken("admin-test", "admin123");
    }

    /**
     * Obtém um access token do Keycloak para um usuário específico.
     * 
     * Faz uma requisição POST ao endpoint de token do Keycloak usando
     * OAuth2 Resource Owner Password Credentials Grant.
     * 
     * @param username username do usuário no Keycloak
     * @param password senha do usuário
     * @return JWT access token válido
     */
    protected String getAccessToken(String username, String password) {
        String tokenEndpoint = keycloak.getAuthServerUrl() + "/realms/" + REALM_NAME + "/protocol/openid-connect/token";

        Map<String, String> formParams = new HashMap<>();
        formParams.put("client_id", CLIENT_ID);
        formParams.put("client_secret", CLIENT_SECRET);
        formParams.put("grant_type", "password");
        formParams.put("username", username);
        formParams.put("password", password);

        io.restassured.response.Response response = given()
            .contentType(ContentType.URLENC)
            .formParams(formParams)
            .when()
            .post(tokenEndpoint);
        
        System.out.println("Token request to: " + tokenEndpoint);
        System.out.println("Response status: " + response.statusCode());
        System.out.println("Response body: " + response.body().asString());
        
        return response
            .then()
            .statusCode(200)
            .extract()
            .path("access_token");
    }

    /**
     * Obtém um refresh token do Keycloak.
     * 
     * Útil para testes de renovação de token.
     */
    protected String getRefreshToken(String username, String password) {
        String tokenEndpoint = keycloak.getAuthServerUrl() + "/realms/" + REALM_NAME + "/protocol/openid-connect/token";

        Map<String, String> formParams = new HashMap<>();
        formParams.put("client_id", CLIENT_ID);
        formParams.put("client_secret", CLIENT_SECRET);
        formParams.put("grant_type", "password");
        formParams.put("username", username);
        formParams.put("password", password);

        return given()
            .contentType(ContentType.URLENC)
            .formParams(formParams)
            .when()
            .post(tokenEndpoint)
            .then()
            .statusCode(200)
            .extract()
            .path("refresh_token");
    }

    /**
     * Renova um access token usando refresh token.
     */
    protected String refreshAccessToken(String refreshToken) {
        String tokenEndpoint = keycloak.getAuthServerUrl() + "/realms/" + REALM_NAME + "/protocol/openid-connect/token";

        Map<String, String> formParams = new HashMap<>();
        formParams.put("client_id", CLIENT_ID);
        formParams.put("client_secret", CLIENT_SECRET);
        formParams.put("grant_type", "refresh_token");
        formParams.put("refresh_token", refreshToken);

        return given()
            .contentType(ContentType.URLENC)
            .formParams(formParams)
            .when()
            .post(tokenEndpoint)
            .then()
            .statusCode(200)
            .extract()
            .path("access_token");
    }

    /**
     * Decodifica um JWT para inspeção nos testes.
     * 
     * Útil para validar claims do token.
     */
    protected Map<String, Object> decodeJwt(String token) {
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            throw new IllegalArgumentException("Invalid JWT format");
        }

        String payload = new String(java.util.Base64.getUrlDecoder().decode(parts[1]));
        
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper()
                .readValue(payload, new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            throw new RuntimeException("Failed to decode JWT", e);
        }
    }
}
