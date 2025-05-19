package com.example.demo.controller;

import com.example.demo.model.Organizzazione;
import com.example.demo.model.Utente;
import com.example.demo.payloadDTO.request.RegisterRequest;
import com.example.demo.payloadDTO.response.MessageResponse;
import com.example.demo.payloadDTO.response.UserResponse;
import com.example.demo.repository.OrganizzazioneRepository;
import com.example.demo.repository.UtenteRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/utenti")
public class UtenteController {
    @Autowired
    private UtenteRepository utenteRepository;

    @Autowired
    private OrganizzazioneRepository organizzazioneRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @GetMapping
    public ResponseEntity<List<UserResponse>> getAllUtenti() {
        List<UserResponse> utenti = utenteRepository.findAll().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(utenti);
    }

    // Ottiene tutti gli utenti (solo per ADMIN)
    // GET --> http://localhost:8080/api/utenti
    @GetMapping("/organizzazione/{organizzazioneId}")
    public ResponseEntity<List<UserResponse>> getUtentiByOrganizzazione(@PathVariable Long organizzazioneId) {
        List<UserResponse> utenti = utenteRepository.findByOrganizzazione_Id(organizzazioneId).stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(utenti);
    }

    // Ottiene un utente specifico
    // GET --> http://localhost:8080/api/utenti/{id}
    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUtenteById(@PathVariable Long id) {
        return utenteRepository.findById(id)
                .map(this::convertToResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createUtente(@Valid @RequestBody RegisterRequest registerRequest) {
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
        user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));

        // Imposta ruolo (default USER se non specificato)
        user.setRuolo("USER");

        // Associa all'organizzazione se specificata
        if (registerRequest.getOrganizzazioneId() != null) {
            Organizzazione organizzazione = organizzazioneRepository.findById(registerRequest.getOrganizzazioneId())
                    .orElse(null);
            user.setOrganizzazione(organizzazione);
        }

        Utente savedUser = utenteRepository.save(user);
        return ResponseEntity.ok(convertToResponse(savedUser));
    }

    // Cancella un Utente con L'ID(solo ADMIN)
    // DELETE --> http://localhost:8080/api/utenti/{id}
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteUtente(@PathVariable Long id) {
        if (!utenteRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }

        utenteRepository.deleteById(id);
        return ResponseEntity.ok(MessageResponse.success("Utente eliminato con successo"));
    }

    private UserResponse convertToResponse(Utente utente) {
        UserResponse response = new UserResponse();
        response.setId(utente.getId());
        response.setUsername(utente.getNomeUtente());
        response.setEmail(utente.getEmail());
        response.setNome(utente.getNome());
        response.setCognome(utente.getCognome());
        response.setRuolo(utente.getRuolo());
        response.setAttivo(utente.getAttivo() != null ? utente.getAttivo() : true);
        response.setDataCreazione(utente.getDataCreazione());

        if (utente.getOrganizzazione() != null) {
            response.setOrganizzazioneId(utente.getOrganizzazione().getId());
            response.setNomeOrganizzazione(utente.getOrganizzazione().getNome());
        }

        return response;
    }
}