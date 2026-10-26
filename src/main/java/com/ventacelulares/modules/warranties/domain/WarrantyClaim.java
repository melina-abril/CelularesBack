package com.ventacelulares.modules.warranties.domain;

import java.time.ZonedDateTime;
import java.util.List;

public record WarrantyClaim(String id, String imei, String reportedFault, List<String> photos,
                            String receivingBranch, ClaimStatus status, String diagnosis,
                            String rejectionReason, ZonedDateTime createdAt, ZonedDateTime updatedAt) {
    public WarrantyClaim {
        photos = List.copyOf(photos);
    }

    public WarrantyClaim withStatus(ClaimStatus newStatus, String newDiagnosis,
                                    String newRejectionReason, ZonedDateTime now) {
        return new WarrantyClaim(id, imei, reportedFault, photos, receivingBranch, newStatus,
                newDiagnosis == null ? diagnosis : newDiagnosis, newRejectionReason, createdAt, now);
    }
}
