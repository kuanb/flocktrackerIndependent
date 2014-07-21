package org.urbanlaunchpad.flocktracker.views;

import android.content.Context;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import com.squareup.otto.Bus;
import org.urbanlaunchpad.flocktracker.CommonEvents;
import org.urbanlaunchpad.flocktracker.R;
import org.urbanlaunchpad.flocktracker.adapters.DrawerListViewAdapter;
import org.urbanlaunchpad.flocktracker.menu.RowItem;

import java.util.List;

public class DrawerView extends DrawerLayout {
  private ListView chapterListView;
  private ListView fixedNavigationListView;
  private Bus eventBus;

  private SelectChapterEvent selectChapterEvent = new SelectChapterEvent(-1);

  public DrawerView(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  @Override
  protected void onFinishInflate() {
    super.onFinishInflate();
    chapterListView = (ListView) findViewById(R.id.chapter_drawer);
    fixedNavigationListView = (ListView) findViewById(R.id.fixed_navigation);

    // set a custom shadow that overlays the main content when the drawer opens
    setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);

    // set up the drawer's list view with items and click listener
    fixedNavigationListView.setAdapter((new ArrayAdapter<String>(getContext(),
        R.layout.old_chapter_list_item, new String[]{
        getContext().getString(R.string.hub_page_title),
        getContext().getString(R.string.statistics_page_title)}
    )));

    fixedNavigationListView
        .setOnItemClickListener(new FixedNavigationItemClickListener());

    chapterListView.setOnItemClickListener(new DrawerItemClickListener());
  }

  public void init(Bus eventBus, List<RowItem> rowItems) {
    this.eventBus = eventBus;
    chapterListView.setAdapter(new DrawerListViewAdapter(getContext(), R.layout.chapter_drawer_list_item, rowItems));
  }

  public void selectSurveyChapter(int chapterIndex) {
    fixedNavigationListView.setItemChecked(-1, true);
    chapterListView.setItemChecked(chapterIndex, true);
    closeDrawers();
  }

  public void showHubPage() {
    fixedNavigationListView.setItemChecked(0, true);
    chapterListView.setItemChecked(-1, true);
    closeDrawers();
  }

  public void showStatisticsPage() {
    fixedNavigationListView.setItemChecked(1, true);
    chapterListView.setItemChecked(-1, true);
    closeDrawers();
  }

  private class FixedNavigationItemClickListener implements
      ListView.OnItemClickListener {

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position,
        long id) {
      Log.d("Clicked on fixed position", position + "");
      if (position == 0) {
        eventBus.post(CommonEvents.requestHubPageEvent);
      } else {
        eventBus.post(CommonEvents.requestStatisticsPageEvent);
      }
    }
  }

  private class DrawerItemClickListener implements
      ListView.OnItemClickListener {

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position,
        long id) {
      Log.d("Clicked on drawer position", position + "");
      selectChapterEvent.chapterNumber = position;
      eventBus.post(selectChapterEvent);
    }
  }

  public class SelectChapterEvent {
    public int chapterNumber;

    public SelectChapterEvent(int chapterNumber) {
      this.chapterNumber = chapterNumber;
    }
  }

}
