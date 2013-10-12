package com.wrt.android.getjar.demo;

import android.app.Application;
import com.wrt.android.getjar.GetJarHelper;

/**
 * Created with IntelliJ IDEA.
 * User: IstiN
 * Date: 12.10.13
 * Time: 8.17
 */
public class CustomApplication extends Application{

    private GetJarHelper mGetJarHelper;

    @Override
    public void onCreate() {
        super.onCreate();
        mGetJarHelper = new GetJarHelper(this,"[your_app_token]");
    }

    @Override
    public Object getSystemService(String name) {
        if (GetJarHelper.GET_JAR_APPLICATION_HELPER_KEY.equals(name)) {
            return mGetJarHelper;
        }
        return super.getSystemService(name);
    }
}
