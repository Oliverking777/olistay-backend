package olistay.backend.service.impl;

import lombok.RequiredArgsConstructor;
import olistay.backend.dto.AuthResponseDTO;
import olistay.backend.dto.LoginRequestDTO;
import olistay.backend.dto.RegisterRequestDTO;
import olistay.backend.dto.UserResponseDTO;
import olistay.backend.entity.RefreshToken;
import olistay.backend.entity.User;
import olistay.backend.enums.Role;
import olistay.backend.exception.InvalidCredentialsException;
import olistay.backend.exception.TokenRefreshException;
import olistay.backend.exception.UserAlreadyExistsException;
import olistay.backend.repository.UserRepository;
import olistay.backend.security.JwtUtil;
import olistay.backend.service.AuthResult;
import olistay.backend.service.AuthService;
import olistay.backend.service.RefreshTokenService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;

    @Override
    @Transactional
    public AuthResult register(RegisterRequestDTO request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new UserAlreadyExistsException("An account with this email already exists");
        }

        User user = User.builder()
                .firstName(request.firstName())
                .lastName(request.lastName())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .phoneNumber(request.phoneNumber())
                .role(Role.GUEST) // every new account starts as GUEST; never set from client input
                .build();

        User savedUser = userRepository.save(user);

        return buildAuthResult(savedUser);
    }

    @Override
    @Transactional
    public AuthResult login(LoginRequestDTO request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.email(), request.password())
            );
        } catch (BadCredentialsException ex) {
            // Deliberately generic message — see InvalidCredentialsException javadoc.
            throw new InvalidCredentialsException("Invalid email or password");
        }

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));

        return buildAuthResult(user);
    }

    @Override
    @Transactional
    public AuthResult refresh(String incomingRefreshToken) {
        if (incomingRefreshToken == null || incomingRefreshToken.isBlank()) {
            throw new TokenRefreshException("No refresh token provided");
        }

        RefreshToken validToken = refreshTokenService.validateForRotation(incomingRefreshToken);
        User user = validToken.getUser();

        // If validateForRotation returned an already-rotated token (the
        // grace-window duplicate-request case), don't rotate again — just
        // issue a fresh access token off it. Only rotate when this is the
        // first time the token is being redeemed.
        RefreshToken tokenToReturn = validToken;
        if (!validToken.isUsed()) {
            RefreshToken newRefreshToken = refreshTokenService.createRefreshToken(user);
            refreshTokenService.rotateToken(validToken, newRefreshToken);
            tokenToReturn = newRefreshToken;
        }

        String accessToken = jwtUtil.generateAccessToken(user);
        AuthResponseDTO body = AuthResponseDTO.of(accessToken, UserResponseDTO.fromEntity(user));

        return new AuthResult(body, tokenToReturn.getToken());
    }

    @Override
    @Transactional
    public void logout(String incomingRefreshToken) {
        if (incomingRefreshToken == null || incomingRefreshToken.isBlank()) {
            return; // nothing to revoke; treat logout as a no-op rather than an error
        }

        // Refresh tokens are opaque strings, not JWTs, so the user is found
        // via the token's own User association, not by parsing the token.
        // Use the plain lookup — logout must work even with an expired or
        // already-used token; we should not trigger theft detection here.
        RefreshToken refreshToken = refreshTokenService.findByTokenOrThrow(incomingRefreshToken);
        refreshTokenService.revokeAllUserTokens(refreshToken.getUser());
    }

    /**
     * Shared by register() and login(): issues an access token plus a brand
     * new refresh token, and packages both into an AuthResult so the
     * controller can return the JSON body and set the httpOnly cookie from a
     * single service call.
     */
    private AuthResult buildAuthResult(User user) {
        String accessToken = jwtUtil.generateAccessToken(user);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);
        AuthResponseDTO body = AuthResponseDTO.of(accessToken, UserResponseDTO.fromEntity(user));
        return new AuthResult(body, refreshToken.getToken());
    }
}
