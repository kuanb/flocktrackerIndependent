package org.urbanlaunchpad.flocktracker.models;

import android.net.Uri;

import java.util.HashSet;
import java.util.Set;

public class Question {
	private QuestionType type;
	private boolean isTracker;

	private int chapterNumber;
	private int chapterQuestionCount;
	private Question[] loopQuestions;

	private int questionNumber;
	private String questionText;
	private Answer[] answers;
	private String questionID;
	private boolean otherEnabled;
	private String jumpID;

	// Loop related variables
	private boolean inLoop;
	private int loopTotal; // total number of loops
	private int loopIteration; // current number of loop
	private int loopPosition; // within the loop, which question
	private int loopQuestionCount;

	// Image
	private Uri image;
	private Uri[] loopQuestionImages;

	// Selected Answers
	private Set<String> selectedAnswers = new HashSet<String>();
	private Set<String>[] loopQuestionSelectedAnswers;

	public QuestionType getType() {
		return type;
	}

	public void setType(QuestionType type) {
		this.type = type;
	}

	public int getChapterNumber() {
		return chapterNumber;
	}

	public int getChapterQuestionCount() {
		return chapterQuestionCount;
	}

	public void setChapterInfo(int chapterNumber, int chapterQuestionCount) {
		this.chapterNumber = chapterNumber;
		this.chapterQuestionCount = chapterQuestionCount;

		if (loopQuestions != null) {
			for (Question template : loopQuestions) {
				template.setChapterInfo(chapterNumber, chapterQuestionCount);
			}
		}
	}

	public String getQuestionText() {
		return questionText;
	}

	public void setQuestionText(String questionText) {
		this.questionText = questionText;
	}

	public Answer[] getAnswers() {
		return answers;
	}

	public void setAnswers(Answer[] answers) {
		this.answers = answers;
	}

	public String getJumpID() {
		return jumpID;
	}

	public void setjumpID(String jumpID) {
		this.jumpID = jumpID;
	}

	public String getQuestionID() {
		return questionID;
	}

	public void setQuestionID(String questionID) {
		this.questionID = questionID;
	}

	public boolean isOtherEnabled() {
		return otherEnabled;
	}

	public void setOtherEnabled(boolean otherEnabled) {
		this.otherEnabled = otherEnabled;
	}

	public boolean isInLoop() {
		return inLoop;
	}

	public void setInLoop(boolean inLoop) {
		this.inLoop = inLoop;
	}

	public void initializeLoop(int loopTotal, int loopIteration,
			int loopPosition) {
		if (this.loopQuestionSelectedAnswers == null) {
			this.loopQuestionSelectedAnswers = new Set[loopTotal];

			for (int i = 0; i < loopTotal; i++) {
				loopQuestionSelectedAnswers[i] = new HashSet<String>();
			}
		}
		if (this.loopQuestionImages == null) {
			this.loopQuestionImages = new Uri[loopTotal];
		}
		this.loopTotal = loopTotal;
		this.loopIteration = loopIteration;
		this.loopPosition = loopPosition;
		this.inLoop = true;
	}

	public void updateLoopInfo(int loopIteration, int loopPosition) {
		this.loopIteration = loopIteration;
		this.loopPosition = loopPosition;
	}

	public int getLoopPosition() {
		return loopPosition;
	}

	public int getLoopTotal() {
		return loopTotal;
	}

	public int getLoopIteration() {
		return loopIteration;
	}

	public Set<String> getSelectedAnswers() {
		if (inLoop) {
			return loopQuestionSelectedAnswers[loopIteration];
		} else {
			return selectedAnswers;
		}
	}

	public void setSelectedAnswers(Set<String> selectedAnswers) {
		if (inLoop) {
			loopQuestionSelectedAnswers[loopIteration] = selectedAnswers;
		} else {
			this.selectedAnswers = selectedAnswers;
		}
	}

	public Uri getImage() {
		if (inLoop) {
			return loopQuestionImages[loopIteration];
		} else {
			return image;
		}
	}

	public void setImage(Uri image) {
		if (inLoop) {
			this.loopQuestionImages[loopIteration] = image;
		} else {
			this.image = image;
		}
	}

	public int getQuestionNumber() {
		return questionNumber;
	}

	public void setQuestionNumber(int questionNumber) {
		this.questionNumber = questionNumber;
		if (loopQuestions != null) {
			for (Question template : loopQuestions) {
				template.setQuestionNumber(questionNumber);
			}
		}
	}

	public Question[] getLoopQuestions() {
		return loopQuestions;
	}

	public void setLoopQuestions(Question[] loopQuestions) {
		this.loopQuestions = loopQuestions;
	}

	public int getLoopQuestionCount() {
		return loopQuestionCount;
	}

	public void setLoopQuestionCount(int loopQuestionCount) {
		this.loopQuestionCount = loopQuestionCount;
	}

	public boolean isTracker() {
		return isTracker;
	}

	public void setTracker(boolean isTracker) {
		this.isTracker = isTracker;
	}

	public enum QuestionType {
		MULTIPLE_CHOICE, OPEN_NUMBER, OPEN_TEXT, IMAGE, CHECKBOX, ORDERED, LOOP
	}
}
