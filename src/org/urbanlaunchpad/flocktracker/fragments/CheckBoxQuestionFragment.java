package org.urbanlaunchpad.flocktracker.fragments;

import android.annotation.SuppressLint;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.LinearLayout;
import com.squareup.otto.Bus;
import org.urbanlaunchpad.flocktracker.R;
import org.urbanlaunchpad.flocktracker.models.Question;
import org.urbanlaunchpad.flocktracker.views.AnswerView;

import java.util.HashSet;
import java.util.Set;

@SuppressLint("ValidFragment")
public class CheckBoxQuestionFragment extends QuestionFragment {
  private AnswerView[] answersLayout;
  private LinearLayout answersContainer;
  private Set<Integer> selectedAnswers = new HashSet<Integer>();

  private OnClickListener onClickListener = new OnClickListener() {
    @Override
    public void onClick(View v) {
      AnswerView answerView = (AnswerView) v;
      answerView.toggle();
      if (answerView.isEnabled()) {
        selectedAnswers.add(v.getId());
      } else {
        selectedAnswers.remove(v.getId());
      }
    }
  };

  public CheckBoxQuestionFragment(Question question, QuestionType questionType, Bus eventBus) {
    super(question, questionType, eventBus);
  }

  @Override
  public void setupLayout(View rootView) {
    HashSet<String> savedAnswers = (HashSet<String>) ((HashSet<String>) getQuestion().getSelectedAnswers()).clone();
    answersContainer = (LinearLayout) rootView.findViewById(R.id.answer_layout);

    boolean hasOther = getQuestion().isOtherEnabled();
    String[] answers = getQuestion().getAnswers();
    int numAnswers = hasOther ? answers.length + 1 : answers.length;
    answersLayout = new AnswerView[numAnswers];

    // Add listeners for answers
    for (int i = 0; i < answers.length; i++) {
      answersLayout[i] = (AnswerView) getInflater().inflate(
          R.layout.question_answer_checkbox, null);
      answersLayout[i].initialize(getQuestion().getType(), answers[i], false);
      answersLayout[i].setOnClickListener(onClickListener);
      answersLayout[i].setId(i);
      if (savedAnswers != null && savedAnswers.contains(answers[i])) {
        onClickListener.onClick(answersLayout[i]);
        savedAnswers.remove(answers[i]);
      }
      answersContainer.addView(answersLayout[i]);
    }

    if (hasOther) {
      answersLayout[numAnswers - 1] = (AnswerView) getInflater().inflate(
          R.layout.question_answer_checkbox, null);
      answersLayout[numAnswers - 1].setId(numAnswers - 1);
      if (savedAnswers != null && !savedAnswers.isEmpty()) {
        answersLayout[numAnswers - 1].initialize(getQuestion().getType(),
            savedAnswers.iterator().next(), true);
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
    Set<String> answers = new HashSet<String>();
    for (int index : selectedAnswers) {
      String answer = answersLayout[index].getAnswer().toString();
      if (!answer.isEmpty()) {
        answers.add(answersLayout[index].getAnswer().toString());
      }
    }
    return answers;
  }
}