package com.example.demo.payloadDTO.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DocumentoRequest {
    private Long organizzazioneId;
    private String titolo;

}