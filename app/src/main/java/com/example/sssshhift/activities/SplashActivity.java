package com.example.sssshhift.activities;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import com.example.sssshhift.MainActivity;
import com.example.sssshhift.R;

public class SplashActivity extends AppCompatActivity {

    private static final String TAG = "SplashActivity";
    private static final int SPLASH_DELAY = 3000; // 3 seconds for enhanced animations
    private static final String PREFS_NAME = "app_prefs";
    private static final String KEY_ONBOARDING_COMPLETED = "onboarding_completed";

    private SharedPreferences prefs;

    // Views
    private ImageView logo, logoRing;
    private TextView title, tagline, loadingText, versionText;
    private LinearLayout featureHighlight, loadingDots;
    private ProgressBar splashProgress;
    private ImageView backgroundCircle1, backgroundCircle2;
    private View[] dots = new View[3];

    // Animation handler
    private Handler animationHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Initialize SharedPreferences
        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        animationHandler = new Handler(Looper.getMainLooper());

        // Debug: Check current onboarding status
        boolean onboardingCompleted = prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false);
        Log.d(TAG, "=== SPLASH ACTIVITY DEBUG ===");
        Log.d(TAG, "Onboarding completed: " + onboardingCompleted);

        // Additional debug: Check all preferences
        Log.d(TAG, "All SharedPreferences:");
        for (String key : prefs.getAll().keySet()) {
            Log.d(TAG, "  " + key + " = " + prefs.getAll().get(key));
        }
        Log.d(TAG, "=============================");

        initializeViews();
        startEnhancedAnimationSequence();

        // Navigate after animations complete
        animationHandler.postDelayed(this::navigateToNextScreen, SPLASH_DELAY);
    }

    private void initializeViews() {
        // Main elements
        logo = findViewById(R.id.splash_logo);
        logoRing = findViewById(R.id.logo_ring);
        title = findViewById(R.id.splash_title);
        tagline = findViewById(R.id.splash_tagline);
        loadingText = findViewById(R.id.loading_text);
        versionText = findViewById(R.id.version_text);
        featureHighlight = findViewById(R.id.feature_highlight);
        loadingDots = findViewById(R.id.loading_dots);
        splashProgress = findViewById(R.id.splash_progress);

        // Background elements
        backgroundCircle1 = findViewById(R.id.background_circle1);
        backgroundCircle2 = findViewById(R.id.background_circle2);

        // Loading dots - Make sure these exist in your layout
        if (findViewById(R.id.dot1) != null && findViewById(R.id.dot2) != null && findViewById(R.id.dot3) != null) {
            dots[0] = findViewById(R.id.dot1);
            dots[1] = findViewById(R.id.dot2);
            dots[2] = findViewById(R.id.dot3);
        }
    }

    private void startEnhancedAnimationSequence() {
        // Phase 1: Background elements (0-300ms)
        animateBackgroundElements();

        // Phase 2: Logo entrance with ring (500ms)
        animationHandler.postDelayed(this::animateLogo, 500);

        // Phase 3: Title animation (1000ms)
        animationHandler.postDelayed(this::animateTitle, 1000);

        // Phase 4: Tagline fade in (1400ms)
        animationHandler.postDelayed(this::animateTagline, 1400);

        // Phase 5: Feature highlights (1800ms)
        animationHandler.postDelayed(this::animateFeatureHighlight, 1800);

        // Phase 6: Loading elements (2200ms)
        animationHandler.postDelayed(this::animateLoadingElements, 2200);

        // Phase 7: Version text (2600ms)
        animationHandler.postDelayed(this::animateVersionText, 2600);
    }

    private void animateBackgroundElements() {
        if (backgroundCircle1 != null && backgroundCircle2 != null) {
            // Subtle rotation for background circles
            ObjectAnimator rotation1 = ObjectAnimator.ofFloat(backgroundCircle1, "rotation", 0f, 360f);
            rotation1.setDuration(20000);
            rotation1.setRepeatCount(ObjectAnimator.INFINITE);
            rotation1.setInterpolator(new AccelerateDecelerateInterpolator());

            ObjectAnimator rotation2 = ObjectAnimator.ofFloat(backgroundCircle2, "rotation", 360f, 0f);
            rotation2.setDuration(25000);
            rotation2.setRepeatCount(ObjectAnimator.INFINITE);
            rotation2.setInterpolator(new AccelerateDecelerateInterpolator());

            rotation1.start();
            rotation2.start();
        }
    }

    private void animateLogo() {
        if (logo != null) {
            // Initial phone entrance animation
            AnimatorSet logoAnimSet = new AnimatorSet();

            ObjectAnimator scaleX = ObjectAnimator.ofFloat(logo, "scaleX", 0f, 1.1f, 1f);
            ObjectAnimator scaleY = ObjectAnimator.ofFloat(logo, "scaleY", 0f, 1.1f, 1f);
            ObjectAnimator alpha = ObjectAnimator.ofFloat(logo, "alpha", 0f, 1f);
            ObjectAnimator rotation = ObjectAnimator.ofFloat(logo, "rotation", -30f, 0f);

            logoAnimSet.playTogether(scaleX, scaleY, alpha, rotation);
            logoAnimSet.setDuration(700);
            logoAnimSet.setInterpolator(new OvershootInterpolator(1.1f));

            // Start vibration animation after entrance
            logoAnimSet.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    startVibratingAnimation();
                }
            });

            logoAnimSet.start();
        }
    }

    private void startVibratingAnimation() {
        if (logo != null) {
            // Create vibration animation
            AnimatorSet vibrateSet = new AnimatorSet();

            // Horizontal vibration
            ObjectAnimator translateX = ObjectAnimator.ofFloat(logo, "translationX",
                    0f, -3f, 3f, -3f, 3f, -2f, 2f, -1f, 1f, 0f);
            translateX.setDuration(500);
            translateX.setRepeatCount(ObjectAnimator.INFINITE);
            translateX.setRepeatMode(ObjectAnimator.RESTART);

            // Subtle rotation
            ObjectAnimator rotate = ObjectAnimator.ofFloat(logo, "rotation",
                    0f, -2f, 2f, -2f, 2f, -1f, 1f, 0f);
            rotate.setDuration(500);
            rotate.setRepeatCount(ObjectAnimator.INFINITE);
            rotate.setRepeatMode(ObjectAnimator.RESTART);

            vibrateSet.playTogether(translateX, rotate);
            vibrateSet.start();

            // Animate the ring effect
            animateVibratingRing();
        }
    }

    private void animateVibratingRing() {
        if (logoRing != null) {
            // Create expanding ring animation
            AnimatorSet ringAnimSet = new AnimatorSet();

            ObjectAnimator alpha = ObjectAnimator.ofFloat(logoRing, "alpha", 0f, 0.3f, 0f);
            alpha.setRepeatCount(ObjectAnimator.INFINITE);
            alpha.setDuration(1000);

            ObjectAnimator scaleX = ObjectAnimator.ofFloat(logoRing, "scaleX", 0.8f, 1.2f);
            scaleX.setRepeatCount(ObjectAnimator.INFINITE);
            scaleX.setDuration(1000);

            ObjectAnimator scaleY = ObjectAnimator.ofFloat(logoRing, "scaleY", 0.8f, 1.2f);
            scaleY.setRepeatCount(ObjectAnimator.INFINITE);
            scaleY.setDuration(1000);

            ringAnimSet.playTogether(alpha, scaleX, scaleY);
            ringAnimSet.setInterpolator(new DecelerateInterpolator());
            
            ringAnimSet.start();
        }
    }

    private void animateTitle() {
        if (title != null) {
            // Enhanced title animation with scale and fade
            AnimatorSet titleAnimSet = new AnimatorSet();

            ObjectAnimator alpha = ObjectAnimator.ofFloat(title, "alpha", 0f, 1f);
            ObjectAnimator scaleX = ObjectAnimator.ofFloat(title, "scaleX", 0.8f, 1.05f, 1f);
            ObjectAnimator scaleY = ObjectAnimator.ofFloat(title, "scaleY", 0.8f, 1.05f, 1f);
            ObjectAnimator translationY = ObjectAnimator.ofFloat(title, "translationY", 30f, 0f);

            titleAnimSet.playTogether(alpha, scaleX, scaleY, translationY);
            titleAnimSet.setDuration(600);
            titleAnimSet.setInterpolator(new OvershootInterpolator(0.8f));
            titleAnimSet.start();
        }
    }

    private void animateTagline() {
        if (tagline != null) {
            // Elegant tagline animation
            AnimatorSet taglineAnimSet = new AnimatorSet();

            ObjectAnimator alpha = ObjectAnimator.ofFloat(tagline, "alpha", 0f, 1f);
            ObjectAnimator translationY = ObjectAnimator.ofFloat(tagline, "translationY", 20f, 0f);
            ObjectAnimator scaleX = ObjectAnimator.ofFloat(tagline, "scaleX", 0.9f, 1f);

            taglineAnimSet.playTogether(alpha, translationY, scaleX);
            taglineAnimSet.setDuration(500);
            taglineAnimSet.setInterpolator(new DecelerateInterpolator());
            taglineAnimSet.start();
        }
    }

    private void animateFeatureHighlight() {
        if (featureHighlight != null) {
            // Feature highlight animation
            AnimatorSet featureAnimSet = new AnimatorSet();

            ObjectAnimator alpha = ObjectAnimator.ofFloat(featureHighlight, "alpha", 0f, 1f);
            ObjectAnimator translationY = ObjectAnimator.ofFloat(featureHighlight, "translationY", 15f, 0f);

            featureAnimSet.playTogether(alpha, translationY);
            featureAnimSet.setDuration(400);
            featureAnimSet.setInterpolator(new DecelerateInterpolator());
            featureAnimSet.start();
        }
    }

    private void animateLoadingElements() {
        // Loading text animation
        if (loadingText != null) {
            ObjectAnimator loadingAlpha = ObjectAnimator.ofFloat(loadingText, "alpha", 0f, 1f);
            loadingAlpha.setDuration(300);
            loadingAlpha.start();
        }

        // Loading dots animation
        if (loadingDots != null) {
            ObjectAnimator dotsAlpha = ObjectAnimator.ofFloat(loadingDots, "alpha", 0f, 1f);
            dotsAlpha.setDuration(300);
            dotsAlpha.start();

            // Animated dots pulsing effect
            animationHandler.postDelayed(this::startDotsPulsing, 400);
        }
    }

    private void startDotsPulsing() {
        for (int i = 0; i < dots.length; i++) {
            final View dot = dots[i];
            if (dot != null) {
                final int delay = i * 200;

                animationHandler.postDelayed(() -> {
                    ObjectAnimator pulse = ObjectAnimator.ofFloat(dot, "alpha", 1f, 0.3f);
                    pulse.setDuration(600);
                    pulse.setRepeatCount(ObjectAnimator.INFINITE);
                    pulse.setRepeatMode(ObjectAnimator.REVERSE);
                    pulse.setInterpolator(new AccelerateDecelerateInterpolator());
                    pulse.start();
                }, delay);
            }
        }
    }

    private void animateVersionText() {
        if (versionText != null) {
            ObjectAnimator alpha = ObjectAnimator.ofFloat(versionText, "alpha", 0f, 1f);
            alpha.setDuration(300);
            alpha.start();
        }
    }

    private void navigateToNextScreen() {
        // Get fresh SharedPreferences instance to avoid caching issues
        SharedPreferences freshPrefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean onboardingCompleted = freshPrefs.getBoolean(KEY_ONBOARDING_COMPLETED, false);

        Log.d(TAG, "=== NAVIGATION DEBUG ===");
        Log.d(TAG, "Navigating... Onboarding completed: " + onboardingCompleted);
        Log.d(TAG, "Fresh check - Onboarding completed: " + onboardingCompleted);

        Intent intent;

        if (!onboardingCompleted) {
            // First time user - show onboarding
            Log.d(TAG, "Starting OnboardingActivity");
            intent = new Intent(SplashActivity.this, OnboardingActivity.class);
        } else {
            // Returning user - go directly to main app
            Log.d(TAG, "Starting MainActivity");
            intent = new Intent(SplashActivity.this, MainActivity.class);
        }

        Log.d(TAG, "Starting activity: " + intent.getComponent().getClassName());
        Log.d(TAG, "========================");

        startActivity(intent);
        finish();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clean up animation handlers
        if (animationHandler != null) {
            animationHandler.removeCallbacksAndMessages(null);
        }
    }

    // Debug method - you can call this to reset onboarding for testing
    public void resetOnboarding() {
        SharedPreferences.Editor editor = prefs.edit();
        editor.remove(KEY_ONBOARDING_COMPLETED);
        editor.apply();
        Log.d(TAG, "Onboarding reset for testing");
    }

    // Method to manually test the flag
    public void testOnboardingFlag() {
        boolean flag = prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false);
        Log.d(TAG, "TEST: Onboarding completed flag = " + flag);
    }
}