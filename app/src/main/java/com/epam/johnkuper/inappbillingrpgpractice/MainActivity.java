package com.epam.johnkuper.inappbillingrpgpractice;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.epam.johnkuper.inappbillingrpgpractice.util.IabHelper;
import com.epam.johnkuper.inappbillingrpgpractice.util.IabResult;
import com.epam.johnkuper.inappbillingrpgpractice.util.Inventory;
import com.epam.johnkuper.inappbillingrpgpractice.util.Purchase;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "InAppBattleRPG";
    private static final String SKU_GOLD_SWORD = "sword_upgrade";
    private static final String SKU_HEALTH_BOTTLE = "health_bottle";
    private static final String DEVELOPER_PAYLOAD = "23khfhKSDFdflj239KJASD2345wefLasdfk";
    private static final int REQUEST_ID = 14556;

    private int mHeroHP = 100;
    private int mWolfHP = 100;
    private int mHeroDamage = 10;
    private int mWolfDamage = 20;
    private ImageView mBottle;
    private ImageView mHeroWeapon;
    private ImageView mWolfWeapon;
    private boolean mIsGameOver;
    private boolean mIsSwordUpgraded;
    private IabHelper mIabHelper;
    private Purchase mLastBottlePurchase;
    private Purchase mLastSwordPurchase;
    private TextView mTvHeroHP;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);
        setupViews();

        String base64EncodedPublicKey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAoR3OjeWGxON4YO2IpOiR7ormoF7RG+zXt3lV+fcukp5yM0083mqNH5N8Qsl9PEDo" +
                "8R0kBU+Cvgl/V0EOPq9BPyDEimMxgKh5B03S/jh4XNRZzTXZs9junJa4icN/HuVyMRCMYpE29DJYbgOsYtxqpTAxfDFKS12JALbHEfz8OOcaYxa1vntl5EVd3UJtQPkUT/Mo" +
                "8ZWRuFEdUxXfOj4gVW3n3Chbo9FMVgDKyce7WeJSPpM4O4Hgs3v+i8hcTNLjJsPFJRKoHRLh6lsJlc0PwOOpP2V9EjrKeX7lIJVsCsJCvwrQHQ47hT9+FvqHi30pW5rdK+sc" +
                "UvkMj3iIKj1/RwIDAQAB";

        // Create the helper, passing it our context and the public key to verify signatures with
        Log.d(TAG, "Creating IAB helper.");
        mIabHelper = new IabHelper(MainActivity.this, base64EncodedPublicKey);

        // enable debug logging (for a production application, you should set this to false).
        mIabHelper.enableDebugLogging(true);

        // Start setup. This is asynchronous and the specified listener
        // will be called once setup completes.
        Log.d(TAG, "Starting setup.");
        mIabHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
            public void onIabSetupFinished(IabResult result) {
                Log.d(TAG, "Setup finished.");

                if (!result.isSuccess()) {
                    // Oh noes, there was a problem.
                    showAlert("Problem setting up in-app billing: " + result);
                    return;
                }

                // Have we been disposed of in the meantime? If so, quit.
                if (mIabHelper == null) return;

                // IAB is fully set up. Now, let's get an inventory of stuff we own.
                Log.d(TAG, "Setup successful. Querying inventory.");
                mIabHelper.queryInventoryAsync(mGotInventoryListener);
            }
        });
    }

    private void setupViews() {
        mTvHeroHP = (TextView) findViewById(R.id.tvHeroHp);
        mTvHeroHP.setText(String.valueOf(mHeroHP));
        final TextView tvWolfHP = (TextView) findViewById(R.id.tvWolfHp);
        tvWolfHP.setText(String.valueOf(mWolfHP));

        View.OnClickListener onClickListener = new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                v.setClickable(false);
                Animation shakeAnimation = AnimationUtils.loadAnimation(MainActivity.this, R.anim.shake);
                shakeAnimation.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {
                        switch (v.getId()) {
                            case R.id.ivHeroWeapon:
                                if (mWolfHP <= mHeroDamage) {
                                    mWolfHP = 0;
                                    tvWolfHP.setText(String.valueOf(mWolfHP));
                                    mIsGameOver = true;
                                    sayToast(getString(R.string.wolf_was_defeated));
                                } else {
                                    mWolfHP = mWolfHP - mHeroDamage;
                                    tvWolfHP.setText(String.valueOf(mWolfHP));
                                }
                                break;
                            case R.id.ivWolfWeapon:
                                if (mHeroHP <= mWolfDamage) {
                                    mHeroHP = 0;
                                    mTvHeroHP.setText(String.valueOf(mHeroHP));
                                    mIsGameOver = true;
                                    sayToast(getString(R.string.hero_was_defeated));
                                } else {
                                    mHeroHP = mHeroHP - mWolfDamage;
                                    mTvHeroHP.setText(String.valueOf(mHeroHP));
                                }
                                break;
                            case R.id.ivHeroBottle:
                                consumeHealthBottle();
                                break;
                        }
                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        switch (v.getId()) {
                            case R.id.ivHeroWeapon:
                                if (mIsGameOver) {
                                    setupWeaponsVisibility(View.GONE);
                                    break;
                                }
                                mHeroWeapon.setVisibility(View.GONE);
                                mWolfWeapon.setVisibility(View.VISIBLE);
                                break;
                            case R.id.ivWolfWeapon:
                                if (mIsGameOver) {
                                    setupWeaponsVisibility(View.GONE);
                                    break;
                                }
                                mWolfWeapon.setVisibility(View.GONE);
                                mHeroWeapon.setVisibility(View.VISIBLE);
                                break;
                        }
                        v.setClickable(true);
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {

                    }
                });
                v.startAnimation(shakeAnimation);
            }
        };

        mBottle = (ImageView) findViewById(R.id.ivHeroBottle);
        mHeroWeapon = (ImageView) findViewById(R.id.ivHeroWeapon);
        mWolfWeapon = (ImageView) findViewById(R.id.ivWolfWeapon);

        mBottle.setOnClickListener(onClickListener);
        mHeroWeapon.setOnClickListener(onClickListener);
        mWolfWeapon.setOnClickListener(onClickListener);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (mIsGameOver) {
            sayToast(getString(R.string.game_is_over));
            return true;
        }
        switch (item.getItemId()) {
            case R.id.action_buy_bottle:
                onHealthButtonActionClick();
                return true;
            case R.id.action_upgrade_sword:
                onSwordUpgradeActionClick();
                return true;
            case R.id.action_drop_sword:
                dropSword();
                return true;
        }
        return false;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        // very important:
        Log.d(TAG, "Destroying helper.");
        if (mIabHelper != null) {
            mIabHelper.dispose();
            mIabHelper = null;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "onActivityResult(" + requestCode + "," + resultCode + "," + data);
        if (mIabHelper == null) return;

        // Pass on the activity result to the helper for handling
        if (!mIabHelper.handleActivityResult(requestCode, resultCode, data)) {
            // not handled, so handle it ourselves (here's where you'd
            // perform any handling of activity results not related to in-app
            // billing...
            super.onActivityResult(requestCode, resultCode, data);
        } else {
            Log.d(TAG, "onActivityResult handled by IABUtil.");
        }
    }

    // Listener that's called when we finish querying the items and subscriptions we own
    IabHelper.QueryInventoryFinishedListener mGotInventoryListener = new IabHelper.QueryInventoryFinishedListener() {
        public void onQueryInventoryFinished(IabResult result, Inventory inventory) {
            Log.d(TAG, "Query inventory finished.");

            // Have we been disposed of in the meantime? If so, quit.
            if (mIabHelper == null) return;

            // Is it a failure?
            if (result.isFailure()) {
                showAlert("Failed to query inventory: " + result);
                return;
            }

            Log.d(TAG, "Query inventory was successful.");

            /*
             * Check for items we own. Notice that for each purchase, we check
             * the developer payload to see if it's correct! See
             * verifyDeveloperPayload().
             */

            // Do we have the sword upgrade?
            Purchase swordPurchase = inventory.getPurchase(SKU_GOLD_SWORD);
            mIsSwordUpgraded = (swordPurchase != null && verifyDeveloperPayload(swordPurchase));
            Log.d(TAG, "Hero with " + (mIsSwordUpgraded ? "GOLD" : "WOODEN") + " SWORD");
            if (mIsSwordUpgraded) {
                mHeroWeapon.setImageDrawable(ContextCompat.getDrawable(MainActivity.this, R.drawable.ic_gold_sword));
                mHeroDamage = 30;
                mLastSwordPurchase = swordPurchase;
            }

            Purchase bottlePurchase = inventory.getPurchase(SKU_HEALTH_BOTTLE);
            if (bottlePurchase != null && verifyDeveloperPayload(bottlePurchase)) {
                Log.d(TAG, "We have bottle. Displayed it.");
                mBottle.setVisibility(View.VISIBLE);
                mLastBottlePurchase = bottlePurchase;
                return;
            }
            Log.d(TAG, "Initial inventory query finished; enabling main UI.");
        }
    };

    // Callback for when a purchase is finished
    IabHelper.OnIabPurchaseFinishedListener mPurchaseFinishedListener = new IabHelper.OnIabPurchaseFinishedListener() {
        public void onIabPurchaseFinished(IabResult result, Purchase purchase) {
            Log.d(TAG, "Purchase finished: " + result + ", purchase: " + purchase);

            // if we were disposed of in the meantime, quit.
            if (mIabHelper == null) return;

            if (result.isFailure()) {
                showAlert("Error purchasing: " + result);
                return;
            }
            if (!verifyDeveloperPayload(purchase)) {
                showAlert("Error purchasing. Authenticity verification failed.");
                return;
            }

            Log.d(TAG, "Purchase successful.");

            if (purchase.getSku().equals(SKU_HEALTH_BOTTLE)) {
                Log.d(TAG, "Purchase is health bottle. Setup UI");
                mBottle.setVisibility(View.VISIBLE);
                mLastBottlePurchase = purchase;
            } else if (purchase.getSku().equals(SKU_GOLD_SWORD)) {
                Log.d(TAG, "Purchase is sword upgrade. Congratulating user.");
                sayToast("Now your sword became very powerful");
                mIsSwordUpgraded = true;
                mHeroWeapon.setImageDrawable(ContextCompat.getDrawable(MainActivity.this, R.drawable.ic_gold_sword));
                mHeroDamage = 30;
                mLastSwordPurchase = purchase;
            }
        }
    };

    // Called when consumption is complete
    IabHelper.OnConsumeFinishedListener mConsumeFinishedListener = new IabHelper.OnConsumeFinishedListener() {
        public void onConsumeFinished(Purchase purchase, IabResult result) {
            Log.d(TAG, "Consumption finished. Purchase: " + purchase + ", result: " + result);

            if (mIabHelper == null) return;

            if (result.isSuccess()) {
                switch (purchase.getSku()) {
                    case SKU_GOLD_SWORD:
                        mHeroWeapon.setImageDrawable(ContextCompat.getDrawable(MainActivity.this, R.drawable.ic_wood_sword));
                        mHeroDamage = 10;
                        mLastSwordPurchase = null;
                        mIsSwordUpgraded = false;
                        sayToast("Your sword became very weak. Be careful");
                        break;
                    case SKU_HEALTH_BOTTLE:
                        mBottle.setVisibility(View.GONE);
                        mHeroHP += 50;
                        mTvHeroHP.setText(String.valueOf(mHeroHP));
                        mLastBottlePurchase = null;
                        sayToast("You feel much better");
                        break;
                }
                Log.d(TAG, "Consumption successful");
            } else {
                showAlert("Error while consuming: " + result);
            }
            Log.d(TAG, "End consumption flow.");
        }
    };

    private void onHealthButtonActionClick() {
        if (mBottle.getVisibility() == View.VISIBLE) {
            sayToast("You already have health bottle. Drink it first");
            return;
        }

        // launch the health bottle purchase UI flow.
        // We will be notified of completion via mPurchaseFinishedListener
//        setWaitScreen(true);
        Log.d(TAG, "Launching purchase flow for health bottle.");

        mIabHelper.launchPurchaseFlow(MainActivity.this, SKU_HEALTH_BOTTLE, REQUEST_ID,
                mPurchaseFinishedListener, DEVELOPER_PAYLOAD);
    }

    private void onSwordUpgradeActionClick() {
        if (mIsSwordUpgraded) {
            sayToast("Your sword already is very powerful");
            return;
        }

        Log.d(TAG, "Launching purchase flow for upgrade sword.");

        mIabHelper.launchPurchaseFlow(MainActivity.this, SKU_GOLD_SWORD, REQUEST_ID,
                mPurchaseFinishedListener, DEVELOPER_PAYLOAD);
    }

    private void consumeHealthBottle() {
        if (mLastBottlePurchase != null) {
            mIabHelper.consumeAsync(mLastBottlePurchase, mConsumeFinishedListener);
        }
    }

    private void dropSword() {
        if (mLastSwordPurchase != null) {
            mIabHelper.consumeAsync(mLastSwordPurchase, mConsumeFinishedListener);
        } else {
            sayToast("You can't drop your wooden sword");
        }
    }

    /**
     * Verifies the developer payload of a purchase.
     */
    boolean verifyDeveloperPayload(Purchase p) {
        return DEVELOPER_PAYLOAD.equals(p.getDeveloperPayload());
    }

    private void showAlert(String message) {
        Log.e(TAG, "**** InAppBillingRPG Error: " + message);
        AlertDialog.Builder bld = new AlertDialog.Builder(MainActivity.this);
        bld.setMessage("Error: " + message);
        bld.setNeutralButton("OK", null);
        Log.d(TAG, "Showing showAlert dialog: " + message);
        bld.create().show();
    }

    private void setupWeaponsVisibility(int visibility) {
        mHeroWeapon.setVisibility(visibility);
        mWolfWeapon.setVisibility(visibility);
        mBottle.setVisibility(visibility);
    }

    private void sayToast(String message) {
        Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show();
    }
}
