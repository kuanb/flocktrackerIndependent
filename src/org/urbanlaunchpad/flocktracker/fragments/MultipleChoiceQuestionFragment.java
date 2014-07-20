package org.urbanlaunchpad.flocktracker.fragments;

import android.annotation.SuppressLint;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.squareup.otto.Bus;

import org.urbanlaunchpad.flocktracker.R;
import org.urbanlaunchpad.flocktracker.models.Question;
import org.urbanlaunchpad.flocktracker.views.AnswerView;

@SuppressLint("ValidFragment")
public class MultipleChoiceQuestionFragment extends QuestionFragment {
  private AnswerView[] answersLayout;
  private LinearLayout answersContainer;
  private int selectedAnswerIndex = -1;

  private OnClickListener onClickListener = new OnClickListener() {
    @Override
    public void onClick(View v) {
      // Disable the last clicked one
      if (selectedAnswerIndex != -1) {
        answersLayout[selectedAnswerIndex].disable();
      }

      ((AnswerView) v).enable();
      selectedAnswerIndex = v.getId();
      getListener().onSelectedAnswer(
          ((AnswerView) v).getAnswer().toString());
    }
  };

  public MultipleChoiceQuestionFragment(QuestionActionListener listener,
      Question question, QuestionType questionType, Bus eventBus) {
    super(listener, question, questionType, eventBus);
  }

  @Override
  public void setupLayout(View rootView) {
    answersContainer = (LinearLayout) rootView.findViewById(R.id.answer_layout);

    boolean hasOther = getQuestion().isOtherEnabled();
    String[] answers = getQuestion().getAnswers();
    int numAnswers = hasOther ? answers.length : answers.length + 1;
    answersLayout = new AnswerView[numAnswers];

    // Add listeners for answers
    for (int i = 0; i < answers.length; i++) {
      answersLayout[i] = (AnswerView) getInflater().inflate(
          R.layout.question_answer, null);
      answersLayout[i].initialize(getQuestion().getType(), answers[i]);
      answersLayout[i].setOnClickListener(onClickListener);
      answersLayout[i].setId(i);
      answersContainer.addView(answersLayout[i]);
    }
    
    if (hasOther) {
      answersLayout[numAnswers - 1] = (AnswerView) getInflater().inflate(
          R.layout.question_answer, null);
      answersLayout[numAnswers - 1].initialize(getQuestion().getType(),
          null);
      answersLayout[numAnswers - 1].setOnClickListener(onClickListener);
      answersContainer.addView(answersLayout[numAnswers - 1]);
    }
    prepopulateQuestion();
  }

  @Override
  public void prepopulateQuestion() {
    // if (selectedAnswers != null) {
    // if (selectedAnswers.get(0) == answersLayout.length) {
    // EditText answerText = (EditText) answersLayout[answersLayout.length]
    // .findViewById(ANSWER_TAG);
    // answerText.setText(jquestion.getString("Answer"));
    // }
    // }
  }
}
