package org.urbanlaunchpad.flocktracker.fragments;

import android.annotation.SuppressLint;
import android.graphics.drawable.StateListDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.squareup.otto.Bus;
import org.urbanlaunchpad.flocktracker.R;
import org.urbanlaunchpad.flocktracker.models.Question;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

@SuppressLint("ValidFragment")
public class CheckBoxQuestionFragment extends QuestionFragment {
  private final int CB_TAG = -2;
  private final int ANSWER_TAG = -3;
  private LinearLayout[] answersLayout;
  private ArrayList<Integer> selectedAnswers = new ArrayList<Integer>();

  private OnClickListener onClickListener = new OnClickListener() {
    @Override
    public void onClick(View v) {
      toggleCheckBox((LinearLayout) v);
      // sendAnswer();
    }
  };

  public CheckBoxQuestionFragment(Question question, QuestionType questionType, Bus eventBus) {
    super(question, questionType, eventBus);
  }

  private void toggleCheckBox(LinearLayout v) {
    CheckBox cb = (CheckBox) v.getChildAt(0);
    TextView tv = (TextView) v.getChildAt(1);
    if (cb.isChecked()) {
      selectedAnswers.remove((Integer) v.getId());
      cb.setChecked(false);
      tv.setTextColor(getResources().getColor(R.color.text_color_light));
    } else {
      cb.setChecked(true);
      selectedAnswers.add(v.getId());
      tv.setTextColor(getResources().getColor(R.color.answer_selected));
    }
  }

  @Override
  public void setupLayout(View rootView) {

    LinearLayout answersContainer = (LinearLayout) rootView
        .findViewById(R.id.answer_layout);
    boolean hasOther = getQuestion().isOtherEnabled();
    String[] answers = getQuestion().getAnswers();

    int numAnswers = hasOther ? answers.length : answers.length + 1;
    answersLayout = new LinearLayout[numAnswers];
    Set<String> selectedAnswers = getQuestion().getSelectedAnswers();

    for (int i = 0; i < answers.length; ++i) {
      // Custom Checkbox.
      CheckBox cbanswer = new CheckBox(getActivity());
      cbanswer.setId(CB_TAG);
      cbanswer.setBackgroundResource(R.drawable.custom_checkbox);
      cbanswer.setButtonDrawable(new StateListDrawable());
      cbanswer.setClickable(false);

      // Text for the answer
      TextView tvanswer = new TextView(getActivity());
      tvanswer.setId(ANSWER_TAG);
      tvanswer.setText(answers[i]);

      // Text formating.
      tvanswer.setTextSize(20);
      tvanswer.setPadding(20, 20, 0, 20);
      tvanswer.setTextColor(getResources().getColor(
          R.color.text_color_light));

      // Checkbox formatting.
      LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
          60, 60);
      layoutParams.gravity = Gravity.CENTER_VERTICAL;
      cbanswer.setLayoutParams(layoutParams);

      // linearlayout for checkbox and answer.
      answersLayout[i] = new LinearLayout(getActivity());
      answersLayout[i].setOrientation(LinearLayout.HORIZONTAL);

      // Adding both to LinearLayout.
      answersLayout[i].addView(cbanswer);
      answersLayout[i].addView(tvanswer);
      answersLayout[i].setOnClickListener(onClickListener);
      answersLayout[i].setId(i);
      answersContainer.addView(answersLayout[i]);

      // Pre-populate.
      if (selectedAnswers.contains(answers[i])) {
        onClickListener.onClick(answersLayout[i]);
      }
    }
    if (hasOther) {
      final int i = numAnswers - 1;

      // Custom checkbox
      // otherCB = new CheckBox(getActivity());
      // otherCB.setId(CB_TAG);
      // otherCB.setBackgroundResource(R.drawable.custom_checkbox);
      // otherCB.setButtonDrawable(new StateListDrawable());
      // otherCB.setClickable(false);
      //
      // // Answer text
      // final EditText otherET = new EditText(getActivity());
      // otherET.setHint(getResources().getString(R.string.other_hint));
      // otherET.setImeOptions(EditorInfo.IME_ACTION_DONE);
      // otherET.setTextColor(getResources().getColor(
      // R.color.text_color_light));
      //
      // // Checkbox formatting
      // LinearLayout.LayoutParams layoutParams = new
      // LinearLayout.LayoutParams(
      // 60, 60);
      // layoutParams.gravity = Gravity.CENTER_VERTICAL;
      // otherCB.setLayoutParams(layoutParams);
      //
      // // Text formatting.
      //
      // LinearLayout.LayoutParams layoutParamsText = new
      // LinearLayout.LayoutParams(
      // LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
      // layoutParamsText.gravity = Gravity.CENTER_VERTICAL;
      // layoutParamsText.setMargins(20, 20, 0, 20);
      // otherET.setLayoutParams(layoutParamsText);
      // otherET.setBackgroundResource(R.drawable.edit_text);
      // otherET.setSingleLine();
      // otherET.setTextColor(getResources().getColor(
      // R.color.text_color_light));
      // otherET.setTextSize(20);
      // otherET.setTypeface(Typeface.create("sans-serif-light",
      // Typeface.NORMAL));
      //
      // otherET.setOnTouchListener(new OnTouchListener() {
      // @Override
      // public boolean onTouch(View v, MotionEvent event) {
      // if (MotionEvent.ACTION_UP == event.getAction()) {
      // toggleCheckBox(answersLayout[i]);
      // }
      // return false;
      // }
      // });
      //
      // // Add both to a linear layout
      // answersLayout[i] = new LinearLayout(getActivity());
      // answersLayout[i].setOrientation(LinearLayout.HORIZONTAL);
      // answersLayout[i].addView(otherCB);
      // answersLayout[i].addView(otherET);
      // answersLayout[i].setId(ANSWER_TAG);
      // answersLayout[i].setOnClickListener(onClickListener);

      // answerlayout.addView(answersLayout[i]);

      // otherET.setOnEditorActionListener(new
      // TextView.OnEditorActionListener() {
      // @Override
      // public boolean onEditorAction(TextView v, int actionId,
      // KeyEvent event) {
      // toggleCheckBox(answersLayout[i]);
      // return false;
      // }
      // });
    }
    // sendAnswer();
  }

  // @Override
  // public void sendAnswer() {
  // // Sending the answer to the main activity.
  // if (!selectedAnswers.isEmpty()) {
  // String answerString = "";
  // for (int j = 0; j <= numAnswers; ++j) {
  // if (selectedAnswers.contains(j)) {
  // View answerView = answersLayout[j].findViewById(ANSWER_TAG);
  // if (answerView instanceof TextView) {
  // TextView answerTextView = (TextView) answerView;
  // answerString += answerTextView.getText() + ",";
  // } else if (answerView instanceof EditText) {
  // EditText answerEditText = (EditText) answerView;
  // answerString += answerEditText.getText() + ",";
  // }
  //
  // }
  // }
  // answerString = answerString.substring(0, answerString.length() - 1);
  // }
  // }

  @Override
  public Set<String> getSelectedAnswers() {
    Set<String> answers = new HashSet<String>();
    for (int index : selectedAnswers) {
      answers.add(getQuestion().getAnswers()[index]);
    }
    return answers;
  }
}
