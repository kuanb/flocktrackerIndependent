package org.urbanlaunchpad.flocktracker;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.WindowManager;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;
import dagger.ObjectGraph;
import org.urbanlaunchpad.flocktracker.controllers.*;
import org.urbanlaunchpad.flocktracker.fragments.HubPageFragment;
import org.urbanlaunchpad.flocktracker.fragments.StatisticsPageFragment;
import org.urbanlaunchpad.flocktracker.helpers.GoogleDriveHelper;
import org.urbanlaunchpad.flocktracker.models.Metadata;
import org.urbanlaunchpad.flocktracker.models.Question;
import org.urbanlaunchpad.flocktracker.util.LocationUtil;
import org.urbanlaunchpad.flocktracker.util.StringUtil;
import org.urbanlaunchpad.flocktracker.views.DrawerView;

import javax.inject.Inject;

public class SurveyorActivity extends Activity {

  public static final int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;
  @Inject public static GoogleDriveHelper driveHelper;
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
    getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

    objectGraph = ObjectGraph.create(new FlocktrackerModule(this));
    objectGraph.inject(this);
    objectGraph.injectStatics();
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
    stopTrip();
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
      stopTrip();
      finish();
    } else if (questionController.isQuestionShowing()) {
      Question question = questionController.getCurrentQuestion();
      if (questionController.isAskingTripQuestions()) {
        if (question.getQuestionNumber() == 0) {
          showHubPage();
          questionController.stopAskingTripQuestions();
          return;
        }
      } else if (question.getChapterNumber() == 0
                 && question.getQuestionNumber() == 0) {
        showHubPage();
        return;
      }

      switchToPreviousQuestion();
    } else {
      super.onBackPressed();
    }
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode,
    Intent intent) {
    super.onActivityResult(requestCode, resultCode, intent);
    // Choose what to do based on the request code
    switch (requestCode) {
      case GoogleDriveHelper.REQUEST_ACCOUNT_PICKER:
        if (resultCode == RESULT_OK && intent != null
            && intent.getExtras() != null) {
          driveHelper.requestAccountPicker(intent);
        }
        break;
      case GoogleDriveHelper.REQUEST_AUTHORIZATION:
        if (resultCode != Activity.RESULT_OK) {
          startActivityForResult(
            IniconfigActivity.credential.newChooseAccountIntent(),
            GoogleDriveHelper.REQUEST_ACCOUNT_PICKER);
        }
        break;
      case GoogleDriveHelper.CAPTURE_IMAGE:
        questionController.getCurrentQuestionFragment().onActivityResult(requestCode, resultCode, intent);
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
    questionController.resetLoop();
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

  /*
   * Location tracking helper
   */

  public void stopTrip() {
    hubPageController.stopTrip();
    locationController.stopTrip();
    questionController.resetTrip();
    statisticsPageController.stopTrip();
  }

  public ObjectGraph getObjectGraph() {
    return objectGraph;
  }

  /**
   * Question handling
   */

  private void switchToNextQuestion() {
    questionController.switchToNextQuestion();
    drawerController.selectSurveyChapter(questionController
      .getCurrentQuestion().getChapterNumber());
  }

  private void switchToPreviousQuestion() {
    questionController.switchToPreviousQuestion();
    drawerController.selectSurveyChapter(questionController
      .getCurrentQuestion().getChapterNumber());
  }

  /*
   * EventBus event handling
   */

  @Subscribe
  public void onToggleTrip(HubPageFragment.RequestToggleTripEvent event) {
    if (metadata.getTripID() == null) {
      questionController.askTripQuestions();
    } else {
      stopTrip();
    }
  }

  @Subscribe
  public void onReachedEndOfTrackerSurvey(
    QuestionController.ReachedEndOfTrackerSurveyEvent event) {
    showHubPage();
    locationController.startTrip();
    statisticsPageController.startTrip();
  }

  @Subscribe
  public void onSurveyStartRequested(
    HubPageFragment.RequestStartSurveyEvent event) {
    metadata.setSurveyID("S" + StringUtil.createID());
    questionController.startSurvey();
    drawerController.selectSurveyChapter(0);
  }

  @Subscribe
  public void onSubmitButtonClicked(CommonEvents.SubmitSurveyEvent event) {
    showHubPage();
    questionController.submitSurvey();
    statisticsPageController.submitSurvey();
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
  public void onStatisticsPageRequested(
    CommonEvents.RequestStatisticsPageEvent event) {
    showStatisticsPage();
  }

  @Subscribe
  public void onStatisticsPageShown(
    StatisticsPageFragment.StatisticsPageAttachedEvent event) {
    showStatisticsPage();
  }

  @Subscribe
  public void onNextQuestionButtonClicked(
    CommonEvents.NextQuestionPressedEvent event) {
    switchToNextQuestion();
  }

  @Subscribe
  public void onPrevQuestionButtonClicked(
    CommonEvents.PreviousQuestionPressedEvent event) {
    switchToPreviousQuestion();
  }

  @Subscribe
  public void onQuestionShown(CommonEvents.QuestionShownEvent event) {
    Question question = event.question;
    if (questionController.isAskingTripQuestions()) {
      questionController.updateTrackerPosition(question
        .getQuestionNumber());
    } else { // TODO: Figure out title and drawer settings for tracker
      // questions.
      questionController.updateSurveyPosition(
        question.getChapterNumber(), question.getQuestionNumber());
      drawerController.selectSurveyChapter(question.getChapterNumber());
    }
  }

  @Subscribe
  public void onQuestionHidden(CommonEvents.QuestionHiddenEvent event) {
		if (!questionController.isAskingTripQuestions()){
			int fromJumpChapterPosition = questionController.getCurrenChapterPosition();
			int fromJumpQuestionPosition = questionController.getCurrentQuestionPosition();
		} else {
			int fromJumpTrackerQuestionPosition = questionController.getCurrentTrackerQuestionPosition();
		}	
    Question question = event.question;
    event.question.setSelectedAnswers(event.selectedAnswers);
  }

  @Subscribe
  public void onChapterRequested(DrawerView.SelectChapterEvent event) {
    questionController.resetLoop();
    if (questionController.isAskingTripQuestions()) {
      questionController.resetTrip();
    }
    questionController.updateSurveyPosition(event.chapterNumber, 0);
    questionController.showCurrentQuestion();
    drawerController.selectSurveyChapter(event.chapterNumber);
  }
}