package org.urbanlaunchpad.flocktracker.controllers;

import org.urbanlaunchpad.flocktracker.fragments.HubPageFragment;
import org.urbanlaunchpad.flocktracker.fragments.HubPageManager;
import org.urbanlaunchpad.flocktracker.models.Metadata;

import javax.inject.Inject;

public class HubPageController implements HubPageManager.HubPageActionListener {
  private Metadata metadata;
  private QuestionController questionController;
  private HubPageFragment fragment;

  @Inject
  public HubPageController(Metadata metadata, QuestionController questionController) {
    this.metadata = metadata;
    this.questionController = questionController;
  }

  public void setFragment(HubPageFragment fragment) {
    this.fragment = fragment;
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
