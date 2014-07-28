package org.urbanlaunchpad.flocktracker.controllers;

import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.widget.Toast;
import com.squareup.otto.Bus;
import org.json.JSONException;
import org.json.JSONObject;
import org.urbanlaunchpad.flocktracker.ProjectConfig;
import org.urbanlaunchpad.flocktracker.R;
import org.urbanlaunchpad.flocktracker.fragments.*;
import org.urbanlaunchpad.flocktracker.helpers.ColumnCheckHelper;
import org.urbanlaunchpad.flocktracker.helpers.SubmissionHelper;
import org.urbanlaunchpad.flocktracker.models.Chapter;
import org.urbanlaunchpad.flocktracker.models.Metadata;
import org.urbanlaunchpad.flocktracker.models.Question;
import org.urbanlaunchpad.flocktracker.models.Question.QuestionType;
import org.urbanlaunchpad.flocktracker.models.Submission;
import org.urbanlaunchpad.flocktracker.util.JSONUtil;
import org.urbanlaunchpad.flocktracker.util.QuestionUtil;

import javax.inject.Inject;

public class QuestionController {
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
  private int numLoopQuestions = -1;
  private int loopIteration = -1; // Iteration step where the loop process is.
  private int loopTotal = -1;

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

  public void stopAskingTripQuestions() {
    isAskingTripQuestions = false;
    resetTrip();
  }

  public void startSurvey() {
    questionPosition = 0;
    showCurrentQuestion();
  }

  private void startLoop() {
    loopPosition = 0;
    loopIteration = 0;
    loopTotal = Integer.parseInt(getCurrentQuestionFragment().getSelectedAnswers().iterator().next());
    numLoopQuestions = getCurrentQuestion().getLoopQuestions().length;
    inLoop = true;
  }

  public void submitSurvey() {
    new Thread(new Runnable() {
      public void run() {
        Submission submission = new Submission();
        submission.setChapters(chapterList);
        submission.setType(Submission.Type.SURVEY);
        submission.setMetadata(metadata);
        submissionHelper.saveSubmission(submission);
      }
    }).start();
    resetSurvey();
  }

  public void showCurrentQuestion() {
    Question currentQuestion = getCurrentQuestion();

    switch (currentQuestion.getType()) {
      case MULTIPLE_CHOICE:
        currentQuestionFragment = new MultipleChoiceQuestionFragment(currentQuestion,
            QuestionUtil.getQuestionPositionType(currentQuestion, chapterList.length), eventBus);
        break;
      case LOOP:
        if (currentQuestion.getSelectedAnswers().size() > 0) {
          startLoop();
        }
      case OPEN_NUMBER:
      case OPEN_TEXT:
        currentQuestionFragment = new OpenQuestionFragment(currentQuestion,
            QuestionUtil.getQuestionPositionType(currentQuestion, chapterList.length), eventBus);
        break;
      case IMAGE:
        currentQuestionFragment = new ImageQuestionFragment(currentQuestion,
            QuestionUtil.getQuestionPositionType(currentQuestion, chapterList.length), eventBus);
        break;
      case CHECKBOX:
        currentQuestionFragment = new CheckBoxQuestionFragment(currentQuestion,
            QuestionUtil.getQuestionPositionType(currentQuestion, chapterList.length), eventBus);
        break;
      case ORDERED:
        currentQuestionFragment = new OrderedListQuestionFragment(currentQuestion,
            QuestionUtil.getQuestionPositionType(currentQuestion, chapterList.length), eventBus);
        break;
    }

    FragmentTransaction transaction = fragmentManager.beginTransaction();
    transaction.replace(R.id.surveyor_frame, currentQuestionFragment);
    transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
    transaction.addToBackStack(null);
    transaction.commit();
  }

  public Question getCurrentQuestion() {
    Question currentQuestion;
    if (isAskingTripQuestions) {
      if (inLoop) {
        currentQuestion = trackingQuestions[trackerQuestionPosition].getLoopQuestions()[loopPosition];
        currentQuestion.initializeLoop(loopTotal, loopIteration, loopPosition);
      } else {
        currentQuestion = trackingQuestions[trackerQuestionPosition];
      }
    } else {
      if (inLoop) {
        currentQuestion = getCurrentChapter().getQuestions()[questionPosition].getLoopQuestions()[loopPosition];
        currentQuestion.initializeLoop(loopTotal, loopIteration, loopPosition);
      } else {
        currentQuestion = getCurrentChapter().getQuestions()[questionPosition];
      }
    }

    return currentQuestion;
  }

  public QuestionFragment getCurrentQuestionFragment() {
    return currentQuestionFragment;
  }

  public void switchToPreviousQuestion() {
    if (inLoop) {
      // First question in iteration.
      if (loopPosition == 0) {
        // First loop question. Switch out of loop.
        if (loopIteration == 0) {
          inLoop = false;
        } else { // Go back an iteration.
          loopIteration--;
          showCurrentQuestion();
          return;
        }
      } else { // Not first question in iteration. Go back.
        loopPosition--;
        showCurrentQuestion();
        return;
      }
    }

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

  public void switchToNextQuestion() {
    Question currentQuestion = getCurrentQuestion();

    // If this is the question that has looped questions, initialize the loop.
    if (!inLoop && currentQuestion.getType() == QuestionType.LOOP
        && currentQuestionFragment.getSelectedAnswers().size() > 0) {
      startLoop();
      showCurrentQuestion();
      return;
    } else if (inLoop) {
      // We have reached the last loop question in the iteration.
      if (loopPosition == numLoopQuestions - 1) {
        // We have finished all iterations of the loop questions. Continue to next
        // question.
        if (loopIteration == loopTotal - 1) {
          inLoop = false;
        } else { // Continue into next iteration.
          loopIteration++;
          loopPosition = 0;
          showCurrentQuestion();
          return;
        }
      } else {
        // Continue onto next question in this iteration.
        loopPosition++;
        showCurrentQuestion();
        return;
      }
    }

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

  public void updateTrackerPosition(int questionPosition) {
    this.trackerQuestionPosition = questionPosition;
  }

  public void updateSurveyPosition(int chapterPosition, int questionPosition) {
    if (this.chapterPosition != chapterPosition || this.questionPosition != questionPosition) {
      this.chapterPosition = chapterPosition;
      this.questionPosition = questionPosition;
      this.inLoop = false;
    }
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
    if (currentQuestionFragment == null) {
      return false;
    }
    return currentQuestionFragment.isVisible();
  }

  private Chapter getCurrentChapter() {
    return chapterList[chapterPosition];
  }

  public class ReachedEndOfTrackerSurveyEvent {}
}
