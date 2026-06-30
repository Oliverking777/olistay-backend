package olistay.backend.service;

import lombok.RequiredArgsConstructor;
import olistay.backend.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Bridges Spring Security to our domain. Since {@link olistay.backend.entity.User}
 * already implements UserDetails directly, this just does the repository lookup —
 * no separate adapter/wrapper class is needed.
 *
 * "Username" here is the user's email, consistent with LoginRequestDTO and
 * with User.getUsername() returning email.
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("No user found with email: " + email));
    }
}
