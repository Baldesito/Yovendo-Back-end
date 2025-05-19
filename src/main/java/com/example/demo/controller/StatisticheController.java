package com.example.demo.controller;

import com.example.demo.model.Documento;
import com.example.demo.model.Utente;
import com.example.demo.repository.ConversazioneRepository;
import com.example.demo.repository.DocumentoRepository;
import com.example.demo.repository.MessaggioRepository;
import com.example.demo.repository.UtenteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/statistiche")
public class StatisticheController {

    private final ConversazioneRepository conversazioneRepository;
    private final MessaggioRepository messaggioRepository;
    private final DocumentoRepository documentoRepository;
    private final UtenteRepository utenteRepository;

    @Autowired
    public StatisticheController(
            ConversazioneRepository conversazioneRepository,
            MessaggioRepository messaggioRepository,
            DocumentoRepository documentoRepository,
            UtenteRepository utenteRepository) {
        this.conversazioneRepository = conversazioneRepository;
        this.messaggioRepository = messaggioRepository;
        this.documentoRepository = documentoRepository;
        this.utenteRepository = utenteRepository;
    }

    // Ottiene statistiche generali per la dashboard (conversazioni, messaggi, documenti)
    // GET --> http://localhost:8080/api/statistiche/dashboard
    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();

        // Conteggi generali
        long totalConversazioni = conversazioneRepository.count();
        stats.put("totalConversazioni", totalConversazioni);

        long messaggiTotali = messaggioRepository.count();
        stats.put("messaggiTotali", messaggiTotali);

        long documentiTotali = documentoRepository.count();
        stats.put("documentiTotali", documentiTotali);

        // Conversazioni attive vs chiuse
        long conversazioniAttive = conversazioneRepository.findAll().stream()
                .filter(c -> "attiva".equals(c.getStato()))
                .count();
        stats.put("conversazioniAttive", conversazioniAttive);

        // Documenti elaborati vs in attesa
        List<Documento> documenti = documentoRepository.findAll();
        long documentiElaborati = documenti.stream().filter(Documento::getElaborato).count();
        stats.put("documentiElaborati", documentiElaborati);
        stats.put("documentiInAttesa", documentiTotali - documentiElaborati);

        // Attività recente (ultime 24 ore)
        LocalDateTime yesterday = LocalDateTime.now().minusDays(1);
        long messaggiRecenti = messaggioRepository.findAll().stream()
                .filter(m -> m.getOrarioInvio() != null && m.getOrarioInvio().isAfter(yesterday))
                .count();
        stats.put("messaggiUltime24h", messaggiRecenti);

        // Aggiungi informazioni sul top seller
        Map<String, Object> topSellerInfo = getTopSellerInfo();
        stats.put("topSellerName", topSellerInfo.get("name"));
        stats.put("topSellerPerformance", topSellerInfo.get("performance"));

        return ResponseEntity.ok(stats);
    }

    // Metodo per ottenere informazioni sul top seller
    private Map<String, Object> getTopSellerInfo() {
        Map<String, Object> result = new HashMap<>();

        // Esempio: trova l'utente con più conversazioni o messaggi
        // Questa logica potrebbe essere più complessa in base ai tuoi requisiti
        // Ad esempio, potrebbe essere basata su vendite, chiamate di successo, ecc.

        try {
            // Opzione 1: Trova l'utente con il ruolo "VENDITORE" e più conversazioni/messaggi
            // Sostituisci questa query con la logica effettiva per determinare il top seller
            Optional<Utente> topSeller = utenteRepository.findAll().stream()
                    .filter(u -> "VENDITORE".equals(u.getRuolo()))
                    .findFirst();

            if (topSeller.isPresent()) {
                Utente seller = topSeller.get();
                result.put("name", seller.getNome() + " " + seller.getCognome());
                result.put("performance", 85.5); // Percentuale di successo o altra metrica
            } else {
                result.put("name", "Nessun venditore trovato");
                result.put("performance", 0);
            }
        } catch (Exception e) {
            // Fallback in caso di errore
            result.put("name", "Balde"); // Fallback al nome hardcoded
            result.put("performance", 75.0);
        }

        return result;
    }

    // Ottiene statistiche specifiche per una organizzazione
    @GetMapping("/organizzazione/{organizzazioneId}")
    public ResponseEntity<Map<String, Object>> getOrganizationStats(@PathVariable Long organizzazioneId) {
        Map<String, Object> stats = new HashMap<>();

        // Conteggi specifici per organizzazione
        long conversazioni = conversazioneRepository.findByOrganizzazioneId(organizzazioneId).size();
        stats.put("totalConversazioni", conversazioni);

        long documenti = documentoRepository.findByOrganizzazioneId(organizzazioneId).size();
        stats.put("documentiTotali", documenti);

        long documentiElaborati = documentoRepository.findByOrganizzazioneIdAndElaborato(organizzazioneId, true).size();
        stats.put("documentiElaborati", documentiElaborati);

        // Elenco dei tipi di documenti
        Map<String, Long> tipiDocumento = documentoRepository.findByOrganizzazioneId(organizzazioneId).stream()
                .collect(Collectors.groupingBy(Documento::getTipoContenuto, Collectors.counting()));
        stats.put("tipiDocumento", tipiDocumento);

        return ResponseEntity.ok(stats);
    }
}