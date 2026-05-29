package com.valihameed.ufcfightpredictor.registration;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@AllArgsConstructor
@EqualsAndHashCode
@ToString
public class RegistrationRequest {
    @NotBlank
    @Size(max = 100)
    private final String firstName;

    @NotBlank
    @Size(max = 100)
    private final String lastName;

    @NotBlank
    @Email
    private final String email;

    @NotBlank
    @Size(min = 8, max = 128)
    private final String password;

    @NotBlank
    @Size(min = 3, max = 30)
    private final String userName;

}
