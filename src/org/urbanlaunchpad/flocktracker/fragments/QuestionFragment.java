package org.urbanlaunchpad.flocktracker.fragments;

import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.squareup.otto.Bus;
import org.urbanlaunchpad.flocktracker.R;
import org.urbanlaunchpad.flocktracker.models.Question;
import org.urbanlaunchpad.flocktracker.views.NavButtonsManager;

public abstract class QuestionFragment extends Fragment {

  // Loop stuff
  Boolean inLoopBoolean;
  Integer loopTotalInteger;
  Integer loopIterationInteger;
  Integer loopPositionInteger;
  private QuestionActionListener listener;
  private NavButtonsManager navButtonsManager;
  private Question question;
  private QuestionType questionType;
  private TextView questionView;
  private Bus eventBus;
  private QuestionAttachedEvent questionAttachedEvent = new QuestionAttachedEvent();

  public QuestionFragment(QuestionActionListener listener, Question question,
      QuestionType questionType, Bus eventBus) {
    this.listener = listener;
    this.question = question;
    this.questionType = questionType;
    this.eventBus = eventBus;
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState) {
    View rootView = inflater.inflate(R.layout.fragment_question, container,
        false);

    navButtonsManager = (NavButtonsManager) rootView
        .findViewById(R.id.questionButtons);
    navButtonsManager.setQuestionType(listener, questionType);
    questionView = (TextView) rootView.findViewById(R.id.question_view);
    questionView.setText(question.getQuestionText());

    setupLayout(rootView);
    prepopulateQuestion();
    eventBus.post(questionAttachedEvent);

    return rootView;
  }

  abstract void setupLayout(View rootView);

  abstract void prepopulateQuestion();

  public Question getQuestion() {
    return question;
  }

  protected QuestionType getQuestionType() {
    return questionType;
  }

  protected QuestionActionListener getListener() {
    return listener;
  }

  protected LayoutInflater getInflater() {
    return (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
  }

  /**
   * Enum to specify question type
   */
  public enum QuestionType {
    FIRST, NORMAL, LAST, TRIP_FIRST, TRIP_NORMAL
  }

  /**
   * Listener for answer changes
   */
  public interface QuestionActionListener {
    void onSelectedAnswer(String answer);

    void onPrevQuestionButtonClicked();

    void onNextQuestionButtonClicked();

    void onSubmitButtonClicked();
  }

  public class QuestionAttachedEvent {}
}