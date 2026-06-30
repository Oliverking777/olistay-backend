package olistay.backend.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import olistay.backend.dto.AuthResponseDTO;
import olistay.backend.dto.LoginRequestDTO;
import olistay.backend.dto.RegisterRequestDTO;
import olistay.backend.service.AuthResult;
import olistay.backend.service.AuthService;
import olistay.backend.util.CookieUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Authentication endpoints. All routes under /auth/** are publicly accessible
 * (configured in SecurityConfig), except /auth/logout which requires a valid
 * access token.
 *
 * Pattern: service methods return an AuthResult (body + rawRefreshToken).
 * This controller pulls them apart — body goes into the JSON response,
 * rawRefreshToken goes into the httpOnly cookie via CookieUtil. The service
 * layer never touches HttpServletResponse.
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final CookieUtil cookieUtil;

    /**
     * POST /auth/register
     * Registers a new GUEST account and returns an access token.
     * Sets the httpOnly refresh cookie on the response.
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponseDTO> register(
            @Valid @RequestBody RegisterRequestDTO request,
            HttpServletResponse response
    ) {
        AuthResult result = authService.register(request);
        cookieUtil.addRefreshTokenCookie(response, result.rawRefreshToken());
        return ResponseEntity.status(HttpStatus.CREATED).body(result.body());
    }

    /**
     * POST /auth/login
     * Authenticates credentials and returns an access token.
     * Sets the httpOnly refresh cookie on the response.
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(
            @Valid @RequestBody LoginRequestDTO request,
            HttpServletResponse response
    ) {
        AuthResult result = authService.login(request);
        cookieUtil.addRefreshTokenCookie(response, result.rawRefreshToken());
        return ResponseEntity.ok(result.body());
    }

    /**
     * POST /auth/refresh
     * Reads the refresh token from the httpOnly cookie, validates it, rotates
     * it, and issues a new access token + new refresh cookie.
     * No request body needed — everything comes from the cookie.
     */
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponseDTO> refresh(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        String incomingToken = cookieUtil.getRefreshTokenFromCookies(request)
                .orElseThrow(() -> new olistay.backend.exception.TokenRefreshException(
                        "No refresh token cookie found"
                ));

        AuthResult result = authService.refresh(incomingToken);
        cookieUtil.addRefreshTokenCookie(response, result.rawRefreshToken());
        return ResponseEntity.ok(result.body());
    }

    /**
     * POST /auth/logout
     * Revokes all active sessions for the authenticated user and clears the
     * refresh cookie. Requires a valid access token (authenticated endpoint).
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        cookieUtil.getRefreshTokenFromCookies(request)
                .ifPresent(authService::logout);

        cookieUtil.clearRefreshTokenCookie(response);
        return ResponseEntity.noContent().build();
    }
}