package org.urbanlaunchpad.flocktracker;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.util.Log;
import org.urbanlaunchpad.flocktracker.controllers.LocationController;

import javax.inject.Inject;

public class TrackerAlarm extends BroadcastReceiver {

  public static final String TAG = "TrackerAlarm";
  public static final Integer TRACKER_INTERVAL = 30000;
  private LocationController locationController;

  @Inject
  public TrackerAlarm(LocationController locationController) {
    this.locationController = locationController;
  }

  @SuppressLint("Wakelock")
  @Override
  public void onReceive(Context context, Intent intent) {
    Log.d("TrackerAlarm", "Starting alarm");
    PowerManager pm = (PowerManager) context
        .getSystemService(Context.POWER_SERVICE);
    PowerManager.WakeLock wl = pm.newWakeLock(
        PowerManager.PARTIAL_WAKE_LOCK, "");
    wl.acquire();

    if (locationController != null) {
      new Thread(new Runnable() {
        public void run() {
          locationController.saveLocation();
        }
      }).start();
    } else {
      Intent intentAlarm = new Intent(context, TrackerAlarm.class);
      PendingIntent sender = PendingIntent.getBroadcast(context, 1,
          intentAlarm, PendingIntent.FLAG_UPDATE_CURRENT);
      AlarmManager alarmManager = (AlarmManager) context
          .getSystemService(Context.ALARM_SERVICE);
      alarmManager.cancel(sender);
    }

    wl.release();
  }

}