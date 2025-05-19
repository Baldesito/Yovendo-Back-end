package com.example.demo.payloadDTO.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConversazioneResponse {
    private Long id;
    private Long organizzazioneId;
    private String telefonoCliente;
    private String stato;
    private LocalDateTime orarioInizio;
    private LocalDateTime orarioFine;
    private Long numeroMessaggi;
    private String ultimoMessaggioTesto;
    private LocalDateTime ultimoMessaggioData;
    private Boolean ultimoMessaggioDaCliente;
}