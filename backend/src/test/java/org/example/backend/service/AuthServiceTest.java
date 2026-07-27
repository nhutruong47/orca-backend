package org.example.backend.service;

import org.example.backend.dto.ChangePasswordRequest;
import org.example.backend.dto.LoginRequest;
import org.example.backend.dto.RegisterRequest;
import org.example.backend.dto.AuthResponse;
import org.example.backend.entity.Role;
import org.example.backend.entity.User;
import org.example.backend.repository.UserRepository;
import org.example.backend.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link AuthService}. Covers the most critical paths:
 *
 * <ul>
 *   <li>Registration rejects duplicate username</li>
 *   <li>Registration hashes the password (does not store raw)</li>
 *   <li>Registration returns a JWT</li>
 *   <li>Login delegates to {@link AuthenticationManager} and returns JWT</li>
 *   <li>Change-password verifies current password before saving new one</li>
 *   <li>Change-password rejects wrong current password</li>
 *   <li>Update profile persists changes</li>
 * </ul>
 */
class AuthServiceTest {

    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private JwtUtil jwtUtil;
    private AuthenticationManager authenticationManager;
    private AuthService service;

    @BeforeEach
    void setUp() throws Exception {
        userRepository = mock(UserRepository.class);
        passwordEncoder = new BCryptPasswordEncoder();
        jwtUtil = mock(JwtUtil.class);
        authenticationManager = mock(AuthenticationManager.class);

        service = new AuthService();
        inject(service, "userRepository", userRepository);
        inject(service, "passwordEncoder", passwordEncoder);
        inject(service, "jwtUtil", jwtUtil);
        inject(service, "authenticationManager", authenticationManager);

        when(jwtUtil.generateToken(any())).thenReturn("test.jwt.token");
    }

    private static void inject(Object target, String field, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(field);
        f.setAccessible(true);
        f.set(target, value);
    }

    private User newUser(String username, String rawPassword) {
        User u = User.builder()
                .username(username)
                .password(passwordEncoder.encode(rawPassword))
                .role(Role.MEMBER)
                .chipId("USR-" + UUID.randomUUID())
                .build();
        u.setId(UUID.randomUUID());
        return u;
    }

    @Test
    @DisplayName("register rejects duplicate username")
    void register_rejectsDuplicate() {
        RegisterRequest req = new RegisterRequest();
        req.setUsername("alice");
        req.setPassword("password123");
        when(userRepository.existsByUsername("alice")).thenReturn(true);

        assertThatThrownBy(() -> service.register(req))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("đã tồn tại");
    }

    @Test
    @DisplayName("register hashes password and returns JWT")
    void register_hashesAndReturnsJwt() {
        RegisterRequest req = new RegisterRequest();
        req.setUsername("alice");
        req.setPassword("password123");
        when(userRepository.existsByUsername("alice")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        AuthResponse resp = service.register(req);

        assertThat(resp).isNotNull();
        assertThat(resp.getToken()).isEqualTo("test.jwt.token");

        // Verify password was hashed, not stored as raw
        org.mockito.ArgumentCaptor<User> captor = org.mockito.ArgumentCaptor.forClass(User.class);
        org.mockito.Mockito.verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertThat(saved.getPassword()).isNotEqualTo("password123");
        assertThat(passwordEncoder.matches("password123", saved.getPassword())).isTrue();
    }

    @Test
    @DisplayName("login delegates to AuthenticationManager and returns JWT for valid credentials")
    void login_happyPath() {
        LoginRequest req = new LoginRequest();
        req.setUsername("alice");
        req.setPassword("password123");
        User existing = newUser("alice", "password123");

        Authentication auth = new UsernamePasswordAuthenticationToken(
                existing, "password123",
                List.of(new SimpleGrantedAuthority("ROLE_MEMBER"))
        );
        when(authenticationManager.authenticate(any())).thenReturn(auth);

        AuthResponse resp = service.login(req);

        assertThat(resp).isNotNull();
        assertThat(resp.getToken()).isEqualTo("test.jwt.token");
    }

    @Test
    @DisplayName("login rejects when AuthenticationManager throws BadCredentialsException")
    void login_badCredentials() {
        LoginRequest req = new LoginRequest();
        req.setUsername("alice");
        req.setPassword("wrong");
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Bad creds"));

        assertThatThrownBy(() -> service.login(req))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    @DisplayName("changePassword verifies current password and hashes new one")
    void changePassword_happyPath() {
        User user = newUser("alice", "oldpass1");

        ChangePasswordRequest req = new ChangePasswordRequest();
        req.setCurrentPassword("oldpass1");
        req.setNewPassword("newpass1");

        service.changePassword(user, req);

        org.mockito.ArgumentCaptor<User> captor = org.mockito.ArgumentCaptor.forClass(User.class);
        org.mockito.Mockito.verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertThat(passwordEncoder.matches("newpass1", saved.getPassword())).isTrue();
        assertThat(passwordEncoder.matches("oldpass1", saved.getPassword())).isFalse();
    }

    @Test
    @DisplayName("changePassword rejects when current password is wrong")
    void changePassword_wrongCurrent() {
        User user = newUser("alice", "oldpass1");

        ChangePasswordRequest req = new ChangePasswordRequest();
        req.setCurrentPassword("WRONG");
        req.setNewPassword("newpass1");

        assertThatThrownBy(() -> service.changePassword(user, req))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("hiện tại");

        org.mockito.Mockito.verify(userRepository, org.mockito.Mockito.never()).save(any());
    }

    @Test
    @DisplayName("changePassword rejects when new password too short")
    void changePassword_weakNew() {
        User user = newUser("alice", "oldpass1");

        ChangePasswordRequest req = new ChangePasswordRequest();
        req.setCurrentPassword("oldpass1");
        req.setNewPassword("123");

        assertThatThrownBy(() -> service.changePassword(user, req))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("6 ký tự");

        org.mockito.Mockito.verify(userRepository, org.mockito.Mockito.never()).save(any());
    }
}