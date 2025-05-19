package com.example.demo.payloadDTO.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MessaggioResponse {
    private Long id;
    private Long conversazioneId;
    private String contenuto;
    private Boolean daCliente;
    private LocalDateTime orarioInvio;
    private Boolean elaborato;



}