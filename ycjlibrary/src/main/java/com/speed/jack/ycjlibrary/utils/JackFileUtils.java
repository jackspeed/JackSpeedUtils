package com.speed.jack.ycjlibrary.utils;

import android.content.Context;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.util.Log;

/**
 * 用途：
 *
 * @version V1.0
 * @FileName: com.speed.jack.ycjlibrary.utils.JackFileUtils.java
 * @author: ycj
 * @date: 2017-10-21 22:11
 */

public class JackFileUtils {

    /**
     * 通知android媒体库更新文件夹
     *
     * @param filePath ilePath 文件绝对路径，、/sda/aaa/jjj.jpg
     */
    public void scanFile(Context context, String filePath) {
        try {
            MediaScannerConnection.scanFile(context, new String[] { filePath }, null,
                    new MediaScannerConnection.OnScanCompletedListener() {
                        public void onScanCompleted(String path, Uri uri) {
                            Log.i("*******", "Scanned " + path + ":");
                            Log.i("*******", "-> uri=" + uri);
                        }
                    });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
