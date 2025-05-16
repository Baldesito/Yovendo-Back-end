package com.example.demo.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Type;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "conversazioni")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Conversazione {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "organizzazione_id", nullable = false)
    private Organizzazione organizzazione;

    @Column(name = "telefono_cliente", nullable = false)
    private String telefonoCliente;

    @Column(name = "orario_inizio")
    private LocalDateTime orarioInizio;

    @Column(name = "orario_fine")
    private LocalDateTime orarioFine;

    @Column(name = "stato")
    private String stato = "attiva";

    @OneToMany(mappedBy = "conversazione", cascade = CascadeType.ALL)
    private List<Messaggio> messaggi = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        orarioInizio = LocalDateTime.now();
    }

    @Setter(AccessLevel.NONE) // Disabilita la generazione del setter Lombok
    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> contesto = new HashMap<>();

    // Metodo setter personalizzato che accetta direttamente una Map
    public void setContesto(Map<String, Object> contesto) {
        this.contesto = contesto != null ? contesto : new HashMap<>();
    }

    // Metodo setter aggiuntivo che accetta una stringa JSON
    public void setContesto(String jsonString) {
        if (jsonString == null || jsonString.isEmpty()) {
            this.contesto = new HashMap<>();
            return;
        }

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            this.contesto = objectMapper.readValue(jsonString, Map.class);
        } catch (JsonProcessingException e) {
            // In caso di errore nel parsing JSON, imposta un contesto vuoto
            this.contesto = new HashMap<>();
            System.err.println("Errore nel parsing JSON per il contesto: " + e.getMessage());
        }
    }
}