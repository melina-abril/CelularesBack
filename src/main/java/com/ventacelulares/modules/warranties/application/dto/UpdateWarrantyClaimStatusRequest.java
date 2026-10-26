package com.ventacelulares.modules.warranties.application.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateWarrantyClaimStatusRequest(@NotBlank String status,
                                               String diagnosis, String rejectionReason) {
}
