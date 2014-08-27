package org.urbanlaunchpad.flocktracker.fragments;

import android.annotation.SuppressLint;
import android.app.ActionBar.LayoutParams;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;

import com.squareup.otto.Bus;

import org.urbanlaunchpad.flocktracker.R;
import org.urbanlaunchpad.flocktracker.adapters.StableArrayAdapter;
import org.urbanlaunchpad.flocktracker.menu.DynamicListView;
import org.urbanlaunchpad.flocktracker.models.Answer;
import org.urbanlaunchpad.flocktracker.models.Question;

import java.util.*;

@SuppressLint("ValidFragment")
public class OrderedListQuestionFragment extends QuestionFragment {

	private List<String> answerList;
	private List<String> originalAnswerList;
	private DynamicListView answerListView;
	private LinearLayout answerLayout;
	private Button skipButton;
	private String generalJumpID;

	private OnClickListener skipButtonOnClickListener = new OnClickListener() {
		@Override
		public void onClick(View v) {
			resetLayout(answerList = originalAnswerList);
			disableSkipButton();
		}
	};

	public OrderedListQuestionFragment(Question question,
			QuestionType questionType, Bus eventBus) {
		super(question, questionType, eventBus);
	}

	@Override
	public void setupLayout(View rootView) {
		skipButton = new Button(getActivity());
		disableSkipButton();
		Question currentQuestion = getQuestion();
		generalJumpID = currentQuestion.getJumpID();
		Set<String> selectedAnswers = currentQuestion.getSelectedAnswers();
		if (selectedAnswers.size() > 0) {
			answerList = new ArrayList<String>(selectedAnswers);
			enableSkipButton();
		} else {
			Answer answers[] = currentQuestion.getAnswers();
			originalAnswerList = new ArrayList<String>(answers.length);
			for (int i = 0; i < answers.length; ++i) {
				originalAnswerList.add(answers[i].getAnswerText());
			}
			answerList = originalAnswerList;
		}

		answerLayout = (LinearLayout) rootView.findViewById(R.id.answer_layout);
		answerLayout.getLayoutParams().height = ViewGroup.LayoutParams.MATCH_PARENT;
		answerLayout.setWeightSum(6f);

		answerListView = new DynamicListView(getActivity(), this);
		answerListView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
		answerListView.setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(View view, MotionEvent motionEvent) {
				enableSkipButton();
				return false;
			}
		});

		LinearLayout.LayoutParams lParams1 = new LinearLayout.LayoutParams(
				LayoutParams.MATCH_PARENT, 0);
		LinearLayout.LayoutParams lParams2 = new LinearLayout.LayoutParams(
				LayoutParams.MATCH_PARENT, 0);
		lParams1.weight = 5f;
		lParams2.weight = 1f;

		answerLayout.addView(answerListView);
		answerLayout.addView(skipButton);
		answerListView.setLayoutParams(lParams1);
		skipButton.setLayoutParams(lParams2);
		skipButton.setOnClickListener(skipButtonOnClickListener);

		resetLayout(answerList);
	}

	private void resetLayout(List<String> answerList) {
		StableArrayAdapter adapter = new StableArrayAdapter(getActivity(),
				R.layout.ordered_answer, answerList);
		answerListView.setCheeseList(answerList);
		answerListView.setAdapter(adapter);
		answerListView.invalidateViews();
	}

	private void enableSkipButton() {
		skipButton.setEnabled(true);
		skipButton.setText(R.string.skip_question);
	}

	private void disableSkipButton() {
		skipButton.setEnabled(false);
		skipButton.setText(R.string.question_skipped);
	}

	@Override
	public Set<String> getSelectedAnswers() {
		if (skipButton.isEnabled()) {
			LinkedHashSet<String> selectedAnswers = new LinkedHashSet<String>();
			StableArrayAdapter adapter = (StableArrayAdapter) answerListView
					.getAdapter();
			for (int i = 0; i < adapter.getCount(); ++i) {
				selectedAnswers.add(adapter.getItem(i));
			}
			return selectedAnswers;
		}
		return new LinkedHashSet<String>();
	}

	@Override
	public String getCurrentJump() {
		return generalJumpID;
	}
}
