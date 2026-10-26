package com.ventacelulares.modules.warranties.domain;

import java.util.Optional;

public interface WarrantyRepository {
    Optional<WarrantySale> findSale(String imei, String dni, String invoiceNumber);
    Optional<WarrantyClaim> findClaim(String claimId);
    String nextClaimId();
    WarrantyClaim save(WarrantyClaim claim);
}
