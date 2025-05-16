package com.example.demo.payloadDTO.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DocumentoResponse {
    private Long id;
    private Long organizzazioneId;
    private String titolo;
    private String tipoContenuto;
    private Boolean elaborato;
    private String statoElaborazione;
    private LocalDateTime dataCaricamento;
}