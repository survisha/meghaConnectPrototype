package gov.meghalaya.meghaconnect.controller;

import gov.meghalaya.meghaconnect.dto.AuthRequest;
import gov.meghalaya.meghaconnect.dto.AuthResponse;
import gov.meghalaya.meghaconnect.security.JwtService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final JwtService jwtService;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequest request) {
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );
        UserDetails user = userDetailsService.loadUserByUsername(request.getUsername());
        String token = jwtService.generateToken(user);
        return ResponseEntity.ok(AuthResponse.builder()
            .token(token)
            .username(user.getUsername())
            .role(user.getAuthorities().iterator().next().getAuthority())
            .expiresIn(86400L)
            .build());
    }
}
