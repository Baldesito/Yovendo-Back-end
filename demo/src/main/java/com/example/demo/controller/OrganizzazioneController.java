package com.example.demo.controller;

import com.example.demo.model.Organizzazione;
import com.example.demo.payloadDTO.request.OrganizzazioneRequest;
import com.example.demo.payloadDTO.response.OrganizzazioneResponse;
import com.example.demo.repository.OrganizzazioneRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/organizzazioni")
public class OrganizzazioneController {

    private final OrganizzazioneRepository organizzazioneRepository;

    @Autowired
    public OrganizzazioneController(OrganizzazioneRepository organizzazioneRepository) {
        this.organizzazioneRepository = organizzazioneRepository;
    }


    // Ottiene l'elenco di tutte le organizzazioni
    // GET --> http://localhost:8080/api/organizzazioni
    @GetMapping
    public ResponseEntity<List<OrganizzazioneResponse>> getAllOrganizzazioni() {
        List<OrganizzazioneResponse> organizzazioni = organizzazioneRepository.findAll().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(organizzazioni);
    }

    // Ottiene i dettagli di una specifica organizzazione
    // GET --> http://localhost:8080/api/organizzazioni/{id}
    @GetMapping("/{id}")
    public ResponseEntity<OrganizzazioneResponse> getOrganizzazioneById(@PathVariable Long id) {
        return organizzazioneRepository.findById(id)
                .map(this::convertToResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Crea una nuova organizzazione
    // POST --> http://localhost:8080/api/organizzazioni
    @PostMapping
    public ResponseEntity<OrganizzazioneResponse> createOrganizzazione(@RequestBody OrganizzazioneRequest request) {
        Organizzazione organizzazione = new Organizzazione();
        organizzazione.setNome(request.getNome());
        organizzazione.setNumeroWhatsapp(request.getNumeroWhatsapp());
        organizzazione.setTonoDiVoce(request.getTonoDiVoce());

        Organizzazione savedOrganizzazione = organizzazioneRepository.save(organizzazione);
        return ResponseEntity.ok(convertToResponse(savedOrganizzazione));
    }


    // Aggiorna i dati di un'organizzazione esistente
    // PUT --> http://localhost:8080/api/organizzazioni/{id}
    @PutMapping("/{id}")
    public ResponseEntity<OrganizzazioneResponse> updateOrganizzazione(
            @PathVariable Long id,
            @RequestBody OrganizzazioneRequest request) {

        return organizzazioneRepository.findById(id)
                .map(organizzazione -> {
                    organizzazione.setNome(request.getNome());
                    organizzazione.setNumeroWhatsapp(request.getNumeroWhatsapp());
                    organizzazione.setTonoDiVoce(request.getTonoDiVoce());
                    return organizzazioneRepository.save(organizzazione);
                })
                .map(this::convertToResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }


    // Elimina un'organizzazione
    // DELETE --> http://localhost:8080/api/organizzazioni/{id}
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOrganizzazione(@PathVariable Long id) {
        if (organizzazioneRepository.existsById(id)) {
            organizzazioneRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    private OrganizzazioneResponse convertToResponse(Organizzazione organizzazione) {
        OrganizzazioneResponse response = new OrganizzazioneResponse();
        response.setId(organizzazione.getId());
        response.setNome(organizzazione.getNome());
        response.setNumeroWhatsapp(organizzazione.getNumeroWhatsapp());
        response.setTonoDiVoce(organizzazione.getTonoDiVoce());
        response.setDataCreazione(organizzazione.getDataCreazione());
        return response;
    }
}