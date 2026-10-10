package com.eventdriven.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CancelOrderRequest {

    @Schema(description = "Reason for order cancellation", example = "Customer requested cancellation")
    @Size(max = 255, message = "Cancellation reason cannot exceed 255 characters")
    private String reason;
}
