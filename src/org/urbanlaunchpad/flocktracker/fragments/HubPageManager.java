package org.urbanlaunchpad.flocktracker.fragments;

public interface HubPageManager {
  public interface HubPageActionListener {
    void onMaleCountChanged(int maleCount);
    void onFemaleCountChanged(int femaleCount);
  }
}
