package org.urbanlaunchpad.flocktracker.util;

public class StringUtil {
  public static String createID() {
    String ID = null;
    Integer randy;
    for (int i = 0; i < 7; ++i) {
      randy = (int) (Math.random() * ((9) + 1));
      if (i == 0) {
        ID = randy.toString();
      } else {
        ID = ID + randy.toString();
      }
    }

    return ID;
  }
}
