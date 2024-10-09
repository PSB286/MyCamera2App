package com.psb.myapplication.Utils;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

// 图片处理类
public class ImageUtils {
    private static final String TAG = "ImageUtils";                     // TAG
    public static Uri imageUri= null;                                          // 图片的uri

    /**
     * 旋转位图并可选择是否翻转和回收原位图
     *
     * @param source           源位图对象
     * @param degree           旋转角度，以90度为单位
     * @param flipHorizontal   是否沿水平轴翻转位图
     * @param recycle          是否回收源位图资源
     * @return                 返回旋转后的位图对象
     */
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

    /**
     * 获取最新的缩略图Bitmap
     * 该方法会查询系统媒体库中最新的图片或视频，并返回其缩略图Bitmap
     * 如果最新的媒体文件是视频，则返回视频的缩略图；如果是图片，则返回图片的缩略图
     *
     * @param sContext 上下文，用于查询媒体库
     * @return 最新媒体文件的缩略图Bitmap，如果没有媒体文件则返回null
     */
    public static Bitmap getLatestThumbBitmap(Context sContext) {
        Bitmap bitmap = null;
        long imagelatestTime = 0;
        long videolatestTime = 0;
        //  按照时间顺序降序查询
        Cursor cursor = MediaStore.Images.Media.query(sContext.getContentResolver(), MediaStore.Images.Media.EXTERNAL_CONTENT_URI,new String[]{MediaStore.Images.Media._ID, MediaStore.Images.Media.DATA, MediaStore.Images.Media.DATE_MODIFIED} , null, null, MediaStore.Files.FileColumns.DATE_MODIFIED + " DESC");
        // 查询视频
        Cursor videoCursor = sContext.getContentResolver().query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, new String[]{MediaStore.Video.Media._ID, MediaStore.Video.Media.DATA,MediaStore.Video.Media.DATE_MODIFIED}, null, null, MediaStore.Files.FileColumns.DATE_MODIFIED + " DESC"
        );
        Log.d("getLatestThumbBitmap", "getLatestThumbBitmap: " + cursor.getCount() + ", " + videoCursor.getCount());
        if(cursor.getCount()==0&&videoCursor.getCount()==0)
        {
            return bitmap;
        }
        else if(cursor.getCount()==0&&videoCursor.getCount()!=0)
        {
            boolean firstvideo = videoCursor.moveToFirst();
            //videolatestTime = videoCursor.getLong(videoCursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_MODIFIED));
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
            return bitmap;
        }
        else if(cursor.getCount()!=0&&videoCursor.getCount()==0)
        {
            boolean first = cursor.moveToFirst();
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
            return bitmap;
        }
        assert videoCursor != null;
        boolean firstvideo = videoCursor.moveToFirst();
        boolean first = cursor.moveToFirst();
        Log.d("--Bitmap--", "getLatestThumbBitmap: " + firstvideo + ", " + first);


        
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
