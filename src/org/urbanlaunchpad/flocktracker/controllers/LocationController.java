package org.urbanlaunchpad.flocktracker.controllers;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import org.urbanlaunchpad.flocktracker.R;
import org.urbanlaunchpad.flocktracker.SurveyorActivity;
import org.urbanlaunchpad.flocktracker.TrackerAlarm;
import org.urbanlaunchpad.flocktracker.helpers.SubmissionHelper;
import org.urbanlaunchpad.flocktracker.models.Metadata;
import org.urbanlaunchpad.flocktracker.models.Statistics;
import org.urbanlaunchpad.flocktracker.models.Submission;
import org.urbanlaunchpad.flocktracker.util.StringUtil;

import javax.inject.Inject;

public class LocationController implements
    GooglePlayServicesClient.ConnectionCallbacks,
    GooglePlayServicesClient.OnConnectionFailedListener {
  public static final int UPDATE_INTERVAL_IN_SECONDS = 5;
  private static final int MILLISECONDS_PER_SECOND = 1000;
  private static final long UPDATE_INTERVAL = MILLISECONDS_PER_SECOND
      * UPDATE_INTERVAL_IN_SECONDS;
  private static final int FASTEST_INTERVAL_IN_SECONDS = 1;
  private static final long FASTEST_INTERVAL = MILLISECONDS_PER_SECOND
      * FASTEST_INTERVAL_IN_SECONDS;
  private Context context;
  private Metadata metadata;
  private SubmissionHelper submissionHelper;
  private Statistics statistics;
  private LocationClient locationClient;
  private LocationRequest locationRequest;
  private boolean isTripStarted;
  private LocationListener locationListener = new LocationListener() {
    @Override
    public void onLocationChanged(final Location location) {
      if (isTripStarted) {
        statistics.updateLocation(location);
        metadata.setCurrentLocation(location);
      }
    }
  };

  @Inject
  public LocationController(Context context, SubmissionHelper submissionHelper, Metadata metadata,
      Statistics statistics) {
    this.context = context;
    this.submissionHelper = submissionHelper;
    this.metadata = metadata;
    this.statistics = statistics;

    locationClient = new LocationClient(context, this, this);

    locationRequest = LocationRequest.create();
    locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    locationRequest.setInterval(UPDATE_INTERVAL);
    locationRequest.setFastestInterval(FASTEST_INTERVAL);
  }

  public void saveLocation() {
    if (locationClient.isConnected()) {
      Submission submission = new Submission();
      submission.setType(Submission.Type.TRACKER);
      submission.setMetadata(metadata);
      submissionHelper.saveSubmission(submission);
    }
  }

  public void startTrip() {
    this.isTripStarted = true;
    locationClient.connect();
    startTracker();
    metadata.setTripID("T" + StringUtil.createID());
  }

  public void stopTrip() {
    if (locationClient.isConnected()) {
      locationClient.removeLocationUpdates(locationListener);
    }
    locationClient.disconnect();
    cancelTracker();
    isTripStarted = false;
    metadata.setCurrentLocation(null);
    metadata.setTripID(null);
  }

  private void startTracker() {
    AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
    Intent intentAlarm = new Intent(TrackerAlarm.TAG);
    PendingIntent pi = PendingIntent.getBroadcast(context, 1, intentAlarm,
        PendingIntent.FLAG_UPDATE_CURRENT);
    alarmManager.setRepeating(AlarmManager.RTC_WAKEUP,
        System.currentTimeMillis(), TrackerAlarm.TRACKER_INTERVAL, pi);
    Log.d("TrackerAlarm", "TrackerAlarm working.");
  }

  private void cancelTracker() {
    Log.d("TrackerAlarm", "Cancelling tracker");

    Intent intentAlarm = new Intent(TrackerAlarm.TAG);
    PendingIntent sender = PendingIntent.getBroadcast(context, 1, intentAlarm,
        PendingIntent.FLAG_UPDATE_CURRENT);
    AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

    alarmManager.cancel(sender);
  }

  @Override
  public void onConnected(Bundle bundle) {
    locationClient.requestLocationUpdates(locationRequest, locationListener);
    metadata.setCurrentLocation(locationClient.getLastLocation());
    statistics.updateLocation(locationClient.getLastLocation());

    Toast.makeText(context, R.string.tracking_on, Toast.LENGTH_SHORT).show();
  }

  @Override
  public void onDisconnected() {
    Toast.makeText(context, R.string.disconnected, Toast.LENGTH_SHORT).show();
  }

  @Override
  public void onConnectionFailed(ConnectionResult connectionResult) {
    if (connectionResult.hasResolution()) {
      try {
        // Start an Activity that tries to resolve the error
        connectionResult.startResolutionForResult((Activity) context,
            SurveyorActivity.CONNECTION_FAILURE_RESOLUTION_REQUEST);
        /*
         * Thrown if Google Play services canceled the original
				 * PendingIntent
				 */
      } catch (IntentSender.SendIntentException e) {
        // Log the error
        e.printStackTrace();
      }
    } else {
      /*
			 * If no resolution is available, display a dialog to the user with
			 * the error.
			 */
      Toast.makeText(context, connectionResult.getErrorCode(),
          Toast.LENGTH_SHORT).show();
    }
  }
}
