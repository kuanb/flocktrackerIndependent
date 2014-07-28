package org.urbanlaunchpad.flocktracker.models;

import android.location.Location;
import com.google.api.services.fusiontables.Fusiontables;
import org.urbanlaunchpad.flocktracker.IniconfigActivity;
import org.urbanlaunchpad.flocktracker.ProjectConfig;
import org.urbanlaunchpad.flocktracker.SurveyorActivity;
import org.urbanlaunchpad.flocktracker.helpers.GoogleDriveHelper;
import org.urbanlaunchpad.flocktracker.util.LocationUtil;

import javax.inject.Inject;
import java.io.IOException;
import java.util.Set;

public class Submission {
  public static final Integer MAX_QUERY_LENGTH = 2000; // max length allowed by fusion table

  private Chapter[] chapters;
  private Metadata metadata;
  private Type type;

  public Metadata getMetadata() {
    return metadata;
  }

  public void setMetadata(Metadata metadata) {
    this.metadata = metadata;
  }

  public Chapter[] getChapters() {
    return chapters;
  }

  public void setChapters(Chapter[] chapters) {
    this.chapters = chapters;
  }

  public Type getType() {
    return type;
  }

  public void setType(Type type) {
    this.type = type;
  }


  /**
   * Submits this submission to the appropriate table.
   *
   * @return true if submission succeeded.
   */
  public boolean submit() {
    boolean success = false;

    try {
      submitImages();
      String query = getQueryForSubmission();

      if (query.length() >= MAX_QUERY_LENGTH) {
        // Send initial insert and get row ID
        query = getMetadataInsertQuery();
        int rowID = Integer
            .parseInt((String) IniconfigActivity.fusiontables
                .query().sql(query)
                .setKey(ProjectConfig.get().getApiKey())
                .execute().getRows().get(0).get(0));

        Thread.sleep(100);
        // Send rest of info one at a time
        sendQuery(getUpdateQueryGivenRow(rowID, "Username", ProjectConfig.get().getUsername()));

        for (Chapter chapter : chapters) {
          for (Question question : chapter.getQuestions()) {
            Thread.sleep(100);
            sendQuery(getUpdateQueryGivenRow(rowID, question.getQuestionID(),
                question.getSelectedAnswers().toString()));
          }
        }
      } else {
        sendQuery(query);
      }
      success = true;
    } catch (Exception e) {
      e.printStackTrace();
    }
    return success;
  }

  /**
   * Helper method to upload images associated with this submission.
   *
   * @return true if success, false otherwise.
   */
  private boolean submitImages() {
    if (chapters == null) {
      return true;
    }

    try {
      for (Chapter chapter : chapters) {
        for (Question question : chapter.getQuestions()) {
          // Upload images
          if (question.getImage() != null) {
            String fileLink = question.getImage().getPath();
            SurveyorActivity.driveHelper.saveFileToDrive(fileLink);
          }
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
      return false;
    }
    return true;
  }

  /**
   * Helper method to get the full string query to submit the entire submission.
   *
   * @return
   */
  private String getQueryForSubmission() {
    // Create and submit query
    StringBuilder questionIDString = new StringBuilder();
    StringBuilder answerString = new StringBuilder("'");

    Location currentLocation = metadata.getCurrentLocation();
    String locationString = LocationUtil.getLngLatAlt(currentLocation);
    String query = "";

    double latitude, longitude, altitude;
    if (currentLocation == null) {
      latitude = longitude = altitude = 0;
    } else {
      latitude = currentLocation.getLatitude();
      longitude = currentLocation.getLongitude();
      altitude = currentLocation.getAltitude();
    }

    switch (type) {
      case TRACKER:
        query = "INSERT INTO "
            + ProjectConfig.get().getTrackerTableID()
            + " (Location,Lat,Lng,Alt,Date,TripID,Username,TotalCount,FemaleCount,MaleCount,Speed) VALUES ("
            + "'<Point><coordinates>" + locationString + "</coordinates></Point>','" + latitude + "','" + longitude
            + "','" + altitude + "','" + metadata.getTimeStamp() + "','" + metadata.getTripID()
            + "','" + ProjectConfig.get().getUsername() + "','"
            + (metadata.getMaleCount() + metadata.getFemaleCount()) + "','"
            + metadata.getFemaleCount() + "','" + metadata.getMaleCount() + "','" + metadata.getSpeed() + "');";
        break;
      case SURVEY:
        for (Chapter chapter : chapters) {
          for (Question question : chapter.getQuestions()) {
            // Get question ID's and answers
            Set<String> selectedAnswers = question.getSelectedAnswers();
            if (selectedAnswers != null && !selectedAnswers.isEmpty()) {
              questionIDString.append(question.getQuestionID() + ",");
              addQuestionToAnswerString(answerString, question);
            } else {
              questionIDString.append(question.getQuestionID() + ",");
              answerString.append("','");
            }
          }
        }

        query = "INSERT INTO "
            + ProjectConfig.get().getSurveyUploadTableID()
            + " ("
            + questionIDString
            + "Location,Lat,Lng,Alt,Date,SurveyID,TripID,Username,TotalCount,FemaleCount,MaleCount,Speed"
            + ") VALUES (" + answerString
            + "<Point><coordinates>" + locationString
            + "</coordinates></Point>','" + latitude + "','" + longitude
            + "','" + altitude + "','" + metadata.getTimeStamp() + "','" + metadata.getSurveyID()
            + "','" + metadata.getTripID() + "','" + ProjectConfig.get().getUsername() + "','"
            + (metadata.getMaleCount() + metadata.getFemaleCount()) + "','" + metadata.getFemaleCount()
            + "','" + metadata.getMaleCount() + "','" + metadata.getSpeed() + "');";
        break;
    }
    return query;
  }

  private void addQuestionToAnswerString(StringBuilder answerString, Question question) {
    // Get question ID's and answers
    Set<String> selectedAnswers = question.getSelectedAnswers();
    if (selectedAnswers != null && !selectedAnswers.isEmpty()) {
      String selectedAnswer = "";
      // Normal selected answers.
      if (selectedAnswers.size() == 1) {
        selectedAnswer = selectedAnswers.iterator().next();
        answerString.append(selectedAnswer);
      } else {
        answerString.append(selectedAnswers.toString());
      }

      // Loop through and add all the loop question answers in to this question.
      if (question.getType() == Question.QuestionType.LOOP) {
        int loopTotal = Integer.parseInt(selectedAnswer);

        answerString.append(" [");
        for (int i = 0; i < loopTotal; i++) {
          answerString.append("[");
          for (Question loopQuestion : question.getLoopQuestions()) {
            Set<String> loopSelectedAnswers = loopQuestion.getLoopQuestionSelectedAnswers()[i];
            if (loopSelectedAnswers != null) {
              answerString.append(loopSelectedAnswers);
            }
            answerString.append(",");
          }
          answerString.setLength(answerString.length() - 1);
          answerString.append("]");
        }
        answerString.append("]");
      }

      answerString.append("','");
    } else {
      answerString.append("','");
    }
  }

  /**
   * Helper method to get the string query needed to insert only the metadata.
   *
   * @return
   */
  private String getMetadataInsertQuery() {
    String query = null;

    Location currentLocation = metadata.getCurrentLocation();
    String locationString = LocationUtil.getLngLatAlt(currentLocation);

    switch (type) {
      case TRACKER:
        query = "INSERT INTO "
            + ProjectConfig.get().getTrackerTableID()
            + " (Location,Lat,Lng,Alt,Date,TripID,TotalCount,FemaleCount,MaleCount,Speed)"
            + " VALUES (" + "'<Point><coordinates>" + locationString
            + "</coordinates></Point>','" + currentLocation.getLatitude() + "','"
            + currentLocation.getLongitude() + "','" + currentLocation.getAltitude() + "','"
            + metadata.getTimeStamp() + "','" + metadata.getTripID()
            + "','" + (metadata.getMaleCount() + metadata.getFemaleCount()) + "','"
            + metadata.getFemaleCount() + "','" + metadata.getMaleCount() + "','"
            + metadata.getSpeed() + "');";
        break;
      case SURVEY:
        query = "INSERT INTO "
            + ProjectConfig.get().getSurveyUploadTableID()
            + " (Location,Lat,Lng,Alt,Date,SurveyID,TripID,TotalCount,FemaleCount,"
            + "MaleCount,Speed)" + " VALUES ("
            + "'<Point><coordinates>" + locationString
            + "</coordinates></Point>','" + currentLocation.getLatitude() + "','"
            + currentLocation.getLongitude() + "','" + currentLocation.getAltitude() + "','"
            + metadata.getTimeStamp() + "','" + metadata.getSurveyID() + "','"
            + metadata.getTripID() + "','" + (metadata.getMaleCount() + metadata.getFemaleCount())
            + "','" + metadata.getFemaleCount() + "','" + metadata.getMaleCount() + "','"
            + metadata.getSpeed() + "');";
        break;
    }

    return query;
  }

  /**
   * Helper method to get the string query to update a key-value pair given the rowID of the existing table entry.
   *
   * @param rowID
   * @param key
   * @param value
   * @return
   */
  private String getUpdateQueryGivenRow(int rowID, String key, String value) {
    String query = null;
    if (value.isEmpty()) {
      return query;
    }

    switch (type) {
      case TRACKER:
        query = "UPDATE " + ProjectConfig.get().getTrackerTableID() + " SET " + key + " = '"
            + value + "' WHERE ROWID = '" + rowID + "'";
        break;
      case SURVEY:
        query = "UPDATE " + ProjectConfig.get().getSurveyUploadTableID() + " SET " + key + " = '"
            + value + "' WHERE ROWID = '" + rowID + "'";
        break;
    }

    return query;
  }

  /**
   * Helper method to send a fusion table query.
   *
   * @param query
   * @throws IOException
   */
  private void sendQuery(String query) throws IOException {
    Fusiontables.Query.Sql sql = IniconfigActivity.fusiontables.query().sql(query);
    sql.setKey(ProjectConfig.get().getApiKey());
    sql.execute();
  }

  public enum Type {SURVEY, TRACKER}
}
