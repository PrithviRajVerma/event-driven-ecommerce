package com.eventdriven.order.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MergeCartRequest {

    @NotBlank(message = "Guest cart ID cannot be blank")
    private String guestCartId;
}
