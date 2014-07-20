package org.urbanlaunchpad.flocktracker.fragments;

import android.annotation.SuppressLint;
import android.app.ActionBar.LayoutParams;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import com.squareup.otto.Bus;
import org.urbanlaunchpad.flocktracker.R;
import org.urbanlaunchpad.flocktracker.adapters.StableArrayAdapter;
import org.urbanlaunchpad.flocktracker.menu.DynamicListView;
import org.urbanlaunchpad.flocktracker.models.Question;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;

@SuppressLint("ValidFragment")
public class OrderedListQuestionFragment extends QuestionFragment implements
    DynamicListView.SwappingEnded {

  ArrayList<String> answerList = null;
  Button skipButton;

  private OnClickListener skipButtonOnClickListener = new OnClickListener() {
    @Override
    public void onClick(View v) {
      skipButton.setEnabled(false);
      skipButton.setText(R.string.question_skipped);
    }
  };

  public OrderedListQuestionFragment(Question question, QuestionType questionType, Bus eventBus) {
    super(question, questionType, eventBus);
  }

  @Override
  public void setupLayout(View rootView) {
    answerList = new ArrayList<String>(Arrays.asList(getQuestion().getAnswers()));

    ScrollView answerScroll = (ScrollView) rootView
        .findViewById(R.id.answer_scroll_container);
    answerScroll.setVisibility(View.GONE);
    LinearLayout answersContainer = (LinearLayout) rootView
        .findViewById(R.id.ordered_answer_layout);
    answersContainer.setVisibility(View.VISIBLE);
    StableArrayAdapter adapter = new StableArrayAdapter(
        getActivity(), R.layout.ordered_answer, answerList);
    DynamicListView answerlistView = (DynamicListView) new DynamicListView(getActivity(),
        this);
    answerlistView.setCheeseList(answerList);
    answerlistView.setAdapter(adapter);
    answerlistView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);

    skipButton = (Button) new Button(getActivity());
    skipButton.setEnabled(false);
    skipButton.setText(R.string.question_skipped);

    LinearLayout orderAnswerLayout = new LinearLayout(getActivity());
    orderAnswerLayout.setOrientation(LinearLayout.VERTICAL);
    orderAnswerLayout.setWeightSum(6f);
    LinearLayout.LayoutParams lParams1 = (LinearLayout.LayoutParams) new LinearLayout.LayoutParams(
        LayoutParams.MATCH_PARENT, 0);
    LinearLayout.LayoutParams lParams2 = (LinearLayout.LayoutParams) new LinearLayout.LayoutParams(
        LayoutParams.MATCH_PARENT, 0);
    lParams1.weight = 5f;
    lParams2.weight = 1f;

    orderAnswerLayout.addView(answerlistView);
    orderAnswerLayout.addView(skipButton);
    answerlistView.setLayoutParams(lParams1);
    skipButton.setLayoutParams(lParams2);
    skipButton.setOnClickListener(skipButtonOnClickListener);

    answersContainer.addView(orderAnswerLayout);

    // sendAnswer();
  }

  // @Override
  public void sendAnswer() {
    if (skipButton != null) {
      skipButton.setEnabled(true);
      skipButton.setText(R.string.skip_question);
    }
    // answerString = getorderedAnswers();
    // selectedAnswers = new ArrayList<Integer>();
    //
    // for (int i = 0; i < totalanswers; ++i) {
    // for (int j = 0; j < totalanswers; ++j) {
    // if (originalAnswerList.get(i).equals(answerList.get(j))) {
    // selectedAnswers.add(j);
    // break;
    // }
    // }
    // }
  }

  private String getOrderedAnswers() {
    String answer = null;
    // StableArrayAdapter List = (StableArrayAdapter) answerlistView
    // .getAdapter();
    // for (int i = 0; i < answerList.size(); ++i) {
    // if (i == 0) {
    // answer = "(";
    // } else {
    // answer = answer + ",";
    // }
    // answer = answer + List.getItem(i);
    // if (i == (answerList.size() - 1)) {
    // answer = answer + ")";
    // }
    // }
    return answer;
  }

  @Override
  public Set<String> getSelectedAnswers() {
    return null;
  }

}
