package olistay.backend.util;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Optional;

/**
 * Utility for managing the httpOnly refresh token cookie.
 *
 * ResponseCookie (Spring) is used instead of javax.servlet.http.Cookie so
 * we can set SameSite=Strict declaratively without manual header appending.
 */
@Component
public class CookieUtil {

    @Value("${jwt.refresh-cookie-name}")
    private String cookieName;

    @Value("${jwt.refresh-expiration-ms}")
    private long refreshExpirationMs;

    /**
     * Adds the httpOnly refresh cookie to the response.
     * Called on register, login, and every successful /auth/refresh.
     */
    public void addRefreshTokenCookie(HttpServletResponse response, String token) {
        ResponseCookie cookie = ResponseCookie.from(cookieName, token)
                .httpOnly(true)
                .secure(true)          // set false in local dev if not using HTTPS
                .path("/auth")         // scoped to /auth/** so the cookie isn't sent on every API call
                .maxAge(refreshExpirationMs / 1000)
                .sameSite("Strict")
                .build();

        response.addHeader("Set-Cookie", cookie.toString());
    }

    /**
     * Clears the refresh cookie on logout by returning a cookie with
     * maxAge=0 and an empty value.
     */
    public void clearRefreshTokenCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(cookieName, "")
                .httpOnly(true)
                .secure(true)
                .path("/auth")
                .maxAge(0)
                .sameSite("Strict")
                .build();

        response.addHeader("Set-Cookie", cookie.toString());
    }

    /**
     * Reads the refresh token value from the incoming request's cookies.
     * Returns empty if the cookie is absent (caller should treat as
     * unauthenticated / no session to refresh).
     */
    public Optional<String> getRefreshTokenFromCookies(HttpServletRequest request) {
        if (request.getCookies() == null) return Optional.empty();

        return Arrays.stream(request.getCookies())
                .filter(cookie -> cookieName.equals(cookie.getName()))
                .map(Cookie::getValue)
                .findFirst();
    }
}