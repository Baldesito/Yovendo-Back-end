package com.example.demo.model;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name="organizzazioni")
public class Organizzazione {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nome", nullable = false)
    private String nome;

    @Column(name = "numero_whatsapp")
    private String numeroWhatsapp;

    @Column(name = "tono_di_voce")
    private String tonoDiVoce;

    @Column(name = "data_creazione")
    private LocalDateTime dataCreazione;

    @PrePersist
    protected void onCreate(){
        dataCreazione = LocalDateTime.now();
    }
}
