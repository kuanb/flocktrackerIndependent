package org.urbanlaunchpad.flocktracker.controllers;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.support.v4.app.ActionBarDrawerToggle;
import android.view.MenuItem;
import android.view.View;
import com.squareup.otto.Bus;
import org.urbanlaunchpad.flocktracker.R;
import org.urbanlaunchpad.flocktracker.menu.RowItem;
import org.urbanlaunchpad.flocktracker.views.DrawerView;

import javax.inject.Inject;
import java.util.ArrayList;

public class DrawerController {
  private static final Integer INCOMPLETE_CHAPTER = R.drawable.complete_red;
  private static final Integer COMPLETE_CHAPTER = R.drawable.complete_green;
  private static final Integer HALF_COMPLETE_CHAPTER = R.drawable.complete_orange;

  private Context context;
  private ActionBar actionBar;
  private ActionBarDrawerToggle actionBarDrawerToggle;
  private DrawerView drawerView;
  private QuestionController questionController;
  private CharSequence title;
  private CharSequence drawerTitle;

  @Inject
  public DrawerController(Context context, QuestionController questionController, Bus eventBus) {
    this.context = context;
    this.drawerView = (DrawerView) ((Activity) context).findViewById(R.id.chapter_drawer_layout);
    this.questionController = questionController;
    this.actionBar = ((Activity) context).getActionBar();
    this.title = ((Activity) context).getTitle();
    this.drawerTitle = title.toString();

    ArrayList<RowItem> rowItems = new ArrayList<RowItem>();
    for (String chapterTitle : questionController.getChapterTitles()) {
      RowItem rowItem = new RowItem(INCOMPLETE_CHAPTER, chapterTitle);
      rowItems.add(rowItem);
    }

    drawerView.init(eventBus, rowItems);
    actionBarDrawerToggle = new ActionBarDrawerToggle((Activity) context, /* host Activity */
        drawerView,
        R.drawable.ic_drawer, /* nav drawer image to replace 'Up' caret */
        R.string.chapter_drawer_open, /* For accessibility */
        R.string.chapter_drawer_close /* For accessibility */) {
      public void onDrawerClosed(View view) {
        setTitle(title);
      }

      public void onDrawerOpened(View drawerView) {
        setTitle(drawerTitle);
      }
    };

    drawerView.setDrawerListener(actionBarDrawerToggle);
  }

  public void showHubPage() {
    drawerView.showHubPage();
    setTitle(context.getString(R.string.hub_page_title));
  }

  public void showStatisticsPage() {
    drawerView.showStatisticsPage();
    setTitle(context.getString(R.string.statistics_page_title));
  }

  public void selectSurveyChapter(int chapterIndex) {
    drawerView.selectSurveyChapter(chapterIndex);
    setTitle(questionController.getChapterTitles()[chapterIndex]);
  }

  private void setTitle(CharSequence title) {
    this.title = title;
    actionBar.setTitle(title);
  }

  public void onConfigurationChanged(Configuration newConfig) {
    actionBarDrawerToggle.onConfigurationChanged(newConfig);
  }

  public void onPostCreate() {
    actionBarDrawerToggle.syncState();
  }

  public void onOptionsItemSelected(MenuItem menuItem) {
    actionBarDrawerToggle.onOptionsItemSelected(menuItem);
  }

}
