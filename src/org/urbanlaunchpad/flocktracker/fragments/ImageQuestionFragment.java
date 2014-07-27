package org.urbanlaunchpad.flocktracker.fragments;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;
import java.util.Set;

@SuppressLint("ValidFragment")
public class ImageQuestionFragment extends QuestionFragment {
	private LinearLayout answersContainer;
	private Uri imagePath;
	Uri cameraIntentUri = null;
	public static final int CAPTURE_IMAGE = 3;

	private OnClickListener cameraButtonOnClickListener = new OnClickListener() {
		@Override
		public void onClick(View v) {
			String fileName = createImageFilename();
			cameraIntentUri = createImageUri(fileName);
			SurveyorActivity.driveHelper.startCameraIntent(fileName);
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
		new addThumbnail().execute("");
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
				imagePath = cameraIntentUri;
				//imagePath = (Uri) data.getExtras().get(MediaStore.EXTRA_OUTPUT);
				// Bitmap photo = (Bitmap) data.getExtras().get("data");
				// saveBitmap(photo, imagePath.toString());
				getQuestion().setImage(imagePath);
				new addThumbnail().execute("");
			}

			// try {
			// SurveyorActivity.driveHelper.saveFileToDrive(imagePath
			// .toString());
			// } catch (IOException e) {
			// // TODO Auto-generated catch block
			// e.printStackTrace();
			// }
		}

	}

	public void startCameraIntent() {

		String mediaStorageDir = Environment.getExternalStoragePublicDirectory(
				Environment.DIRECTORY_PICTURES).getPath();
		String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
				.format(new Date());
		cameraIntentUri = Uri.fromFile(new File(mediaStorageDir
				+ File.separator + "IMG_" + timeStamp + ".jpg"));

		Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
		// cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, cameraIntentUri);

		getActivity().startActivityForResult(cameraIntent, CAPTURE_IMAGE);

		// // Container Activity must handle the onActivityResult and send it to
		// this fragment for it to work properly.
		// String mediaStorageDir =
		// Environment.getExternalStoragePublicDirectory(
		// Environment.DIRECTORY_PICTURES).getPath();
		//
		// File folder = new File(mediaStorageDir + File.sepa rator +
		// "FlockTracker");
		// if (!folder.exists()) {
		// folder.mkdir();
		// }
		//
		// String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
		// .format(new Date());
		// cameraIntentUri = Uri.fromFile(new File(folder.getAbsolutePath(),
		// "IMG_" + timeStamp + ".jpg"));
		// // cameraIntentUri = Uri.fromFile(new File(mediaStorageDir +
		// File.separator + "IMG_" + timeStamp + ".jpg"));
		// Log.v("file Uri", cameraIntentUri.toString());
		//
		// Intent cameraIntent = new
		// Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
		// cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, cameraIntentUri);
		//
		// getActivity().startActivityForResult(cameraIntent, CAPTURE_IMAGE);
	}

	private class addThumbnail extends AsyncTask<String, Void, String> {
		Bitmap imageBitmap = null;

		@Override
		protected String doInBackground(String... params) {
			imageBitmap = getBitmap(getQuestion().getImage());
			return "Executed";
		}

		@Override
		protected void onPostExecute(String result) {
			if (!(imageBitmap == null)) {
				ImageView prevImage = new ImageView(getActivity());
				prevImage.setImageBitmap(imageBitmap);
				answersContainer.addView(prevImage);
			}
		}

		@Override
		protected void onPreExecute() {
		}

		@Override
		protected void onProgressUpdate(Void... values) {
		}
	}

	public Bitmap getBitmap(Uri imagePath) {
		Bitmap imageBitmap = null;
		if (!(imagePath == null)) {
			imagePath = getQuestion().getImage();
			try {
				// Bitmap imageBitmap = ImageHelper.decodeSampledBitmapFromPath(
				// imagePath.getPath(), 512, 512);
				// prevImage.setImageBitmap(imageBitmap);
				// prevImage.setPadding(10, 30, 10, 10);
				//
				// answersContainer.addView(prevImage);

				imageBitmap = BitmapFactory.decodeFile(imagePath.toString(),
						null);
				float rotation = ImageHelper.rotationForImage(imagePath);
				if (rotation != 0) {
					Matrix matrix = new Matrix();
					matrix.preRotate(rotation);
					imageBitmap = Bitmap.createBitmap(imageBitmap, 0, 0,
							imageBitmap.getWidth(), imageBitmap.getHeight(),
							matrix, true);
				}

				imageBitmap.compress(CompressFormat.JPEG, 25,
						new FileOutputStream(imagePath.getPath()));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return imageBitmap;
	}

	public void saveBitmap(Bitmap photo, String path) {
		Log.v("SAVE", path);
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		// photo.compress(Bitmap.CompressFormat.JPEG, 40, bytes);
		Log.v("file Uri to save", path);
		// you can create a new file name "test.jpg" in sdcard folder.
		File f = new File(path);
		try {
			f.createNewFile();
			FileOutputStream fo = new FileOutputStream(f);
			fo.write(bytes.toByteArray());

			// remember close de FileOutput
			fo.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// write the bytes in file

	}

	
	
	public Uri createImageUri(String fileName) {
		String mediaStorageDir = Environment.getExternalStoragePublicDirectory(
				Environment.DIRECTORY_PICTURES).getPath();
		Uri fileUri = Uri.fromFile(new java.io.File(mediaStorageDir
				+ java.io.File.separator + fileName));
		return fileUri;

	}
	
	private String createImageFilename() {
		String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
		.format(new Date());		
		String fileName = "IMG_" + timeStamp + ".jpg";
		return fileName;
	}

}
