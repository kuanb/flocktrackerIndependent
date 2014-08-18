package org.urbanlaunchpad.flocktracker.fragments;

import android.annotation.SuppressLint;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.LinearLayout;

import com.squareup.otto.Bus;

import org.urbanlaunchpad.flocktracker.R;
import org.urbanlaunchpad.flocktracker.models.Answer;
import org.urbanlaunchpad.flocktracker.models.Question;
import org.urbanlaunchpad.flocktracker.views.AnswerView;

import java.util.HashSet;
import java.util.Set;

@SuppressLint("ValidFragment")
public class CheckBoxQuestionFragment extends QuestionFragment {
	private AnswerView[] answersLayout;
	private LinearLayout answersContainer;
	private Set<Integer> selectedAnswers = new HashSet<Integer>();
	private String generalJumpID;

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

	public CheckBoxQuestionFragment(Question question,
			QuestionType questionType, Bus eventBus) {
		super(question, questionType, eventBus);
	}

	@Override
	public void setupLayout(View rootView) {
		answersContainer = (LinearLayout) rootView
				.findViewById(R.id.answer_layout);
		Question currentQuestion = getQuestion();
		generalJumpID = currentQuestion.getJumpID();
		Set<String> selectedAnswers = currentQuestion.getSelectedAnswers();
		selectedAnswers = (Set<String>) ((HashSet<String>) selectedAnswers)
				.clone();

		boolean hasOther = currentQuestion.isOtherEnabled();
		Answer[] answers = currentQuestion.getAnswers();
		int numAnswers = hasOther ? answers.length + 1 : answers.length;
		answersLayout = new AnswerView[numAnswers];

		// Add listeners for answers
		for (int i = 0; i < answers.length; i++) {
			answersLayout[i] = (AnswerView) getInflater().inflate(
					R.layout.question_answer_checkbox, null);
			answersLayout[i].initialize(currentQuestion.getType(),
					answers[i].getAnswerText(), false);
			answersLayout[i].setOnClickListener(onClickListener);
			answersLayout[i].setId(i);
			if (selectedAnswers != null
					&& selectedAnswers.contains(answers[i].getAnswerText())) {
				onClickListener.onClick(answersLayout[i]);
				selectedAnswers.remove(answers[i].getAnswerText());
			}
			answersContainer.addView(answersLayout[i]);
		}

		if (hasOther) {
			answersLayout[numAnswers - 1] = (AnswerView) getInflater().inflate(
					R.layout.question_answer_checkbox, null);
			answersLayout[numAnswers - 1].setId(numAnswers - 1);
			if (selectedAnswers != null && !selectedAnswers.isEmpty()) {
				answersLayout[numAnswers - 1].initialize(currentQuestion
						.getType(), selectedAnswers.iterator().next(), true);
				onClickListener.onClick(answersLayout[numAnswers - 1]);
			} else {
				answersLayout[numAnswers - 1].initialize(
						currentQuestion.getType(), null, true);
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

	@Override
	public String getCurrentJump() {
		// TODO Auto-generated method stub
		return null;
	}
}