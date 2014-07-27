package org.urbanlaunchpad.flocktracker.controllers;

import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.SharedPreferences;
import org.urbanlaunchpad.flocktracker.ProjectConfig;
import org.urbanlaunchpad.flocktracker.R;
import org.urbanlaunchpad.flocktracker.fragments.StatisticsPageFragment;
import org.urbanlaunchpad.flocktracker.models.Statistics;

import javax.inject.Inject;
import java.util.Calendar;

public class StatisticsPageController {
  private SharedPreferences prefs;
  private Statistics statistics;
  private FragmentManager fragmentManager;
  private StatisticsPageFragment fragment;

  @Inject
  public StatisticsPageController(Context context, Statistics statistics) {
    this.fragmentManager = ((Activity) context).getFragmentManager();
    this.fragment = new StatisticsPageFragment();
    this.statistics = statistics;

    prefs = ProjectConfig.get().getSharedPreferences();
    // Load statistics from previous run-through
    statistics.setTotalDistanceBefore(prefs.getFloat("totalDistanceBefore", 0));
    statistics.setRidesCompleted(prefs.getInt("ridesCompleted", 0));
    statistics.setSurveysCompleted(prefs.getInt("surveysCompleted", 0));
  }

  public void showStatisticsPage() {
    if (!isStatisticsPageShowing()) {
      FragmentTransaction transaction = fragmentManager.beginTransaction();
      transaction.replace(R.id.surveyor_frame, fragment);
      transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
      transaction.addToBackStack(null);
      transaction.commit();
    }
  }

  public boolean isStatisticsPageShowing() {
    return fragment.isVisible();
  }

  public void submitSurvey() {
    statistics.setSurveysCompleted(statistics.getSurveysCompleted() + 1);
  }

  public void startTrip() {
    statistics.setStartTripTime(Calendar.getInstance());
  }

  public void stopTrip() {
    statistics.setStartTripTime(null);
    statistics.setRidesCompleted(statistics.getRidesCompleted() + 1);
    statistics.setTotalDistanceBefore(statistics.getTotalDistanceBefore() + statistics.getTripDistance());
    statistics.setTripDistance(0);
  }

}
