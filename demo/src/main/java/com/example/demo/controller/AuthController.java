package com.example.demo.controller;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.example.demo.model.Organizzazione;
import com.example.demo.model.Utente;
import com.example.demo.payloadDTO.request.LoginRequest;
import com.example.demo.payloadDTO.request.RegisterRequest;
import com.example.demo.payloadDTO.response.MessageResponse;
import com.example.demo.payloadDTO.response.UserResponse;
import com.example.demo.repository.OrganizzazioneRepository;
import com.example.demo.repository.UtenteRepository;
import com.example.demo.security.model.UserDetailsImpl;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import static javax.swing.UIManager.put;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    @Autowired
    AuthenticationManager authenticationManager;

    @Autowired
    UtenteRepository utenteRepository;

    @Autowired
    OrganizzazioneRepository organizzazioneRepository;

    @Autowired
    PasswordEncoder encoder;

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration}")
    private long jwtExpiration;

    // Effettua il login e restituisce un token JWT
    // POST --> http://localhost:8080/api/auth/login
    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getEmail(), loginRequest.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);

        // Genera il token JWT manualmente
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        List<String> roles = userDetails.getAuthorities().stream()
                .map(item -> item.getAuthority())
                .collect(Collectors.toList());

        String jwt = JWT.create()
                .withSubject(userDetails.getEmail())
                .withClaim("id", userDetails.getId())
                .withClaim("email", userDetails.getEmail())
                .withClaim("roles", roles)
                .withIssuedAt(new Date())
                .withExpiresAt(new Date(System.currentTimeMillis() + jwtExpiration))
                .sign(Algorithm.HMAC256(jwtSecret.getBytes()));

        // Costruisci una risposta manuale invece di usare JwtResponse
        return ResponseEntity.ok(new HashMap<String, Object>() {{
            put("token", jwt);
            put("id", userDetails.getId());
            put("email", userDetails.getEmail());
            put("roles", roles);
            put("organizzazioneId", userDetails.getOrganizzazioneId());
        }});
    }

    // Registra un nuovo utente
    // POST --> http://localhost:8080/api/auth/register
    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody RegisterRequest registerRequest) {
        if (utenteRepository.findByNomeUtente(registerRequest.getUsername()).isPresent()) {
            return ResponseEntity
                    .badRequest()
                    .body(MessageResponse.error("Errore: Username già in uso!"));
        }

        if (utenteRepository.findByEmail(registerRequest.getEmail()).isPresent()) {
            return ResponseEntity
                    .badRequest()
                    .body(MessageResponse.error("Errore: Email già in uso!"));
        }

        // Crea nuovo utente
        Utente user = new Utente();
        user.setNomeUtente(registerRequest.getUsername());
        user.setEmail(registerRequest.getEmail());
        user.setNome(registerRequest.getNome());
        user.setCognome(registerRequest.getCognome());
        user.setPassword(encoder.encode(registerRequest.getPassword()));

        // Imposta ruolo (default USER se non specificato)
        user.setRuolo("USER");

        // Associa all'organizzazione se specificata
        if (registerRequest.getOrganizzazioneId() != null) {
            Organizzazione organizzazione = organizzazioneRepository.findById(registerRequest.getOrganizzazioneId())
                    .orElse(null);
            user.setOrganizzazione(organizzazione);
        }

        utenteRepository.save(user);

        return ResponseEntity.ok(MessageResponse.success("Utente registrato con successo!"));
    }

    // Ottiene le informazioni dell'utente autenticato
    // GET --> http://localhost:8080/api/auth/me
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.badRequest().body(MessageResponse.error("Utente non autenticato"));
        }

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        Utente user = utenteRepository.findById(userDetails.getId()).orElse(null);

        if (user == null) {
            return ResponseEntity.badRequest().body(MessageResponse.error("Utente non trovato"));
        }

        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setUsername(user.getNomeUtente());
        response.setEmail(user.getEmail());
        response.setNome(user.getNome());
        response.setCognome(user.getCognome());
        response.setRuolo(user.getRuolo());
        response.setAttivo(user.getAttivo() != null ? user.getAttivo() : true);
        response.setDataCreazione(user.getDataCreazione());

        if (user.getOrganizzazione() != null) {
            response.setOrganizzazioneId(user.getOrganizzazione().getId());
            response.setNomeOrganizzazione(user.getOrganizzazione().getNome());
        }

        return ResponseEntity.ok(response);
    }
}