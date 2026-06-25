package org.example.backend.service;

import org.example.backend.dto.AuthResponse;
import org.example.backend.dto.ChangePasswordRequest;
import org.example.backend.dto.LoginRequest;
import org.example.backend.dto.RegisterRequest;
import org.example.backend.dto.ResetPasswordRequest;
import org.example.backend.dto.UpdateProfileRequest;
import org.example.backend.entity.Role;
import org.example.backend.entity.User;
import org.example.backend.repository.UserRepository;
import org.example.backend.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private AuthenticationManager authenticationManager;

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username đã tồn tại: " + request.getUsername());
        }

        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.MEMBER)
                .chipId(generateUniqueChipId())
                .build();

        userRepository.save(user);
        String token = jwtUtil.generateToken(user);
        return buildResponse(token, user);
    }

    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));
        User user = (User) authentication.getPrincipal();
        String token = jwtUtil.generateToken(user);
        return buildResponse(token, user);
    }

    public AuthResponse updateProfile(User user, UpdateProfileRequest request) {
        if (request.getFullName() != null) {
            user.setFullName(request.getFullName().trim());
        }
        if (request.getAvatar() != null) {
            user.setAvatar(request.getAvatar());
        }
        if (request.getEmail() != null) {
            user.setEmail(request.getEmail());
        }
        userRepository.save(user);
        String token = jwtUtil.generateToken(user);
        return buildResponse(token, user);
    }

    public void changePassword(User user, ChangePasswordRequest request) {
        validateNewPassword(request.getNewPassword());
        if (request.getCurrentPassword() == null
                || !passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new RuntimeException("Mật khẩu hiện tại không đúng.");
        }
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    public void resetPassword(User user, ResetPasswordRequest request) {
        validateNewPassword(request.getNewPassword());
        if (request.getUsername() == null
                || !user.getUsername().equalsIgnoreCase(request.getUsername().trim())) {
            throw new RuntimeException("Tên đăng nhập xác nhận không đúng.");
        }
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    private void validateNewPassword(String password) {
        if (password == null || password.length() < 6) {
            throw new RuntimeException("Mật khẩu mới phải có ít nhất 6 ký tự.");
        }
    }

    private AuthResponse buildResponse(String token, User user) {
        AuthResponse r = new AuthResponse();
        r.setToken(token);
        r.setId(user.getId() != null ? user.getId().toString() : null);
        r.setUsername(user.getUsername());
        r.setFullName(user.getFullName());
        r.setEmail(user.getEmail());
        r.setRole(user.getRole().name());
        return r;
    }

    private String generateUniqueChipId() {
        String chipId;
        do {
            chipId = "USR-" + UUID.randomUUID();
        } while (userRepository.findByChipId(chipId).isPresent());
        return chipId;
    }
}
