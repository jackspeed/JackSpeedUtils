package com.speed.jack.ycjlibrary.http;

import com.speed.jack.ycjlibrary.GlobalData;

/**
 * 数据返回类型、字段名称可根据具体情况修改
 * Created by adming on 2017/5/4.
 */

public class BaseResponse<T> {
    public int    result;
    public int    code;
    public String msg;
    public T      data;

    public int getResult() {
        return result;
    }

    public void setResult(int result) {
        this.result = result;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public boolean isSuccess() {
        return result == GlobalData.CODE_SUCCESS;
    }
}
