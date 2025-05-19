package com.example.demo.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "chunk_documenti")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChunkDocumento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "documento_id", nullable = false)
    private Documento documento;

    @Column(name = "indice_chunk", nullable = false)
    private Integer indiceChunk;

    @Column(name = "testo_chunk", columnDefinition = "TEXT", nullable = false)
    private String testoChunk;

    @Column(name = "embedding", columnDefinition = "bytea")
    private byte[] embedding;

    @Column(name = "data_creazione")
    private LocalDateTime dataCreazione;

    @PrePersist
    protected void onCreate() {
        dataCreazione = LocalDateTime.now();
    }
}