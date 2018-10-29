package com.gtu.ozturk.fourinarow;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.CompoundButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;
import java.util.Locale;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;

public class MainActivity extends Activity {
    private TextView selectedSize;
    private SeekBar sizeSelector;
    private TextView selectedTime;
    private SeekBar timeSelector;
    private TextView selectedDifficulty;
    private SeekBar difficultySelector;
    private RadioButton pvp;
    private RadioButton cvp;
    private final int MINSIZE = 5;
    private final int MAXSIZE = 40;
    private final int MAXSECONDS = 61;
    private final int MINSECONDS = 5;
    private final int MINDIFFICULTY = 1;//will multiply by 2 for AI Search Depth
    private final int MAXDIFFICULTY = 4;

    private AdView mAdView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //making the window fullscreen
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);

        MobileAds.initialize(this,
                "ca-app-pub-3940256099942544~3347511713");
        mAdView = (AdView) findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);


        selectedSize = (TextView) findViewById(R.id.boardSizeText);
        sizeSelector = (SeekBar) findViewById(R.id.sizeSeekBar);
        selectedTime = (TextView) findViewById(R.id.timerText);
        selectedDifficulty = (TextView) findViewById(R.id.difficultyText);
        timeSelector = (SeekBar) findViewById(R.id.timerSeekBar);
        difficultySelector = (SeekBar) findViewById(R.id.difficultySeekBar);

        pvp = (RadioButton) findViewById(R.id.pvpRadiobutton);
        cvp = (RadioButton) findViewById(R.id.cvpRadiobutton);
        //what is that really
        sizeSelector.setMax(MAXSIZE - MINSIZE);
        selectedSize.setText(String.format(Locale.getDefault(),"%s%d%s%d",
                getResources().getString(R.string.size_seekbar), MINSIZE, "X", MINSIZE));

        difficultySelector.setMax(MAXDIFFICULTY - MINDIFFICULTY);
        selectedDifficulty.setText(String.format(Locale.getDefault(), "%s", R.string.ai_difficulty));
        timeSelector.setMax(MAXSECONDS - MINSECONDS);
        selectedTime.setText(String.format(Locale.getDefault(), "%s%s%s",
                getResources().getString(R.string.time_limit), " ", getResources().getString(R.string.timer_selector)));
        timeSelector.setProgress(MAXSECONDS + MINSECONDS);

        timeSelector.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            /**
             * Updates time limit text according to seekbar change
             */
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                progress = progress + MINSECONDS;
                if (timeSelector.getProgress() + MINSECONDS >= MAXSECONDS)
                    selectedTime.setText(String.format(Locale.getDefault(), "%s%s%s%s",
                            getResources().getString(R.string.time_limit), " ",
                            " ", getResources().getString(R.string.timer_selector)));
                else
                    selectedTime.setText(String.format(Locale.getDefault(),"%s%s%d%s%s",
                        getResources().getString(R.string.time_limit), " ",
                            progress, " ", getResources().getString(R.string.time_metric)));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });


        //Changing Size Text according to seekbar change
        sizeSelector.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean b) {
                progress = progress + MINSIZE;
                selectedSize.setText(String.format(Locale.getDefault(),"%s%d%s%d",
                        getResources().getString(R.string.size_seekbar), progress, "X", progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        //Updates Ai difficulty text
        difficultySelector.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                progress = progress + MINDIFFICULTY;

                switch (progress) {
                    case 1:
                        selectedDifficulty.setText(String.format(Locale.getDefault(),"%s%s%s",
                                getResources().getString(R.string.ai_difficulty), " ",
                                getResources().getString(R.string.very_easy) ));
                        break;
                    case 2:
                        selectedDifficulty.setText(String.format(Locale.getDefault(),"%s%s%s",
                                getResources().getString(R.string.ai_difficulty), " ",
                                getResources().getString(R.string.easy) ));
                        break;
                    case 3:
                        selectedDifficulty.setText(String.format(Locale.getDefault(),"%s%s%s",
                                getResources().getString(R.string.ai_difficulty), " ",
                                getResources().getString(R.string.medium) ));
                        break;
                    case 4:
                        selectedDifficulty.setText(String.format(Locale.getDefault(),"%s%s%s",
                                getResources().getString(R.string.ai_difficulty), " ",
                                getResources().getString(R.string.hard) ));
                        break;
                }


            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        //Opens up ai menu if player versus computer mode is selected
        pvp.setOnCheckedChangeListener(new RadioButton.OnCheckedChangeListener() {
            /**
             * Called when the checked state of a compound button has changed.
             *
             * @param buttonView The compound button view whose state has changed.
             * @param isChecked  The new checked state of buttonView.
             */
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    difficultySelector.setVisibility(View.GONE);
                    selectedDifficulty.setVisibility(View.GONE);
                }

                else {
                    difficultySelector.setVisibility(View.VISIBLE);
                    selectedDifficulty.setVisibility(View.VISIBLE);
                    selectedDifficulty.setText(String.format(Locale.getDefault(),"%s%s%s",
                            getResources().getString(R.string.ai_difficulty), " ",
                            getResources().getString(R.string.very_easy) ));
                    //Changing visibility breaks configuration
                    difficultySelector.setProgress(MINDIFFICULTY);

                }
            }
        });
    }

    /**
     * Starts the game activity with passing game mode time limit size and aiDepth(intelligence)
     * intent extras to it
     * @param view for using as button function
     */
    public void  startGame(View view) {
        Intent intent = new Intent(this, GameActivity.class);
        intent.putExtra("size", sizeSelector.getProgress() + MINSIZE);
        if (timeSelector.getProgress() + MINSECONDS >= MAXSECONDS)
            intent.putExtra("timeLimit", -1);
        else
        intent.putExtra("timeLimit", timeSelector.getProgress() + MINSIZE);

        if (pvp.isChecked())
            intent.putExtra("gameMode", true);
        else
            intent.putExtra("gameMode", false);

        //Ai has 4 different intelligence states and also seekbar does
        //Calculating the actual tree depth for ai before passing it as an intent extra
        intent.putExtra("aiDepth", 2*(difficultySelector.getProgress() + MINDIFFICULTY));
        startActivity(intent);

    }
}
