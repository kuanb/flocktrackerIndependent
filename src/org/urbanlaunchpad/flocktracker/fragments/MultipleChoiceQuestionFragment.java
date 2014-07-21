package org.urbanlaunchpad.flocktracker.fragments;

import android.annotation.SuppressLint;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.LinearLayout;
import com.squareup.otto.Bus;
import org.urbanlaunchpad.flocktracker.R;
import org.urbanlaunchpad.flocktracker.models.Question;
import org.urbanlaunchpad.flocktracker.views.AnswerView;

import java.util.Collections;
import java.util.Set;

@SuppressLint("ValidFragment")
public class MultipleChoiceQuestionFragment extends QuestionFragment {
  private AnswerView[] answersLayout;
  private LinearLayout answersContainer;
  private int selectedAnswerIndex = -1;
  private AnswerView otherAnswer;

  private OnClickListener onClickListener = new OnClickListener() {
    @Override
    public void onClick(View v) {
      // Disable the last clicked one
      if (selectedAnswerIndex != -1) {
        answersLayout[selectedAnswerIndex].disable();
      }

      ((AnswerView) v).enable();
      selectedAnswerIndex = v.getId();
    }
  };

  public MultipleChoiceQuestionFragment(Question question, QuestionType questionType, Bus eventBus) {
    super(question, questionType, eventBus);
  }

  @Override
  public void setupLayout(View rootView) {
    Set<String> selectedAnswers = getQuestion().getSelectedAnswers();
    answersContainer = (LinearLayout) rootView.findViewById(R.id.answer_layout);

    boolean hasOther = getQuestion().isOtherEnabled();
    String[] answers = getQuestion().getAnswers();
    int numAnswers = hasOther ? answers.length + 1 : answers.length;
    answersLayout = new AnswerView[numAnswers];

    // Add listeners for answers
    for (int i = 0; i < answers.length; i++) {
      answersLayout[i] = (AnswerView) getInflater().inflate(
          R.layout.question_answer, null);
      answersLayout[i].initialize(getQuestion().getType(), answers[i], false);
      answersLayout[i].setOnClickListener(onClickListener);
      answersLayout[i].setId(i);
      if (selectedAnswers != null && selectedAnswers.contains(answers[i])) {
        onClickListener.onClick(answersLayout[i]);
      }
      answersContainer.addView(answersLayout[i]);
    }

    if (hasOther) {
      answersLayout[numAnswers - 1] = (AnswerView) getInflater().inflate(
          R.layout.question_answer, null);
      otherAnswer = answersLayout[numAnswers - 1];
      answersLayout[numAnswers - 1].setId(numAnswers - 1);
      if (selectedAnswerIndex == -1 && selectedAnswers != null && !selectedAnswers.isEmpty()) {
        answersLayout[numAnswers - 1].initialize(getQuestion().getType(),
            selectedAnswers.iterator().next(), true);
        onClickListener.onClick(answersLayout[numAnswers - 1]);
      } else {
        answersLayout[numAnswers - 1].initialize(getQuestion().getType(),
            null, true);
      }
      answersLayout[numAnswers - 1].setOnClickListener(onClickListener);
      answersContainer.addView(answersLayout[numAnswers - 1]);
    }
  }

  @Override
  public Set<String> getSelectedAnswers() {
    if (selectedAnswerIndex == -1) {
      return null;
    }

    // Other answer.
    if (getQuestion().isOtherEnabled() && selectedAnswerIndex == getQuestion().getAnswers().length) {
      return Collections.singleton(otherAnswer.getAnswer().toString());
    }

    return Collections.singleton(getQuestion().getAnswers()[selectedAnswerIndex]);
  }
}