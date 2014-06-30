package org.urbanlaunchpad.flocktracker.util;

import org.urbanlaunchpad.flocktracker.fragments.QuestionFragment;
import org.urbanlaunchpad.flocktracker.models.Question;
import org.urbanlaunchpad.flocktracker.models.Question.QuestionType;

public class QuestionUtil {

  public static QuestionType getQuestionTypeFromString(String type) {
    if (type.equals("MC")) {
      return QuestionType.MULTIPLE_CHOICE;
    }

    if (type.equals("ON")) {
      return QuestionType.OPEN_NUMBER;
    }

    if (type.equals("OT")) {
      return QuestionType.OPEN_TEXT;
    }

    if (type.equals("IM")) {
      return QuestionType.IMAGE;
    }

    if (type.equals("CB")) {
      return QuestionType.CHECKBOX;
    }

    if (type.equals("LP")) {
      return QuestionType.LOOP;
    }

    if (type.equals("OL")) {
      return QuestionType.ORDERED;
    }

    return null;
  }

  public static String getColumnTypeFromQuestionType(QuestionType type) {
    switch (type) {
      case MULTIPLE_CHOICE:
        return "STRING";
      case OPEN_NUMBER:
        return "NUMBER";
      case OPEN_TEXT:
      case IMAGE:
      case CHECKBOX:
      case ORDERED:
      case LOOP:
        return "STRING";
    }
    return null;
  }

  public static QuestionFragment.QuestionType getQuestionPositionType(Question question, int numChapters) {
    int questionPosition = question.getQuestionNumber();
    if (question.isTracker()) {
      if (questionPosition == 0) {
        return QuestionFragment.QuestionType.TRIP_FIRST;
      } else {
        return QuestionFragment.QuestionType.TRIP_NORMAL;
      }
    } else {
      int chapterQuestionCount = question.getChapter().getQuestionCount();
      int chapterPosition = question.getChapter().getChapterNumber();
      if (questionPosition == 0 && chapterPosition == 0) {
        return QuestionFragment.QuestionType.FIRST;
      } else if (questionPosition == chapterQuestionCount - 1 && chapterPosition == numChapters - 1) {
        return QuestionFragment.QuestionType.LAST;
      } else {
        return QuestionFragment.QuestionType.NORMAL;
      }
    }
  }
}
