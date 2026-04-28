package com.meghaconnect.automation.apitests;

import com.meghaconnect.automation.config.ApiClient;
import com.meghaconnect.automation.config.ConfigManager;
import io.restassured.response.Response;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.util.HashMap;
import java.util.Map;

/**
 * Login API Test - Handles authentication API operations
 * Tests login endpoint and token management
 */
public class LoginApiTest {
    private static final Logger logger = LogManager.getLogger(LoginApiTest.class);
    
    private static final String LOGIN_ENDPOINT = "/api/v1/auth/login";
    private static final String PROFILE_ENDPOINT = "/api/v1/auth/profile";
    private static final String LOGOUT_ENDPOINT = "/api/v1/auth/logout";
    
    private String authToken;
    private String userId;
    private String userRole;

    // ==================== LOGIN API TESTS ====================

    /**
     * Test successful login API call
     * @param username Username
     * @param password Password
     * @return Response object
     */
    public Response testLoginWithValidCredentials(String username, String password) {
        logger.info("═══════════════════════════════════════════════════");
        logger.info("🔐 Testing Login API with valid credentials");
        logger.info("═══════════════════════════════════════════════════");
        logger.info("  Username: " + username);

        try {
            Map<String, String> loginPayload = buildLoginPayload(username, password);
            
            Response response = ApiClient.post(LOGIN_ENDPOINT, loginPayload);
            
            logger.info("📋 Response Status: " + response.getStatusCode());
            logger.info("📋 Response Time: " + response.getTime() + "ms");
            
            if (response.getStatusCode() == 200) {
                logger.info("✓ Login API call successful (HTTP 200)");
                
                // Extract and store token
                try {
                    this.authToken = response.jsonPath().getString("data.token");
                    this.userId = response.jsonPath().getString("data.userId");
                    this.userRole = response.jsonPath().getString("data.role");
                    
                    if (authToken != null && !authToken.isEmpty()) {
                        ApiClient.setAuthToken(authToken);
                        logger.info("✓ Bearer token extracted and set");
                        logger.info("  User ID: " + userId);
                        logger.info("  Role: " + userRole);
                    }
                } catch (Exception e) {
                    logger.error("⚠ Failed to extract token from response", e);
                }
            } else {
                logger.error("✗ Login API call failed with status: " + response.getStatusCode());
                logger.error("  Response: " + response.asString());
            }
            
            return response;
            
        } catch (Exception e) {
            logger.error("✗ Login API test error", e);
            throw new RuntimeException("Login API test failed: " + e.getMessage(), e);
        }
    }

    /**
     * Test login API with invalid credentials
     * @param username Username
     * @param password Password
     * @return Response object
     */
    public Response testLoginWithInvalidCredentials(String username, String password) {
        logger.info("═══════════════════════════════════════════════════");
        logger.info("🔐 Testing Login API with invalid credentials");
        logger.info("═══════════════════════════════════════════════════");

        try {
            Map<String, String> loginPayload = buildLoginPayload(username, password);
            Response response = ApiClient.post(LOGIN_ENDPOINT, loginPayload);
            
            logger.info("📋 Response Status: " + response.getStatusCode());
            
            if (response.getStatusCode() == 401 || response.getStatusCode() == 400) {
                logger.info("✓ Correctly rejected invalid credentials (HTTP " + response.getStatusCode() + ")");
                String errorMessage = response.jsonPath().getString("message");
                logger.info("  Error Message: " + errorMessage);
            } else {
                logger.warn("⚠ Unexpected response status: " + response.getStatusCode());
            }
            
            return response;
            
        } catch (Exception e) {
            logger.error("✗ Login API test error", e);
            throw new RuntimeException("Invalid credentials test failed: " + e.getMessage(), e);
        }
    }

    /**
     * Test login with missing username
     * @param password Password
     * @return Response object
     */
    public Response testLoginWithMissingUsername(String password) {
        logger.info("📋 Testing login with missing username");
        
        try {
            Map<String, String> payload = new HashMap<>();
            payload.put("password", password);
            
            Response response = ApiClient.post(LOGIN_ENDPOINT, payload);
            
            logger.info("📋 Response Status: " + response.getStatusCode());
            if (response.getStatusCode() == 400) {
                logger.info("✓ Correctly rejected missing username");
            }
            
            return response;
            
        } catch (Exception e) {
            logger.error("✗ Test error", e);
            throw new RuntimeException("Test failed: " + e.getMessage(), e);
        }
    }

    /**
     * Test login with missing password
     * @param username Username
     * @return Response object
     */
    public Response testLoginWithMissingPassword(String username) {
        logger.info("📋 Testing login with missing password");
        
        try {
            Map<String, String> payload = new HashMap<>();
            payload.put("username", username);
            
            Response response = ApiClient.post(LOGIN_ENDPOINT, payload);
            
            logger.info("📋 Response Status: " + response.getStatusCode());
            if (response.getStatusCode() == 400) {
                logger.info("✓ Correctly rejected missing password");
            }
            
            return response;
            
        } catch (Exception e) {
            logger.error("✗ Test error", e);
            throw new RuntimeException("Test failed: " + e.getMessage(), e);
        }
    }

    /**
     * Test login with empty credentials
     * @return Response object
     */
    public Response testLoginWithEmptyCredentials() {
        logger.info("📋 Testing login with empty credentials");
        
        try {
            Map<String, String> payload = new HashMap<>();
            payload.put("username", "");
            payload.put("password", "");
            
            Response response = ApiClient.post(LOGIN_ENDPOINT, payload);
            
            logger.info("📋 Response Status: " + response.getStatusCode());
            if (response.getStatusCode() == 400) {
                logger.info("✓ Correctly rejected empty credentials");
            }
            
            return response;
            
        } catch (Exception e) {
            logger.error("✗ Test error", e);
            throw new RuntimeException("Test failed: " + e.getMessage(), e);
        }
    }

    // ==================== PROFILE API ====================

    /**
     * Get user profile (requires authentication)
     * @return Response object
     */
    public Response getUserProfile() {
        logger.info("👤 Fetching user profile");
        
        try {
            if (authToken == null || authToken.isEmpty()) {
                logger.warn("⚠ No auth token available, profile fetch may fail");
            }
            
            Response response = ApiClient.get(PROFILE_ENDPOINT);
            
            logger.info("📋 Response Status: " + response.getStatusCode());
            if (response.getStatusCode() == 200) {
                logger.info("✓ Profile retrieved successfully");
            }
            
            return response;
            
        } catch (Exception e) {
            logger.error("✗ Profile fetch failed", e);
            throw new RuntimeException("Failed to fetch profile: " + e.getMessage(), e);
        }
    }

    // ==================== LOGOUT API ====================

    /**
     * Test logout API call
     * @return Response object
     */
    public Response testLogout() {
        logger.info("🔓 Testing logout");
        
        try {
            Response response = ApiClient.post(LOGOUT_ENDPOINT, new HashMap<>());
            
            logger.info("📋 Response Status: " + response.getStatusCode());
            if (response.getStatusCode() == 200) {
                logger.info("✓ Logout successful");
                authToken = null;
                ApiClient.clearAuthToken();
            }
            
            return response;
            
        } catch (Exception e) {
            logger.error("✗ Logout failed", e);
            throw new RuntimeException("Logout failed: " + e.getMessage(), e);
        }
    }

    // ==================== TOKEN MANAGEMENT ====================

    /**
     * Validate response contains token
     * @param response Response object
     * @return true if token present, false otherwise
     */
    public boolean validateTokenPresent(Response response) {
        try {
            logger.info("🔎 Validating token presence in response");
            
            if (response.getStatusCode() != 200) {
                logger.warn("⚠ Response status not 200");
                return false;
            }
            
            String token = response.jsonPath().getString("data.token");
            if (token != null && !token.isEmpty()) {
                logger.info("✓ Token present: " + maskToken(token));
                return true;
            } else {
                logger.error("✗ Token not found in response");
                return false;
            }
            
        } catch (Exception e) {
            logger.error("✗ Validation error", e);
            return false;
        }
    }

    /**
     * Validate token format (JWT)
     * @param token Token to validate
     * @return true if valid JWT format, false otherwise
     */
    public boolean validateTokenFormat(String token) {
        try {
            logger.info("🔎 Validating JWT token format");
            
            // JWT format: header.payload.signature
            if (token == null || token.isEmpty()) {
                logger.error("✗ Token is empty");
                return false;
            }
            
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                logger.error("✗ Invalid JWT structure (expected 3 parts, got " + parts.length + ")");
                return false;
            }
            
            logger.info("✓ Valid JWT format");
            return true;
            
        } catch (Exception e) {
            logger.error("✗ Token format validation error", e);
            return false;
        }
    }

    /**
     * Validate response has required fields
     * @param response Response object
     * @return true if all required fields present, false otherwise
     */
    public boolean validateResponseStructure(Response response) {
        try {
            logger.info("🔎 Validating response structure");
            
            if (response.getStatusCode() != 200) {
                logger.warn("⚠ Response status not 200");
                return false;
            }
            
            String token = response.jsonPath().getString("data.token");
            String userId = response.jsonPath().getString("data.userId");
            String role = response.jsonPath().getString("data.role");
            
            boolean hasToken = token != null && !token.isEmpty();
            boolean hasUserId = userId != null && !userId.isEmpty();
            boolean hasRole = role != null && !role.isEmpty();
            
            logger.info("  Token present: " + hasToken);
            logger.info("  User ID present: " + hasUserId);
            logger.info("  Role present: " + hasRole);
            
            if (hasToken && hasUserId && hasRole) {
                logger.info("✓ Response structure valid");
                return true;
            } else {
                logger.error("✗ Missing required fields in response");
                return false;
            }
            
        } catch (Exception e) {
            logger.error("✗ Response structure validation error", e);
            return false;
        }
    }

    // ==================== HELPER METHODS ====================

    /**
     * Build login payload
     * @param username Username
     * @param password Password
     * @return Map with login payload
     */
    private Map<String, String> buildLoginPayload(String username, String password) {
        Map<String, String> payload = new HashMap<>();
        payload.put("username", username);
        payload.put("password", password);
        return payload;
    }

    /**
     * Mask token for logging (show only first and last 4 chars)
     * @param token Token to mask
     * @return Masked token
     */
    private String maskToken(String token) {
        if (token == null || token.length() < 8) {
            return "***";
        }
        return token.substring(0, 4) + "..." + token.substring(token.length() - 4);
    }

    /**
     * Get current auth token
     * @return Auth token
     */
    public String getAuthToken() {
        return authToken;
    }

    /**
     * Get user ID from last login
     * @return User ID
     */
    public String getUserId() {
        return userId;
    }

    /**
     * Get user role from last login
     * @return User role
     */
    public String getUserRole() {
        return userRole;
    }

    /**
     * Clear stored credentials
     */
    public void clearCredentials() {
        authToken = null;
        userId = null;
        userRole = null;
        ApiClient.clearAuthToken();
        logger.info("✓ Stored credentials cleared");
    }
}
