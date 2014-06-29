package org.urbanlaunchpad.flocktracker;

public class CommonEvents {
  public static final RequestHubPageEvent requestHubPageEvent = new RequestHubPageEvent();
  public static final RequestStatisticsPageEvent requestStatisticsPageEvent = new RequestStatisticsPageEvent();

  public static class RequestHubPageEvent {}
  public static class RequestStatisticsPageEvent {}
}
