package com.bigbrightpaints.erp.modules.portal.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record EnterpriseDashboardSnapshot(
        Window window,
        Financial financial,
        Sales sales,
        Operations operations,
        Ratios ratios,
        Trends trends,
        List<Alert> alerts,
        Breakdown breakdowns
) {
    public record Window(LocalDate currentWindowStart,
                         LocalDate currentWindowEnd,
                         LocalDate compareStart,
                         LocalDate compareEnd,
                         String timezone,
                         String bucket) {}

    public record Financial(String currency,
                            BigDecimal netRevenue,
                            BigDecimal taxRevenue,
                            BigDecimal grossRevenue,
                            BigDecimal cogs,
                            BigDecimal grossMargin,
                            BigDecimal cashBalance,
                            BigDecimal arOutstanding,
                            BigDecimal overdueOutstanding,
                            ArAging aging) {}

    public record ArAging(BigDecimal current,
                          BigDecimal days1to30,
                          BigDecimal days31to60,
                          BigDecimal days61to90,
                          BigDecimal over90,
                          BigDecimal total) {}

    public record Sales(BigDecimal bookedBacklog,
                        long openOrders,
                        BigDecimal bookedOrderValue,
                        long bookedOrderCount,
                        Double orderToCashDays) {}

    public record Operations(BigDecimal inventoryValue,
                             Double inventoryTurns,
                             BigDecimal producedQty,
                             BigDecimal packedQty,
                             BigDecimal dispatchedQty,
                             Double yieldPct,
                             Double wastagePct) {}

    public record Ratios(Double grossMarginPct,
                         Double overduePct,
                         Double inventoryTurns,
                         Double onTimeDispatchPct) {}

    public record Trends(List<SeriesPoint> revenue,
                         List<SeriesPoint> cogs,
                         List<SeriesPoint> cash,
                         List<SeriesPoint> arOverdue) {}

    public record SeriesPoint(String periodStart, BigDecimal value) {}

    public record Alert(String severity, String code, String message, Map<String, Object> details) {}

    public record Breakdown(List<TopDealer> topDealers,
                            List<TopSku> topSkus,
                            List<OverdueInvoice> overdueInvoices) {}

    public record TopDealer(String name, BigDecimal revenue, Double sharePct) {}

    public record TopSku(String code, BigDecimal revenue, BigDecimal quantity) {}

    public record OverdueInvoice(String invoiceNumber,
                                 String dealerName,
                                 LocalDate dueDate,
                                 long daysOverdue,
                                 BigDecimal outstanding) {}
}
