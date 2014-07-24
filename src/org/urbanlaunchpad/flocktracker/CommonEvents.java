package org.urbanlaunchpad.flocktracker;

import org.urbanlaunchpad.flocktracker.models.Question;

import java.util.Set;

public class CommonEvents {
  public static final RequestHubPageEvent requestHubPageEvent = new RequestHubPageEvent();
  public static final RequestStatisticsPageEvent requestStatisticsPageEvent = new RequestStatisticsPageEvent();

  public static class RequestHubPageEvent {}
  public static class RequestStatisticsPageEvent {}

  public static class QuestionEvent {
    public Question question;
    public Set<String> selectedAnswers;

    public QuestionEvent(Question question, Set<String> selectedAnswers) {
      this.question = question;
      this.selectedAnswers = selectedAnswers;
    }
  }

  public static class QuestionShownEvent extends QuestionEvent {
    public QuestionShownEvent(Question question, Set<String> selectedAnswers) {
      super(question, selectedAnswers);
    }
  }

  public static class QuestionHiddenEvent extends QuestionEvent {
    public QuestionHiddenEvent(Question question, Set<String> selectedAnswers) {
      super(question, selectedAnswers);
    }
  }

  public static class NextQuestionPressedEvent {}
  public static class PreviousQuestionPressedEvent {}
  public static class SubmitSurveyEvent {}
}
