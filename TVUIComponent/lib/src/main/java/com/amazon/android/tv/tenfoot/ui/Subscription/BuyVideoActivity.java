package com.amazon.android.tv.tenfoot.ui.Subscription;


import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.amazon.android.contentbrowser.ContentBrowser;
import com.amazon.android.contentbrowser.helper.AuthHelper;
import com.amazon.android.contentbrowser.helper.PurchaseHelper;
import com.amazon.android.model.content.Content;
import com.amazon.android.model.event.ProductsUpdateEvent;
import com.amazon.android.model.event.ProgressOverlayDismissEvent;
import com.amazon.android.model.event.PurchaseEvent;
import com.amazon.android.tv.tenfoot.R;
import com.amazon.android.tv.tenfoot.ui.Subscription.Model.Consumer;
import com.amazon.android.ui.fragments.ErrorDialogFragment;
import com.amazon.android.utils.ErrorUtils;
import com.amazon.android.utils.NetworkUtils;
import com.amazon.android.utils.Preferences;
import com.zype.fire.auth.ZypeAuthentication;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by Evgeny Cherkasov on 24.09.2018.
 */

public class BuyVideoActivity extends Activity implements ErrorDialogFragment.ErrorDialogFragmentListener {
    private static final String TAG = BuyVideoActivity.class.getName();

    private static final int REQUEST_CREATE_LOGIN = 110;

    private LinearLayout layoutConfirm;

    private TextView textVideo;
    private LinearLayout layoutLogin;
    private Button buttonLogin;
    private LinearLayout layoutLoggedIn;
    private TextView textConsumerEmail;

    private Button buttonConfirm;
    private Button buttonRestore;
    private Button buttonCancel;

    private String sku = null;
    private String price = null;
    private boolean isVideoPurchased = false;

    private ContentBrowser contentBrowser;
    private ErrorDialogFragment dialogError = null;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_buy_video);

        contentBrowser = ContentBrowser.getInstance(this);

        layoutConfirm = (LinearLayout) findViewById(R.id.layoutConfirm);

        textVideo = (TextView) findViewById(R.id.textVideo);

        layoutLogin = (LinearLayout) findViewById(R.id.layoutLogin);
        buttonLogin = (Button) findViewById(R.id.buttonLogin);
        buttonLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onLogin();
            }
        });

        layoutLoggedIn = (LinearLayout) findViewById(R.id.layoutLoggeIn);
        textConsumerEmail = (TextView) findViewById(R.id.textConsumerEmail);

        buttonConfirm = (Button) findViewById(R.id.buttonConfirm);
        buttonConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onConfirm();
            }
        });

        buttonRestore = (Button) findViewById(R.id.buttonRestore);
        buttonRestore.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onRestore();
            }
        });

        buttonCancel = (Button) findViewById(R.id.buttonCancel);
        buttonCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onCancel();
            }
        });

        updateViews();
        bindViews();

        // TODO: Get sku from the 'Marketplace ids' field of the video object
        sku = "test.sku";
        Set<String> skuSet = new HashSet<>();
        skuSet.add(sku);
        ContentBrowser.getInstance(this).getPurchaseHelper().handleProductsChain(this, skuSet);
    }

    @Override
    protected void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    protected void onStop() {
        EventBus.getDefault().unregister(this);
        EventBus.getDefault().post(new ProgressOverlayDismissEvent(true));
        super.onStop();
    }

    // //////////
    // UI
    //
    private void bindViews() {
        Content content = ContentBrowser.getInstance(this).getLastSelectedContent();
        textVideo.setText(content.getTitle());
    }

    private void updateViews() {
        if (isVideoPurchased) {
            buttonConfirm.setVisibility(View.GONE);
            buttonRestore.setVisibility(View.VISIBLE);
        }
        else {
            buttonConfirm.setVisibility(View.VISIBLE);
            buttonRestore.setVisibility(View.GONE);
            if (!TextUtils.isEmpty(price)) {
                buttonConfirm.setText(String.format(getString(R.string.buy_video_button_confirm_price), price));
            } else {
                buttonConfirm.setText(getString(R.string.buy_video_button_confirm));
            }
        }
        if (contentBrowser.isUserLoggedIn()) {
            layoutLoggedIn.setVisibility(View.VISIBLE);
            layoutLogin.setVisibility(View.GONE);
            textConsumerEmail.setText(Preferences.getString(ZypeAuthentication.PREFERENCE_CONSUMER_EMAIL));
        }
        else {
            layoutLoggedIn.setVisibility(View.GONE);
            layoutLogin.setVisibility(View.VISIBLE);
        }
    }

    private void closeScreen() {
        contentBrowser.onAuthenticationStatusUpdateEvent(new AuthHelper.AuthenticationStatusUpdateEvent(true));
        EventBus.getDefault().post(new AuthHelper.AuthenticationStatusUpdateEvent(true));

        setResult(RESULT_OK);
        finish();
    }

    // //////////
    // Actions
    //
    private void onLogin() {
        contentBrowser.getAuthHelper()
                .isAuthenticated()
                .subscribe(isAuthenticatedResultBundle -> {
                    if (isAuthenticatedResultBundle.getBoolean(AuthHelper.RESULT)) {
                        updateViews();
                    }
                    else {
                        contentBrowser.getAuthHelper()
                                .authenticateWithActivity()
                                .subscribe(resultBundle -> {
                                    if (resultBundle != null) {
                                        if (resultBundle.getBoolean(AuthHelper.RESULT)) {
                                            updateViews();
                                        }
                                        else {
                                            contentBrowser.getNavigator().runOnUpcomingActivity(() -> contentBrowser.getAuthHelper()
                                                    .handleErrorBundle(resultBundle));
                                        }
                                    }
                                    else {
                                        updateViews();
                                    }
                                });
                    }
                });
    }

    private void onConfirm() {
        buyVideo();
    }

    private void onRestore() {
        restorePurchase();
    }

    private void onCancel() {
        setResult(RESULT_CANCELED);
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        contentBrowser.handleOnActivityResult(this, requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_CREATE_LOGIN:
                updateViews();
                break;
        }
    }

    //
    // ErrorDialogFragmentListener
    //
    /**
     * Callback method to define the button behaviour for this activity.
     *
     * @param errorDialogFragment The fragment listener.
     * @param errorButtonType     The display text on the button
     * @param errorCategory       The error category determined by the client.
     */
    @Override
    public void doButtonClick(ErrorDialogFragment errorDialogFragment, ErrorUtils.ERROR_BUTTON_TYPE errorButtonType, ErrorUtils.ERROR_CATEGORY errorCategory) {
        if (dialogError  != null) {
            dialogError .dismiss();
        }
    }

    private void buyVideo() {
        contentBrowser.getPurchaseHelper().setBuyVideoSKU(sku);
        if (contentBrowser.isUserLoggedIn()) {
            contentBrowser.actionTriggered(this, contentBrowser.getLastSelectedContent(),
                    ContentBrowser.CONTENT_ACTION_BUY, null, null);
        }
        else {
            Intent intent = new Intent(BuyVideoActivity.this, CreateLoginActivity.class);
            startActivityForResult(intent, REQUEST_CREATE_LOGIN);
        }

    }

    private void restorePurchase() {

    }

    private void login(Consumer consumer) {
        if (NetworkUtils.isConnectedToNetwork(this)) {
            (new AsyncTask<Void, Void, Map>() {
                @Override
                protected Map<String, Object> doInBackground(Void... params) {
                    return ZypeAuthentication.getAccessToken(consumer.email, consumer.password);
                }

                @Override
                protected void onPostExecute(Map response) {
                    super.onPostExecute(response);
                    if (response != null) {
                        // Successful login.
                        ZypeAuthentication.saveAccessToken(response);
                        closeScreen();
                    }
                    else {
                        dialogError = ErrorDialogFragment.newInstance(BuyVideoActivity.this, ErrorUtils.ERROR_CATEGORY.ZYPE_VERIFY_SUBSCRIPTION_ERROR, BuyVideoActivity.this);
                        dialogError.show(getFragmentManager(), ErrorDialogFragment.FRAGMENT_TAG_NAME);
                    }
                }
            }).execute();
        }
        else {
            dialogError = ErrorDialogFragment.newInstance(BuyVideoActivity.this, ErrorUtils.ERROR_CATEGORY.NETWORK_ERROR, BuyVideoActivity.this);
            dialogError.show(getFragmentManager(), ErrorDialogFragment.FRAGMENT_TAG_NAME);
        }
    }

    /**
     * Event bus event listener method to detect products broadcast.
     *
     * @param event Broadcast event for progress overlay dismiss.
     */
    @Subscribe
    public void onProductsUpdateEvent(ProductsUpdateEvent event) {
        ArrayList<HashMap<String, String>> products = (ArrayList<HashMap<String, String>>) event.getExtras().getSerializable(PurchaseHelper.RESULT_PRODUCTS);
        if (products != null && !products.isEmpty()) {
            for (HashMap<String, String> product : products) {
                if (product.get("SKU").equals(sku)) {
                    price = products.get(0).get("Price");
                    // TODO: Check if the product already purchased and update action button - Buy ot Restore
                    break;
                }
            }
            updateViews();
        }
        else {
            dialogError = ErrorDialogFragment.newInstance(BuyVideoActivity.this,
                    ErrorUtils.ERROR_CATEGORY.ZYPE_BUY_VIDEO_ERROR_PRODUCT,
                    BuyVideoActivity.this);
            dialogError.show(getFragmentManager(), ErrorDialogFragment.FRAGMENT_TAG_NAME);
        }
    }

    /**
     * Event bus listener method to detect purchase broadcast.
     *
     * @param event Broadcast event for progress overlay dismiss.
     */
    @Subscribe
    public void onPurchaseEvent(PurchaseEvent event) {
        if (event.getExtras().getBoolean(PurchaseHelper.RESULT)) {
            if (event.getExtras().getBoolean(PurchaseHelper.RESULT_VALIDITY)) {
                isVideoPurchased = true;
                closeScreen();
            } else {
                isVideoPurchased = false;
                updateViews();
                dialogError = ErrorDialogFragment.newInstance(BuyVideoActivity.this,
                        ErrorUtils.ERROR_CATEGORY.ZYPE_BUY_VIDEO_ERROR_VERIFY,
                        BuyVideoActivity.this);
                dialogError.show(getFragmentManager(), ErrorDialogFragment.FRAGMENT_TAG_NAME);
            }
        }
    }


}
