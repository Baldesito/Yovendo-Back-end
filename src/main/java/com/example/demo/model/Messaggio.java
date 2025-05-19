package com.example.demo.model;

import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Entity
@Table(name = "messaggi")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Messaggio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "conversazione_id", nullable = false)
    private Conversazione conversazione;

    @Column(name = "contenuto", nullable = false, columnDefinition = "TEXT")
    private String contenuto;

    @Column(name = "da_cliente")
    private Boolean daCliente = false;

    @Column(name = "orario_invio")
    private LocalDateTime orarioInvio;

    @Column(name = "elaborato")
    private Boolean elaborato = false;

    @Column(name = "punteggio_confidenza")
    private Double punteggio_confidenza;

    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> documentiRiferiti = new HashMap<>();

    @PrePersist
    protected void onCreate() {
        orarioInvio = LocalDateTime.now();
    }
}