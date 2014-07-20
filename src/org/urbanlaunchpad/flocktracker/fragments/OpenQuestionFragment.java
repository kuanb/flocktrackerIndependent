package org.urbanlaunchpad.flocktracker.fragments;

import android.view.View;
import android.widget.LinearLayout;
import com.squareup.otto.Bus;
import org.urbanlaunchpad.flocktracker.R;
import org.urbanlaunchpad.flocktracker.models.Question;
import org.urbanlaunchpad.flocktracker.views.AnswerView;

import java.util.Collections;
import java.util.Set;

public class OpenQuestionFragment extends QuestionFragment {
  private LinearLayout answersContainer;
  private AnswerView answerView;
  private View.OnClickListener onClickListener = new View.OnClickListener() {
    @Override
    public void onClick(View view) {
      ((AnswerView) view).enable();
    }
  };

  public OpenQuestionFragment(Question question, QuestionType questionType,
      Bus eventBus) {
    super(question, questionType, eventBus);
  }

  @Override
  public void setupLayout(View rootView) {
    answersContainer = (LinearLayout) rootView.findViewById(R.id.answer_layout);
    answerView = (AnswerView) getInflater().inflate(R.layout.question_answer_open, null);
    answerView.setOnClickListener(onClickListener);

    // Pre-populate
    Set<String> selectedAnswers = getQuestion().getSelectedAnswers();
    if (!selectedAnswers.isEmpty()) {
      answerView.initialize(getQuestion().getType(), selectedAnswers.iterator().next(), false);
      onClickListener.onClick(answerView);
    } else {
      answerView.initialize(getQuestion().getType(), null, false);
    }

    answersContainer.addView(answerView);
  }

  @Override
  public Set<String> getSelectedAnswers() {
    return Collections.singleton(answerView.getAnswer().toString());
  }

}
