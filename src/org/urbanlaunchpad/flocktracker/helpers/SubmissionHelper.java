package org.urbanlaunchpad.flocktracker.helpers;

import android.content.SharedPreferences;
import android.util.Log;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.urbanlaunchpad.flocktracker.ProjectConfig;
import org.urbanlaunchpad.flocktracker.models.Submission;

import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;

public class SubmissionHelper {
  private LinkedList<Submission> surveySubmissionQueue;
  private LinkedList<Submission> trackerSubmissionQueue;
  private boolean savingTrackerSubmission = false;
  private boolean savingSurveySubmission = false;
  private boolean submittingSubmission = false;

  private SharedPreferences prefs;

  public SubmissionHelper() {
    prefs = ProjectConfig.get().getSharedPreferences();

    // Load saved submission queues.
    String surveySubmissionQueueGson = prefs.getString("surveySubmissionQueue", null);
    if (surveySubmissionQueueGson == null) {
      surveySubmissionQueue = new LinkedList<Submission>();
    } else {
      Type listType = new TypeToken<LinkedList<Submission>>() {
      }.getType();
      surveySubmissionQueue = new Gson().fromJson(surveySubmissionQueueGson, listType);
    }

    String trackerSubmissionQueueGson = prefs.getString("trackerSubmissionQueue", null);
    if (trackerSubmissionQueueGson == null) {
      trackerSubmissionQueue = new LinkedList<Submission>();
    } else {
      Type listType = new TypeToken<LinkedList<Submission>>() {
      }.getType();
      trackerSubmissionQueue = new Gson().fromJson(trackerSubmissionQueueGson, listType);
    }

    if (!surveySubmissionQueue.isEmpty() || !trackerSubmissionQueue.isEmpty()) {
      spawnSubmission();
    }
  }

  public void saveSubmission(Submission submission) {
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    String timestamp = sdf.format(new Date());
    submission.getMetadata().setTimeStamp(timestamp);

    if (submission.getType().equals(Submission.Type.TRACKER)) {
      savingTrackerSubmission = true;
      synchronized (trackerSubmissionQueue) {
        trackerSubmissionQueue.add(submission);
        prefs.edit().putString("trackerSubmissionQueue", new Gson().toJson(trackerSubmissionQueue)).commit();
        savingTrackerSubmission = false;
      }
    } else if (submission.getType().equals(Submission.Type.SURVEY)) {
      savingSurveySubmission = true;
      synchronized (surveySubmissionQueue) {
        surveySubmissionQueue.add(submission);
        prefs.edit().putString("surveySubmissionQueue", new Gson().toJson(surveySubmissionQueue)).commit();
        savingSurveySubmission = false;
      }
    }

    if (!submittingSubmission) {
      spawnSubmission();
    }
  }

  private void spawnSubmission() {
    new Thread(new Runnable() {
      public void run() {
        while (true) {
          synchronized (trackerSubmissionQueue) {
            if (trackerSubmissionQueue.isEmpty()) {
              submittingSubmission = false;
            } else {
              submittingSubmission = true;
            }
          }

          if (!submittingSubmission) {
            synchronized (surveySubmissionQueue) {
              if (surveySubmissionQueue.isEmpty()) {
              } else {
                submittingSubmission = true;
              }
            }
          }

          if (!submittingSubmission) {
            Log.d("Spawn submission queue", "Queue is empty");
            break;
          }

          // Iterate through queues to submit surveys
          while (!surveySubmissionQueue.isEmpty()) {
            Submission submission = surveySubmissionQueue.pop();
            if (submission.submit()) {
              prefs.edit().putString("surveySubmissionQueue", new Gson().toJson(surveySubmissionQueue)).commit();
            } else { // Failed. Try again
              try {
                surveySubmissionQueue.add(submission);
                Thread.sleep(3000);
              } catch (InterruptedException e) {
                e.printStackTrace();
              }
            }
          }

          // Iterate through queues to submit tracker updates
          while (!trackerSubmissionQueue.isEmpty()) {
            Submission submission = trackerSubmissionQueue.pop();
            if (submission.submit()) {
              prefs.edit().putString("trackerSubmissionQueue", new Gson().toJson(trackerSubmissionQueue)).commit();
            } else { // Failed. Try again
              try {
                trackerSubmissionQueue.add(submission);
                Thread.sleep(3000);
              } catch (InterruptedException e) {
                e.printStackTrace();
              }
            }
          }
        }
        submittingSubmission = false;
      }
    }).start();
  }
}
