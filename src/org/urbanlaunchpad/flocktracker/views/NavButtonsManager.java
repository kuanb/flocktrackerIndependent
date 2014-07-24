package org.urbanlaunchpad.flocktracker.views;

import org.urbanlaunchpad.flocktracker.fragments.QuestionFragment.QuestionType;

public interface NavButtonsManager {

  /**
   * Used to initialize nav buttons view based on whether it's first, middle, last, or
   * the only question in a chapter.
   *
   * @param type
   */
  void setQuestionType(QuestionType type);
}
