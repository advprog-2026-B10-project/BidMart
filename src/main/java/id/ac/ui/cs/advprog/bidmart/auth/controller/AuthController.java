package id.ac.ui.cs.advprog.bidmart.auth.controller;

import id.ac.ui.cs.advprog.bidmart.auth.dto.AuthResponse;
import id.ac.ui.cs.advprog.bidmart.auth.dto.LoginRequest;
import id.ac.ui.cs.advprog.bidmart.auth.dto.RegisterRequest;
import id.ac.ui.cs.advprog.bidmart.auth.entity.User;
import id.ac.ui.cs.advprog.bidmart.auth.repository.UserRepository;
import id.ac.ui.cs.advprog.bidmart.auth.service.AuthService;
import lombok.RequiredArgsConstructor;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepository;

    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody RegisterRequest request) {
        authService.register(request);
        return ResponseEntity.ok("User registered successfully. Please verify your email.");
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/verify")
    public ResponseEntity<String> verify(@RequestParam("token") String token) {
        authService.verifyUser(token);
        return ResponseEntity.ok("Account verified successfully! You can now login.");
    }

    @GetMapping("/users")
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(userRepository.findAll());
    }
}