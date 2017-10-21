package com.speed.jack.ycjlibrary.http;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.util.Log;
import com.blankj.utilcode.util.AppUtils;
import com.blankj.utilcode.util.StringUtils;
import com.speed.jack.ycjlibrary.GlobalData;
import com.speed.jack.ycjlibrary.cache.ACache;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.TimeUnit;
import okhttp3.Cache;
import okhttp3.CacheControl;
import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Created by ycj on 2017/5/4.
 */

public abstract class RetrofitUtils {
    private static Retrofit     mRetrofit;
    private static OkHttpClient mOkHttpClient;
    private        Context      mContext;

    /**
     * 初始化方法必须调用
     *
     * @param context Context
     */
    public void init(Context context) {
        mContext = context;
        initOkHttpClient(context);
        initRetrofit();
    }

    /**
     * 初始化Retrofit对象
     */
    private void initRetrofit() {
        if (null == mRetrofit) {
            //Retrofit2后使用build设计模式
            mRetrofit = new Retrofit.Builder()
                    //设置服务器路径
                    .baseUrl(GlobalData.API_SERVER)
                    //设置使用okhttp网络请求
                    .client(mOkHttpClient)
                    //添加转化库，默认是Gson
                    .addConverterFactory(GsonConverterFactory.create())
                    //添加回调库，采用RxJava
                    .addCallAdapterFactory(RxJavaCallAdapterFactory.create()).build();
        }
    }

    public <T> T getApi(Class<T> service) {
        return mRetrofit.create(service);
    }

    /**
     * 初始化OkHttpClient对象
     */
    private void initOkHttpClient(Context context) {
        if (null == mOkHttpClient) {
            //设置缓存目录
            File cacheDirectory = new File(context.getExternalCacheDir(), "MyCache");
            Cache cache = new Cache(cacheDirectory, 100 * 1024 * 1024);

            HttpLogInterceptor loggingInterceptor = new HttpLogInterceptor();
            loggingInterceptor.setLevel(HttpLogInterceptor.Level.BODY);

            mOkHttpClient = new OkHttpClient.Builder().cookieJar(
                    new CookiesManager())//设置一个自动管理cookies的管理器
                                                      .addInterceptor(
                                                              loggingInterceptor)//添加网络请求URL日志输出拦截器
                                                      .addInterceptor(mTokenInterceptor)//添加Token拦截器
                                                      .addNetworkInterceptor(
                                                              TestInterceptor)//添加网络连接器
                                                      .addInterceptor(TestInterceptor)
                                                      //                    .addNetworkInterceptor(new CookiesInterceptor(MyApplication.getInstance().getApplicationContext()))
                                                      //设置请求读写的超时时间
                                                      .connectTimeout(10, TimeUnit.SECONDS)
                                                      .writeTimeout(10, TimeUnit.SECONDS)
                                                      .readTimeout(10, TimeUnit.SECONDS)
                                                      .cache(cache)
                                                      .build();
        }
    }

    /**
     * 云端响应头拦截器，用来配置缓存策略
     * Dangerous interceptor that rewrites the server's cache-control header.
     */
    private final Interceptor TestInterceptor   = new Interceptor() {
        @Override public Response intercept(Chain chain) throws IOException {
            Request request = chain.request();
            if (!isNetworkReachable(mContext)) {
                request = request.newBuilder().cacheControl(CacheControl.FORCE_CACHE).build();
            }
            Response originalResponse = chain.proceed(request);
            if (isNetworkReachable(mContext)) {
                //有网的时候读接口上的@Headers里的配置，你可以在这里进行统一的设置
                String cacheControl = request.cacheControl().toString();
                return originalResponse.newBuilder()
                                       .header("Cache-Control", cacheControl)
                                       .removeHeader("Pragma")
                                       .build();
            } else {
                int maxStale = 60 * 60 * 24 * 365; // 无网络时间
                return originalResponse.newBuilder()
                                       .header("Cache-Control",
                                               "public, only-if-cached, max-stale=" + maxStale)
                                       .removeHeader("Pragma")
                                       .build();
            }
        }
    };
    /**
     * 云端响应头拦截器
     * 用于添加统一请求头  请按照自己的需求添加
     */
    private final Interceptor mTokenInterceptor = new Interceptor() {
        @Override public Response intercept(Chain chain) throws IOException {
            Request authorised;
            //TODO:token路径更换
            String token = ACache.get(mContext).getAsString(GlobalData.TOKEN);
            if (!StringUtils.isEmpty(token)) {
                authorised = chain.request()
                                  .newBuilder()
                                  .header("Content-Type", "application/json; charset=utf-8")
                                  .addHeader("clientAgent",
                                          "Android|" + Build.VERSION.RELEASE + "|" + Build.MODEL)
                                  .addHeader("clientVersion", "Android|" +
                                          AppUtils.getAppVersionName(mContext.getPackageName()))
                                  .header("clientIP", getLocalIpAddress())
                                  .addHeader("token", token)
                                  .build();
            } else {
                authorised = chain.request()
                                  .newBuilder()
                                  .header("Content-Type", "application/json; charset=utf-8")
                                  .addHeader("clientAgent",
                                          "Android|" + Build.VERSION.RELEASE + "|" + Build.MODEL)
                                  .addHeader("clientVersion", "Android|" +
                                          AppUtils.getAppVersionName(mContext.getPackageName()))
                                  .header("clientIP", getLocalIpAddress())
                                  .build();
            }

            Response response = chain.proceed(authorised);
            //TODO:code为208表示token失效了code服务端确定
            if (response.code() == 208) {
                //TODO：重新登录？
                //关掉原先的response资源，避免资源泄露
                //                    response.close();
                return response;
            }
            return chain.proceed(authorised);
        }
    };

    /**
     * 自动管理Cookies
     */
    private class CookiesManager implements CookieJar {
        private final PersistentCookieStore cookieStore = new PersistentCookieStore(mContext);

        @Override public void saveFromResponse(HttpUrl url, List<Cookie> cookies) {
            if (cookies != null && cookies.size() > 0) {
                for (Cookie item : cookies) {
                    cookieStore.add(url, item);
                }
            }
        }

        @Override public List<Cookie> loadForRequest(HttpUrl url) {
            return cookieStore.get(url);
        }
    }

    /**
     * 判断网络是否可用
     *
     * @param context Context对象
     */
    private static Boolean isNetworkReachable(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(
                Context.CONNECTIVITY_SERVICE);
        NetworkInfo current = cm.getActiveNetworkInfo();
        return current != null && (current.isAvailable());
    }

    /*
     *获取设备IP地址
     */
    private static String getLocalIpAddress() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces();
                 en.hasMoreElements(); ) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses();
                     enumIpAddr.hasMoreElements(); ) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    //加上这个地址获取的一定是IPV4地址  不加的话 有可能是IPV6地址
                    if (!inetAddress.isLoopbackAddress() && !inetAddress.isLinkLocalAddress()) {
                        return inetAddress.getHostAddress();
                    }
                }
            }
        } catch (SocketException ex) {
            Log.e("VOLLEY", ex.toString());
        }
        return "127.0.0.1(error)";
    }
}
