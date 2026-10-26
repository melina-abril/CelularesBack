package com.ventacelulares.modules.warranties.application.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record WarrantyClaimResponse(String id, String imei, String reportedFault,
                                    List<String> photos, String receivingBranch, String status,
                                    String diagnosis, String rejectionReason,
                                    OffsetDateTime createdAt, OffsetDateTime updatedAt) {
}
