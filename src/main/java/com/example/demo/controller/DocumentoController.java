package com.example.demo.controller;

import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.model.Documento;
import com.example.demo.model.Organizzazione;
import com.example.demo.payloadDTO.response.DocumentoResponse;
import com.example.demo.repository.DocumentoRepository;
import com.example.demo.repository.OrganizzazioneRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/documenti")
public class DocumentoController {

    private final DocumentoRepository documentoRepository;
    private final OrganizzazioneRepository organizzazioneRepository;

    @Autowired
    public DocumentoController(DocumentoRepository documentoRepository, OrganizzazioneRepository organizzazioneRepository) {
        this.documentoRepository = documentoRepository;
        this.organizzazioneRepository = organizzazioneRepository;
    }

    // Ottiene l'elenco di tutti i documenti
    // GET --> http://localhost:8080/api/documenti
    @GetMapping
    public ResponseEntity<List<DocumentoResponse>> getAllDocumenti() {
        List<DocumentoResponse> documenti = documentoRepository.findAll().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(documenti);
    }

    // Ottiene tutti i documenti di una specifica organizzazione
    // GET --> http://localhost:8080/api/documenti/organizzazione/{organizzazioneId}
    @GetMapping("/organizzazione/{organizzazioneId}")
    public ResponseEntity<List<DocumentoResponse>> getDocumentiByOrganizzazione(@PathVariable Long organizzazioneId) {
        List<DocumentoResponse> documenti = documentoRepository.findByOrganizzazioneId(organizzazioneId).stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(documenti);
    }


    // Carica un nuovo documento (multipart form con file, titolo e organizzazioneId)
    // POST --> http://localhost:8080/api/documenti
    @PostMapping
    public ResponseEntity<DocumentoResponse> uploadDocumento(
            @RequestParam("file") MultipartFile file,
            @RequestParam("titolo") String titolo,
            @RequestParam("organizzazioneId") Long organizzazioneId) throws IOException {

        Organizzazione organizzazione = organizzazioneRepository.findById(organizzazioneId)
                .orElseThrow(() -> new ResourceNotFoundException("Organizzazione", "id", organizzazioneId));

        String fileName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
        Path filePath = Paths.get("uploads/documenti/" + fileName);
        Files.createDirectories(filePath.getParent());
        Files.write(filePath, file.getBytes());

        Documento documento = new Documento();
        documento.setTitolo(titolo);
        documento.setOrganizzazione(organizzazione);
        documento.setPercorsoFile(filePath.toString());
        documento.setTipoContenuto(file.getContentType());

        Documento savedDocumento = documentoRepository.save(documento);
        return ResponseEntity.ok(convertToResponse(savedDocumento));
    }


    // Elimina un documento
    // DELETE --> http://localhost:8080/api/documenti/{id}
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDocumento(@PathVariable Long id) {
        if (documentoRepository.existsById(id)) {
            documentoRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    private DocumentoResponse convertToResponse(Documento documento) {
        DocumentoResponse response = new DocumentoResponse();
        response.setId(documento.getId());
        response.setOrganizzazioneId(documento.getOrganizzazione().getId());
        response.setTitolo(documento.getTitolo());
        response.setTipoContenuto(documento.getTipoContenuto());
        response.setElaborato(documento.getElaborato());
        response.setStatoElaborazione(documento.getStatoElaborazione());
        response.setDataCaricamento(documento.getDataCaricamento());
        return response;
    }
}