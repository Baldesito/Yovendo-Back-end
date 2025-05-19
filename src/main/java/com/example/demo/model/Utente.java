package com.example.demo.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "utenti")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Utente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nome_utente", nullable = false, unique = true)
    private String nomeUtente;

    @Column(name = "password", nullable = false)
    private String password;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "nome")
    private String nome;

    @Column(name = "cognome")
    private String cognome;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "organizzazione_id")
    private Organizzazione organizzazione;

    @Column(name = "ruolo", nullable = false)
    private String ruolo; // ADMIN, USER

    @Column(name = "attivo")
    private Boolean attivo = true;

    @Column(name = "data_creazione")
    private LocalDateTime dataCreazione;

    @PrePersist
    protected void onCreate() {
        dataCreazione = LocalDateTime.now();
    }
}