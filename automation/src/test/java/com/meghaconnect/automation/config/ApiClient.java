package com.meghaconnect.automation.config;

import io.restassured.RestAssured;
import io.restassured.http.Header;
import io.restassured.specification.RequestSpecification;
import io.restassured.response.Response;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;
import static io.restassured.RestAssured.*;

/**
 * API Client - Centralized REST API request builder and handler
 * Manages authentication tokens and API communication
 */
public class ApiClient {
    private static final Logger logger = LogManager.getLogger(ApiClient.class);
    private static String authToken = null;
    private static final Map<String, String> defaultHeaders = new HashMap<>();

    static {
        // Set base URI for all API requests
        RestAssured.baseURI = ConfigManager.getApiBaseUrl();
        logger.info("✓ API Base URI configured: " + RestAssured.baseURI);
    }

    /**
     * Get RequestSpecification with default headers and token
     * @return RequestSpecification
     */
    public static RequestSpecification getRequestSpec() {
        RequestSpecification spec = given()
                .contentType("application/json")
                .accept("application/json")
                .relaxedHTTPSValidation();

        // Add default headers
        for (Map.Entry<String, String> header : defaultHeaders.entrySet()) {
            spec.header(header.getKey(), header.getValue());
        }

        // Add auth token if available
        if (authToken != null && !authToken.isEmpty()) {
            spec.header("Authorization", "Bearer " + authToken);
        }

        return spec;
    }

    /**
     * Authenticate and get JWT token
     * @param username Username
     * @param password Password
     * @return Bearer token
     */
    public static String authenticate(String username, String password) {
        logger.info("🔐 Authenticating user: " + username);

        try {
            String loginEndpoint = ConfigManager.getApiAuthTokenEndpoint();

            Map<String, String> loginPayload = new HashMap<>();
            loginPayload.put("username", username);
            loginPayload.put("password", password);

            logger.debug("  Endpoint: POST " + loginEndpoint);

            Response response = getRequestSpec()
                    .body(loginPayload)
                    .post(loginEndpoint);

            logResponse(response);

            if (response.getStatusCode() == 200) {
                authToken = response.jsonPath().getString("data.token");

                if (authToken != null && !authToken.isEmpty()) {
                    logger.info("✓ Authentication successful");
                    logger.info("  Token: " + maskToken(authToken));
                    return authToken;
                } else {
                    logger.error("✗ Token not found in response");
                    throw new RuntimeException("Token not found in authentication response");
                }
            } else {
                logger.error("✗ Authentication failed with status: " + response.getStatusCode());
                logger.error("  Response: " + response.asString());
                throw new RuntimeException("Authentication failed: " + response.getStatusLine());
            }

        } catch (Exception e) {
            logger.error("✗ Authentication error", e);
            throw new RuntimeException("Failed to authenticate: " + e.getMessage(), e);
        }
    }

    /**
     * Set authentication token manually
     * @param token JWT token
     */
    public static void setAuthToken(String token) {
        authToken = token;
        logger.info("✓ Auth token set: " + maskToken(token));
    }

    /**
     * Get current auth token
     * @return Current token or null
     */
    public static String getAuthToken() {
        return authToken;
    }

    /**
     * Clear authentication token
     */
    public static void clearAuthToken() {
        authToken = null;
        logger.info("✓ Auth token cleared");
    }

    /**
     * Add custom header
     * @param key Header key
     * @param value Header value
     */
    public static void addHeader(String key, String value) {
        defaultHeaders.put(key, value);
        logger.debug("✓ Header added: " + key);
    }

    /**
     * Remove custom header
     * @param key Header key
     */
    public static void removeHeader(String key) {
        defaultHeaders.remove(key);
        logger.debug("✓ Header removed: " + key);
    }

    /**
     * Perform GET request
     * @param endpoint API endpoint
     * @return Response object
     */
    public static Response get(String endpoint) {
        logger.info("📤 GET: " + endpoint);
        Response response = getRequestSpec().get(endpoint);
        logResponse(response);
        return response;
    }

    /**
     * Perform POST request
     * @param endpoint API endpoint
     * @param body Request body
     * @return Response object
     */
    public static Response post(String endpoint, Object body) {
        logger.info("📤 POST: " + endpoint);
        Response response = getRequestSpec().body(body).post(endpoint);
        logResponse(response);
        return response;
    }

    /**
     * Perform PUT request
     * @param endpoint API endpoint
     * @param body Request body
     * @return Response object
     */
    public static Response put(String endpoint, Object body) {
        logger.info("📤 PUT: " + endpoint);
        Response response = getRequestSpec().body(body).put(endpoint);
        logResponse(response);
        return response;
    }

    /**
     * Perform DELETE request
     * @param endpoint API endpoint
     * @return Response object
     */
    public static Response delete(String endpoint) {
        logger.info("📤 DELETE: " + endpoint);
        Response response = getRequestSpec().delete(endpoint);
        logResponse(response);
        return response;
    }

    /**
     * Perform PATCH request
     * @param endpoint API endpoint
     * @param body Request body
     * @return Response object
     */
    public static Response patch(String endpoint, Object body) {
        logger.info("📤 PATCH: " + endpoint);
        Response response = getRequestSpec().body(body).patch(endpoint);
        logResponse(response);
        return response;
    }

    /**
     * Log response details
     * @param response Response object
     */
    private static void logResponse(Response response) {
        logger.debug("  Status: " + response.getStatusCode());
        logger.debug("  Time: " + response.getTime() + "ms");
        if (response.getStatusCode() >= 400) {
            logger.error("  Response: " + response.asString());
        }
    }

    /**
     * Mask token for logging (show only first and last 4 chars)
     * @param token Token to mask
     * @return Masked token
     */
    private static String maskToken(String token) {
        if (token == null || token.length() < 8) {
            return "***";
        }
        return token.substring(0, 4) + "..." + token.substring(token.length() - 4);
    }

    /**
     * Reset API client to default state
     */
    public static void reset() {
        authToken = null;
        defaultHeaders.clear();
        logger.info("✓ API client reset to default state");
    }
}
