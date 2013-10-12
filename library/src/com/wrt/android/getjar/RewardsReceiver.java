package com.wrt.android.getjar;

import android.os.Bundle;
import android.os.ResultReceiver;
import android.util.Log;
import com.getjar.sdk.response.*;

import java.util.Map;

/**
 * This is the class representing objects that can be passed into the creation of the GetJarContext
 * through GetJarManager::createContext().  The object will receive purchase events when the GetJar SDK
 * responds from processing a transaction request.
 * 
 * @author GetJar Inc.
 * @see http://developer.getjar.com/university/getjar-rewards-sdk/
 */
public class RewardsReceiver extends ResultReceiver {

    public static final String TAG = "RewardsReceiver";
    private GetJarHelper mHelper = null;

	public RewardsReceiver(GetJarHelper helper) {
		super(null);
		this.mHelper = helper;
	}
	
	@Override
	protected void onReceiveResult (int resultCode, Bundle resultData) {
		
		for(String key : resultData.keySet()) {
			
			Object value = resultData.get(key);
			if (value instanceof PurchaseSucceededResponse) {
                PurchaseSucceededResponse presp = (PurchaseSucceededResponse) value;
                String productName = presp.getProductName();
                String productId = presp.getProductId();
                long amount = presp.getAmount();
                this.mHelper.onProductPurchasedSuccess(productId, productName, amount, presp);
                return;

			} else if(value instanceof BlacklistedResponse) {
                String metadata = String.format("Callback from the GetJar SDK [%1$s] [%2$s]", value.getClass().getName(), ((BlacklistedResponse) value).getBlacklistType().name());
                Log.d("RewardsReceiver", metadata);

				/* (Optional) handle response for a blacklisted app here */
                this.mHelper.onProductPurchasedError(GetJarHelper.PurchaseErrorType.BLACKLISTED, metadata);
			} else if(value instanceof CloseResponse) {
				Log.d("RewardsReceiver", String.format("Callback from the GetJar SDK [%1$s]", value.getClass().getName()));

				// Save state that the Rewards UI has been closed and is not showing.
				// This is in case the OS reclaims the app resources while the user is away in Google Play.
				Log.d("RewardsReceiver", "State tracking: Cleared any Rewards UI state");

				/* (Optional) handle CloseReponse, sent when the GetJarPage is closed */
		        return;
			
			} else if(PurchaseResponse.class.isAssignableFrom(value.getClass())) {
				Log.d("RewardsReceiver", String.format(
						"Callback from the GetJar SDK [%1$s] [itemCost:%2$d itemId:%3$s itemName:%4$s transactionId:%5$s]", 
						value.getClass().getName(), 
						((PurchaseResponse)value).getAmount(),
						((PurchaseResponse)value).getProductId(),
						((PurchaseResponse)value).getProductName(),
						((PurchaseResponse)value).getTransactionId()));
			} else if(value instanceof DeviceUnsupportedResponse) {
				StringBuilder logMessage = new StringBuilder();
				Map<String, String> deviceMetadata = ((DeviceUnsupportedResponse)value).getDeviceMetadata();
				if(deviceMetadata != null) {
					logMessage.append("\r\ndeviceMetadata:");
					for(String name : deviceMetadata.keySet()) {
						logMessage.append("\r\n");
						logMessage.append(name);
						logMessage.append("=");
						logMessage.append(deviceMetadata.get(name));
					}
				}
				Log.d(TAG, String.format("Callback from the GetJar SDK [%1$s] [%2$s]", value.getClass().getName(), logMessage.toString()));
                this.mHelper.onProductPurchasedError(GetJarHelper.PurchaseErrorType.DEVICE_UNSUPPORTED, logMessage.toString());
			}
		}
	}
}
