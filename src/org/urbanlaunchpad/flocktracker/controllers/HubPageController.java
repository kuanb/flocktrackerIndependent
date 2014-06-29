package org.urbanlaunchpad.flocktracker.controllers;

import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import org.urbanlaunchpad.flocktracker.R;
import org.urbanlaunchpad.flocktracker.fragments.HubPageFragment;
import org.urbanlaunchpad.flocktracker.fragments.HubPageManager;
import org.urbanlaunchpad.flocktracker.models.Metadata;

import javax.inject.Inject;

public class HubPageController implements HubPageManager.HubPageActionListener {
  private Metadata metadata;
  private HubPageFragment hubPageFragment;
  private FragmentManager fragmentManager;

  @Inject
  public HubPageController(Context context, Metadata metadata) {
    this.metadata = metadata;
    this.fragmentManager = ((Activity) context).getFragmentManager();
    this.hubPageFragment = new HubPageFragment(this, metadata.getMaleCount(), metadata.getFemaleCount());
  }

  public void showHubPage() {
    // Update fragments
    if (!isHubPageShowing()) {
      FragmentTransaction transaction = fragmentManager.beginTransaction();
      transaction.replace(R.id.surveyor_frame, hubPageFragment);
      transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
      transaction.addToBackStack(null);
      transaction.commit();
    }
  }

  public boolean isHubPageShowing() {
    return hubPageFragment.isVisible();
  }

  public void stopTrip() {
    hubPageFragment.stopTrip();
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
