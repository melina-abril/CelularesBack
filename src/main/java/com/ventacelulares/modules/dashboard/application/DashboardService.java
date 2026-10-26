package com.ventacelulares.modules.dashboard.application;

import com.ventacelulares.modules.dashboard.application.dto.CriticalStockResponse;
import com.ventacelulares.modules.dashboard.application.dto.PagedResponse;
import com.ventacelulares.modules.dashboard.application.dto.SalesMetricsResponse;
import com.ventacelulares.modules.dashboard.application.dto.TopModelResponse;
import com.ventacelulares.modules.dashboard.application.dto.TransferResponse;
import com.ventacelulares.modules.dashboard.domain.DashboardRepository;
import com.ventacelulares.modules.dashboard.domain.SaleRecord;
import com.ventacelulares.shared.domain.Sucursal;
import com.ventacelulares.shared.security.CurrentUser;
import com.ventacelulares.shared.security.Roles;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
public class DashboardService {
    private final DashboardRepository repository;
    private final CurrentUser currentUser;
    private final Clock clock;

    public DashboardService(DashboardRepository repository, CurrentUser currentUser, Clock clock) {
        this.repository = repository;
        this.currentUser = currentUser;
        this.clock = clock;
    }

    public SalesMetricsResponse getSalesMetrics(LocalDate from, LocalDate to, Sucursal branch) {
        LocalDate end = to == null ? LocalDate.now(clock) : to;
        LocalDate start = from == null ? end.minusDays(29) : from;
        List<SaleRecord> sales = filteredSales(start, end, scopedBranch(branch));
        BigDecimal total = sales.stream().map(sale -> sale.amount().amount())
                .reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_EVEN);
        long withTradeIn = sales.stream().filter(SaleRecord::tradeIn).count();
        BigDecimal average = sales.isEmpty() ? BigDecimal.ZERO.setScale(2) :
                total.divide(BigDecimal.valueOf(sales.size()), 2, RoundingMode.HALF_EVEN);
        BigDecimal percentage = sales.isEmpty() ? BigDecimal.ZERO.setScale(2) :
                BigDecimal.valueOf(withTradeIn * 100).divide(BigDecimal.valueOf(sales.size()), 2, RoundingMode.HALF_EVEN);
        // Respuesta dummy calculada a partir de los datos de ejemplo; reemplazar al conectar persistencia.
        return new SalesMetricsResponse(start, end, sales.size(), total, average, percentage,
                breakdown(sales, "branch"), breakdown(sales, "channel"), breakdown(sales, "payment"));
    }

    public PagedResponse<TopModelResponse> getTopModels(LocalDate from, LocalDate to, Sucursal branch, int page, int size) {
        LocalDate end = to == null ? LocalDate.now(clock) : to;
        LocalDate start = from == null ? end.minusDays(29) : from;
        Map<String, List<SaleRecord>> grouped = new TreeMap<>();
        for (SaleRecord sale : filteredSales(start, end, scopedBranch(branch))) {
            grouped.computeIfAbsent(sale.model(), ignored -> new ArrayList<>()).add(sale);
        }
        List<Map.Entry<String, List<SaleRecord>>> ranked = grouped.entrySet().stream()
                .sorted(Comparator.<Map.Entry<String, List<SaleRecord>>>comparingInt(entry -> entry.getValue().size())
                        .reversed().thenComparing(Map.Entry::getKey)).toList();
        List<TopModelResponse> result = new ArrayList<>();
        for (int index = 0; index < ranked.size(); index++) {
            Map.Entry<String, List<SaleRecord>> entry = ranked.get(index);
            BigDecimal total = entry.getValue().stream().map(sale -> sale.amount().amount())
                    .reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_EVEN);
            result.add(new TopModelResponse(index + 1, entry.getKey(), entry.getValue().size(), total));
        }
        // Ranking dummy; reemplazar al conectar persistencia.
        return paginate(result, page, size);
    }

    public PagedResponse<CriticalStockResponse> getCriticalStock(Sucursal branch, int page, int size) {
        Sucursal scope = scopedBranch(branch);
        LocalDate lastWeek = LocalDate.now(clock).minusDays(7);
        // Stock dummy: solo variantes con ventas recientes y menos de dos unidades disponibles.
        List<CriticalStockResponse> result = repository.inventory().stream()
                .filter(item -> scope == null || item.branch() == scope)
                .filter(item -> item.availableUnits() < 2 && !item.lastSaleDate().isBefore(lastWeek))
                .sorted(Comparator.comparing((com.ventacelulares.modules.dashboard.domain.InventoryRecord item) -> item.branch().name())
                        .thenComparing(com.ventacelulares.modules.dashboard.domain.InventoryRecord::model))
                .map(item -> new CriticalStockResponse(item.branch(), item.model(), item.availableUnits(), item.lastSaleDate()))
                .toList();
        return paginate(result, page, size);
    }

    public PagedResponse<TransferResponse> getTransfersInTransit(Sucursal branch, int page, int size) {
        Sucursal scope = scopedBranch(branch);
        // Transferencias dummy en tránsito; reemplazar al conectar persistencia.
        List<TransferResponse> result = repository.transfersInTransit().stream()
                .filter(item -> scope == null || item.origin() == scope || item.destination() == scope)
                .sorted(Comparator.comparing(com.ventacelulares.modules.dashboard.domain.TransferRecord::dispatchedAt).reversed())
                .map(item -> new TransferResponse(item.id(), item.model(), item.imei(), item.origin(),
                        item.destination(), item.dispatchedAt(), "EN_TRANSITO"))
                .toList();
        return paginate(result, page, size);
    }

    private Sucursal scopedBranch(Sucursal requested) {
        if (!Roles.SUPERVISOR.equals(currentUser.role())) {
            return requested;
        }
        Sucursal assigned = currentUser.sucursal()
                .orElseThrow(() -> new AccessDeniedException("El supervisor no tiene sucursal asignada"));
        if (requested != null && requested != assigned) {
            throw new AccessDeniedException("No puede consultar otra sucursal");
        }
        return assigned;
    }

    private List<SaleRecord> filteredSales(LocalDate from, LocalDate to, Sucursal branch) {
        return repository.sales().stream()
                .filter(sale -> !sale.date().isBefore(from) && !sale.date().isAfter(to))
                .filter(sale -> branch == null || sale.branch() == branch)
                .toList();
    }

    private List<SalesMetricsResponse.Breakdown> breakdown(List<SaleRecord> sales, String dimension) {
        Map<String, List<SaleRecord>> grouped = new TreeMap<>();
        for (SaleRecord sale : sales) {
            String key = switch (dimension) {
                case "branch" -> sale.branch().name();
                case "channel" -> sale.channel();
                default -> sale.paymentMethod();
            };
            grouped.computeIfAbsent(key, ignored -> new ArrayList<>()).add(sale);
        }
        return grouped.entrySet().stream().map(entry -> new SalesMetricsResponse.Breakdown(
                entry.getKey(), entry.getValue().size(), entry.getValue().stream()
                        .map(sale -> sale.amount().amount()).reduce(BigDecimal.ZERO, BigDecimal::add)
                        .setScale(2, RoundingMode.HALF_EVEN))).toList();
    }

    private <T> PagedResponse<T> paginate(List<T> items, int page, int size) {
        long offset = (long) page * size;
        int from = (int) Math.min(offset, items.size());
        int to = (int) Math.min(offset + size, items.size());
        return new PagedResponse<>(items.subList(from, to), page, size, items.size(),
                (items.size() + size - 1) / size);
    }
}
