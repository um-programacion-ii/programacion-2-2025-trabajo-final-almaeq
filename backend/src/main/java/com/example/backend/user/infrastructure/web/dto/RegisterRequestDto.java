package com.example.backend.user.infrastructure.web.dto;

import lombok.Data;

@Data
public class RegisterRequestDto {
    private String username;
    private String email;
    private String password;
    private String firstName;
    private String lastName;
}
