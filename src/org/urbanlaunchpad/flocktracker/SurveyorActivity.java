package org.urbanlaunchpad.flocktracker;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.MenuItem;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;
import dagger.ObjectGraph;
import org.urbanlaunchpad.flocktracker.controllers.*;
import org.urbanlaunchpad.flocktracker.fragments.HubPageFragment;
import org.urbanlaunchpad.flocktracker.fragments.QuestionFragment;
import org.urbanlaunchpad.flocktracker.fragments.StatisticsPageFragment;
import org.urbanlaunchpad.flocktracker.helpers.GoogleDriveHelper;
import org.urbanlaunchpad.flocktracker.helpers.ImageHelper;
import org.urbanlaunchpad.flocktracker.models.Metadata;
import org.urbanlaunchpad.flocktracker.models.Question;
import org.urbanlaunchpad.flocktracker.util.LocationUtil;
import org.urbanlaunchpad.flocktracker.util.StringUtil;
import org.urbanlaunchpad.flocktracker.views.DrawerView;

import javax.inject.Inject;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Arrays;

public class SurveyorActivity extends Activity {
  public static final int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;
  public static GoogleDriveHelper driveHelper;
  @Inject QuestionController questionController;
  @Inject HubPageController hubPageController;
  @Inject StatisticsPageController statisticsPageController;
  @Inject LocationController locationController;
  @Inject DrawerController drawerController;
  @Inject Metadata metadata;
  @Inject Bus eventBus;
  @Inject TrackerAlarm trackerAlarm;
  private ObjectGraph objectGraph;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_surveyor);

    driveHelper = new GoogleDriveHelper(this);

    objectGraph = ObjectGraph.create(new FlocktrackerModule(this));
    objectGraph.inject(this);
    eventBus.register(this);

    // Check for location services.
    LocationUtil.checkLocationConfig(this);

    getActionBar().setDisplayHomeAsUpEnabled(true);
    getActionBar().setHomeButtonEnabled(true);

    if (savedInstanceState == null) {
      showHubPage();
    }
  }

	/*
   * Activity Lifecycle Handlers
	 */

  @Override
  protected void onDestroy() {
    questionController.resetTrip();
    locationController.stopTrip();
    super.onDestroy();
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    // No call for super(). Bug on API Level > 11.
  }

  @Override
  protected void onResume() {
    super.onResume();
    registerReceiver(trackerAlarm, new IntentFilter(TrackerAlarm.TAG));
  }

  @Override
  protected void onPause() {
    super.onPause();
    unregisterReceiver(trackerAlarm);
  }

  @Override
  protected void onPostCreate(Bundle savedInstanceState) {
    super.onPostCreate(savedInstanceState);
    drawerController.onPostCreate();
  }

  @Override
  public void onConfigurationChanged(Configuration newConfig) {
    super.onConfigurationChanged(newConfig);
    drawerController.onConfigurationChanged(newConfig);
  }

  @Override
  public void onBackPressed() {
    if (hubPageController.isHubPageShowing()) {
      locationController.stopTrip();
      finish();
    } else if (questionController.isQuestionShowing()) {
      Question question = questionController.getCurrentQuestion();
      if (questionController.isAskingTripQuestions()) {
        if (question.getQuestionNumber() == 0) {
          showHubPage();
          questionController.stopAskingTripQuestions();
          return;
        }
      } else if (question.getChapterNumber() == 0 && question.getQuestionNumber() == 0) {
        showHubPage();
        return;
      }

      questionController.onPrevQuestionButtonClicked(new CommonEvents.PreviousQuestionPressedEvent(null));
    } else {
      super.onBackPressed();
    }
  }


  @Override
  protected void onActivityResult(int requestCode, int resultCode,
      Intent intent) {

    // Choose what to do based on the request code
    switch (requestCode) {

      case GoogleDriveHelper.REQUEST_ACCOUNT_PICKER:
        if (resultCode == RESULT_OK && intent != null
            && intent.getExtras() != null) {
          driveHelper.requestAccountPicker(intent);
        }
        break;
      case GoogleDriveHelper.REQUEST_AUTHORIZATION:
        if (resultCode == Activity.RESULT_OK) {
          if (questionController.isAskingTripQuestions()) {
            ArrayList<Integer> key = new ArrayList<Integer>(
                Arrays.asList(
                    questionController.getCurrentQuestion().getQuestionNumber(), -1,
                    -1)
            );
//					SurveyHelper.prevTrackerImages
//							.put(key, driveHelper.fileUri);
          } else {
            Question question = questionController.getCurrentQuestion();
            ArrayList<Integer> key = new ArrayList<Integer>(
                Arrays.asList(question.getChapterNumber(),
                    question.getQuestionNumber(), -1, -1)
            );
//					SurveyHelper.prevImages.put(key, driveHelper.fileUri);
          }
//				currentQuestionFragment.ImageLayout();
        } else {
          startActivityForResult(
              IniconfigActivity.credential.newChooseAccountIntent(),
              GoogleDriveHelper.REQUEST_ACCOUNT_PICKER);
        }
        break;
      case GoogleDriveHelper.CAPTURE_IMAGE:
        try {
          Bitmap imageBitmap = BitmapFactory.decodeFile(
              driveHelper.fileUri.getPath(), null);
          float rotation = ImageHelper.rotationForImage(Uri
              .fromFile(new File(driveHelper.fileUri.getPath())));
          if (rotation != 0) {
            Matrix matrix = new Matrix();
            matrix.preRotate(rotation);
            imageBitmap = Bitmap.createBitmap(imageBitmap, 0, 0,
                imageBitmap.getWidth(), imageBitmap.getHeight(),
                matrix, true);
          }

          imageBitmap.compress(CompressFormat.JPEG, 25,
              new FileOutputStream(driveHelper.fileUri.getPath()));
        } catch (Exception e) {
          e.printStackTrace();
        }

        if (resultCode == Activity.RESULT_OK) {
          if (questionController.isAskingTripQuestions()) {
            ArrayList<Integer> key = new ArrayList<Integer>(
                Arrays.asList(
                    questionController.getCurrentQuestion().getQuestionNumber(), -1,
                    -1)
            );
//					SurveyHelper.prevTrackerImages
//							.put(key, driveHelper.fileUri);
          } else {
            Question question = questionController.getCurrentQuestion();
            ArrayList<Integer> key = new ArrayList<Integer>(
                Arrays.asList(question.getChapterNumber(),
                    question.getQuestionNumber(), -1, -1)
            );
//					SurveyHelper.prevImages.put(key, driveHelper.fileUri);
          }
//				currentQuestionFragment.ImageLayout();
        }
        break;

      // If the request code matches the code sent in onConnectionFailed
      case CONNECTION_FAILURE_RESOLUTION_REQUEST:

        switch (resultCode) {
          // If Google Play services resolved the problem
          case Activity.RESULT_OK:

            // Log the result
            Log.d("Location", "Resolved connection");
            break;

          // If any other result was returned by Google Play services
          default:
            // Log the result
            Log.e("Location", "Could not resolve connection");

            break;
        }

        // If any other request code was received
      default:
        // Report that this Activity received an unknown requestCode
        Log.e("SurveyorActivity activity", "unknown request code");
        break;
    }
  }

	/*
   * Drawer Logic
	 */

  public boolean onOptionsItemSelected(MenuItem item) {
    drawerController.onOptionsItemSelected(item);
    return true;
  }

  private void showHubPage() {
    if (questionController.isAskingTripQuestions()) {
      questionController.resetTrip();
    }

    questionController.updateSurveyPosition(0, 0);
    hubPageController.showHubPage();
    drawerController.showHubPage();
    setTitle(getString(R.string.hub_page_title));
  }

  private void showStatisticsPage() {
    if (questionController.isAskingTripQuestions()) {
      questionController.resetTrip();
    }

    questionController.updateSurveyPosition(0, 0);
    statisticsPageController.showStatisticsPage();
    drawerController.showStatisticsPage();
  }

  public void stopTripDialog() {
    Builder dialog;
    dialog = new AlertDialog.Builder(this);
    dialog.setMessage(this.getResources().getString(
        R.string.stop_tracker_message));
    dialog.setPositiveButton(this.getResources().getString(R.string.yes),
        new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface paramDialogInterface,
              int paramInt) {
            stopTrip();
          }
        }
    );
    dialog.setNegativeButton(this.getString(R.string.no),
        new DialogInterface.OnClickListener() {

          @Override
          public void onClick(DialogInterface paramDialogInterface,
              int paramInt) {
            // Nothing happens.
          }
        }
    );
    dialog.show();
  }

	/*
   * Location tracking helper
	 */

  public void stopTrip() {
    locationController.stopTrip();
    questionController.resetTrip();
    statisticsPageController.stopTrip();
  }

  public ObjectGraph getObjectGraph() {
    return objectGraph;
  }

  /*
   * EventBus event handling
   */

  @Subscribe
  public void onToggleTrip(HubPageFragment.RequestToggleTripEvent event) {
    if (metadata.getTripID() == null) {
      metadata.setTripID("T" + StringUtil.createID());
      questionController.askTripQuestions();
    } else {
      locationController.stopTrip();
      metadata.setTripID(null);
      metadata.setCurrentLocation(null);
      statisticsPageController.stopTrip();
      hubPageController.stopTrip();
    }
  }

  @Subscribe
  public void onReachedEndOfTrackerSurvey(QuestionController.ReachedEndOfTrackerSurveyEvent event) {
    showHubPage();
    locationController.startTrip();
    statisticsPageController.startTrip();
  }

  @Subscribe
  public void startSurvey(HubPageFragment.RequestStartSurveyEvent event) {
    metadata.setSurveyID("S" + StringUtil.createID());
    questionController.startSurvey();
    drawerController.selectSurveyChapter(0);
  }

  @Subscribe
  public void onHubPageRequested(CommonEvents.RequestHubPageEvent event) {
    showHubPage();
  }

  @Subscribe
  public void onHubPageShown(HubPageFragment.HubPageAttachedEvent event) {
    showHubPage();
  }

  @Subscribe
  public void onStatisticsPageRequested(CommonEvents.RequestStatisticsPageEvent event) {
    showStatisticsPage();
  }

  @Subscribe
  public void onStatisticsPageShown(StatisticsPageFragment.StatisticsPageAttachedEvent event) {
    showStatisticsPage();
  }

  @Subscribe
  public void onQuestionShown(QuestionFragment.QuestionAttachedEvent event) {
    Question question = event.question;
    if (questionController.isAskingTripQuestions()) {
      questionController.updateTrackerPosition(question.getQuestionNumber());
    } else { //TODO: Figure out title and drawer settings for tracker questions.
      questionController.updateSurveyPosition(question.getChapterNumber(), question.getQuestionNumber());
      drawerController.selectSurveyChapter(question.getChapterNumber());
    }
  }

  @Subscribe
  public void onChapterRequested(DrawerView.SelectChapterEvent event) {
    if (questionController.isAskingTripQuestions()) {
      questionController.resetTrip();
    }
    questionController.updateSurveyPosition(event.chapterNumber, 0);
    questionController.showCurrentQuestion();
    drawerController.selectSurveyChapter(event.chapterNumber);
  }

}
