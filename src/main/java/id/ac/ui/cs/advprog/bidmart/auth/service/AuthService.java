package id.ac.ui.cs.advprog.bidmart.auth.service;

import id.ac.ui.cs.advprog.bidmart.auth.dto.*;
import id.ac.ui.cs.advprog.bidmart.auth.entity.Role;
import id.ac.ui.cs.advprog.bidmart.auth.entity.User;
import id.ac.ui.cs.advprog.bidmart.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final JwtService jwtService; 
    private final EmailService emailService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public void register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already registered");
        }

        Role assignedRole;
        try {
            assignedRole = Role.valueOf(request.getRole().toUpperCase());
            if (assignedRole == Role.ADMIN) {
                throw new RuntimeException("Cannot register as Admin");
            }
        } catch (Exception e) {
            assignedRole = Role.BUYER; 
        }

        String token = UUID.randomUUID().toString();

        User user = User.builder()
            .email(request.getEmail())
            .password(passwordEncoder.encode(request.getPassword()))
            .displayName(request.getDisplayName())
            .role(assignedRole)
            .verificationToken(token)
            .isEnabled(false) 
            .build();

        userRepository.save(user);

        emailService.sendVerificationEmail(user.getEmail(), token);
        
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid credentials");
        }

        if (!user.isEnabled()) {
            throw new RuntimeException("Please verify your email first");
        }

        String token = jwtService.generateToken(user);
        return new AuthResponse(token, user.getEmail(), user.getRole().name());
    }

    public void verifyUser(String token) {
        User user = userRepository.findByVerificationToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid verification token"));
        
        user.setEnabled(true);
        user.setVerificationToken(null); 
        userRepository.save(user);
    }
}