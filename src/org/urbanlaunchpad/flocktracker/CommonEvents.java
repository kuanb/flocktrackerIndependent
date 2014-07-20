package org.urbanlaunchpad.flocktracker;

import java.util.Set;

public class CommonEvents {
  public static final RequestHubPageEvent requestHubPageEvent = new RequestHubPageEvent();
  public static final RequestStatisticsPageEvent requestStatisticsPageEvent = new RequestStatisticsPageEvent();

  public static class RequestHubPageEvent {
  }

  public static class RequestStatisticsPageEvent {
  }

  public static class QuestionEvent {
    public Set<String> selectedAnswers;

    public QuestionEvent(Set<String> selectedAnswers) {
      this.selectedAnswers = selectedAnswers;
    }
  }

  public static class NextQuestionPressedEvent extends QuestionEvent {
    public NextQuestionPressedEvent(Set<String> selectedAnswers) {
      super(selectedAnswers);
    }
  }

  public static class PreviousQuestionPressedEvent extends QuestionEvent {
    public PreviousQuestionPressedEvent(Set<String> selectedAnswers) {
      super(selectedAnswers);
    }
  }

  public static class SubmitSurveyEvent extends QuestionEvent {
    public SubmitSurveyEvent(Set<String> selectedAnswers) {
      super(selectedAnswers);
    }
  }
}
