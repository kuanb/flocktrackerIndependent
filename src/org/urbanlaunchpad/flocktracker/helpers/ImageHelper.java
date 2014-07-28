package org.urbanlaunchpad.flocktracker.helpers;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;

import javax.annotation.Nullable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class ImageHelper {

  @Nullable
  public static Bitmap getBitmapFromUri(Uri fileUri) {
    Bitmap imageBitmap = null;
    try {
      imageBitmap = BitmapFactory.decodeFile(
        fileUri.getPath(), null);
      float rotation = ImageHelper.rotationForImage(Uri
        .fromFile(new java.io.File(fileUri.getPath())));
      if (rotation != 0) {
        Matrix matrix = new Matrix();
        matrix.preRotate(rotation);
        imageBitmap = Bitmap.createBitmap(imageBitmap, 0, 0,
          imageBitmap.getWidth(), imageBitmap.getHeight(),
          matrix, true);
      }

      imageBitmap.compress(Bitmap.CompressFormat.JPEG, 25,
        new FileOutputStream(fileUri.getPath()));
    } catch (Exception e) {
      e.printStackTrace();
    }
    return imageBitmap;
  }

  public static int calculateInSampleSize(BitmapFactory.Options options,
      int reqWidth, int reqHeight) {
    // Raw height and width of image
    final int height = options.outHeight;
    final int width = options.outWidth;
    int inSampleSize = 1;

    if (height > reqHeight || width > reqWidth) {

      final int halfHeight = height / 2;
      final int halfWidth = width / 2;

      // Calculate the largest inSampleSize value that is a power of 2 and
      // keeps both height and width larger than the requested height and width.
      while ((halfHeight / inSampleSize) > reqHeight
          && (halfWidth / inSampleSize) > reqWidth) {
        inSampleSize *= 2;
      }
    }
    return inSampleSize;
  }

  public static Bitmap decodeSampledBitmapFromPath(String pathName,
      int reqWidth, int reqHeight) {

    // First decode with inJustDecodeBounds=true to check dimensions
    final BitmapFactory.Options options = new BitmapFactory.Options();
    options.inJustDecodeBounds = true;
    BitmapFactory.decodeFile(pathName, options);

    // Calculate inSampleSize
    options.inSampleSize = calculateInSampleSize(options, reqWidth,
        reqHeight);

    // Decode bitmap with inSampleSize set
    options.inJustDecodeBounds = false;
    return BitmapFactory.decodeFile(pathName, options);
  }

  public static float rotationForImage(Uri uri) {
    try {
      ExifInterface exif = new ExifInterface(uri.getPath());
      int rotation = (int) exifOrientationToDegrees(exif.getAttributeInt(
          ExifInterface.TAG_ORIENTATION,
          ExifInterface.ORIENTATION_NORMAL));
      return rotation;
    } catch (IOException e) {
      return 0;
    }
  }

  /**
   * Get rotation in degrees
   */
  private static float exifOrientationToDegrees(int exifOrientation) {
    if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_90) {
      return 90;
    } else if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_180) {
      return 180;
    } else if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_270) {
      return 270;
    }
    return 0;
  }

}
