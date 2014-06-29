package org.urbanlaunchpad.flocktracker.controllers;

import android.content.Context;
import android.content.SharedPreferences;
import org.urbanlaunchpad.flocktracker.ProjectConfig;
import org.urbanlaunchpad.flocktracker.SurveyorActivity;
import org.urbanlaunchpad.flocktracker.models.Statistics;

import javax.inject.Inject;
import java.util.Calendar;

public class StatisticsPageController {
  SurveyorActivity surveyorActivity;

  private SharedPreferences prefs;
  private Statistics statistics;

  @Inject
  public StatisticsPageController(Context context, Statistics statistics) {
    this.surveyorActivity = (SurveyorActivity) context;
    this.statistics = statistics;

    prefs = ProjectConfig.get().getSharedPreferences();
    // Load statistics from previous run-through
    statistics.setTotalDistanceBefore(prefs.getFloat("totalDistanceBefore", 0));
    statistics.setRidesCompleted(prefs.getInt("ridesCompleted", 0));
    statistics.setSurveysCompleted(prefs.getInt("surveysCompleted", 0));
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
