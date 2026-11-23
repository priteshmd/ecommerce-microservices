package com.pritesh.qa.users_service;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UserService {

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtService jwt;

    public UserService(UserRepository users, PasswordEncoder encoder, JwtService jwt) {
        this.users = users;
        this.encoder = encoder;
        this.jwt = jwt;
    }

    @Transactional
    public UserResponse register(RegisterRequest req) {
        String email = req.email().toLowerCase();
        if (users.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
        }
        User saved = users.save(new User(email, encoder.encode(req.password())));
        return new UserResponse(saved.getId(), saved.getEmail());
    }

    public AuthResponse login(LoginRequest req) {
        User user = users.findByEmail(req.email().toLowerCase())
                .orElseThrow(this::invalidCredentials);
        if (!encoder.matches(req.password(), user.getPasswordHash())) {
            throw invalidCredentials();
        }
        return new AuthResponse(jwt.generateToken(user.getEmail()));
    }

    private ResponseStatusException invalidCredentials() {
        // Same message for unknown email and wrong password, so attackers can't probe for accounts
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
    }
}