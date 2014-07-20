package org.urbanlaunchpad.flocktracker.fragments;

import android.graphics.Typeface;
import android.text.InputType;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.LinearLayout;
import com.squareup.otto.Bus;
import org.urbanlaunchpad.flocktracker.R;
import org.urbanlaunchpad.flocktracker.models.Question;
import org.urbanlaunchpad.flocktracker.views.AnswerView;

import java.util.Collections;
import java.util.Set;

public class OpenQuestionFragment extends QuestionFragment {
  private EditText openET;
  private boolean askingNumbers;
  private LinearLayout answersContainer;

  public OpenQuestionFragment(Question question, QuestionType questionType,
      Bus eventBus) {
    super(question, questionType, eventBus);
  }

  @Override
  public void setupLayout(View rootView) {
    answersContainer = (LinearLayout) rootView.findViewById(R.id.answer_layout);

    Question.QuestionType questionType = getQuestion().getType();
    if (questionType.equals(Question.QuestionType.OPEN_NUMBER) || questionType.equals(Question.QuestionType.LOOP)) {
      askingNumbers = true;
    }

    openET = new EditText(getActivity());
    openET.setHint(getResources().getString(R.string.answer_hint));
    openET.setImeOptions(EditorInfo.IME_ACTION_DONE);
    if (askingNumbers) {
      openET.setInputType(InputType.TYPE_CLASS_NUMBER
          | InputType.TYPE_NUMBER_FLAG_DECIMAL);
    }
    openET.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
    openET.setSingleLine();
    openET.setTextSize(20);
    openET.setTextColor(getResources().getColor(R.color.text_color_light));
    openET.setBackgroundResource(R.drawable.edit_text);
    openET.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        ((AnswerView) view).enable();
      }
    });

    // Pre-populate
    Set<String> selectedAnswers = getQuestion().getSelectedAnswers();
    if (!selectedAnswers.isEmpty()) {
      openET.setText(selectedAnswers.iterator().next());
    }

    answersContainer.addView(openET);
  }

  @Override
  public Set<String> getSelectedAnswers() {
    return Collections.singleton(openET.getText().toString());
  }

}
