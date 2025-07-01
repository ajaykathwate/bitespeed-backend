package com.bitespeed.task.contact.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
@AllArgsConstructor
public class ContactRequestDto {

    @Schema(description = "User email", example = "ajay@example.com")
    private String email;

    @Schema(description = "User phone number", example = "9876543210")
    private String phoneNumber;
}
