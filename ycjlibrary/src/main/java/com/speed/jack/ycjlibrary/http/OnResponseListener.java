package com.speed.jack.ycjlibrary.http;

/**
 * Created by adming on 2017/5/4.
 */

public interface OnResponseListener<T> {
    void onSuccess(T data);

    void onFailure(String msg);
}
