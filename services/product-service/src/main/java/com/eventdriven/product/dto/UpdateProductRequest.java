package com.eventdriven.product.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record UpdateProductRequest(

        @NotBlank
        @Size(max = 255)
        String name,

        String description,

        @NotNull
        @DecimalMin(value = "0.00")
        @Digits(integer = 17, fraction = 2)
        BigDecimal price,

        @NotBlank
        @Size(min = 3, max = 3)
        String currency,

        @Size(max = 2048)
        String imageUrl

) {
}