package com.speed.jack.ycjlibrary;

import android.app.Application;
import com.blankj.utilcode.util.Utils;

/**
 * Created by adming on 2017/5/11.
 */

public class SimpleApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        Utils.init(this);
    }
}
