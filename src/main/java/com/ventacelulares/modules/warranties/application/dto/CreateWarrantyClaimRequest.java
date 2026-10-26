package com.ventacelulares.modules.warranties.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.util.List;

public record CreateWarrantyClaimRequest(
        @NotBlank @Pattern(regexp = "[0-9]{15}") String imei,
        @NotBlank String reportedFault,
        @NotNull List<@NotBlank String> photos,
        @NotBlank String receivingBranch) {
}
