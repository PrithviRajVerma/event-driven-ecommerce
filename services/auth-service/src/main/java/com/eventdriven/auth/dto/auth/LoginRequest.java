package com.eventdriven.auth.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.Length;

@Getter
@Setter
public class LoginRequest {

    @Email
    @NotBlank
    @Length(max = 320)
    private String email;

    @NotBlank
    @Size(min = 8, max = 100)
    private String password;

}
