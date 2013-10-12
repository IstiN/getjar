package com.wrt.android.getjar;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

public class ProductManager {

    public static final String GET_JAR_EXT_PREF = "get_jar_ext_pref";

    public static boolean isLicensed(Context context, String productId){
    	SharedPreferences prefs = context.getSharedPreferences(GET_JAR_EXT_PREF, Context.MODE_PRIVATE);
    	return prefs.getBoolean(productId, false);
    }
    
    public static void setLicensed(Context context, String productId){
    	SharedPreferences prefs = context.getSharedPreferences(GET_JAR_EXT_PREF, Context.MODE_PRIVATE);
    	Editor editor = prefs.edit();
    	editor.putBoolean(productId, true);
    	editor.commit();
    }
}
