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

import java.util.Set;

public abstract class QuestionFragment extends Fragment {

  // Loop stuff
  Boolean inLoopBoolean;
  Integer loopTotalInteger;
  Integer loopIterationInteger;
  Integer loopPositionInteger;
  private NavButtonsManager navButtonsManager;
  private Question question;
  private QuestionType questionType;
  private TextView questionView;
  private Bus eventBus;
  private QuestionAttachedEvent questionAttachedEvent = new QuestionAttachedEvent();

  public QuestionFragment(Question question, QuestionType questionType, Bus eventBus) {
    this.question = question;
    this.questionType = questionType;
    this.eventBus = eventBus;
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState) {
    View rootView = inflater.inflate(R.layout.fragment_question, container, false);
    navButtonsManager = (NavButtonsManager) rootView.findViewById(R.id.questionButtons);
    navButtonsManager.setQuestionType(this, questionType);
    questionView = (TextView) rootView.findViewById(R.id.question_view);
    questionView.setText(question.getQuestionText());
    setupLayout(rootView);
    return rootView;
  }

  @Override
  public void onStart() {
    super.onStart();
    questionAttachedEvent.question = question;
    eventBus.post(questionAttachedEvent);
  }

  abstract void setupLayout(View rootView);

  public Question getQuestion() {
    return question;
  }

  protected LayoutInflater getInflater() {
    return (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
  }

  public abstract Set<String> getSelectedAnswers();

  /**
   * Enum to specify question type
   */
  public enum QuestionType {
    FIRST, NORMAL, LAST, TRIP_FIRST, TRIP_NORMAL
  }

  public class QuestionAttachedEvent {
    public Question question;
  }
}