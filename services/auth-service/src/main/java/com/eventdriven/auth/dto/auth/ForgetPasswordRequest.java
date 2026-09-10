package com.eventdriven.auth.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.Length;

@Getter
@Setter
public class ForgetPasswordRequest {

    @NotBlank
    @Email
    @Length(max = 320)
    private String email;

}
