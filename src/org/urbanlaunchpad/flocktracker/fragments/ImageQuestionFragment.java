package org.urbanlaunchpad.flocktracker.fragments;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.squareup.otto.Bus;

import org.urbanlaunchpad.flocktracker.R;
import org.urbanlaunchpad.flocktracker.SurveyorActivity;
import org.urbanlaunchpad.flocktracker.helpers.ImageHelper;
import org.urbanlaunchpad.flocktracker.models.Question;

import java.util.Collections;
import java.util.Set;

@SuppressLint("ValidFragment")
public class ImageQuestionFragment extends QuestionFragment {
	private LinearLayout answersContainer;
	private Uri imagePath;

	private OnClickListener cameraButtonOnClickListener = new OnClickListener() {
		@Override
		public void onClick(View v) {
			SurveyorActivity.driveHelper.startCameraIntent();
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
		LinearLayout answersContainer = (LinearLayout) rootView
				.findViewById(R.id.answer_layout);
		answersContainer.addView(cameraButton);
		addThumbnail();
	}

	@Override
	public Set<String> getSelectedAnswers() {
		return Collections.singleton(getQuestion().getImage().toString());
	}
	

	public void addThumbnail() {
		if (!(getQuestion().getImage() == null)) {
			imagePath = getQuestion().getImage();
			ImageView prevImage = new ImageView(getActivity());
			try {
				Bitmap imageBitmap = ImageHelper.decodeSampledBitmapFromPath(
						imagePath.getPath(), 512, 512);
				prevImage.setImageBitmap(imageBitmap);
				prevImage.setPadding(10, 30, 10, 10);

				answersContainer.addView(prevImage);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}
