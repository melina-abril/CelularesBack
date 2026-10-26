package com.ventacelulares.modules.dashboard.domain;

import java.util.List;

public interface DashboardRepository {
    List<SaleRecord> sales();

    List<InventoryRecord> inventory();

    List<TransferRecord> transfersInTransit();
}
