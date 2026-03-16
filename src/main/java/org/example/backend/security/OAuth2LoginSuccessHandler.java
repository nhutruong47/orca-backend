package org.example.backend.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.backend.entity.Role;
import org.example.backend.entity.User;
import org.example.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

        @Autowired
        private UserRepository userRepository;
        @Autowired
        private JwtUtil jwtUtil;

        @Value("${app.frontend.url:http://localhost:5173}")
        private String frontendUrl;

        @Override
        public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                        Authentication authentication) throws IOException {
                OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
                String email = oAuth2User.getAttribute("email");
                String name = oAuth2User.getAttribute("name");

                User user = userRepository.findByUsername(email)
                                .orElseGet(() -> {
                                        User newUser = new User();
                                        newUser.setUsername(email);
                                        newUser.setPassword("");
                                        newUser.setRole(Role.MEMBER);
                                        newUser.setFullName(name);
                                        newUser.setEmail(email);
                                        return userRepository.save(newUser);
                                });

                String token = jwtUtil.generateToken(user);
                String redirectUrl = frontendUrl + "/oauth2/callback?token=" + token
                                + "&username=" + user.getUsername()
                                + "&role=" + user.getRole().name();
                getRedirectStrategy().sendRedirect(request, response, redirectUrl);
        }
}
