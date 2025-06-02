package com.example.sssshhift.activities;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.PathShape;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AnticipateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import com.example.sssshhift.MainActivity;
import com.example.sssshhift.R;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import android.view.Choreographer;
import java.util.concurrent.TimeUnit;

public class SplashActivity extends AppCompatActivity {

    private static final String TAG = "SplashActivity";
    private static final int SPLASH_DELAY = 3500; // 3.5 seconds
    private static final String PREFS_NAME = "app_prefs";
    private static final String KEY_ONBOARDING_COMPLETED = "onboarding_completed";

    private SharedPreferences prefs;
    private Handler animationHandler;

    // Views
    private ImageView logo, waveBg1, waveBg2;
    private TextView title, tagline, loadingText;
    private ProgressBar progressBar;
    private View[] soundWaves = new View[10];
    private String[] loadingPhrases = {
        "Initializing...",
        "Loading profiles...",
        "Setting up silence...",
        "Almost ready..."
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        animationHandler = new Handler(Looper.getMainLooper());

        // Debug logging
        boolean onboardingCompleted = prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false);
        Log.d(TAG, "=== SPLASH ACTIVITY DEBUG ===");
        Log.d(TAG, "Onboarding completed: " + onboardingCompleted);
        Log.d(TAG, "All SharedPreferences:");
        for (String key : prefs.getAll().keySet()) {
            Log.d(TAG, "  " + key + " = " + prefs.getAll().get(key));
        }
        Log.d(TAG, "=============================");

        initializeViews();
        startAnimationSequence();

        // Navigate after animations
        animationHandler.postDelayed(this::navigateToNextScreen, SPLASH_DELAY);
    }

    private void initializeViews() {
        // Main elements
        logo = findViewById(R.id.splash_logo);
        title = findViewById(R.id.splash_title);
        tagline = findViewById(R.id.splash_tagline);
        loadingText = findViewById(R.id.loading_text);
        progressBar = findViewById(R.id.splash_progress);
        waveBg1 = findViewById(R.id.wave_bg_1);
        waveBg2 = findViewById(R.id.wave_bg_2);

        // Initialize sound wave bars
        for (int i = 0; i < 10; i++) {
            int resId = getResources().getIdentifier("wave_" + (i + 1), "id", getPackageName());
            soundWaves[i] = findViewById(resId);
        }
    }

    private void startAnimationSequence() {
        // 1. Start background wave animations
        animateBackgroundWaves();

        // 2. Animate logo entrance (0-500ms)
        animateLogo();

        // 3. Start sound wave animation (500ms)
        animationHandler.postDelayed(this::startSoundWaveAnimation, 500);

        // 4. Animate title and tagline (800ms)
        animationHandler.postDelayed(this::animateTitleAndTagline, 800);

        // 5. Start loading animation (1200ms)
        animationHandler.postDelayed(this::startLoadingAnimation, 1200);
    }

    private void animateBackgroundWaves() {
        // Animate top wave
        ObjectAnimator waveAnim1 = ObjectAnimator.ofFloat(waveBg1, "translationX", 0f, -200f);
        waveAnim1.setDuration(3000);
        waveAnim1.setRepeatCount(ValueAnimator.INFINITE);
        waveAnim1.setRepeatMode(ValueAnimator.REVERSE);
        waveAnim1.setInterpolator(new LinearInterpolator());
        waveAnim1.start();

        // Animate bottom wave (opposite direction)
        ObjectAnimator waveAnim2 = ObjectAnimator.ofFloat(waveBg2, "translationX", -200f, 0f);
        waveAnim2.setDuration(3000);
        waveAnim2.setRepeatCount(ValueAnimator.INFINITE);
        waveAnim2.setRepeatMode(ValueAnimator.REVERSE);
        waveAnim2.setInterpolator(new LinearInterpolator());
        waveAnim2.start();
    }

    private void animateLogo() {
        logo.setScaleX(0f);
        logo.setScaleY(0f);
        logo.setRotation(-30f);
        logo.setAlpha(0f);

        AnimatorSet logoAnim = new AnimatorSet();
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(logo, "scaleX", 0f, 1.2f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(logo, "scaleY", 0f, 1.2f, 1f);
        ObjectAnimator rotation = ObjectAnimator.ofFloat(logo, "rotation", -30f, 0f);
        ObjectAnimator alpha = ObjectAnimator.ofFloat(logo, "alpha", 0f, 1f);

        logoAnim.playTogether(scaleX, scaleY, rotation, alpha);
        logoAnim.setDuration(1000);
        logoAnim.setInterpolator(new AccelerateDecelerateInterpolator());
        logoAnim.start();

        // Add subtle floating animation after entrance
        animationHandler.postDelayed(() -> {
            ObjectAnimator floatAnim = ObjectAnimator.ofFloat(logo, "translationY", 0f, -8f, 0f);
            floatAnim.setDuration(2000);
            floatAnim.setRepeatCount(ValueAnimator.INFINITE);
            floatAnim.setRepeatMode(ValueAnimator.REVERSE);
            floatAnim.setInterpolator(new AccelerateDecelerateInterpolator());
            floatAnim.start();
        }, 1000);
    }

    private void startSoundWaveAnimation() {
        for (int i = 0; i < soundWaves.length; i++) {
            final View wave = soundWaves[i];
            wave.setAlpha(0f);
            wave.setScaleY(0.3f);

            // Create wave animation
            ObjectAnimator alpha = ObjectAnimator.ofFloat(wave, "alpha", 0f, 1f);
            alpha.setRepeatCount(ValueAnimator.INFINITE);
            alpha.setRepeatMode(ValueAnimator.REVERSE);

            ObjectAnimator scaleY = ObjectAnimator.ofFloat(wave, "scaleY", 0.3f, 1f, 0.3f);
            scaleY.setRepeatCount(ValueAnimator.INFINITE);
            scaleY.setRepeatMode(ValueAnimator.REVERSE);

            AnimatorSet waveAnim = new AnimatorSet();
            waveAnim.playTogether(alpha, scaleY);
            waveAnim.setDuration(1000);
            waveAnim.setStartDelay(i * 100);
            waveAnim.setInterpolator(new AccelerateDecelerateInterpolator());
            waveAnim.start();
        }
    }

    private void animateTitleAndTagline() {
        // Animate title with bounce effect
        title.setAlpha(0f);
        title.setTranslationY(40f);
        AnimatorSet titleAnim = new AnimatorSet();
        
        ObjectAnimator titleAlpha = ObjectAnimator.ofFloat(title, "alpha", 0f, 1f);
        titleAlpha.setDuration(400);
        
        ObjectAnimator titleTranslate = ObjectAnimator.ofFloat(title, "translationY", 40f, -5f, 0f);
        titleTranslate.setDuration(700);
        
        ObjectAnimator titleScale = ObjectAnimator.ofFloat(title, "scaleX", 0.8f, 1.1f, 1f);
        ObjectAnimator titleScaleY = ObjectAnimator.ofFloat(title, "scaleY", 0.8f, 1.1f, 1f);
        titleScale.setDuration(700);
        titleScaleY.setDuration(700);
        
        titleAnim.playTogether(titleAlpha, titleTranslate, titleScale, titleScaleY);
        titleAnim.start();

        // Animate tagline with fade and slide
        tagline.setAlpha(0f);
        tagline.setTranslationY(20f);
        AnimatorSet taglineAnim = new AnimatorSet();
        
        ObjectAnimator taglineAlpha = ObjectAnimator.ofFloat(tagline, "alpha", 0f, 1f);
        taglineAlpha.setDuration(400);
        
        ObjectAnimator taglineTranslate = ObjectAnimator.ofFloat(tagline, "translationY", 20f, 0f);
        taglineTranslate.setDuration(500);
        
        taglineAnim.playTogether(taglineAlpha, taglineTranslate);
        taglineAnim.setStartDelay(300);
        taglineAnim.start();
    }

    private void startLoadingAnimation() {
        // Show and animate progress bar with fade and expand
        progressBar.setAlpha(0f);
        progressBar.setScaleX(0.3f);
        progressBar.setProgress(0);

        AnimatorSet progressAnim = new AnimatorSet();
        ObjectAnimator fadeIn = ObjectAnimator.ofFloat(progressBar, "alpha", 0f, 1f);
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(progressBar, "scaleX", 0.3f, 1f);
        progressAnim.playTogether(fadeIn, scaleX);
        progressAnim.setDuration(500);
        progressAnim.start();

        ObjectAnimator progress = ObjectAnimator.ofInt(progressBar, "progress", 0, 100);
        progress.setDuration(2000);
        progress.setInterpolator(new AccelerateDecelerateInterpolator());
        progress.setStartDelay(300);
        progress.start();

        // Start typing animation for loading text with fade
        loadingText.setAlpha(0f);
        ObjectAnimator textFade = ObjectAnimator.ofFloat(loadingText, "alpha", 0f, 1f);
        textFade.setDuration(300);
        textFade.start();

        // Start typing animation
        animateLoadingText(0);
    }

    private void animateLoadingText(final int index) {
        if (index >= loadingPhrases.length) return;
        
        String text = loadingPhrases[index];
        loadingText.setText("");
        
        final int[] charIndex = {0};
        final Handler handler = new Handler();
        final Runnable typingRunnable = new Runnable() {
            @Override
            public void run() {
                if (charIndex[0] < text.length()) {
                    loadingText.setText(text.substring(0, ++charIndex[0]));
                    handler.postDelayed(this, 50);
                } else if (index < loadingPhrases.length - 1) {
                    handler.postDelayed(() -> animateLoadingText(index + 1), 500);
                }
            }
        };
        handler.postDelayed(typingRunnable, 100);
    }

    private void navigateToNextScreen() {
        boolean onboardingCompleted = prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false);
        Log.d(TAG, "=== NAVIGATION DEBUG ===");
        Log.d(TAG, "Navigating... Onboarding completed: " + onboardingCompleted);
        
        // Fresh check of onboarding status
        onboardingCompleted = prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false);
        Log.d(TAG, "Fresh check - Onboarding completed: " + onboardingCompleted);

        // Create ripple transition effect
        createRippleTransition(() -> {
            // Start appropriate activity
            Log.d(TAG, "Starting MainActivity");
            Intent intent = new Intent(this, MainActivity.class);
            Log.d(TAG, "Starting activity: " + intent.getComponent().getClassName());
            startActivity(intent);
            overridePendingTransition(R.anim.main_enter, R.anim.splash_exit);
            finish();
            Log.d(TAG, "========================");
        });
    }

    private void createRippleTransition(Runnable onComplete) {
        final View rootView = getWindow().getDecorView().findViewById(android.R.id.content);
        final Choreographer choreographer = Choreographer.getInstance();
        
        // Create animated elements
        TextView appNameText = new TextView(this);
        appNameText.setText("SSSSHHIFT");
        appNameText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 40);
        appNameText.setTypeface(Typeface.DEFAULT_BOLD);
        appNameText.setTextColor(Color.WHITE);
        appNameText.setAlpha(0f);
        
        FrameLayout.LayoutParams textParams = new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        );
        textParams.gravity = Gravity.CENTER;
        ((ViewGroup) rootView).addView(appNameText, textParams);

        // Create background panel
        View panel = new View(this);
        panel.setBackgroundColor(Color.WHITE);
        FrameLayout.LayoutParams panelParams = new FrameLayout.LayoutParams(
            0,
            ViewGroup.LayoutParams.MATCH_PARENT
        );
        ((ViewGroup) rootView).addView(panel, panelParams);

        // Animation state
        final long startTime = System.nanoTime();
        final long animDuration = TimeUnit.MILLISECONDS.toNanos(1500); // 1.5 seconds total
        final AtomicBoolean isAnimating = new AtomicBoolean(true);
        
        // Fade out current elements first
        AnimatorSet fadeOut = new AnimatorSet();
        List<Animator> fadeAnims = new ArrayList<>();
        fadeAnims.add(ObjectAnimator.ofFloat(progressBar, "alpha", 1f, 0f));
        fadeAnims.add(ObjectAnimator.ofFloat(loadingText, "alpha", 1f, 0f));
        fadeAnims.add(ObjectAnimator.ofFloat(title, "alpha", 1f, 0f));
        fadeAnims.add(ObjectAnimator.ofFloat(tagline, "alpha", 1f, 0f));
        fadeAnims.add(ObjectAnimator.ofFloat(logo, "alpha", 1f, 0f));
        for (View wave : soundWaves) {
            fadeAnims.add(ObjectAnimator.ofFloat(wave, "alpha", 1f, 0f));
        }
        fadeOut.playTogether(fadeAnims);
        fadeOut.setDuration(300);
        fadeOut.start();

        // Choreograph main animation
        new Handler().postDelayed(() -> {
            final Choreographer.FrameCallback frameCallback = new Choreographer.FrameCallback() {
                @Override
                public void doFrame(long frameTimeNanos) {
                    if (!isAnimating.get()) {
                        return;
                    }

                    float progress = Math.min(1f, (frameTimeNanos - startTime) / (float) animDuration);
                    
                    // Custom easing functions
                    float easeInOut = interpolateEaseInOut(progress);
                    float easeOut = interpolateEaseOut(progress);
                    float bounce = interpolateBounce(progress);
                    
                    // Panel width animation (slides in from left)
                    float panelWidth = rootView.getWidth() * easeInOut;
                    panelParams.width = (int) panelWidth;
                    panel.setLayoutParams(panelParams);
                    
                    // Text animations
                    if (progress > 0.3f) {
                        float textProgress = Math.min(1f, (progress - 0.3f) / 0.4f);
                        appNameText.setAlpha(textProgress);
                        
                        // Text scale with bounce
                        float scale = textProgress < 1f ? 0.7f + (0.3f * bounce) : 1f;
                        appNameText.setScaleX(scale);
                        appNameText.setScaleY(scale);
                        
                        // Text rotation
                        float rotation = (1f - textProgress) * 10f;
                        appNameText.setRotation(rotation);
                    }

                    // Final fade to white
                    if (progress > 0.8f) {
                        float fadeProgress = (progress - 0.8f) / 0.2f;
                        panel.setAlpha(1f - fadeProgress);
                        appNameText.setAlpha(1f - fadeProgress);
                    }

                    // Continue animation or finish
                    if (progress < 1f) {
                        choreographer.postFrameCallback(this);
                    } else {
                        isAnimating.set(false);
                        ((ViewGroup) rootView).removeView(panel);
                        ((ViewGroup) rootView).removeView(appNameText);
                        onComplete.run();
                    }
                }
            };

            // Start the choreographed animation
            choreographer.postFrameCallback(frameCallback);
        }, 300);
    }

    private float interpolateEaseInOut(float t) {
        return t < 0.5f ? 
            4f * t * t * t : 
            1f - (float)Math.pow(-2f * t + 2f, 3f) / 2f;
    }

    private float interpolateEaseOut(float t) {
        return 1f - (float)Math.pow(1f - t, 3f);
    }

    private float interpolateBounce(float t) {
        float n1 = 7.5625f;
        float d1 = 2.75f;

        if (t < 1f / d1) {
            return n1 * t * t;
        } else if (t < 2f / d1) {
            t -= 1.5f / d1;
            return n1 * t * t + 0.75f;
        } else if (t < 2.5f / d1) {
            t -= 2.25f / d1;
            return n1 * t * t + 0.9375f;
        } else {
            t -= 2.625f / d1;
            return n1 * t * t + 0.984375f;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (animationHandler != null) {
            animationHandler.removeCallbacksAndMessages(null);
        }
    }

    @Override
    public void finish() {
        super.finish();
        // Remove default transition animation
        overridePendingTransition(0, 0);
    }
}