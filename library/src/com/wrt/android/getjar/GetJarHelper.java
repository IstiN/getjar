package com.wrt.android.getjar;

import android.content.Context;
import com.getjar.sdk.*;
import com.getjar.sdk.listener.EnsureUserAuthListener;
import com.getjar.sdk.listener.IsUnmanagedProductLicensedListener;
import com.getjar.sdk.listener.RecommendedPricesListener;
import com.getjar.sdk.response.PurchaseSucceededResponse;
import com.getjar.sdk.utilities.StringUtility;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Created with IntelliJ IDEA.
 * User: IstiN
 * Date: 12.10.13
 * Time: 6.27
 */
public class GetJarHelper {

    public static final String GET_JAR_APPLICATION_HELPER_KEY = "GET_JAR_APPLICATION_HELPER_KEY";

    private RewardsReceiver mRewardsReceiver;

    private GetJarPage mGetJarPage;

    private GetJarContext mGetJarContext;

    private Localization mLocalization;

    private Context mContext;

    private String mAppToken;

    private String mDeveloperPublicKey;

    private UserAuth mUserAuth;

    private ConcurrentHashMap<String, CopyOnWriteArrayList<PurchaseProductListener>> mListeners = new ConcurrentHashMap<String, CopyOnWriteArrayList<PurchaseProductListener>>();

    public GetJarHelper(Context context, String appToken, String developerPublicKey) {
        super();
        if (context == null) {
            throw new IllegalArgumentException("context can't be null");
        }
        if (appToken == null) {
            throw new IllegalArgumentException("app token can't be null");
        }
        mContext = context;
        mAppToken = appToken;
        mDeveloperPublicKey = developerPublicKey;
        initRewardsReceiver();
        initGetJarContext(context);
        initLocation();
        initGetJarPage();
    }

    private void initGetJarPage() {
        GetJarContext jarContext = getGetJarContext();
        if (jarContext == null) {
            return;
        }
        mGetJarPage = new GetJarPage(jarContext);
    }

    private void initLocation() {
        GetJarContext jarContext = getGetJarContext();
        if (jarContext != null) {
            mLocalization = new Localization(jarContext);
        }
    }

    private void initRewardsReceiver() {
        mRewardsReceiver = new RewardsReceiver(this);
    }

    public Localization getLocalization() {
        if (mLocalization == null) {
            initLocation();
        }
        return mLocalization;
    }

    public GetJarPage getGetJarPage() {
        if (mGetJarPage == null) {
            initGetJarPage();
        }
        return mGetJarPage;
    }

    private GetJarContext getGetJarContext() {
        if (mGetJarContext == null) {
            initGetJarContext(mContext);
        }
        return mGetJarContext;
    }
    private void initGetJarContext(Context context) {
        try {
            mGetJarContext =  GetJarManager.createContext(mAppToken, mDeveloperPublicKey, context, mRewardsReceiver);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static GetJarHelper get(Context context) {
        return (GetJarHelper) get(context, GET_JAR_APPLICATION_HELPER_KEY);
    }

    private static Object get(Context context, String name) {
        if (context == null || name == null){
            throw new IllegalArgumentException("Context and key must not be null");
        }
        Object systemService = context.getSystemService(name);
        if (systemService == null) {
            context = context.getApplicationContext();
            systemService = context.getSystemService(name);
        }
        if (systemService == null) {
            throw new IllegalStateException(name + " not available");
        }
        return systemService;
    }

    protected void onProductPurchasedSuccess(String productId, String productName, long amount, PurchaseSucceededResponse presp) {
        ProductManager.setLicensed(mContext, productId);
        sendSuccessEventToListeners(productId);
    }

    private void sendSuccessEventToListeners(String productId) {
        CopyOnWriteArrayList<PurchaseProductListener> purchaseProductListeners = mListeners.get(productId);
        if (purchaseProductListeners != null && !purchaseProductListeners.isEmpty()) {
            for (PurchaseProductListener purchaseProductListener : purchaseProductListeners) {
                purchaseProductListener.onSuccess(productId);
            }
        }
    }

    private void sendErrorEventToListeners(PurchaseException purchaseException) {
        Set<String> keys = mListeners.keySet();
        if (keys != null && !keys.isEmpty()) {
            for (String key : keys) {
                CopyOnWriteArrayList<PurchaseProductListener> purchaseProductListeners = mListeners.get(key);
                if (purchaseProductListeners != null && !purchaseProductListeners.isEmpty()) {
                    for (PurchaseProductListener purchaseProductListener : purchaseProductListeners) {
                        purchaseProductListener.onError(purchaseException);
                    }
                }
            }
        }
    }

    private boolean isInitialized() {
        Localization localization = getLocalization();
        GetJarContext jarContext = getGetJarContext();
        GetJarPage getJarPage = getGetJarPage();
        return localization != null && jarContext != null && getJarPage != null;
    }

    public void buyLicensedProduct(final String productId, final int price, final int productNameResource, final int productDescriptionResource, final PurchaseProductListener purchaseProductListener) {
        if (ProductManager.isLicensed(mContext, productId)) {
            purchaseProductListener.onSuccess(productId);
            return;
        }
        if (isInitialized()) {
            addListener(productId, purchaseProductListener);
            ensureUserAuth("Auth", new EnsureUserAuthListener() {
                @Override
                public void userAuthCompleted(User user) {
                    if (user == null) {
                        purchaseProductListener.onError(new IllegalStateException("user authorization fails"));
                    } else {
                        loadRecommendedPrices(productId, price, productNameResource, productDescriptionResource, purchaseProductListener);
                    }
                }
            });
        } else {
            purchaseProductListener.onError(new ExceptionInInitializerError("can't initialize GetJar"));
        }

    }

    private void loadRecommendedPrices(final String productId, final int price, final int productNameResource, final int productDescriptionResource, final PurchaseProductListener purchaseProductListener) {
        final Pricing pricing = new Pricing(price);
        List<Pricing> prices = Arrays.asList(new Pricing[]{pricing});
        getLocalization().getRecommendedPricesAsync(prices, new RecommendedPricesListener() {
            @Override
            public void recommendedPricesEvent(RecommendedPrices recommendedPrices) {
                checkLicense(recommendedPrices, productId, pricing, productNameResource, productDescriptionResource, purchaseProductListener);
            }
        });
    }

    private void checkLicense(RecommendedPrices recommendedPrices, String productId, Pricing pricing, int productNameResource, int productDescriptionResource, final PurchaseProductListener purchaseProductListener) {
        GetJarContext getJarContext = getGetJarContext();
        final LicensableProduct licensableProduct = new LicensableProduct(
                productId,
                getJarContext.getAndroidContext().getString(productNameResource),
                getJarContext.getAndroidContext().getString(productDescriptionResource),
                recommendedPrices.getRecommendedPrice(pricing),
                License.LicenseScope.USER);
        Licensing licensing = new Licensing(getJarContext);
        licensing.isUnmanagedProductLicensedAsync(productId, new IsUnmanagedProductLicensedListener() {
            @Override
            public void isUnmanagedProductLicensedEvent(String productId, Boolean isLicensed) {
                if (isLicensed || ProductManager.isLicensed(mContext, productId)) {
                    ProductManager.setLicensed(mContext, productId);
                    purchaseProductListener.onSuccess(productId);
                    return;
                } else {
                    launchRewardsPage(licensableProduct);
                }
            }
        });
    }

    private void launchRewardsPage(Product product) {
        GetJarPage getJarPage = getGetJarPage();
        getJarPage.setProduct(product);
        getJarPage.showPage();
    }

    private void ensureUserAuth(String theTitle, EnsureUserAuthListener listener) {
        if(StringUtility.isNullOrEmpty(theTitle)){throw new IllegalArgumentException("theTitle cannot be null");}
        if(listener==null) {throw new IllegalArgumentException("listener cannot be null");}
        mUserAuth = new UserAuth(getGetJarContext());
        mUserAuth.ensureUserAsync(theTitle, listener);
    }


    public synchronized void unregisterListener(String productId, PurchaseProductListener purchaseProductListener) {
        CopyOnWriteArrayList<PurchaseProductListener> purchaseProductListeners = mListeners.get(productId);
        if (purchaseProductListeners != null && !purchaseProductListeners.isEmpty()) {
            purchaseProductListeners.remove(purchaseProductListener);
        }
    }

    private synchronized void addListener(String productId, PurchaseProductListener purchaseProductListener) {
        CopyOnWriteArrayList<PurchaseProductListener> purchaseProductListeners = mListeners.get(productId);
        if (purchaseProductListeners == null) {
            purchaseProductListeners = new CopyOnWriteArrayList<PurchaseProductListener>();
            purchaseProductListeners.add(purchaseProductListener);
            mListeners.put(productId, purchaseProductListeners);
        } else {
            if (!purchaseProductListeners.contains(purchaseProductListener)) {
                purchaseProductListeners.add(purchaseProductListener);
            }
        }
    }

    public static enum PurchaseErrorType {
        BLACKLISTED, DEVICE_UNSUPPORTED
    }

    public static class PurchaseException extends Exception {

        private PurchaseErrorType mPurchaseErrorType;

        public PurchaseException(String detailMessage, PurchaseErrorType purchaseErrorType) {
            super(detailMessage);
            this.mPurchaseErrorType = purchaseErrorType;
        }

        public PurchaseErrorType getPurchaseErrorType() {
            return mPurchaseErrorType;
        }
    }

    public void onProductPurchasedError(PurchaseErrorType purchaseErrorType, String metadata) {
        sendErrorEventToListeners(new PurchaseException(metadata, purchaseErrorType));
    }

    public static interface PurchaseProductListener {
        void onSuccess(String id);
        void onError(Throwable e);
    }

}
