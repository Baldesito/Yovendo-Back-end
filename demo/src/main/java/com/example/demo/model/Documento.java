package com.example.demo.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Entity
@Table(name = "documenti")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Documento {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "organizzazione_id", nullable = false)
    private Organizzazione organizzazione;

    @Column(name = "titolo", nullable = false)
    private String titolo;

    @Column(name = "percorso_file", nullable = false)
    private String percorsoFile;

    @Column(name = "tipo_contenuto", nullable = false)
    private String tipoContenuto;

    @Column(name = "data_caricamento")
    private LocalDateTime dataCaricamento;

    @Column(name = "elaborato")
    private Boolean elaborato = false;

    @Column(name = "stato_elaborazione")
    private String statoElaborazione = "pendente";

    // evitare confusione - e lo manteniamo come stringa semplice
    @Column(name = "metadati")
    private String metadatiJson = "{}";

    // Campi transient non vengono salvati nel database
    @Transient
    private Map<String, Object> metadati = new HashMap<>();

    @PrePersist
    protected void onCreate() {
        dataCaricamento = LocalDateTime.now();
        sincronizzaMetadatiJson();
    }

    @PreUpdate
    protected void onUpdate() {
        sincronizzaMetadatiJson();
    }

    @PostLoad
    protected void onLoad() {
        caricaMetadatiDaJson();
    }

    // Metodo per sincronizzare la mappa metadati con il campo JSON
    private void sincronizzaMetadatiJson() {
        try {
            if (metadati != null && !metadati.isEmpty()) {
                metadatiJson = objectMapper.writeValueAsString(metadati);
            } else {
                metadatiJson = "{}";
            }
        } catch (JsonProcessingException e) {
            metadatiJson = "{}";
        }
    }

    // Metodo per caricare i metadati dal JSON
    private void caricaMetadatiDaJson() {
        try {
            if (metadatiJson != null && !metadatiJson.isEmpty() && !metadatiJson.equals("{}")) {
                metadati = objectMapper.readValue(metadatiJson, Map.class);
            } else {
                metadati = new HashMap<>();
            }
        } catch (JsonProcessingException e) {
            metadati = new HashMap<>();
        }
    }


    public Map<String, Object> getMetadati() {
        return metadati;
    }

    public void setMetadati(Map<String, Object> metadati) {
        this.metadati = metadati != null ? metadati : new HashMap<>();
        sincronizzaMetadatiJson();
    }
}