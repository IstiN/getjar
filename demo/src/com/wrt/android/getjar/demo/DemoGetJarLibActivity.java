package com.wrt.android.getjar.demo;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import com.wrt.android.getjar.GetJarBillingActivity;

public class DemoGetJarLibActivity extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
    }

    public void onBuyProVersionClick(View view) {
        GetJarBillingActivity.request(this, "all_features", 4, R.string.product_name, R.string.product_description);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == GetJarBillingActivity.REQUEST_CODE && resultCode == RESULT_OK) {
            Toast.makeText(this, "Thanks.", Toast.LENGTH_SHORT).show();
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }
}
