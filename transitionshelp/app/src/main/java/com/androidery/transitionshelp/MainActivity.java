package com.androidery.transitionshelp;

import android.os.Handler;
import android.support.transition.TransitionManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.Random;

public class MainActivity extends AppCompatActivity {

    private static final int MAX_LOADING_TIME = 5000; // milliseconds

    private ViewGroup sceneRoot;
    private EditText etGibberish;
    private Button btnSubmit;
    private TextView tvDesc, tvError, tvErrorDesc, tvLoading;
    private ImageView ivLoading;
    private RelativeLayout rlLoading;
    private Animation animLoading;
    private boolean hasSceneErrorScreen = false;

    private enum ActivityState {
        STATE_INITIAL,
        STATE_LOADING,
        STATE_ERROR,
        STATE_FINAL
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sceneRoot = (ViewGroup) findViewById(R.id.root);

        tvDesc = (TextView) findViewById(R.id.tv_desc);
        tvError = (TextView) findViewById(R.id.tv_error);
        tvErrorDesc = (TextView) findViewById(R.id.tv_error_desc);
        tvLoading = (TextView) findViewById(R.id.tv_loading);

        etGibberish = (EditText) findViewById(R.id.et_gibberish);
        ivLoading = (ImageView) findViewById(R.id.iv_loading);
        btnSubmit = (Button) findViewById(R.id.btn_submit);

        rlLoading = (RelativeLayout) findViewById(R.id.rl_loading);

        // this may need to be accessed by multiple states
        animLoading = AnimationUtils.loadAnimation(this, R.anim.rotate_simple);

        // kick everything off by transitioning to itself
        transitionTo(ActivityState.STATE_INITIAL);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    // if you only have two or three states, you can do them all without writing helper functions
    private void transitionTo(ActivityState state) {

        switch(state) {
            case STATE_INITIAL:
                // within each state you have to change the layout back to how it was, only change
                // back stuff that could have changed in another state
                alignViewBottom(tvError, R.id.tv_desc); // error could have been showing
                alignViewBottom(tvErrorDesc, R.id.rl_gibberish_content); // error desc could have been showing
                alignViewBottom(rlLoading, R.id.rl_gibberish_content); // loading could have been showing

                etGibberish.setEnabled(true);

                tvDesc.setTextColor(getResources().getColor(R.color.white));

                // we also need to set how we transition to next state
                btnSubmit.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // MAKE SURE TO GET THE TRANSITIONS BACKPORT FOR TRANSITION GOODNESS DOWN TO API14
                        android.support.transition.TransitionManager.beginDelayedTransition(sceneRoot);

                        // after the call you can change the layout however you like and the framework will
                        // do the rest. We are using a switch statement to take care of all the transition
                        // information, so we just call our function
                        transitionTo(ActivityState.STATE_LOADING);

                        // hide keyboard
                        View view = getCurrentFocus();
                        if (view != null) {
                            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                        }
                    }
                });

                break;
            case STATE_ERROR:
                // FROM: loading
                // TO: initial, loading
                setViewBelow(tvError, R.id.tv_desc); // show error
                setViewBelow(tvErrorDesc, R.id.rl_gibberish_content); // show error desc
                alignViewBottom(rlLoading, R.id.rl_gibberish_content); // hide loading layout

                ivLoading.clearAnimation();

                etGibberish.setEnabled(true);

                // dynamically change text content
                if (hasSceneErrorScreen) {
                    tvError.setText("Kittens sure are adorable");
                    tvErrorDesc.setText("Please insert the correct gibberish: kittens");
                }
                hasSceneErrorScreen = true;

                // set up button functionality
                btnSubmit.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        TransitionManager.beginDelayedTransition(sceneRoot);
                        transitionTo(ActivityState.STATE_LOADING);

                        // hide keyboard
                        View view = getCurrentFocus();
                        if (view != null) {
                            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                        }
                    }
                });

                // set up edittext functionality
                // since i've had a lot of trouble with managing EditText focus in the past
                // I usually just use a TextWatcher to watch for "focus"
                etGibberish.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {}

                    @Override
                    public void afterTextChanged(Editable s) {
                        // transition back to initial if the text has changed
                        TransitionManager.beginDelayedTransition(sceneRoot);
                        transitionTo(ActivityState.STATE_INITIAL);
                    }
                });

                break;
            case STATE_LOADING:
                // FROM: initial, error
                // TO: error, final
                setViewBelow(rlLoading, R.id.rl_gibberish_content); // show loading layout
                alignViewBottom(tvError, R.id.tv_desc); // hide error text
                alignViewBottom(tvErrorDesc, R.id.rl_gibberish_content); // hide error desc text

                ivLoading.startAnimation(animLoading);

                etGibberish.setEnabled(false);

                btnSubmit.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // clear the previous click listener
                    }
                });

                Runnable runnableServerLoaded = new Runnable() {
                    @Override
                    public void run() {
                        TransitionManager.beginDelayedTransition(sceneRoot);

                        // check validity of edittext
                        if (etGibberish.getText().toString().contentEquals("kittens"))
                            transitionTo(ActivityState.STATE_FINAL);
                        else
                            transitionTo(ActivityState.STATE_ERROR);

                    }
                };

                Handler h = new Handler();
                h.postDelayed(runnableServerLoaded, MAX_LOADING_TIME);

                break;
            case STATE_FINAL:
                // FROM: loading
                // TO: initial
                alignViewBottom(rlLoading, R.id.rl_gibberish_content); // hide loading

                tvDesc.setText("Congratulations, you managed to enter kittens into a text box. This app sure was silly. Press submit again to start over");
                tvDesc.setTextColor(getResources().getColor(R.color.material_blue_grey_800));

                etGibberish.setEnabled(true);

                btnSubmit.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        hasSceneErrorScreen = false;
                        TransitionManager.beginDelayedTransition(sceneRoot);
                        transitionTo(ActivityState.STATE_INITIAL);

                        tvDesc.setText(R.string.layout_header);

                        // hide keyboard
                        View view = getCurrentFocus();
                        if (view != null) {
                            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                        }
                    }
                });

                break;
        }
    }

    // for showing error / error description / loading layout
    // if you copy and paste this code, make sure you are using RelativeLayout (or change these functions)
    private void setViewBelow(View v, int belowId) {
        RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) v.getLayoutParams();
        lp.addRule(RelativeLayout.ALIGN_BOTTOM, 0); // removeRule for pre API 17
        lp.addRule(RelativeLayout.BELOW, belowId);
        v.setLayoutParams(lp);
        v.setVisibility(View.VISIBLE);
    }
    // for hiding error / error description / loading layout
    private void alignViewBottom(View v, int belowId) {
        RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) v.getLayoutParams();
        lp.addRule(RelativeLayout.BELOW, 0); // removeRule for pre API 17
        lp.addRule(RelativeLayout.ALIGN_BOTTOM, belowId);
        v.setLayoutParams(lp);
        v.setVisibility(View.INVISIBLE);
    }
}
