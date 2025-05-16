package com.example.demo.payloadDTO.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {

    @NotBlank(message = "L'email è obbligatoria")
    @Email(message = "L'email deve essere valida")
    private String email;
    @NotBlank
    private String password;
}