package com.example.demo.payloadDTO.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrganizzazioneRequest {
    private String nome;
    private String numeroWhatsapp;
    private String tonoDiVoce;
}