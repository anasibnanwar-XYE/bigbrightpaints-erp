package com.bigbrightpaints.erp.modules.portal.dto;

import java.util.List;

public record WorkforceInsights(
    List<SquadSummary> squads, List<UpcomingMoment> moments, List<PerformanceLeader> leaders) {
  public record SquadSummary(String name, String capacity, String detail) {}

  public record UpcomingMoment(String title, String schedule, String description) {}

  public record PerformanceLeader(String name, String role, String highlight) {}
}
