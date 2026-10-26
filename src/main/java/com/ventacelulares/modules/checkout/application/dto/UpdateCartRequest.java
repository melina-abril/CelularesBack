package com.ventacelulares.modules.checkout.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record UpdateCartRequest(@NotBlank String variantId,
                                @NotNull List<@NotBlank String> accessoryIds,
                                String voucherCode) {}