package com.familyfinance.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class RegistrationForm {

    @NotBlank(message = "Username-ul este obligatoriu")
    @Size(min = 3, max = 50, message = "Username-ul trebuie sa aiba intre 3 si 50 de caractere")
    private String username;

    @NotBlank(message = "Parola este obligatorie")
    @Size(min = 6, message = "Parola trebuie sa aiba minim 6 caractere")
    private String password;

    @NotBlank(message = "Numele complet este obligatoriu")
    private String fullName;

    @NotBlank(message = "Email-ul este obligatoriu")
    @Email(message = "Adresa de email nu este valida")
    private String email;
}