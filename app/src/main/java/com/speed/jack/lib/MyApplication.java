package com.speed.jack.lib;

import com.speed.jack.ycjlibrary.SimpleApplication;
import com.speed.jack.ycjlibrary.http.HttpRequestUtil;

/**
 * 用途：
 *
 * @version V1.0
 * @FileName: com.speed.jack.lib.MyApplication.java
 * @author: ycj
 * @date: 2017-10-21 22:29
 */
public class MyApplication extends SimpleApplication {
    @Override public void onCreate() {
        super.onCreate();
        HttpRequestUtil.getInstance().init(this);
    }
}
