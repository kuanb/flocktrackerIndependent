package org.urbanlaunchpad.flocktracker.views;

import android.content.Context;
import android.text.InputType;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import org.urbanlaunchpad.flocktracker.R;
import org.urbanlaunchpad.flocktracker.models.Question;

public class AnswerView extends LinearLayout {
  private TextView answer;
  private EditText otherAnswer;
  private ImageView image;
  private boolean isOther;
  private Question.QuestionType questionType;
  private boolean enabled = false;

  public AnswerView(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  @Override
  protected void onFinishInflate() {
    super.onFinishInflate();
    this.answer = (TextView) findViewById(R.id.answer_text);
    this.otherAnswer = (EditText) findViewById(R.id.other_answer);
    this.image = (ImageView) findViewById(R.id.answer_image);
  }

  public void initialize(Question.QuestionType questionType,
      String answerText, final boolean isOther) {
    this.questionType = questionType;
    this.isOther = isOther;

    // Check if this is an other
    if (this.isOther) {
      answer.setVisibility(GONE);
      otherAnswer.setVisibility(VISIBLE);
      otherAnswer.setText(answerText);

      // Used to override the touch mechanism for the EditText
      otherAnswer.setOnTouchListener(new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
          if (MotionEvent.ACTION_UP == event.getAction()) {
            enabled = false; // So that we toggle into true.
            callOnClick();
          }
          return false;
        }
      });
    } else {
      answer.setHint(getResources().getString(R.string.answer_hint));
      answer.setText(answerText);
    }

    switch (questionType) {
      case MULTIPLE_CHOICE:
      case CHECKBOX:
        image.setOnClickListener(new OnClickListener() {
          @Override
          public void onClick(View view) {
            callOnClick();
            if (enabled && (isOther || answer instanceof EditText)) {
              InputMethodManager inputMethodManager = (InputMethodManager) getContext().getSystemService(
                  Context.INPUT_METHOD_SERVICE);
              inputMethodManager.toggleSoftInput(InputMethodManager.SHOW_IMPLICIT, 0);
            }
          }
        });
        break;
      case LOOP:
      case OPEN_NUMBER:
        answer.setInputType(InputType.TYPE_CLASS_NUMBER
            | InputType.TYPE_NUMBER_FLAG_DECIMAL);
      case OPEN_TEXT:
        answer.setOnFocusChangeListener(new View.OnFocusChangeListener() {
          @Override
          public void onFocusChange(View v, boolean hasFocus) {
            if (hasFocus) {
              callOnClick();
            }
          }
        });
        break;
      case IMAGE:
        image.setVisibility(GONE);
        break;
      default:
        disable();
        break;
    }
  }

  public void enable() {
    switch (questionType) {
      case MULTIPLE_CHOICE:
        image.setImageResource(R.drawable.ft_cir_grn);
        break;
      case CHECKBOX:
        image.setImageResource(R.drawable.checkbox_check);
        break;
    }

    if (isOther) {
      otherAnswer.setTextColor(getResources().getColor(
          R.color.answer_selected));
      otherAnswer.requestFocus();
    } else {
      answer.setTextColor(getResources()
          .getColor(R.color.answer_selected));
    }
    enabled = true;
  }

  public void disable() {
    switch (questionType) {
      case MULTIPLE_CHOICE:
        image.setImageResource(R.drawable.ft_cir_gry);
        break;
      case CHECKBOX:
        image.setImageResource(R.drawable.checkbox_uncheck);
        break;
    }

    if (isOther) {
      otherAnswer.setTextColor(getResources().getColor(
          R.color.text_color_light));
    } else {
      answer.setTextColor(getResources().getColor(
          R.color.text_color_light));
    }
    enabled = false;
  }

  public void toggle() {
    if (enabled) {
      disable();
    } else {
      enable();
    }
  }

  public boolean isEnabled() {
    return enabled;
  }

  public CharSequence getAnswer() {
    return isOther ? otherAnswer.getText() : answer.getText();
  }

}