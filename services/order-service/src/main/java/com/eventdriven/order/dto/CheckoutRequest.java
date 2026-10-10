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
public class CheckoutRequest {

    @Schema(description = "Customer delivery/shipping address", example = "123 Market St, San Francisco, CA 94103")
    @Size(max = 500, message = "Shipping address cannot exceed 500 characters")
    private String shippingAddress;
}
