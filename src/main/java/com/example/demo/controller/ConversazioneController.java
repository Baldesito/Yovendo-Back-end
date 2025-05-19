package com.example.demo.controller;


import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.model.Conversazione;
import com.example.demo.model.Messaggio;
import com.example.demo.payloadDTO.response.ConversazioneResponse;
import com.example.demo.payloadDTO.response.MessaggioResponse;
import com.example.demo.repository.ConversazioneRepository;
import com.example.demo.repository.MessaggioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/conversazioni")
public class ConversazioneController {

    private final ConversazioneRepository conversazioneRepository;
    private final MessaggioRepository messaggioRepository;

    @Autowired
    public ConversazioneController(
            ConversazioneRepository conversazioneRepository,
            MessaggioRepository messaggioRepository) {
        this.conversazioneRepository = conversazioneRepository;
        this.messaggioRepository = messaggioRepository;
    }


    // Ottiene l'elenco di tutte le conversazioni
    // GET --> http://localhost:8080/api/conversazioni
    @GetMapping
    public ResponseEntity<List<ConversazioneResponse>> getAllConversazioni() {
        List<ConversazioneResponse> conversazioni = conversazioneRepository.findAll().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(conversazioni);
    }

    // Ottiene tutte le conversazioni di una specifica organizzazione
    // GET --> http://localhost:8080/api/conversazioni/organizzazione/{organizzazioneId}
    @GetMapping("/organizzazione/{organizzazioneId}")
    public ResponseEntity<List<ConversazioneResponse>> getConversazioniByOrganizzazione(
            @PathVariable Long organizzazioneId) {
        List<ConversazioneResponse> conversazioni = conversazioneRepository
                .findByOrganizzazioneId(organizzazioneId).stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(conversazioni);
    }


    // Ottiene i dettagli di una specifica conversazione
    // GET --> http://localhost:8080/api/conversazioni/{id}
    @GetMapping("/{id}")
    public ResponseEntity<ConversazioneResponse> getConversazioneById(@PathVariable Long id) {
        return conversazioneRepository.findById(id)
                .map(this::convertToResponse)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResourceNotFoundException("Conversazione", "id", id));
    }


    // Ottiene tutti i messaggi di una specifica conversazione
    // GET --> http://localhost:8080/api/conversazioni/{id}/messaggi
    @GetMapping("/{id}/messaggi")
    public ResponseEntity<List<MessaggioResponse>> getMessaggiByConversazione(@PathVariable Long id) {
        // Verifica che la conversazione esista
        if (!conversazioneRepository.existsById(id)) {
            throw new ResourceNotFoundException("Conversazione", "id", id);
        }

        List<MessaggioResponse> messaggi = messaggioRepository.findByConversazioneIdOrderByOrarioInvioAsc(id)
                .stream()
                .map(this::convertToMessaggioResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(messaggi);
    }


    // Chiude una conversazione attiva
    // PUT --> http://localhost:8080/api/conversazioni/{id}/chiudi
    @PutMapping("/{id}/chiudi")
    public ResponseEntity<ConversazioneResponse> chiudiConversazione(@PathVariable Long id) {
        return conversazioneRepository.findById(id)
                .map(conversazione -> {
                    conversazione.setStato("chiusa");
                    conversazione.setOrarioFine(LocalDateTime.now());
                    return conversazioneRepository.save(conversazione);
                })
                .map(this::convertToResponse)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResourceNotFoundException("Conversazione", "id", id));
    }

    // Riapre una conversazione chiusa
    // PUT --> http://localhost:8080/api/conversazioni/{id}/riapri
    @PutMapping("/{id}/riapri")
    public ResponseEntity<ConversazioneResponse> riapriConversazione(@PathVariable Long id) {
        return conversazioneRepository.findById(id)
                .map(conversazione -> {
                    conversazione.setStato("attiva");
                    conversazione.setOrarioFine(null);
                    return conversazioneRepository.save(conversazione);
                })
                .map(this::convertToResponse)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResourceNotFoundException("Conversazione", "id", id));
    }

    private ConversazioneResponse convertToResponse(Conversazione conversazione) {
        ConversazioneResponse response = new ConversazioneResponse();
        response.setId(conversazione.getId());
        response.setOrganizzazioneId(conversazione.getOrganizzazione().getId());
        response.setTelefonoCliente(conversazione.getTelefonoCliente());
        response.setStato(conversazione.getStato());
        response.setOrarioInizio(conversazione.getOrarioInizio());
        response.setOrarioFine(conversazione.getOrarioFine());

        // Conta i messaggi
        List<Messaggio> messaggi = messaggioRepository.findByConversazioneIdOrderByOrarioInvioAsc(conversazione.getId());
        response.setNumeroMessaggi((long) messaggi.size());

        // Ottieni ultimo messaggio
        if (!messaggi.isEmpty()) {
            Messaggio ultimoMessaggio = messaggi.stream()
                    .max(Comparator.comparing(Messaggio::getOrarioInvio))
                    .orElse(null);

            if (ultimoMessaggio != null) {
                response.setUltimoMessaggioTesto(ultimoMessaggio.getContenuto());
                response.setUltimoMessaggioData(ultimoMessaggio.getOrarioInvio());
                response.setUltimoMessaggioDaCliente(ultimoMessaggio.getDaCliente());
            }
        }

        return response;
    }

    private MessaggioResponse convertToMessaggioResponse(Messaggio messaggio) {
        MessaggioResponse response = new MessaggioResponse();
        response.setId(messaggio.getId());
        response.setConversazioneId(messaggio.getConversazione().getId());
        response.setContenuto(messaggio.getContenuto());
        response.setDaCliente(messaggio.getDaCliente());
        response.setOrarioInvio(messaggio.getOrarioInvio());
        response.setElaborato(messaggio.getElaborato());

        return response;
    }
}