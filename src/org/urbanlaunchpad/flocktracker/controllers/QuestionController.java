package org.urbanlaunchpad.flocktracker.controllers;

import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.support.v4.app.Fragment;
import android.widget.Toast;
import com.squareup.otto.Bus;
import org.json.JSONException;
import org.json.JSONObject;
import org.urbanlaunchpad.flocktracker.ProjectConfig;
import org.urbanlaunchpad.flocktracker.R;
import org.urbanlaunchpad.flocktracker.fragments.*;
import org.urbanlaunchpad.flocktracker.fragments.QuestionFragment.QuestionActionListener;
import org.urbanlaunchpad.flocktracker.helpers.ColumnCheckHelper;
import org.urbanlaunchpad.flocktracker.helpers.SubmissionHelper;
import org.urbanlaunchpad.flocktracker.models.Chapter;
import org.urbanlaunchpad.flocktracker.models.Metadata;
import org.urbanlaunchpad.flocktracker.models.Question;
import org.urbanlaunchpad.flocktracker.models.Submission;
import org.urbanlaunchpad.flocktracker.util.JSONUtil;
import org.urbanlaunchpad.flocktracker.util.QuestionUtil;

import javax.inject.Inject;

public class QuestionController implements QuestionActionListener {
  private Context context;
  private Metadata metadata;
  private FragmentManager fragmentManager;
  private SubmissionHelper submissionHelper;
  private QuestionFragment currentQuestionFragment;

  private int chapterPosition = 0;
  private int questionPosition = 0;
  private int trackerQuestionPosition = 0;
  private Chapter[] chapterList;
  private Question[] trackingQuestions;

  private boolean inLoop = false; // Toggle that turns on if the survey gets into a loop.
  private int loopPosition = -1; // Position in the questions array in the loop the survey is in.
  private int loopIteration = -1; // Iteration step where the loop process is.

  private boolean isAskingTripQuestions = false;
  private ReachedEndOfTrackerSurveyEvent reachedEndOfTrackerSurveyEvent = new ReachedEndOfTrackerSurveyEvent();
  private Bus eventBus;

  @Inject
  public QuestionController(Context context, SubmissionHelper submissionHelper, Metadata metadata, Bus eventBus) {
    this.context = context;
    this.fragmentManager = ((Activity) context).getFragmentManager();
    this.submissionHelper = submissionHelper;
    this.metadata = metadata;
    this.eventBus = eventBus;
    resetSurvey();
    resetTrip();

    // Do column checks.
    new Thread(new Runnable() {
      @Override
      public void run() {
        new ColumnCheckHelper(chapterList, trackingQuestions).runChecks();
      }
    }).run();
  }

  public void askTripQuestions() {
    trackerQuestionPosition = 0;
    isAskingTripQuestions = true;
    showCurrentQuestion();
  }

  public void startSurvey() {
    questionPosition = 0;
    showCurrentQuestion();
  }

  public void showCurrentQuestion() {
    Question currentQuestion = getCurrentQuestion();

    switch (currentQuestion.getType()) {
      case MULTIPLE_CHOICE:
        currentQuestionFragment = new MultipleChoiceQuestionFragment(this, currentQuestion,
            QuestionUtil.getQuestionPositionType(currentQuestion, chapterList.length), eventBus);
        break;
      case OPEN:
        currentQuestionFragment = new OpenQuestionFragment(this, currentQuestion,
            QuestionUtil.getQuestionPositionType(currentQuestion, chapterList.length), eventBus);
        break;
      case IMAGE:
        currentQuestionFragment = new ImageQuestionFragment(this, currentQuestion,
            QuestionUtil.getQuestionPositionType(currentQuestion, chapterList.length), eventBus);
        break;
      case CHECKBOX:
        currentQuestionFragment = new CheckBoxQuestionFragment(this, currentQuestion,
            QuestionUtil.getQuestionPositionType(currentQuestion, chapterList.length), eventBus);
        break;
      case ORDERED:
        currentQuestionFragment = new OrderedListQuestionFragment(this, currentQuestion,
            QuestionUtil.getQuestionPositionType(currentQuestion, chapterList.length), eventBus);
        break;
      case LOOP:
        break;
    }

    FragmentTransaction transaction = fragmentManager.beginTransaction();
    transaction.replace(R.id.surveyor_frame, currentQuestionFragment);
    transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
    transaction.addToBackStack(null);
    transaction.commit();
  }

  public Question getCurrentDisplayedQuestion() {
    return currentQuestionFragment.getQuestion();
  }

  public Question getCurrentQuestion() {
    Question currentQuestion;
    if (isAskingTripQuestions) {
      if (inLoop) {
        currentQuestion = trackingQuestions[trackerQuestionPosition].getLoopQuestions()[loopPosition];
      } else {
        currentQuestion = trackingQuestions[trackerQuestionPosition];
      }
    } else {
      if (inLoop) {
        currentQuestion = getCurrentChapter().getQuestions()[questionPosition].getLoopQuestions()[loopPosition];
      } else {
        currentQuestion = getCurrentChapter().getQuestions()[questionPosition];
      }
    }
    return currentQuestion;
  }

  @Override
  public void onSelectedAnswer(String answer) {
    // TODO(adchia): implement
  }

  /*
   * Nav Buttons listener.
   */
  @Override
  public void onPrevQuestionButtonClicked() {
    if (isAskingTripQuestions) {
      trackerQuestionPosition--;
      showCurrentQuestion();
    } else {
      if (questionPosition == 0) {
        chapterPosition--;
        questionPosition = getCurrentChapter().getQuestionCount() - 1;
        showCurrentQuestion();
      } else {
        questionPosition--;
        showCurrentQuestion();
      }
    }
  }

  @Override
  public void onNextQuestionButtonClicked() {
    if (isAskingTripQuestions) {
      if (trackerQuestionPosition == trackingQuestions.length - 1) {
        // show hub page and start tracking
        isAskingTripQuestions = false;
        eventBus.post(reachedEndOfTrackerSurveyEvent);
      } else {
        trackerQuestionPosition++;
        showCurrentQuestion();
      }
    } else {
      if (questionPosition == getCurrentChapter().getQuestionCount() - 1) {
        chapterPosition++;
        questionPosition = 0;
        showCurrentQuestion();
      } else {
        questionPosition++;
        showCurrentQuestion();
      }
    }
  }

  @Override
  public void onSubmitButtonClicked() {
    new Thread(new Runnable() {
      public void run() {
        Submission submission = new Submission();
        submission.setChapters(chapterList);
        submission.setType(Submission.Type.SURVEY);
        submission.setMetadata(metadata);
        submissionHelper.saveSubmission(submission);
      }
    }).start();
  }

  public void updateSurveyPosition(int chapterPosition, int questionPosition) {
    this.chapterPosition = chapterPosition;
    this.questionPosition = questionPosition;
    this.inLoop = false;
  }

  // TODO(adchia): move into a view
  public String[] getChapterTitles() {
    String[] chapterTitles = new String[chapterList.length];
    for (int i = 0; i < chapterTitles.length; i++) {
      chapterTitles[i] = chapterList[i].getTitle();
    }
    return chapterTitles;
  }

  private void resetSurvey() {
    JSONObject surveyJSONObject = null;

    // parse json survey
    try {
      surveyJSONObject = new JSONObject(ProjectConfig.get().getOriginalJSONSurveyString());
    } catch (JSONException e) {
      Toast.makeText(context, R.string.json_format_error, Toast.LENGTH_SHORT).show();
      e.printStackTrace();
    }

    // Parse survey information.
    chapterList = JSONUtil.parseChapters(context, surveyJSONObject);
    chapterPosition = 0;
    questionPosition = 0;
  }

  public void resetTrip() {
    JSONObject surveyJSONObject = null;

    // parse json survey
    try {
      surveyJSONObject = new JSONObject(ProjectConfig.get().getOriginalJSONSurveyString());
    } catch (JSONException e) {
      Toast.makeText(context, R.string.json_format_error, Toast.LENGTH_SHORT).show();
      e.printStackTrace();
    }

    // Tracking information.
    trackingQuestions = JSONUtil.parseTrackingQuestions(context, surveyJSONObject);
    trackerQuestionPosition = 0;
  }

  public boolean isAskingTripQuestions() {
    return isAskingTripQuestions;
  }

  public boolean isQuestionShowing() {
    return currentQuestionFragment.isVisible();
  }

  private Chapter getCurrentChapter() {
    return chapterList[chapterPosition];
  }

  public class ReachedEndOfTrackerSurveyEvent {}
}
