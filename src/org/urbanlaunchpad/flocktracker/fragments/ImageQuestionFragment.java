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
import android.widget.RelativeLayout;

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
import java.util.HashSet;
import java.util.Set;

@SuppressLint("ValidFragment")
public class ImageQuestionFragment extends QuestionFragment {
	@Inject
	GoogleDriveHelper driveHelper;
	private LinearLayout answersContainer;
	private ImageView thumbnailView;
	private String generalJumpID;

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
		RelativeLayout cameraLayout = (RelativeLayout) getInflater().inflate(
				R.layout.fragment_image_question, null);
		LinearLayout questionView = (LinearLayout) rootView
				.findViewById(R.id.question_layout);
		questionView.removeView(rootView
				.findViewById(R.id.answer_scroll_container));
		generalJumpID = getQuestion().getJumpID();
		answersContainer = (LinearLayout) rootView
				.findViewById(R.id.answer_linear_container);
		answersContainer.setVisibility(View.VISIBLE);
		ImageView cameraButton = (ImageView) cameraLayout
				.findViewById(R.id.camera_button);
		cameraButton.setOnClickListener(cameraButtonOnClickListener);
		answersContainer.addView(cameraLayout);
		thumbnailView = (ImageView) cameraLayout
				.findViewById(R.id.picture_thumbnail);
		Uri imageUri = getQuestion().getImage();
		if (imageUri != null) {
			new ImageProcessTask().execute(imageUri);
		}
	}

	@Override
	public Set<String> getSelectedAnswers() {
		return new HashSet<String>();
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == GoogleDriveHelper.CAPTURE_IMAGE) {
			if (resultCode == Activity.RESULT_OK) {
				new ImageProcessTask().execute(driveHelper.getFileUri());
			}
		}
	}

	private class ImageProcessTask extends AsyncTask<Uri, Void, Bitmap> {
		@Override
		protected Bitmap doInBackground(Uri... params) {
			Uri imageUri = params[0];
			if (!imageUri.equals(getQuestion().getImage())) {
				try {
					Bitmap imageBitmap = BitmapFactory.decodeFile(
							imageUri.getPath(), null);
					float rotation = ImageHelper.rotationForImage(Uri
							.fromFile(new File(imageUri.getPath())));
					if (rotation != 0) {
						Matrix matrix = new Matrix();
						matrix.preRotate(rotation);
						imageBitmap = Bitmap.createBitmap(imageBitmap, 0, 0,
								imageBitmap.getWidth(),
								imageBitmap.getHeight(), matrix, true);
					}

					FileOutputStream outputStream = new FileOutputStream(
							imageUri.getPath());
					imageBitmap.compress(Bitmap.CompressFormat.JPEG, 25,
							outputStream);
					outputStream.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
				getQuestion().setImage(imageUri);
			}
			return ImageHelper.decodeSampledBitmapFromPath(imageUri.getPath(),
					512, 512);
		}

		@Override
		protected void onPostExecute(Bitmap result) {
			if (result != null) {
				thumbnailView.setImageBitmap(result);
			}
		}
	}
}
