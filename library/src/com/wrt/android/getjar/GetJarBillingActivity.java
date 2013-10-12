package com.wrt.android.getjar;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.ProgressBar;


public class GetJarBillingActivity extends Activity implements GetJarHelper.PurchaseProductListener {

    public static final String ERROR = "Error";
    public static final String PRODUCT_ID = "product_Id";
    public static final String PRICE = "price";
    public static final String PRODUCT_NAME_RESOURCE = "product_name_resource";
    private static final String PRODUCT_DESCRIPTION_RESOURCE = "product_description_resource";

    public static final int REQUEST_CODE = 5;

    private boolean isOnCreateCalled = false;

    public static void request(Activity activity, String productId, int price, int productNameResource, int productDescription) {
        Intent intent = new Intent(activity, GetJarBillingActivity.class);
        intent.putExtra(PRODUCT_ID, productId);
        intent.putExtra(PRICE, price);
        intent.putExtra(PRODUCT_NAME_RESOURCE, productNameResource);
        intent.putExtra(PRODUCT_DESCRIPTION_RESOURCE, productDescription);
        activity.startActivityForResult(intent, REQUEST_CODE);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setProgress();
        String productId = getIntent().getStringExtra(PRODUCT_ID);
        int price = getIntent().getIntExtra(PRICE, 70);
        int productNameResource = getIntent().getIntExtra(PRODUCT_NAME_RESOURCE, 0);
        int productDescriptionResource = getIntent().getIntExtra(PRODUCT_DESCRIPTION_RESOURCE, 0);
        GetJarHelper.get(this).buyLicensedProduct(
                productId,
                price,
                productNameResource,
                productDescriptionResource,
                this);
        isOnCreateCalled = true;
    }

    private void setProgress() {
        FrameLayout frameLayout = new FrameLayout(this);
        ProgressBar progress = new ProgressBar(this);
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        layoutParams.gravity = Gravity.CENTER;
        progress.setLayoutParams(layoutParams);
        frameLayout.addView(progress);
        setContentView(frameLayout);
    }


    @Override
    protected void onResume() {
        super.onResume();
        if (isOnCreateCalled) {
            isOnCreateCalled = false;
        } else {
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        GetJarHelper.get(this).unregisterListener(getIntent().getStringExtra(PRODUCT_ID), this);
    }

    @Override
    public void onSuccess(final String id) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                setResult(RESULT_OK);
                finish();
            }
        });
    }

    @Override
    public void onError(final Throwable e) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                simpleDialog(GetJarBillingActivity.this, ERROR, e.getMessage(), OK, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                });
            }
        });
    }

    //DIALOG

    private static final String OK = "Ok";

    public static void applyBackground(AlertDialog alertDialog) {
        if (Build.VERSION.SDK_INT > 10) {
            alertDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
    }

    public static AlertDialog.Builder createBuilder(final Context context) {
        if (Build.VERSION.SDK_INT < 11) {
            return new AlertDialog.Builder(context);
        } else {
            return new AlertDialog.Builder(context, getDialogTheme());
        }
    }

    private static int getDialogTheme() {
        if (Build.VERSION.SDK_INT < 11) {
            return android.R.style.Theme_Dialog;
        } else {
            return android.R.style.Theme_Holo_Light_Dialog;
        }
    }

    public static AlertDialog simpleDialog(final Context context, String title, String message, String btn, final DialogInterface.OnClickListener listener) {
        AlertDialog.Builder builder = createBuilder(context);
        if (title != null) {
            builder.setTitle(title);
        }
        builder.setMessage(message);
        builder.setPositiveButton(btn == null ? OK : btn, listener);
        AlertDialog alertDialog = builder.create();
        applyBackground(alertDialog);
        alertDialog.setCancelable(false);
        try {
            alertDialog.show();
            return alertDialog;
        } catch (Exception e) {
            //quick back
            return null;
        }
    }
}