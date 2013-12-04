package org.urbanlaunchpad.flocktracker;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URLEncoder;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class Iniconfig extends Activity implements View.OnClickListener {

	TextView usernameField;
	TextView projectNameField;
	ImageView cont;
	EditText input;
	String projectName = "";
	String jsonsurveystring;
	JSONObject jsurv = null;
	private AccountManager accountManager;
	final String SCOPE = "https://www.googleapis.com/auth/fusiontables";
	private static final int AUTHORIZATION_CODE = 1993;
	private static final int ACCOUNT_CODE = 1601;
	private String token = null;
	private String username = "";
	AlertDialog.Builder alert;

	private enum EVENT_TYPE {
		GOT_USERNAME, GOT_PROJECT_NAME, PARSED_CORRECTLY, PARSED_INCORRECTLY, INPUT_NAME
	}

	@SuppressLint("HandlerLeak")
	private Handler messageHandler = new Handler() {

		public void handleMessage(Message msg) {
			if (msg.what == EVENT_TYPE.GOT_USERNAME.ordinal()) {
				// have input a name. update it on interface
				usernameField.setText(username);
			} else if (msg.what == EVENT_TYPE.GOT_PROJECT_NAME.ordinal()) {
				// have input a project name. update it on interface
				projectNameField.setText(projectName);
			} else if (msg.what == EVENT_TYPE.PARSED_CORRECTLY.ordinal()) {
				// got survey!
				Toast toast = Toast.makeText(getApplicationContext(),
						"survey parsed!", Toast.LENGTH_SHORT);
				toast.show();
			}  else if (msg.what == EVENT_TYPE.PARSED_INCORRECTLY.ordinal()) {
				// got bad/no survey!
				Toast toast = Toast.makeText(getApplicationContext(),
						"Could not get survey", Toast.LENGTH_SHORT);
				toast.show();
				jsurv = null;
			} else if (msg.what == EVENT_TYPE.INPUT_NAME.ordinal()) {
				input.setText(projectName);
				alert.setView(input);
				// want to display alert to get project name
				alert.show();
				InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
			} else {
				Log.e("Survey Parser", "Error parsing survey");
			}
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_iniconfig);
		accountManager = AccountManager.get(this);

		// initialize fields
		usernameField = (TextView) findViewById(R.id.usernameText);
		projectNameField = (TextView) findViewById(R.id.projectNameText);

		// initialize dialog for inputting project name
		alert = new AlertDialog.Builder(this);
		alert.setTitle("Select project");
		input = new EditText(this);
		alert.setView(input);

		// set listener for ok when user inputs project name
		alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				// save the project name
				projectName = input.getText().toString().trim();

				InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(input.getWindowToken(), 0);
				dialog.dismiss();

				if (!projectName.isEmpty()) {
					// update our interface with project name
					messageHandler.sendEmptyMessage(EVENT_TYPE.GOT_PROJECT_NAME
							.ordinal());

					// get and parse survey
					new Thread(new Runnable() {
						public void run() {
							try {
								jsonsurveystring = getSurvey(projectName);
								Log.v("response", jsonsurveystring);

								try {
									JSONObject array = new JSONObject(
											jsonsurveystring);
									String rows = array.getJSONArray("rows")
											.toString();
									String jsonRows = rows.substring(
											rows.indexOf("{"),
											rows.lastIndexOf("}") + 1);

									// properly format downloaded string
									jsonRows = jsonRows.replaceAll("\\\\n", "");
									jsonRows = jsonRows.replace("\\", "");
									Log.v("JSON Parser string", jsonRows);
									jsurv = new JSONObject(jsonRows);
									messageHandler
											.sendEmptyMessage(EVENT_TYPE.PARSED_CORRECTLY
													.ordinal());
								} catch (JSONException e) {
									Log.e("JSON Parser", "Error parsing data "
											+ e.toString());
									messageHandler
											.sendEmptyMessage(EVENT_TYPE.PARSED_INCORRECTLY
													.ordinal());
								}
							} catch (ClientProtocolException e1) {
								e1.printStackTrace();
							} catch (IOException e1) {
								e1.printStackTrace();
							}
						}
					}).start();
				}
			}
		});

		alert.setNegativeButton("Cancel",
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(input.getWindowToken(), 0);
						dialog.dismiss();
					}
				});

		// set listeners for rows and disable continue button
		View projectNameSelectRow = findViewById(R.id.projectNameRow);
		View usernameSelectRow = findViewById(R.id.usernameRow);
		cont = (ImageView) findViewById(R.id.bcontinue);
		usernameSelectRow.setOnClickListener(this);
		projectNameSelectRow.setOnClickListener(this);
		cont.setOnClickListener(this);
	}

	@Override
	public void onClick(View view) {
		Integer id = view.getId();

		if (id == R.id.usernameRow) {
			// Google credentials
			chooseAccount();
		} else if (id == R.id.projectNameRow) {
			if (username.isEmpty()) {
				Toast toast = Toast.makeText(getApplicationContext(),
						"Select user first please", Toast.LENGTH_SHORT);
				toast.show();
				return;
			}

			input = new EditText(this);
			alert.setView(input);

			// Show the popup dialog to get the project name
			messageHandler.sendEmptyMessage(EVENT_TYPE.INPUT_NAME.ordinal());
		} else if (id == R.id.bcontinue) {
			if (jsurv == null) {
				Toast toast = Toast.makeText(getApplicationContext(),
						"Invalid user/project!", Toast.LENGTH_SHORT);
				toast.show();
				return;
			}

			// Go to survey
			Intent i = new Intent(getApplicationContext(), Surveyor.class);
			i.putExtra("jsonsurvey", jsurv.toString());
			try {
				i.putExtra("token", token);
			} catch (Exception e) {
			}
			startActivity(i);
		}
	}

	/*
	 * Survey getting helper functions
	 */

	public String getSurvey(String tableId) throws ClientProtocolException,
			IOException {
		String MASTER_TABLE_ID = "1isCCC51fe6nWx27aYWKfZWmk9w2Zj6a4yTyQ5c4";
		String query = URLEncoder.encode("SELECT survey_json FROM "
				+ MASTER_TABLE_ID + " WHERE table_id = '" + tableId + "'",
				"UTF-8");
		String apiKey = "AIzaSyB4Nn1k2sML-0aBN2Fk3qOXLF-4zlaNwmg";
		String url = "https://www.googleapis.com/fusiontables/v1/query?key="
				+ apiKey + "&sql=" + query;
		Log.v("Get survey query", url);

		HttpClient httpclient = new DefaultHttpClient();
		HttpGet httpget = new HttpGet(url);
		httpget.setHeader("Authorization", "Bearer " + token);
		HttpResponse response = httpclient.execute(httpget);

		Log.v("Get survey response code", response.getStatusLine()
				.getStatusCode()
				+ " "
				+ response.getStatusLine().getReasonPhrase());

		// receive response as inputStream
		InputStream inputStream = response.getEntity().getContent();

		// convert inputstream to string
		if (inputStream != null)
			return convertInputStreamToString(inputStream);
		else
			return null;
	}

	// convert inputstream to String
	private static String convertInputStreamToString(InputStream inputStream)
			throws IOException {
		BufferedReader bufferedReader = new BufferedReader(
				new InputStreamReader(inputStream, "UTF-8"));
		String line = "";
		String result = "";
		while ((line = bufferedReader.readLine()) != null)
			result += line;

		inputStream.close();
		return result;
	}

	/*
	 * Username selection helper functions
	 */

	private void chooseAccount() {
		// use https://github.com/frakbot/Android-AccountChooser for
		// compatibility with older devices
		Intent intent = AccountManager.newChooseAccountIntent(null, null,
				new String[] { "com.google" }, false, null, null, null, null);
		startActivityForResult(intent, ACCOUNT_CODE);
	}

	private void invalidateToken() {
		AccountManager accountManager = AccountManager.get(this);
		accountManager.invalidateAuthToken("com.google", token);
		token = null;
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if (resultCode == RESULT_OK) {
			if (requestCode == AUTHORIZATION_CODE) {
				requestToken();
			} else if (requestCode == ACCOUNT_CODE) {
				username = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);

				// update our username field
				messageHandler.sendEmptyMessage(EVENT_TYPE.GOT_USERNAME
						.ordinal());

				// invalidate old tokens which might be cached. we want a fresh
				// one, which is guaranteed to work
				invalidateToken();

				requestToken();
			}
		}
	}

	private void requestToken() {
		Account userAccount = null;
		for (Account account : accountManager.getAccountsByType("com.google")) {
			if (account.name.equals(username)) {
				userAccount = account;

				break;
			}
		}

		accountManager.getAuthToken(userAccount, "oauth2:" + SCOPE, null, this,
				new OnTokenAcquired(), null);
	}

	private class OnTokenAcquired implements AccountManagerCallback<Bundle> {

		@Override
		public void run(AccountManagerFuture<Bundle> future) {
			try {
				token = future.getResult().getString(
						AccountManager.KEY_AUTHTOKEN);
			} catch (Exception e) {
				// throw new RuntimeException(e);
			}
		}
	}
}
