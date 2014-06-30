package org.urbanlaunchpad.flocktracker.fragments;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.squareup.otto.Bus;
import org.urbanlaunchpad.flocktracker.ProjectConfig;
import org.urbanlaunchpad.flocktracker.R;
import org.urbanlaunchpad.flocktracker.SurveyorActivity;
import org.urbanlaunchpad.flocktracker.models.Statistics;

import javax.inject.Inject;
import java.util.Timer;
import java.util.TimerTask;

public class StatisticsPageFragment extends Fragment {

  @Inject Statistics statistics;
  @Inject Bus eventBus;
  private StatisticsPageAttachedEvent statisticsPageAttachedEvent = new StatisticsPageAttachedEvent();
  private Timer timer;
  private TextView tripTimeText;
  private TextView tripDistanceText;
  private TextView totalDistanceText;
  private TextView currentAddressText;
  private TextView usernameText;
  private TextView surveysCompletedText;
  private TextView ridesCompletedText;
  private View rootView;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    ((SurveyorActivity) getActivity()).getObjectGraph().inject(this);
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    if (rootView == null) {
      rootView = inflater.inflate(R.layout.fragment_status, container, false);
    }
    tripTimeText = (TextView) rootView.findViewById(R.id.tripTime);
    tripDistanceText = (TextView) rootView.findViewById(R.id.tripDistance);
    totalDistanceText = (TextView) rootView.findViewById(R.id.totalDistance);
    currentAddressText = (TextView) rootView.findViewById(R.id.currentAddress);
    usernameText = (TextView) rootView.findViewById(R.id.user_greeting);
    surveysCompletedText = (TextView) rootView.findViewById(R.id.surveysCompleted);
    ridesCompletedText = (TextView) rootView.findViewById(R.id.ridesCompleted);

    return rootView;
  }

  @Override
  public void onStart() {
    super.onStart();
    eventBus.post(statisticsPageAttachedEvent);
    timer = new Timer();
    timer.scheduleAtFixedRate(new TimerTask() {
      public void run() {
        updateStatusPage();
      }
    }, 0, 1000);
  }

  @Override
  public void onStop() {
    super.onStop();
    timer.cancel();
  }

  private void updateStatusPage() {
    // TODO Make this adaptable to different languages.
    getActivity().runOnUiThread(new Runnable() {
      @Override
      public void run() {
        usernameText.setText("Hi " + ProjectConfig.get().getUsername() + "!");
        surveysCompletedText.setText(Integer.toString(statistics.getSurveysCompleted()));
        ridesCompletedText.setText(Integer.toString(statistics.getRidesCompleted()));
        tripTimeText.setText(statistics.getElapsedTime());
        currentAddressText.setText(statistics.getCurrentAddress());
        tripDistanceText.setText(statistics.getFormattedTripDistance());
        totalDistanceText.setText(statistics.getFormattedTotalDistance());
      }
    });
  }

  public class StatisticsPageAttachedEvent {}
}
