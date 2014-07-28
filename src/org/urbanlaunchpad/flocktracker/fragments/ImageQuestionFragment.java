package org.urbanlaunchpad.flocktracker.fragments;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import com.squareup.otto.Bus;
import org.urbanlaunchpad.flocktracker.R;
import org.urbanlaunchpad.flocktracker.helpers.GoogleDriveHelper;
import org.urbanlaunchpad.flocktracker.helpers.ImageHelper;
import org.urbanlaunchpad.flocktracker.models.Question;

import javax.inject.Inject;
import java.util.Collections;
import java.util.Set;

@SuppressLint("ValidFragment")
public class ImageQuestionFragment extends QuestionFragment {
  @Inject GoogleDriveHelper driveHelper;
  private LinearLayout answersContainer;

  private OnClickListener cameraButtonOnClickListener = new OnClickListener() {
    @Override
    public void onClick(View v) {
      driveHelper.startCameraIntent();
    }
  };

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
    new ImageProcessTask().execute();
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
        new ImageProcessTask().execute(data.getData());
      }
    }
  }

  private class ImageProcessTask extends AsyncTask<Uri, Void, Bitmap> {
    @Override
    protected Bitmap doInBackground(Uri... params) {
      Uri imageUri = params[0];
      getQuestion().setImage(imageUri);
      return ImageHelper.getBitmapFromUri(imageUri);
    }

    @Override
    protected void onPostExecute(Bitmap result) {
      if (result != null) {
        ImageView prevImage = new ImageView(getActivity());
        prevImage.setImageBitmap(result);
        answersContainer.addView(prevImage);
      }
    }
  }
}
