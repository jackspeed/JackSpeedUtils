package com.speed.jack.ycjlibrary.http;

import com.speed.jack.ycjlibrary.GlobalData;
import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;

/**
 * Created by adming on 2017/5/4.
 */

public class HttpRequestUtil extends RetrofitUtils {
    private volatile static HttpRequestUtil instance;

    /***
     * 获取实例
     */
    public static HttpRequestUtil getInstance() {
        if (null == instance) {
            synchronized (HttpRequestUtil.class) {
                if (null == instance) {
                    instance = new HttpRequestUtil();
                }
            }
        }
        return instance;
    }






    /**
     * @param <T>
     * @return
     */
    public static <T> Observable.Transformer<BaseResponse<T>, T> handleResult(final ActivityLifeCycleEvent event, final PublishSubject<ActivityLifeCycleEvent> lifecycleSubject) {

        final Observable<ActivityLifeCycleEvent> compareLifecycleObservable;
        if (lifecycleSubject != null) {
            compareLifecycleObservable = lifecycleSubject.takeFirst(new Func1<ActivityLifeCycleEvent, Boolean>() {
                @Override
                public Boolean call(ActivityLifeCycleEvent activityLifeCycleEvent) {
                    return activityLifeCycleEvent.equals(event);
                }
            });
        } else {
            compareLifecycleObservable = null;
        }
        return new Observable.Transformer<BaseResponse<T>, T>() {
            @Override
            public Observable<T> call(Observable<BaseResponse<T>> tObservable) {

                return tObservable.flatMap(new Func1<BaseResponse<T>, Observable<T>>() {
                    @Override
                    public Observable<T> call(BaseResponse<T> result) {
                        if (result.result == GlobalData.CODE_SUCCESS) {
                            return createData(result.data);
                        } else {
                            return Observable.error(new ApiException(result.result, result.msg));
                        }
                    }
                }).takeUntil(compareLifecycleObservable)
                        .subscribeOn(Schedulers.io())
                        .unsubscribeOn(Schedulers.io())
                        .subscribeOn(AndroidSchedulers.mainThread())
                        .observeOn(AndroidSchedulers.mainThread());
            }
        };
    }

    /**
     * 创建成功的数据
     *
     * @param data
     * @param <T>
     * @return
     */
    private static <T> Observable<T> createData(final T data) {
        return Observable.create(new Observable.OnSubscribe<T>() {
            @Override
            public void call(Subscriber<? super T> subscriber) {
                try {
                    subscriber.onNext(data);
                    subscriber.onCompleted();
                } catch (Exception e) {
                    subscriber.onError(e);
                }
            }
        });

    }

}
