package com.ventacelulares.modules.tradein.domain;

import java.util.Optional;

public interface TradeinRepository {
    Optional<TradeinVoucher> findByCode(String code);
    void save(TradeinVoucher voucher);
}
