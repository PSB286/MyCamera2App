package com.psb.myapplication;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.ImageView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ImageUtils {

    private static final String TAG = "ImageUtils";
    @SuppressLint("StaticFieldLeak")
    private static Context sContext = MyApp.getInstance();
    private static final CameraActivity myApp=CameraActivity.getInstance();
    static Uri imageUri= null;
    private static final String GALLERY_PATH = Environment.getExternalStoragePublicDirectory(Environment
            .DIRECTORY_DCIM) + File.separator + "Camera";

    private static final String[] STORE_IMAGES = {
            MediaStore.Images.Thumbnails._ID,
    };
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMdd_HHmmss");

    public static Bitmap rotateBitmap(Bitmap source, int degree, boolean flipHorizontal, boolean recycle) {
        if (degree == 0 && !flipHorizontal) {
            return source;
        }
        Matrix matrix = new Matrix();
        matrix.postRotate(degree);
        if (flipHorizontal) {
            matrix.postScale(-1, 1);
        }
        Log.d(TAG, "source width: " + source.getWidth() + ", height: " + source.getHeight());
        Log.d(TAG, "rotateBitmap: degree: " + degree);
        Bitmap rotateBitmap = Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, false);
        Log.d(TAG, "rotate width: " + rotateBitmap.getWidth() + ", height: " + rotateBitmap.getHeight());
        if (recycle) {
            source.recycle();
        }
        return rotateBitmap;
    }

    public static void saveImage(byte[] jpeg) {
        String fileName = DATE_FORMAT.format(new Date(System.currentTimeMillis())) + ".jpg";
        File outFile = new File(GALLERY_PATH, fileName);
        Log.d(TAG, "saveImage. filepath: " + outFile.getAbsolutePath());
        FileOutputStream os = null;
        try {
            os = new FileOutputStream(outFile);
            os.write(jpeg);
            os.flush();
            os.close();
            insertToDB(outFile.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (os != null) {
                try {
                    os.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static void saveBitmap(Bitmap bitmap) {
        String fileName = DATE_FORMAT.format(new Date(System.currentTimeMillis())) + ".jpg";
        File outFile = new File(GALLERY_PATH, fileName);
        Log.d(TAG, "saveImage. filepath: " + outFile.getAbsolutePath());
        FileOutputStream os = null;
        try {
            os = new FileOutputStream(outFile);
            boolean success = bitmap.compress(Bitmap.CompressFormat.JPEG, 100, os);
            Log.d(TAG, "saveBitmap: " + success);
            if (success) {
                insertToDB(outFile.getAbsolutePath());
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (os != null) {
                try {
                    os.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static void insertToDB(String picturePath) {
        ContentValues values = new ContentValues();
        ContentResolver resolver = sContext.getContentResolver();
        values.put(MediaStore.Images.ImageColumns.DATA, picturePath);
        values.put(MediaStore.Images.ImageColumns.TITLE, picturePath.substring(picturePath.lastIndexOf("/") + 1));
        values.put(MediaStore.Images.ImageColumns.DATE_TAKEN, System.currentTimeMillis());
        values.put(MediaStore.Images.ImageColumns.MIME_TYPE, "image/jpeg");
        resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
    }

    public static Bitmap getLatestThumbBitmap(Context sContext) {
        Bitmap bitmap = null;
        long imagelatestTime = 0;
        long videolatestTime = 0;
        //  按照时间顺序降序查询
        Cursor cursor = MediaStore.Images.Media.query(sContext.getContentResolver(), MediaStore.Images.Media.EXTERNAL_CONTENT_URI,new String[]{MediaStore.Images.Media._ID, MediaStore.Images.Media.DATA, MediaStore.Images.Media.DATE_MODIFIED} , null, null, MediaStore.Files.FileColumns.DATE_MODIFIED + " DESC");
        // 查询视频
        Cursor videoCursor = sContext.getContentResolver().query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, new String[]{MediaStore.Video.Media._ID, MediaStore.Video.Media.DATA,MediaStore.Video.Media.DATE_MODIFIED}, null, null, MediaStore.Files.FileColumns.DATE_MODIFIED + " DESC"
        );
        assert videoCursor != null;
        boolean firstvideo = videoCursor.moveToFirst();
        boolean first = cursor.moveToFirst();
        
        videolatestTime = videoCursor.getLong(videoCursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_MODIFIED));
        imagelatestTime= cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_MODIFIED));

        if(videolatestTime>imagelatestTime)
        {
        if(firstvideo) {
            Log.d("--Bitmap--", "getLatestThumbBitmap: 存在" + firstvideo);
            imageUri = Uri.withAppendedPath(
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    videoCursor.getString(videoCursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID))
            );

            long videoId = videoCursor.getLong(videoCursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID));
            // 获取视频的缩略图
            bitmap = MediaStore.Video.Thumbnails.getThumbnail(
                    sContext.getContentResolver(),
                    videoId,
                    MediaStore.Video.Thumbnails.MICRO_KIND,
                    null
            );
            Log.d("--Bitmap--", "获取视频缩略图:"+bitmap);
        }
        videoCursor.close();
        }
        else {
        // 获取图片的 URI
            if (first) {
                imageUri = Uri.withAppendedPath(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID))
                );
                if (first) {
                    // 获取图片id
                    long id = cursor.getLong(0);
                    // 获取缩略图
                    bitmap = MediaStore.Images.Thumbnails.getThumbnail(sContext.getContentResolver(), id, MediaStore.Images
                            .Thumbnails.MICRO_KIND, null);
                    // 打印图片宽高
                    Log.d("Bitmap", "bitmap width: " + bitmap.getWidth());
                    Log.d("Bitmap", "bitmap height: " + bitmap.getHeight());
                }
            }
            Log.d("Bitmap", "getLatestThumbBitmap: " + first);
            cursor.close();
        }
        return bitmap;
    }

}
