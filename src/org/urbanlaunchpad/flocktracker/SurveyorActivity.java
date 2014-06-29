package org.urbanlaunchpad.flocktracker;

import android.annotation.SuppressLint;
import android.app.*;
import android.app.AlertDialog.Builder;
import android.content.Context;
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
    hubPageController.setFragment(hubPageFragment);

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
		cancelTracker();
		questionController.resetTrip();
    locationController.disconnect();
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

	public void saveSurvey() {
//		// connect if not tracking
//		if (!mLocationClient.isConnected()) {
//			mLocationClient.connect();
//		}
//		if (mLocationClient.isConnected()) {
//			new Thread(new Runnable() {
//				public void run() {
//					String jsurvString = surveyHelper.jsurv.toString();
//					JSONObject imagePaths = new JSONObject();
//					for (ArrayList<Integer> key : SurveyHelper.prevImages
//							.keySet()) {
//						try {
//							imagePaths.put(key.toString(),
//									SurveyHelper.prevImages.get(key).getPath());
//						} catch (JSONException e) {
//							e.printStackTrace();
//						}
//					}
//					resetSurvey();
//					surveyHelper.jumpString = null;
//
//					// save location tagged survey
//					surveyHelper.saveSubmission(
//							mLocationClient.getLastLocation(), surveyID,
//							tripID, jsurvString, imagePaths, SURVEY_TYPE,
//							maleCount.toString(), femaleCount.toString(),
//							((Integer) (maleCount + femaleCount)).toString(),
//							statisticsPageController.getSpeed().toString());
//					// disconnect if not tracking or not currently
//					// submitting
//					// surveys
//					if (!isTripStarted && !submittingSubmission) {
//						mLocationClient.disconnect();
//					}
//
//					statisticsPageController.surveysCompleted++;
//				}
//			}).start();
//
//			messageHandler.sendEmptyMessage(EVENT_TYPE.SUBMITTED_SURVEY
//					.ordinal());
//		}
	}

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

	private void showCurrentQuestion() {
    questionController.showCurrentQuestion();
//
//		int chapterPosition;
//		int questionPosition;
//		int loopPosition;
//		Boolean inloop;
//		inloop = surveyHelper.inLoop;
//
//		JSONObject currentQuestionJsonObject = null;
//		String currentQuestion = null;
//
//		if (surveyHelper.inLoop) {
//			loopPosition = surveyHelper.getLoopPosition();
//		}
//
//		if (questionController.isAskingTripQuestions()) {
//			chapterPosition = 0;
//			questionPosition = surveyHelper.getChapterPosition();
//
//			// get current trip question
//			try {
//				currentQuestionJsonObject = surveyHelper
//						.getCurrentTripQuestion();
//			} catch (JSONException e) {
//				e.printStackTrace();
//			}
//
//			// Hide submit
//			navButtons.getView().findViewById(R.id.submit_survey_button)
//					.setVisibility(View.INVISIBLE);
//		} else {
//			chapterPosition = surveyHelper.getChapterPosition();
//			questionPosition = surveyHelper.getQuestionPosition();
//
//			// update selected item and title, then close the drawer.
//			fixedNavigationList.setItemChecked(-1, true);
//			chapterDrawerList.setItemChecked(chapterPosition, true);
//			setTitle(surveyHelper.getChapterTitles()[chapterPosition]);
//			drawerLayout.closeDrawer(drawer);
//
//			// Get current question
//			try {
//				currentQuestionJsonObject = surveyHelper.getCurrentQuestion();
//			} catch (JSONException e) {
//				e.printStackTrace();
//			}
//		}
//
//		// If in loop, putting the answer of the current iteration on its place.
//		if (surveyHelper.inLoop) {
//			try {
//				currentQuestionJsonObject.put("Answer",
//						currentQuestionJsonObject.getJSONArray("LoopAnswers")
//								.get(surveyHelper.loopIteration).toString());
//			} catch (JSONException e) {
//				// e.printStackTrace();
//			}
//		}
//
//		currentQuestion = currentQuestionJsonObject.toString();
//
//		// Starting question fragment and passing json question information.
//		currentQuestionFragment = new QuestionFragment(surveyHelper.getCurrentQuestion());
//		Bundle args = new Bundle();
//		args.putString(QuestionFragment.ARG_JSON_QUESTION,
//				question.getQuestionText());
//		args.putInt(QuestionFragment.ARG_CHAPTER_POSITION, question.getChapter().getChapterNumber());
//		args.putInt(QuestionFragment.ARG_QUESTION_POSITION, question.getQuestionNumber());

//		FragmentManager fragmentManager = getFragmentManager();
//		FragmentTransaction transaction = fragmentManager.beginTransaction();

		// show navigation buttons and add new question
//		if (!navButtons.isVisible()) {
//			messageHandler.sendEmptyMessage(EVENT_TYPE.SHOW_NAV_BUTTONS
//					.ordinal());
//		}
//
//		// selectively show previous question button
//		if ((questionController.isAskingTripQuestions() && surveyHelper.getTrackerQuestionPosition() == 0)
//				|| (!questionController.isAskingTripQuestions() && questionPosition == 0)) {
//			navButtons.getView().findViewById(R.id.previous_question_button)
//					.setVisibility(View.INVISIBLE);
//		} else {
//			navButtons.getView().findViewById(R.id.previous_question_button)
//					.setVisibility(View.VISIBLE);
//		}

//		transaction.replace(R.id.surveyor_frame, currentQuestionFragment);
//		transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
//		if (!questionController.isAskingTripQuestions()) {
//			transaction.addToBackStack(null);
//		}
//		transaction.commit();
	}

	/*
	 * Displaying different pages
	 */

	@Override
	public void setTitle(CharSequence title) {
		this.title = title;
		getActionBar().setTitle(this.title);
	}

//
//	/*
//	 * Callback Handlers for Connecting to Google Play (Authentication)
//	 */
//
//	public void NavButtonPressed(NavButtonType type) {
//		switch (type) {
//
//		case PREVIOUS:
//			currentQuestionFragment.saveState();
//			surveyHelper.onPrevQuestionPressed(questionController.isAskingTripQuestions());
//			onBackPressed();
//			break;
//		case NEXT:
//			currentQuestionFragment.saveState();
//			NextQuestionResult result = surveyHelper
//					.onNextQuestionPressed(questionController.isAskingTripQuestions());
//
//			if (questionController.isAskingTripQuestions()) {
//				if (result == NextQuestionResult.END) {
//					questionController.isAskingTripQuestions() = false;
//					surveyHelper.updateSurveyPosition(
//							SurveyHelper.HUB_PAGE_CHAPTER_POSITION,
//							SurveyHelper.HUB_PAGE_QUESTION_POSITION);
//					surveyHelper.prevTrackingPositions = new Stack<Integer>();
//					showHubPage();
//					askTripQuestions();
//					statisticsPageController.askTripQuestions();
//					startTracker();
//					break;
//				}
//			} else {
//				if (result == NextQuestionResult.CHAPTER_END) {
//					rowItems.get(surveyHelper.getChapterPosition() - 1)
//							.setImageId(COMPLETE_CHAPTER);
//				} else if (result == NextQuestionResult.END) {
//					Toast.makeText(this, R.string.end_of_survey,
//							Toast.LENGTH_SHORT).show();
//					submitSurveyInterface();
//					break;
//				}
//
//				RowItem rowItem = rowItems.get(surveyHelper
//						.getChapterPosition());
//				if (rowItem.getImageId() != COMPLETE_CHAPTER) {
//					rowItem.setImageId(HALF_COMPLETE_CHAPTER);
//				}
//			}
//			showCurrentQuestion();
//			break;
//		case SUBMIT:
//			currentQuestionFragment.saveState();
//			submitSurveyInterface();
//			break;
//		}
//	}

	public void AnswerRecieve(String answerStringReceive,
			String jumpStringReceive, ArrayList<Integer> selectedAnswers,
			Boolean inLoopReceive, String questionkindReceive,
			ArrayList<Integer> questionkey) {
		// TODO: fix loop stuff

//		if (questionkindReceive.equals("LP") && (answerStringReceive != null)) {
//			Integer receivedLoopTotal = null;
//			Integer currentLoopTotal = surveyHelper.getCurrentLoopTotal();
//			if (!answerStringReceive.equals("")) {
//				receivedLoopTotal = Integer.parseInt(answerStringReceive);
//				if ((currentLoopTotal != receivedLoopTotal)
//						|| (receivedLoopTotal == 0)) {
//					surveyHelper.clearLoopAnswerHashMap(questionkey.get(0),questionkey.get(1), questionController.isAskingTripQuestions());
//					surveyHelper.loopTotal = receivedLoopTotal;
//					surveyHelper.updateLoopLimit();
//					surveyHelper.initializeLoop();
//					if (!questionController.isAskingTripQuestions()) {
//						surveyHelper.answerCurrentQuestion(answerStringReceive,
//								selectedAnswers);
//					} else {
//						surveyHelper.answerCurrentTrackerQuestion(
//								answerStringReceive, selectedAnswers);
//					}
//				}
//			}
//
//		} else if ((answerStringReceive != null)) {
//			if (!questionController.isAskingTripQuestions()) {
//				surveyHelper.answerCurrentQuestion(answerStringReceive,
//						selectedAnswers);
//			} else {
//				surveyHelper.answerCurrentTrackerQuestion(answerStringReceive,
//						selectedAnswers);
//			}
//		}
//
//		if (jumpStringReceive != null) {
//			surveyHelper.updateJumpString(jumpStringReceive);
//		}
	}

//	@Override
//	public void updateStatusPage() {
//		// hide navigation buttons
//		FragmentManager fragmentManager = getFragmentManager();

//		FragmentTransaction transactionHide = fragmentManager
//				.beginTransaction();
//		transactionHide.hide(navButtons);
//		transactionHide.commit();

//		messageHandler.sendEmptyMessage(EVENT_TYPE.UPDATE_STATS_PAGE.ordinal());
//	}

	/*
	 * Question Navigation Event Handlers
	 */

	/*
	 * Question Event Handlers
	 */

//	@Override
//	public void HubButtonPressed(HubButtonType type) {
//		switch (type) {
//		case UPDATE_PAGE:
//			showingHubPage = true;
//			messageHandler.sendEmptyMessage(EVENT_TYPE.UPDATE_HUB_PAGE
//					.ordinal());
//			break;
//		case TOGGLETRIP:
//			if (isTripStarted) {
//				// Update status page info
//				stopTripDialog();
//			} else {
//				surveyHelper.resetTracker();
//				questionController.isAskingTripQuestions() = true;
//
//				// Starting question fragment and passing json question
//				// information.
//				surveyHelper.updateTrackerPosition(0);
//				showCurrentQuestion();
//			}
//			break;
//		case NEWSURVEY:
//			surveyHelper.updateSurveyPosition(0, 0);
//			surveyHelper.inLoop = false;
//			surveyHelper.loopIteration = -1;
//			surveyHelper.loopPosition = -1;
//			showCurrentQuestion();
//			break;
//		case STATISTICS:
//			if (questionController.isAskingTripQuestions()) {
//				surveyHelper
//						.updateTrackerPosition(SurveyHelper.STATS_PAGE_QUESTION_POSITION);
//			} else {
//				surveyHelper.updateSurveyPosition(
//						SurveyHelper.STATS_PAGE_CHAPTER_POSITION,
//						SurveyHelper.STATS_PAGE_QUESTION_POSITION);
//			}
//			showStatusPage();
//			break;
//		case FEWERMEN:
//			if (maleCount > 0) {
//				maleCount--;
//				updateCount("male");
//			}
//			break;
//		case FEWERWOMEN:
//			if (femaleCount > 0) {
//				femaleCount--;
//				updateCount("female");
//			}
//			break;
//		case MOREMEN:
//			maleCount++;
//			updateCount("male");
//			break;
//		case MOREWOMEN:
//			femaleCount++;
//			updateCount("female");
//			break;
//		default:
//			break;
//		}
//	}

	/*
	 * Status Page Event Handlers
	 */

	public void updateCount(String gender) {
		if (gender.equals("male")) {
			messageHandler.sendEmptyMessage(EVENT_TYPE.MALE_UPDATE.ordinal());
		} else if (gender.equals("female")) {
			messageHandler.sendEmptyMessage(EVENT_TYPE.FEMALE_UPDATE.ordinal());
		}
	}

	/*
	 * Hub Page Event Handlers
	 */

	public void startTracker() {
		AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
		Intent intentAlarm = new Intent(TrackerAlarm.TAG);
		PendingIntent pi = PendingIntent.getBroadcast(this, 1, intentAlarm,
				PendingIntent.FLAG_UPDATE_CURRENT);
		alarmManager.setRepeating(AlarmManager.RTC_WAKEUP,
				System.currentTimeMillis(), TrackerAlarm.TRACKER_INTERVAL, pi);
		Log.d("TrackerAlarm", "TrackerAlarm working.");
	}

	public void cancelTracker() {
		Log.d("TrackerAlarm", "Cancelling tracker");

		Intent intentAlarm = new Intent(this, TrackerAlarm.class);
		PendingIntent sender = PendingIntent.getBroadcast(this, 1, intentAlarm,
				PendingIntent.FLAG_UPDATE_CURRENT);
		AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

		alarmManager.cancel(sender);
	}

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
    locationController.disconnect();
		cancelTracker();
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
      cancelTracker();
      locationController.disconnect();
      metadata.setTripID(null);
      metadata.setCurrentLocation(null);
      statisticsPageController.stopTrip();
      hubPageFragment.stopTrip();
    }
  }

  @Subscribe
  public void onReachedEndOfTrackerSurvey(QuestionController.ReachedEndOfTrackerSurveyEvent event) {
    startTracker();
    showHubPage();
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
			showCurrentQuestion();
      fixedNavigationList.setItemChecked(-1, true);
      drawerLayout.closeDrawer(drawer);
		}
	}

  public ObjectGraph getObjectGraph() {
    return objectGraph;
  }

}
