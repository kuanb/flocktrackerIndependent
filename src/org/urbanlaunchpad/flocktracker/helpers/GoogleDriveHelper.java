package org.urbanlaunchpad.flocktracker.helpers;

import android.accounts.AccountManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.Toast;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.http.FileContent;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.ParentReference;
import org.urbanlaunchpad.flocktracker.IniconfigActivity;
import org.urbanlaunchpad.flocktracker.SurveyorActivity;

import javax.inject.Inject;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;

public class GoogleDriveHelper {

  public static final int REQUEST_ACCOUNT_PICKER = 1;
  public static final int REQUEST_AUTHORIZATION = 2;
  public static final int CAPTURE_IMAGE = 3;
  public static final String PHOTO_FOLDER_ID = "0BzQnDGTR4fYbQUdLeUUwcXFVOUE";
  private Drive service;
  private SurveyorActivity activity;
  private Uri fileUri;

  @Inject
  public GoogleDriveHelper(Context context) {
    this.activity = (SurveyorActivity) context;
    service = getDriveService(IniconfigActivity.credential);
  }

  public void requestAccountPicker(Intent intent) {
    String accountName = intent
      .getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
    if (accountName != null) {
      IniconfigActivity.credential.setSelectedAccountName(accountName);
      service = getDriveService(IniconfigActivity.credential);
      startCameraIntent();
    }
  }

  public void startCameraIntent() {
    String mediaStorageDir = Environment.getExternalStoragePublicDirectory(
        Environment.DIRECTORY_DCIM).getPath() + "/flocktracker";
    new java.io.File(mediaStorageDir).mkdirs();
    String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
        .format(new Date());
    fileUri = Uri.fromFile(new java.io.File(mediaStorageDir
        + java.io.File.separator + "IMG_" + timeStamp + ".jpg"));

    Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
    cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri);

    activity.startActivityForResult(cameraIntent, CAPTURE_IMAGE);
  }

  public String saveFileToDrive(final String imagePath) throws IOException {
    try {
      // File's binary content
      java.io.File fileContent = new java.io.File(imagePath);
      FileContent mediaContent = new FileContent("image/jpeg",
          fileContent);

      // File's metadata.
      File body = new File();
      body.setTitle(fileContent.getName());
      body.setMimeType("image/jpeg");
      body.setParents(Arrays.asList(new ParentReference()
          .setId(PHOTO_FOLDER_ID)));

      File file = service.files().insert(body, mediaContent).execute();

      if (file != null) {
        showToast("Photo uploaded: " + file.getTitle());
      }
      return file.getWebContentLink();
    } catch (UserRecoverableAuthIOException e) {
      activity.startActivityForResult(e.getIntent(),
          REQUEST_AUTHORIZATION);
    }
    return null;
  }

  public Uri getFileUri() {
    return fileUri;
  }

  private void showToast(final String toast) {
    activity.runOnUiThread(new Runnable() {
      @Override
      public void run() {
        Toast.makeText(activity.getApplicationContext(), toast,
            Toast.LENGTH_SHORT).show();
      }
    });
  }

  private Drive getDriveService(GoogleAccountCredential credential) {
    return new Drive.Builder(AndroidHttp.newCompatibleTransport(),
        new GsonFactory(), credential).build();
  }
}
