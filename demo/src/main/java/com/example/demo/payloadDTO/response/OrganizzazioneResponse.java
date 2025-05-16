package com.example.demo.payloadDTO.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrganizzazioneResponse {
    private Long id;
    private String nome;
    private String numeroWhatsapp;
    private String tonoDiVoce;
    private LocalDateTime dataCreazione;
}