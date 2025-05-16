package com.example.demo.payloadDTO.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    private Long id;
    private String username;
    private String email;
    private String nome;
    private String cognome;
    private String ruolo;
    private Long organizzazioneId;
    private String nomeOrganizzazione;
    private boolean attivo;
    private LocalDateTime dataCreazione;
}