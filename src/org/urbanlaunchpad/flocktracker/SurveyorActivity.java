package org.urbanlaunchpad.flocktracker;

import android.annotation.SuppressLint;
import android.app.*;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.*;

import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;
import dagger.ObjectGraph;
import org.urbanlaunchpad.flocktracker.adapters.DrawerListViewAdapter;
import org.urbanlaunchpad.flocktracker.controllers.*;
import org.urbanlaunchpad.flocktracker.fragments.HubPageFragment;
import org.urbanlaunchpad.flocktracker.fragments.StatisticsPageFragment;
import org.urbanlaunchpad.flocktracker.helpers.*;
import org.urbanlaunchpad.flocktracker.menu.RowItem;
import org.urbanlaunchpad.flocktracker.models.Metadata;
import org.urbanlaunchpad.flocktracker.models.Question;
import org.urbanlaunchpad.flocktracker.util.LocationUtil;
import org.urbanlaunchpad.flocktracker.util.StringUtil;

import javax.inject.Inject;
import java.io.File;
import java.io.FileOutputStream;
import java.util.*;

public class SurveyorActivity extends Activity {

	public static final Integer INCOMPLETE_CHAPTER = R.drawable.complete_red;
	public static final Integer COMPLETE_CHAPTER = R.drawable.complete_green;
	public static final Integer HALF_COMPLETE_CHAPTER = R.drawable.complete_orange;

	public static GoogleDriveHelper driveHelper;
  private ObjectGraph objectGraph;

	// Stored queues of surveys to submit
	public static final int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;

	// Drawer fields
	private DrawerLayout drawerLayout;
	private ListView fixedNavigationList;
	private ListView chapterDrawerList;
	private LinearLayout drawer;
	private ActionBarDrawerToggle chapterDrawerToggle;
	private CharSequence chapterDrawerTitle;
	private CharSequence title;
	private List<RowItem> rowItems;
  private HubPageFragment hubPageFragment;

  @Inject QuestionController questionController;
  @Inject HubPageController hubPageController;
  @Inject StatisticsPageController statisticsPageController;
  @Inject LocationController locationController;
  @Inject Metadata metadata;
  @Inject Bus eventBus;
  @Inject TrackerAlarm trackerAlarm;

	@SuppressLint("HandlerLeak")
	private Handler messageHandler = new Handler() {

		@SuppressWarnings("deprecation")
		public void handleMessage(Message msg) {
			if (msg.what == EVENT_TYPE.SUBMITTED_SURVEY.ordinal()) {
				Toast toast = Toast.makeText(getApplicationContext(),
						getResources().getString(R.string.survey_submitted),
						Toast.LENGTH_SHORT);
				toast.show();
//				surveyHelper.updateSurveyPosition(
//						SurveyHelper.HUB_PAGE_CHAPTER_POSITION,
//						SurveyHelper.HUB_PAGE_QUESTION_POSITION);
				showHubPage();
			} else if (msg.what == EVENT_TYPE.SUBMIT_FAILED.ordinal()) {
				Toast toast = Toast.makeText(getApplicationContext(),
						getResources().getString(R.string.submit_failed),
						Toast.LENGTH_SHORT);
				toast.show();
			}
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_surveyor);

    driveHelper = new GoogleDriveHelper(this);

    objectGraph = ObjectGraph.create(new FlocktrackerModule(this));
    objectGraph.inject(this);
    eventBus.register(this);

    // Check for location services.
    LocationUtil.checkLocationConfig(this);

    hubPageFragment = new HubPageFragment(hubPageController, metadata.getMaleCount(), metadata.getFemaleCount());

		// Navigation drawer information.
		title = chapterDrawerTitle = getTitle();
		drawerLayout = (DrawerLayout) findViewById(R.id.chapter_drawer_layout);
		chapterDrawerList = (ListView) findViewById(R.id.chapter_drawer);
		fixedNavigationList = (ListView) findViewById(R.id.fixed_navigation);
		drawer = (LinearLayout) findViewById(R.id.drawer);
		rowItems = new ArrayList<RowItem>();
		for (String chapterTitle : questionController.getChapterTitles()) {
			RowItem rowItem = new RowItem(INCOMPLETE_CHAPTER, chapterTitle);
			rowItems.add(rowItem);
		}

		// set a custom shadow that overlays the main content when the drawer
		// opens
		drawerLayout.setDrawerShadow(R.drawable.drawer_shadow,
        GravityCompat.START);
		// set up the drawer's list view with items and click listener
		fixedNavigationList.setAdapter((new ArrayAdapter<String>(this,
        R.layout.old_chapter_list_item, new String[]{
        getString(R.string.hub_page_title),
        getString(R.string.statistics_page_title)}
    )));
		fixedNavigationList
				.setOnItemClickListener(new FixedNavigationItemClickListener());
		chapterDrawerList.setAdapter(new DrawerListViewAdapter(this,
        R.layout.chapter_drawer_list_item, rowItems));
		chapterDrawerList.setOnItemClickListener(new DrawerItemClickListener());
		getActionBar().setDisplayHomeAsUpEnabled(true);
		getActionBar().setHomeButtonEnabled(true);
		chapterDrawerToggle = new ActionBarDrawerToggle(this, /* host Activity */
        drawerLayout, /* DrawerLayout object */
		R.drawable.ic_drawer, /* nav drawer image to replace 'Up' caret */
		R.string.chapter_drawer_open, /* For accessibility */
		R.string.chapter_drawer_close /* For accessibility */) {
			public void onDrawerClosed(View view) {
				getActionBar().setTitle(title);
			}

			public void onDrawerOpened(View drawerView) {
				getActionBar().setTitle(chapterDrawerTitle);
			}
		};

		drawerLayout.setDrawerListener(chapterDrawerToggle);

		if (savedInstanceState == null) {
			showHubPage();
		}
	}

	/*
	 * Activity Lifecycle Handlers
	 */

	@Override
	protected void onDestroy() {
		questionController.resetTrip();
    locationController.stopTrip();
		super.onDestroy();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		// No call for super(). Bug on API Level > 11.
	}

  @Override
  protected void onResume() {
    super.onResume();
    registerReceiver(trackerAlarm, new IntentFilter(TrackerAlarm.TAG));
  }

  @Override
  protected void onPause() {
    super.onPause();
    unregisterReceiver(trackerAlarm);
  }

  @Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		// Sync the toggle state after onRestoreInstanceState has occurred.
		chapterDrawerToggle.syncState();
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		// Pass any configuration change to the drawer toggles
		chapterDrawerToggle.onConfigurationChanged(newConfig);
	}

//	@Override
//	public void onBackPressed() {
//		if (isTripStarted) {
//			if (surveyHelper.prevTrackingPositions.empty()
//					|| surveyHelper.getTrackerQuestionPosition() == 0) {
//				surveyHelper
//						.updateTrackerPositionOnBack(SurveyHelper.HUB_PAGE_QUESTION_POSITION);
//				showHubPage();
//				return;
//			}
//			// Pop last question off
//			Integer prevPosition = surveyHelper.prevTrackingPositions.pop();
//			surveyHelper.updateTrackerPositionOnBack(prevPosition);
//			showCurrentQuestion();
//			return;
//		}
//
//		if (surveyHelper.prevPositions.isEmpty()) {
//			finish();
//			return;
//		}
//
//		Tuple prevPosition = surveyHelper.prevPositions.pop();
//		// Pop last question off
//		surveyHelper.updateSurveyPositionOnBack(prevPosition.chapterPosition,
//				prevPosition.questionPosition);
//
//		if (surveyHelper.wasJustAtHubPage(prevPosition)) {
//			showHubPage();
//			return;
//		} else if (surveyHelper.wasJustAtStatsPage(prevPosition)) {
//			showStatusPage();
//			return;
//		}
//
//		showCurrentQuestion();
//		return;
//	}


	@Override
	protected void onActivityResult(int requestCode, int resultCode,
			Intent intent) {

		// Choose what to do based on the request code
		switch (requestCode) {

		case GoogleDriveHelper.REQUEST_ACCOUNT_PICKER:
			if (resultCode == RESULT_OK && intent != null
					&& intent.getExtras() != null) {
				driveHelper.requestAccountPicker(intent);
			}
			break;
		case GoogleDriveHelper.REQUEST_AUTHORIZATION:
			if (resultCode == Activity.RESULT_OK) {
				if (questionController.isAskingTripQuestions()) {
					ArrayList<Integer> key = new ArrayList<Integer>(
							Arrays.asList(
									questionController.getCurrentQuestion().getQuestionNumber(), -1,
									-1));
//					SurveyHelper.prevTrackerImages
//							.put(key, driveHelper.fileUri);
				} else {
          Question question = questionController.getCurrentQuestion();
					ArrayList<Integer> key = new ArrayList<Integer>(
							Arrays.asList(question.getChapter().getChapterNumber(),
                  question.getQuestionNumber(), -1, -1));
//					SurveyHelper.prevImages.put(key, driveHelper.fileUri);
				}
//				currentQuestionFragment.ImageLayout();
			} else {
				startActivityForResult(
						IniconfigActivity.credential.newChooseAccountIntent(),
						GoogleDriveHelper.REQUEST_ACCOUNT_PICKER);
			}
			break;
		case GoogleDriveHelper.CAPTURE_IMAGE:
			try {
				Bitmap imageBitmap = BitmapFactory.decodeFile(
						driveHelper.fileUri.getPath(), null);
				float rotation = ImageHelper.rotationForImage(Uri
						.fromFile(new File(driveHelper.fileUri.getPath())));
				if (rotation != 0) {
					Matrix matrix = new Matrix();
					matrix.preRotate(rotation);
					imageBitmap = Bitmap.createBitmap(imageBitmap, 0, 0,
							imageBitmap.getWidth(), imageBitmap.getHeight(),
							matrix, true);
				}

				imageBitmap.compress(CompressFormat.JPEG, 25,
						new FileOutputStream(driveHelper.fileUri.getPath()));
			} catch (Exception e) {
				e.printStackTrace();
			}

			if (resultCode == Activity.RESULT_OK) {
				if (questionController.isAskingTripQuestions()) {
					ArrayList<Integer> key = new ArrayList<Integer>(
							Arrays.asList(
                  questionController.getCurrentQuestion().getQuestionNumber(), -1,
									-1));
//					SurveyHelper.prevTrackerImages
//							.put(key, driveHelper.fileUri);
				} else {
          Question question = questionController.getCurrentQuestion();
          ArrayList<Integer> key = new ArrayList<Integer>(
              Arrays.asList(question.getChapter().getChapterNumber(),
                  question.getQuestionNumber(), -1, -1));
//					SurveyHelper.prevImages.put(key, driveHelper.fileUri);
				}
//				currentQuestionFragment.ImageLayout();
			}
			break;

		// If the request code matches the code sent in onConnectionFailed
		case CONNECTION_FAILURE_RESOLUTION_REQUEST:

			switch (resultCode) {
			// If Google Play services resolved the problem
			case Activity.RESULT_OK:

				// Log the result
				Log.d("Location", "Resolved connection");
				break;

			// If any other result was returned by Google Play services
			default:
				// Log the result
				Log.e("Location", "Could not resolve connection");

				break;
			}

			// If any other request code was received
		default:
			// Report that this Activity received an unknown requestCode
			Log.e("SurveyorActivity activity", "unknown request code");
			break;
		}
	}

	/*
	 * Drawer Logic
	 */

	public boolean onOptionsItemSelected(MenuItem item) {
		// To make the action bar home/up action should open or close the
		// drawer.
		chapterDrawerToggle.onOptionsItemSelected(item);
		return true;
	}

	private void showHubPage() {
		// Update fragments
		FragmentManager fragmentManager = getFragmentManager();
		FragmentTransaction transaction = fragmentManager.beginTransaction();
		transaction.replace(R.id.surveyor_frame, hubPageFragment);
		transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
		transaction.addToBackStack(null);
		transaction.commit();

		// update selected item and title, then close the drawer.
		fixedNavigationList.setItemChecked(0, true);
		chapterDrawerList.setItemChecked(-1, true);
		setTitle(getString(R.string.hub_page_title));
	}

	private void showStatusPage() {
		FragmentManager fragmentManager = getFragmentManager();
		Fragment fragment = new StatisticsPageFragment();

		FragmentTransaction transaction = fragmentManager.beginTransaction();
		transaction.replace(R.id.surveyor_frame, fragment);
		transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
		transaction.addToBackStack(null);
		transaction.commit();

		// update selected item and title, then close the drawer.
		fixedNavigationList.setItemChecked(1, true);
		chapterDrawerList.setItemChecked(-1, true);
		setTitle(getString(R.string.statistics_page_title));
		drawerLayout.closeDrawer(drawer);
	}

	/*
	 * Displaying different pages
	 */

	@Override
	public void setTitle(CharSequence title) {
		this.title = title;
		getActionBar().setTitle(this.title);
	}

	/*
	 * Hub Page Event Handlers
	 */



	public void stopTripDialog() {
		Builder dialog;
		dialog = new AlertDialog.Builder(this);
		dialog.setMessage(this.getResources().getString(
				R.string.stop_tracker_message));
		dialog.setPositiveButton(this.getResources().getString(R.string.yes),
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface paramDialogInterface,
							int paramInt) {
						stopTrip();
					}
				});
		dialog.setNegativeButton(this.getString(R.string.no),
				new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface paramDialogInterface,
							int paramInt) {
						// Nothing happens.
					}
				});
		dialog.show();
	}

	/*
	 * Location tracking helper
	 */

	public void stopTrip() {
    locationController.stopTrip();
		messageHandler.sendEmptyMessage(EVENT_TYPE.UPDATE_HUB_PAGE.ordinal());
		questionController.resetTrip();
		statisticsPageController.stopTrip();
	}


  @Subscribe
  public void handleStatisticsRequest(HubPageFragment.RequestStatisticsEvent event) {
    showStatusPage();
  }

  @Subscribe
  public void startSurvey(HubPageFragment.RequestStartSurveyEvent event) {
    metadata.setSurveyID("S" + StringUtil.createID());
    questionController.startSurvey();
    fixedNavigationList.setItemChecked(-1, true);
    chapterDrawerList.setItemChecked(0, true);
  }

  @Subscribe
  public void onToggleTrip(HubPageFragment.RequestToggleTripEvent event) {
    if (metadata.getTripID() == null) {
      metadata.setTripID("T" + StringUtil.createID());
      questionController.askTripQuestions();
    } else {
      locationController.stopTrip();
      metadata.setTripID(null);
      metadata.setCurrentLocation(null);
      statisticsPageController.stopTrip();
      hubPageFragment.stopTrip();
    }
  }

  @Subscribe
  public void onReachedEndOfTrackerSurvey(QuestionController.ReachedEndOfTrackerSurveyEvent event) {
    showHubPage();
    locationController.startTrip();
    statisticsPageController.startTrip();
  }

  private enum EVENT_TYPE {
		MALE_UPDATE, FEMALE_UPDATE, UPDATE_STATS_PAGE, UPDATE_HUB_PAGE, SHOW_NAV_BUTTONS, SUBMITTED_SURVEY, SUBMIT_FAILED
	}

	private class FixedNavigationItemClickListener implements
			ListView.OnItemClickListener {

		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position,
				long id) {
			Log.d("Clicked on fixed position", position + "");
			if (questionController.isAskingTripQuestions()) {
				questionController.resetTrip();
			}

      questionController.updateSurveyPosition(0, 0);
      if (position == 0) {
				showHubPage();
			} else {
				showStatusPage();
			}
      drawerLayout.closeDrawer(drawer);
    }
	}

	private class DrawerItemClickListener implements
			ListView.OnItemClickListener {

		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position,
				long id) {
			Log.d("Clicked on drawer position", position + "");
			if (questionController.isAskingTripQuestions()) {
				questionController.resetTrip();
			}
			questionController.updateSurveyPosition(position, 0);
			questionController.showCurrentQuestion();
      fixedNavigationList.setItemChecked(-1, true);
      drawerLayout.closeDrawer(drawer);
		}
	}

  public ObjectGraph getObjectGraph() {
    return objectGraph;
  }

}
