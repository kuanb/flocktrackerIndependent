package org.urbanlaunchpad.flocktracker.fragments;

import android.annotation.SuppressLint;
import android.app.ActionBar.LayoutParams;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.Toast;

import com.squareup.otto.Bus;

import org.urbanlaunchpad.flocktracker.R;
import org.urbanlaunchpad.flocktracker.adapters.StableArrayAdapter;
import org.urbanlaunchpad.flocktracker.menu.DynamicListView;
import org.urbanlaunchpad.flocktracker.models.Question;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;

import javax.xml.datatype.Duration;

@SuppressLint("ValidFragment")
public class OrderedListQuestionFragment extends QuestionFragment implements
		DynamicListView.SwappingEnded {

	ArrayList<String> answerList = null;
	ArrayList<String> originalAnswerList = null;
	Button skipButton;
	DynamicListView answerlistView = null;
	ArrayList<Integer> selectedAnswersArrayList = null;
	LinearLayout orderAnswerLayout;
	View iniRootView;

	private OnClickListener skipButtonOnClickListener = new OnClickListener() {
		@Override
		public void onClick(View v) {
			resetAnswers();
			skipButton.setEnabled(false);
			skipButton.setText(R.string.question_skipped);
		}
	};

	public OrderedListQuestionFragment(Question question,
			QuestionType questionType, Bus eventBus) {
		super(question, questionType, eventBus);
	}

	protected void resetAnswers() {
		orderAnswerLayout.removeAllViews();
		setupLayout(iniRootView);
	}

	@Override
	public void setupLayout(View rootView) {
		iniRootView = rootView;
		answerList = new ArrayList<String>(Arrays.asList(getQuestion()
				.getAnswers()));
		originalAnswerList = answerList;
		ScrollView answerScroll = (ScrollView) rootView
				.findViewById(R.id.answer_scroll_container);
		answerScroll.setVisibility(View.GONE);
		LinearLayout answersContainer = (LinearLayout) rootView
				.findViewById(R.id.ordered_answer_layout);
		answersContainer.setVisibility(View.VISIBLE);
		
		StableArrayAdapter adapter = new StableArrayAdapter(getActivity(),
				R.layout.ordered_answer, answerList);
		answerlistView = (DynamicListView) new DynamicListView(getActivity(),
				this);
		answerlistView.setCheeseList(answerList);
		answerlistView.setAdapter(adapter);
		answerlistView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);

		skipButton = (Button) new Button(getActivity());
		skipButton.setEnabled(false);
		skipButton.setText(R.string.question_skipped);

		orderAnswerLayout = new LinearLayout(getActivity());
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
	public void setAnswer() {
		if (skipButton != null) {
			skipButton.setEnabled(true);
			skipButton.setText(R.string.skip_question);
		}
	}

	// private String getOrderedAnswers() {
	// String answer = null;
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
	// return answer;
	// }

	@Override
	public Set<String> getSelectedAnswers() {
		TreeSet<String> selectedAnswers = new TreeSet<String>();
		// for (int i = 0; i < answerList.size(); ++i) {
		// for (int j = 0; j < originalAnswerList.size(); ++j) {
		// if (originalAnswerList.get(i).equals(answerList.get(j))) {
		// selectedAnswers.add(originalAnswerList.get(i));
		// break;
		// }
		// }
		// }
		if (!skipButton.isActivated()) {
			return null;
		} else {
			for (int i = 0; i < answerList.size(); ++i) {
				selectedAnswers.add(answerList.get(i));
				Toast.makeText(getActivity(), answerList.get(i), Toast.LENGTH_SHORT).show();
			}
			return selectedAnswers;
		}
	}
}
