package com.travelo.authservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class EmailRequestDTO {
    
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be a valid email address")
    private String email;

    public EmailRequestDTO() {
    }

    public EmailRequestDTO(String email) {
        this.email = email;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    @Override
    public String toString() {
        return "EmailRequestDTO{" +
                "email='" + email + '\'' +
                '}';
    }
}

