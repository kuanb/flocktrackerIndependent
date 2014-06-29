package org.urbanlaunchpad.flocktracker.controllers;

import org.urbanlaunchpad.flocktracker.fragments.HubPageManager;
import org.urbanlaunchpad.flocktracker.models.Metadata;

import javax.inject.Inject;

public class HubPageController implements HubPageManager.HubPageActionListener {
  private Metadata metadata;

  @Inject
  public HubPageController(Metadata metadata) {
    this.metadata = metadata;
  }

  @Override
  public void onMaleCountChanged(int maleCount) {
    metadata.setMaleCount(maleCount);
  }

  @Override
  public void onFemaleCountChanged(int femaleCount) {
    metadata.setFemaleCount(femaleCount);
  }
}
