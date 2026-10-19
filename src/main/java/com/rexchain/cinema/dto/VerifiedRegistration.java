package com.rexchain.cinema.dto;

import java.time.LocalDate;

/** Registration data whose OTP was consumed; passwordHash is never a raw password. */
public record VerifiedRegistration(
        String fullName,
        String email,
        String phone,
        String gender,
        LocalDate birthDate,
        String passwordHash,
        boolean termsAccepted,
        String username,
        String nationalId,
        Long favoriteCinemaId
) {
    public RegisterRequest asValidationRequest() {
        return new RegisterRequest(fullName, email, phone, gender, birthDate,
                passwordHash, passwordHash, termsAccepted, username, nationalId, favoriteCinemaId);
    }
}
