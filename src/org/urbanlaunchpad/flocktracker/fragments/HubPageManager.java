package org.urbanlaunchpad.flocktracker.fragments;

public interface HubPageManager {
  public interface HubPageActionListener {
    void onToggleTrip();
    void onStartSurvey();
    void onGetStatistics();
    void onMaleCountChanged(int maleCount);
    void onFemaleCountChanged(int femaleCount);
  }
}
