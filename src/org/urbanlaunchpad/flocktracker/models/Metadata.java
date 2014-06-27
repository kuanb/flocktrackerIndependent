package org.urbanlaunchpad.flocktracker.models;

import android.location.Location;

public class Metadata {
  private String timeStamp;
  private String surveyID;
  private String tripID;
  private String imagePaths;
  private Location location;
  private int maleCount;
  private int femaleCount;
  private double speed;

  public Metadata() {
  }

  public Metadata(String timeStamp, String surveyID, String tripID,
      String imagePaths, Location location, int maleCount, int femaleCount, double speed) {
    this.timeStamp = timeStamp;
    this.surveyID = surveyID;
    this.tripID = tripID;
    this.imagePaths = imagePaths;
    this.location = location;
    this.maleCount = maleCount;
    this.femaleCount = femaleCount;
    this.speed = speed;
  }

  public String getTimeStamp() {
    return timeStamp;
  }

  public void setTimeStamp(String timeStamp) {
    this.timeStamp = timeStamp;
  }

  public String getSurveyID() {
    return surveyID;
  }

  public void setSurveyID(String surveyID) {
    this.surveyID = surveyID;
  }

  public String getTripID() {
    return tripID;
  }

  public void setTripID(String tripID) {
    this.tripID = tripID;
  }

  public String getImagePaths() {
    return imagePaths;
  }

  public void setImagePaths(String imagePaths) {
    this.imagePaths = imagePaths;
  }

  public Location getCurrentLocation() {
    return location;
  }

  public void setCurrentLocation(Location location) {
    this.location = location;
  }

  public int getMaleCount() {
    return maleCount;
  }

  public void setMaleCount(int maleCount) {
    this.maleCount = maleCount;
  }

  public int getFemaleCount() {
    return femaleCount;
  }

  public void setFemaleCount(int femaleCount) {
    this.femaleCount = femaleCount;
  }

  public double getSpeed() {
    return speed;
  }

  public void setSpeed(double speed) {
    this.speed = speed;
  }

  @Override
  protected Object clone() throws CloneNotSupportedException {
    return new Metadata(timeStamp, surveyID, tripID, imagePaths, location, maleCount, femaleCount, speed);
  }
}
