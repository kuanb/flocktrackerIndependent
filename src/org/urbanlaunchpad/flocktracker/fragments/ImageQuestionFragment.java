package org.urbanlaunchpad.flocktracker.fragments;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import com.squareup.otto.Bus;
import org.urbanlaunchpad.flocktracker.R;
import org.urbanlaunchpad.flocktracker.SurveyorActivity;
import org.urbanlaunchpad.flocktracker.helpers.GoogleDriveHelper;
import org.urbanlaunchpad.flocktracker.helpers.ImageHelper;
import org.urbanlaunchpad.flocktracker.models.Question;

import javax.inject.Inject;
import java.io.File;
import java.io.FileOutputStream;
import java.util.Collections;
import java.util.Set;

@SuppressLint("ValidFragment")
public class ImageQuestionFragment extends QuestionFragment {
  @Inject GoogleDriveHelper driveHelper;
  private LinearLayout answersContainer;
  private ImageView thumbnailView;

  private OnClickListener cameraButtonOnClickListener = new OnClickListener() {
    @Override
    public void onClick(View v) {
      driveHelper.startCameraIntent();
    }
  };

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    ((SurveyorActivity) getActivity()).getObjectGraph().inject(this);
  }

  public ImageQuestionFragment(Question question, QuestionType questionType,
    Bus eventBus) {
    super(question, questionType, eventBus);
  }

  public void setupLayout(View rootView) {
    ImageView cameraButton = new ImageView(getActivity());
    cameraButton.setImageResource(R.drawable.camera);
    cameraButton.setOnClickListener(cameraButtonOnClickListener);
    answersContainer = (LinearLayout) rootView.findViewById(R.id.answer_layout);
    answersContainer.addView(cameraButton);
    thumbnailView = new ImageView(getActivity());
    thumbnailView.setPadding(10, 30, 10, 10);
    answersContainer.addView(thumbnailView);
    Uri imageUri = getQuestion().getImage();
    if (imageUri != null) {
      new ImageProcessTask().execute(imageUri);
    }
  }

  @Override
  public Set<String> getSelectedAnswers() {
    Uri answerURI = getQuestion().getImage();
    if (!(answerURI == null)) {
      Set<String> answer = Collections.singleton(answerURI.toString());
      return answer;
    } else {
      return Collections.singleton("");
    }
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    if (requestCode == GoogleDriveHelper.CAPTURE_IMAGE) {
      if (resultCode == Activity.RESULT_OK) {
        try {
          String path = driveHelper.getFileUri().getPath();
          Bitmap imageBitmap = BitmapFactory.decodeFile(path, null);
          float rotation = ImageHelper.rotationForImage(Uri
              .fromFile(new File(path)));
          if (rotation != 0) {
            Matrix matrix = new Matrix();
            matrix.preRotate(rotation);
            imageBitmap = Bitmap.createBitmap(imageBitmap, 0, 0,
                imageBitmap.getWidth(), imageBitmap.getHeight(),
                matrix, true);
          }

          FileOutputStream outputStream = new FileOutputStream(path);
          imageBitmap.compress(Bitmap.CompressFormat.JPEG, 25,
              outputStream);
          outputStream.close();
        } catch (Exception e) {
          e.printStackTrace();
        }

        new ImageProcessTask().execute(driveHelper.getFileUri());
      }
    }
  }

  private class ImageProcessTask extends AsyncTask<Uri, Void, Bitmap> {
    @Override
    protected Bitmap doInBackground(Uri... params) {
      Uri imageUri = params[0];
      getQuestion().setImage(imageUri);
      return ImageHelper.decodeSampledBitmapFromPath(imageUri.getPath(), 512, 512);
    }

    @Override
    protected void onPostExecute(Bitmap result) {
      if (result != null) {
        thumbnailView.setImageBitmap(result);
      }
    }
  }
}
