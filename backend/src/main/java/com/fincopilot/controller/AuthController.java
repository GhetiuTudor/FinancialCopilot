package com.fincopilot.controller;

import com.fincopilot.dto.auth.AuthResponse;
import com.fincopilot.dto.auth.LoginRequest;
import com.fincopilot.dto.auth.RegisterRequest;
import com.fincopilot.entity.auth.Organization;
import com.fincopilot.entity.auth.User;
import com.fincopilot.repository.auth.OrganizationRepository;
import com.fincopilot.repository.auth.UserRepository;
import com.fincopilot.security.CurrentUser;
import com.fincopilot.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    // register pt user/ org
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        if (request.getEmail() == null || request.getPassword() == null || request.getOrgName() == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Toate câmpurile sunt obligatorii."));
        }
        if (request.getPassword().length() < 6) {
            return ResponseEntity.badRequest().body(Map.of("error", "Parola trebuie să aibă minim 6 caractere."));
        }

        if (userRepository.findByEmailIgnoreCase(request.getEmail()).isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Adresa de email este deja înregistrată."));
        }

        Organization org = Organization.builder().name(request.getOrgName()).build();
        org = organizationRepository.save(org);

        User user = User.builder().organization(org).email(request.getEmail().toLowerCase().trim())
                .passwordHash(passwordEncoder.encode(request.getPassword())).role("OWNER").build();
        user = userRepository.save(user);

        String token = jwtService.generateToken(user);
        return ResponseEntity.ok(buildAuthResponse(token, user, org));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        if (request.getEmail() == null || request.getPassword() == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email și parola sunt obligatorii."));
        }

        User user = userRepository.findByEmailIgnoreCase(request.getEmail()).orElse(null);

        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            return ResponseEntity.status(401).body(Map.of("error", "Email sau parolă incorectă."));
        }

        String token = jwtService.generateToken(user);
        Organization org = user.getOrganization();
        return ResponseEntity.ok(buildAuthResponse(token, user, org));
    }

    // user curent
    @GetMapping("/me")
    public ResponseEntity<?> me() {
        var principal = CurrentUser.get();
        User user = userRepository.findById(principal.userId()).orElseThrow();
        Organization org = user.getOrganization();

        return ResponseEntity.ok(AuthResponse.builder()
                .user(AuthResponse.UserInfo.builder()
                        .id(user.getId())
                        .email(user.getEmail())
                        .role(user.getRole())
                        .build())
                .org(AuthResponse.OrgInfo.builder()
                        .id(org.getId())
                        .name(org.getName())
                        .build())
                .build());
    }

    private AuthResponse buildAuthResponse(String token, User user, Organization org) {
        return AuthResponse.builder().accessToken(token)
                .user(AuthResponse.UserInfo.builder()
                        .id(user.getId())
                        .email(user.getEmail())
                        .role(user.getRole())
                        .build())
                .org(AuthResponse.OrgInfo.builder()
                        .id(org.getId())
                        .name(org.getName())
                        .build())
                .build();
    }
}
