package com.example.demo.payloadDTO.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WhatsAppMessageRequest {
    private String from;
    private String to;
    private String body;
    private String messageSid;

}