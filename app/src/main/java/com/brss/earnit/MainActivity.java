package com.brss.earnit;

import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.reward.RewardItem;
import com.google.android.gms.ads.reward.RewardedVideoAd;
import com.google.android.gms.ads.reward.RewardedVideoAdListener;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.iid.FirebaseInstanceId;

public class MainActivity extends AppCompatActivity implements RewardedVideoAdListener {

    private FirebaseAuth mAuth;
    private Toolbar toolbar;
    private TextView totalAds;
    private TextView totalInterAds;
    private TextView totalClickCount;
    private int totalAdsCount = 0;
    private int totalInterAdsCount = 0;
    private int totalClickAdsCount = 0;
    Menu context_menu;
    private ProgressDialog adProgress;
    private ProgressDialog progress;
    private Handler handler = new Handler();
    public String uid ;
    private Button watchAd;
    private Button updateCount;
    private AdView mAdView;
    private AdView mAdView2;
    private DatabaseReference mDatabase;
    private InterstitialAd mInterstitialAd;
    private RewardedVideoAd mRewardedVideoAd;
    SharedPreferences sharedPreferences;
    SharedPreferences sharedPreferencesClick;
    SharedPreferences sharedPreferencesInter;
    private NotificationManagerCompat notificationManager;

    private static final int NOTIFICATION_ID = 1;
    private static final String NOTIFICATION_CHANNEL_ID = "my_notification_channel";

//    FirebaseUser currentUser = mAuth.getCurrentUser();


    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        toolbar = (Toolbar) findViewById(R.id.main_page_toolbar);
        setSupportActionBar(toolbar);

        mAuth = FirebaseAuth.getInstance();
        if(mAuth.getCurrentUser() != null){
            uid = mAuth.getCurrentUser().getUid();
        }

        Common.currentToken= FirebaseInstanceId.getInstance().getToken();

        watchAd = (Button) findViewById(R.id.watchAd);
        updateCount = (Button) findViewById(R.id.updateCount);


        totalAds = (TextView) findViewById(R.id.totalAds);
        totalInterAds = (TextView) findViewById(R.id.totalInterAds);
        totalClickCount = (TextView) findViewById(R.id.totalClickCount);

        sharedPreferences = getSharedPreferences("COUNT",MODE_PRIVATE);
        sharedPreferencesInter = getSharedPreferences("COUNTINTER",MODE_PRIVATE);
        sharedPreferencesClick = getSharedPreferences("CLICK",MODE_PRIVATE);

        totalAdsCount = sharedPreferences.getInt("PRVS",0);
        totalClickAdsCount = sharedPreferencesClick.getInt("PRVSCLICK",0);
        totalInterAdsCount = sharedPreferencesInter.getInt("PRVSINTER",0);

        totalAds.setText("Total Videos Watched : "+sharedPreferences.getInt("PRVS",0));
        totalClickCount.setText("Total Clicks : "+sharedPreferencesClick.getInt("PRVSCLICK",0));
        totalInterAds.setText("Total Photos Watched : "+sharedPreferencesInter.getInt("PRVSINTER",0));


        adProgress = new ProgressDialog(this);
        adProgress.setTitle("Loading your AD");
        adProgress.setMessage("Please wait while we load your Ad.");
        adProgress.setCanceledOnTouchOutside(false);

        mAdView = findViewById(R.id.bannerView1);
        mAdView2 = findViewById(R.id.bannerView2);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);
        mAdView2.loadAd(adRequest);


        mInterstitialAd = new InterstitialAd(this);
        mInterstitialAd.setAdUnitId("ca-app-pub-6354922265068750/6131115467");
        mInterstitialAd.loadAd(new AdRequest.Builder().build());
        mInterstitialAd.setAdListener(new AdListener() {
            @Override
            public void onAdClosed() {
                // Load the next interstitial.
                mInterstitialAd.loadAd(new AdRequest.Builder().build());
                ++totalInterAdsCount;
                Toast.makeText(MainActivity.this, "You watched an Inter Ad!" , Toast.LENGTH_SHORT).show();
                SharedPreferences.Editor editor = sharedPreferencesInter.edit();
                editor.putInt("PRVSINTER",totalInterAdsCount);
                editor.apply();
                totalInterAds.setText("Total Photos Watched : "+sharedPreferencesInter.getInt("PRVSINTER",0));
            }

            @Override
            public void onAdLeftApplication() {
                ++totalClickAdsCount;
                Toast.makeText(MainActivity.this, "You Clicked on an Ad!" , Toast.LENGTH_SHORT).show();
                SharedPreferences.Editor editor = sharedPreferencesClick.edit();
                editor.putInt("PRVSCLICK",totalClickAdsCount);
                editor.apply();
                totalClickCount.setText("Total Clicks : "+sharedPreferencesClick.getInt("PRVSCLICK",0));
            }

        });

        MobileAds.initialize(this, "ca-app-pub-6354922265068750/7827340511");

        mRewardedVideoAd = MobileAds.getRewardedVideoAdInstance(this);
        mRewardedVideoAd.setRewardedVideoAdListener(this);
        loadRewardedVideoAd();

        watchAd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (mInterstitialAd.isLoaded()) {
                    mInterstitialAd.show();
                    if (mRewardedVideoAd.isLoaded()) {
                        mRewardedVideoAd.show();
                        if (mInterstitialAd.isLoaded()) {
                            mInterstitialAd.show();
                        }
                    }
                } else {
                    adProgress.show();
                    loadAdAfterDelay();
                    loadRewardedVideoAd();
                    mInterstitialAd.loadAd(new AdRequest.Builder().build());
                }

            }
        });



        updateCount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {


                mDatabase = FirebaseDatabase.getInstance().getReference("Users").child(uid);
                mDatabase.keepSynced(true);
                progress = new ProgressDialog(MainActivity.this);
                progress.setTitle("Updating Database");
                progress.setMessage("Please wait while we update your Count!");
                progress.setCanceledOnTouchOutside(false);
                progress.show();
                mDatabase.child("Total_Ads").setValue(""+sharedPreferences.getInt("PRVS",0)).addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        mDatabase.child("Total_InterAds").setValue(""+sharedPreferencesInter.getInt("PRVSINTER",0)).addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                mDatabase.child("Total_ClickAds").setValue(""+sharedPreferencesClick.getInt("PRVSCLICK",0)).addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        if (task.isSuccessful()) {
                                            mInterstitialAd.loadAd(new AdRequest.Builder().build());
                                            loadRewardedVideoAd();
                                            Toast.makeText(MainActivity.this, "Updated Successfully!", Toast.LENGTH_SHORT).show();
                                            notificationShow();
                                            progress.dismiss();

                                        }
                                        else {

                                            progress.hide();

                                            String task_result = task.getException().getMessage().toString();

                                            Toast.makeText(MainActivity.this, "Error : " + task_result, Toast.LENGTH_LONG).show();
                                            Log.d("Error : ", task_result);

                                        }
                                    }
                                });

                            }
                        });


                    }
                });
            }
        });


    }

    public void loadAdAfterDelay() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (mInterstitialAd.isLoaded()) {
                    adProgress.dismiss();
                    mInterstitialAd.show();
                } else {
                    loadAdAfterDelay();
                    loadRewardedVideoAd();
                }
            }
        }, 1000);
    }

    private void loadRewardedVideoAd() {
        mRewardedVideoAd.loadAd("ca-app-pub-6354922265068750/9391703627",
                new AdRequest.Builder().build());
    }

    @Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly.

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {

            sendToStart();

        } else {

//            Toast.makeText(this, "Your session has expired", Toast.LENGTH_LONG).show();

        }


    }


    @Override
    protected void onStop() {
        super.onStop();

        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {

            sendToStart();

        } else {

//            Toast.makeText(this, "Your session has expired", Toast.LENGTH_LONG).show();

        }
    }

    private void sendToStart() {

        Intent startIntent = new Intent(MainActivity.this, LoginActivity.class);
        startActivity(startIntent);
        finish();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);

        if (item.getItemId() == R.id.main_logout_btn) {

            //  mUserRef.child("online").setValue(ServerValue.TIMESTAMP);

            FirebaseAuth.getInstance().signOut();
            sendToStart();

        }
        if (item.getItemId() == R.id.main_settings_btn) {

            Intent settingsIntent = new Intent(MainActivity.this, AccountSettingsActivity.class);
            startActivity(settingsIntent);

        }

        return true;
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onRewarded(RewardItem reward) {
        ++totalAdsCount;
        Toast.makeText(this, "You watched an Ad!" , Toast.LENGTH_SHORT).show();

        SharedPreferences.Editor editor = sharedPreferences.edit();

        editor.putInt("PRVS",totalAdsCount);
        editor.apply();
        totalAds.setText("Total Video Ads Watched : "+sharedPreferences.getInt("PRVS",0));
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onRewardedVideoAdLeftApplication() {
        ++totalClickAdsCount;
        Toast.makeText(MainActivity.this, "You Clicked on an Ad!" , Toast.LENGTH_SHORT).show();
        SharedPreferences.Editor editor = sharedPreferencesClick.edit();
        editor.putInt("PRVSCLICK",totalInterAdsCount);
        editor.apply();
        totalInterAds.setText("Total Clicks : "+sharedPreferencesClick.getInt("PRVSCLICK",0));
    }

    @Override
    public void onRewardedVideoAdClosed() {
        loadRewardedVideoAd();
    }

    @Override
    public void onRewardedVideoAdFailedToLoad(int errorCode) {
        loadRewardedVideoAd();
    }

    @Override
    public void onRewardedVideoAdLoaded() {
    }

    @Override
    public void onRewardedVideoAdOpened() {
    }

    @Override
    public void onRewardedVideoStarted() {
    }

    @Override
    public void onResume() {
        mRewardedVideoAd.resume(this);
        super.onResume();
    }

    @Override
    public void onPause() {
        mRewardedVideoAd.pause(this);
        super.onPause();
    }

    @Override
    public void onDestroy() {
        mRewardedVideoAd.destroy(this);
        super.onDestroy();
    }

    public void notificationShow(){



        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel notificationChannel = new NotificationChannel(NOTIFICATION_CHANNEL_ID,
                    "My Notifications",
                    NotificationManager.IMPORTANCE_HIGH);

            // Configure the notification channel.
            notificationChannel.setDescription("Channel description");
            notificationChannel.enableLights(true);
            notificationManager.createNotificationChannel(notificationChannel);

        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("Previous ad counts were saved successfully")
                .setContentText("Total Ads : "+(sharedPreferences.getInt("PRVS",0)+sharedPreferencesInter.getInt("PRVSINTER",0)))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE);

        notificationManager.notify(NOTIFICATION_ID, builder.build());




    }
}



