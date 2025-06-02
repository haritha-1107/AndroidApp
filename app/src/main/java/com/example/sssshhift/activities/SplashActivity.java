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
import android.graphics.PathMeasure;
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
    private static final String PREFS_NAME = "app_prefs";
    private static final String KEY_ONBOARDING_COMPLETED = "onboarding_completed";
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        
        // Start animation sequence immediately
        createRippleTransition(() -> {
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
            finish();
        });
    }

    private void createRippleTransition(Runnable onComplete) {
        final View rootView = getWindow().getDecorView().findViewById(android.R.id.content);
        final Choreographer choreographer = Choreographer.getInstance();

        // Create background
        rootView.setBackgroundColor(Color.WHITE);

        // Create outline view for drawing the square outline
        View outlineView = new View(this);
        outlineView.setAlpha(0f);
        
        // Create the outline path drawable
        ShapeDrawable outlineShape = new ShapeDrawable();
        outlineShape.getPaint().setColor(Color.parseColor("#6B4DE6"));
        outlineShape.getPaint().setStyle(Paint.Style.STROKE);
        outlineShape.getPaint().setStrokeWidth(dpToPx(4));
        outlineShape.getPaint().setAntiAlias(true);
        
        // Create outline path
        Path outlinePath = new Path();
        int finalSquareSize = dpToPx(120);
        int initialSquareSize = dpToPx(200);  // Bigger initial size
        float cornerRadius = dpToPx(30);
        
        outlinePath.moveTo(initialSquareSize - cornerRadius, 0);
        outlineShape.setShape(new PathShape(outlinePath, initialSquareSize, initialSquareSize));
        outlineView.setBackground(outlineShape);
        
        FrameLayout.LayoutParams outlineParams = new FrameLayout.LayoutParams(initialSquareSize, initialSquareSize);
        outlineParams.gravity = Gravity.CENTER;
        ((ViewGroup) rootView).addView(outlineView, outlineParams);

        // Create filled square view
        View filledSquare = new View(this);
        GradientDrawable squareBg = new GradientDrawable();
        squareBg.setShape(GradientDrawable.RECTANGLE);
        squareBg.setColor(Color.parseColor("#6B4DE6"));
        squareBg.setCornerRadius(cornerRadius);
        filledSquare.setBackground(squareBg);
        filledSquare.setAlpha(0f);
        
        FrameLayout.LayoutParams squareParams = new FrameLayout.LayoutParams(initialSquareSize, initialSquareSize);
        squareParams.gravity = Gravity.CENTER;
        ((ViewGroup) rootView).addView(filledSquare, squareParams);

        // Create S path view
        View sPathView = new View(this);
        sPathView.setAlpha(0f);
        
        ShapeDrawable sShape = new ShapeDrawable();
        sShape.getPaint().setColor(Color.WHITE);
        sShape.getPaint().setStyle(Paint.Style.FILL);
        sShape.getPaint().setAntiAlias(true);
        
        Path sPath = new Path();
        float padding = dpToPx(25);
        float strokeWidth = dpToPx(16);
        
        sPath.moveTo(initialSquareSize - padding - strokeWidth, padding);
        sPath.cubicTo(
            initialSquareSize - padding - strokeWidth, padding,
            padding + strokeWidth, padding + ((initialSquareSize - 2 * padding) * 0.2f),
            padding + strokeWidth, padding + ((initialSquareSize - 2 * padding) * 0.35f)
        );
        sPath.cubicTo(
            padding + strokeWidth, padding + ((initialSquareSize - 2 * padding) * 0.5f),
            initialSquareSize - padding - strokeWidth, padding + ((initialSquareSize - 2 * padding) * 0.5f),
            initialSquareSize - padding - strokeWidth, padding + ((initialSquareSize - 2 * padding) * 0.65f)
        );
        sPath.cubicTo(
            initialSquareSize - padding - strokeWidth, padding + ((initialSquareSize - 2 * padding) * 0.8f),
            padding + strokeWidth, initialSquareSize - padding,
            padding + strokeWidth, initialSquareSize - padding
        );
        
        sShape.setShape(new PathShape(sPath, initialSquareSize, initialSquareSize));
        sPathView.setBackground(sShape);
        
        FrameLayout.LayoutParams sPathParams = new FrameLayout.LayoutParams(initialSquareSize, initialSquareSize);
        sPathParams.gravity = Gravity.CENTER;
        ((ViewGroup) rootView).addView(sPathView, sPathParams);

        // Create text views
        TextView appNameText = new TextView(this);
        appNameText.setText("Ssshhift");
        appNameText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 40);
        appNameText.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        appNameText.setTextColor(Color.parseColor("#6B4DE6"));
        appNameText.setLetterSpacing(0.05f);
        appNameText.setAlpha(0f);

        TextView subtitleText = new TextView(this);
        subtitleText.setText("Smart Profile Manager");
        subtitleText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        subtitleText.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        subtitleText.setTextColor(Color.parseColor("#9B8AE6"));
        subtitleText.setLetterSpacing(0.02f);
        subtitleText.setAlpha(0f);

        FrameLayout.LayoutParams textParams = new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        );
        textParams.gravity = Gravity.CENTER_VERTICAL;
        textParams.leftMargin = dpToPx(180);
        
        FrameLayout.LayoutParams subtitleParams = new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        );
        subtitleParams.gravity = Gravity.CENTER_VERTICAL;
        subtitleParams.leftMargin = dpToPx(180);
        subtitleParams.topMargin = dpToPx(40);

        ((ViewGroup) rootView).addView(appNameText, textParams);
        ((ViewGroup) rootView).addView(subtitleText, subtitleParams);

        // Animation state
        final long startTime = System.nanoTime();
        final long animDuration = TimeUnit.MILLISECONDS.toNanos(4000);
        final AtomicBoolean isAnimating = new AtomicBoolean(true);
        final Path fullOutlinePath = new Path();
        createSquareOutlinePath(fullOutlinePath, initialSquareSize, cornerRadius);

        // Start animation immediately
        final Choreographer.FrameCallback frameCallback = new Choreographer.FrameCallback() {
            @Override
            public void doFrame(long frameTimeNanos) {
                if (!isAnimating.get()) return;

                float progress = Math.min(1f, (frameTimeNanos - startTime) / (float) animDuration);

                // Draw L shapes (0-20%)
                if (progress <= 0.2f) {
                    float outlineProgress = progress / 0.2f;
                    Path currentPath = new Path();
                    
                    // Top-right L
                    currentPath.moveTo(initialSquareSize - cornerRadius, 0);
                    currentPath.lineTo(initialSquareSize, 0);
                    currentPath.lineTo(initialSquareSize, initialSquareSize / 2 * outlineProgress);
                    
                    // Bottom-left L
                    currentPath.moveTo(0, initialSquareSize);
                    currentPath.lineTo(0, initialSquareSize - (initialSquareSize / 2 * outlineProgress));
                    
                    ShapeDrawable currentShape = new ShapeDrawable();
                    currentShape.getPaint().setColor(Color.parseColor("#6B4DE6"));
                    currentShape.getPaint().setStyle(Paint.Style.STROKE);
                    currentShape.getPaint().setStrokeWidth(dpToPx(4));
                    currentShape.getPaint().setAntiAlias(true);
                    currentShape.setShape(new PathShape(currentPath, initialSquareSize, initialSquareSize));
                    
                    outlineView.setBackground(currentShape);
                    outlineView.setAlpha(1f);
                }

                // Complete square outline (20-40%)
                if (progress > 0.2f && progress <= 0.4f) {
                    float outlineProgress = (progress - 0.2f) / 0.2f;
                    Path currentPath = new Path();
                    
                    // Continue from top-right L
                    currentPath.moveTo(initialSquareSize - cornerRadius, 0);
                    currentPath.lineTo(initialSquareSize, 0);
                    currentPath.lineTo(initialSquareSize, initialSquareSize / 2);
                    // Complete top-right to bottom-right
                    currentPath.lineTo(initialSquareSize, initialSquareSize * (0.5f + (0.5f * outlineProgress)));
                    
                    // Continue from bottom-left L
                    currentPath.moveTo(0, initialSquareSize);
                    currentPath.lineTo(0, initialSquareSize / 2);
                    // Complete bottom-left to top-left
                    currentPath.lineTo(0, initialSquareSize * (0.5f - (0.5f * outlineProgress)));
                    
                    // Draw horizontal lines based on progress
                    if (outlineProgress > 0) {
                        float horizontalProgress = Math.min(1f, outlineProgress * 2);
                        
                        // Top horizontal line
                        currentPath.moveTo(initialSquareSize - (initialSquareSize * horizontalProgress), 0);
                        currentPath.lineTo(initialSquareSize, 0);
                        
                        // Bottom horizontal line
                        currentPath.moveTo(0, initialSquareSize);
                        currentPath.lineTo(initialSquareSize * horizontalProgress, initialSquareSize);
                    }
                    
                    ShapeDrawable currentShape = new ShapeDrawable();
                    currentShape.getPaint().setColor(Color.parseColor("#6B4DE6"));
                    currentShape.getPaint().setStyle(Paint.Style.STROKE);
                    currentShape.getPaint().setStrokeWidth(dpToPx(4));
                    currentShape.getPaint().setAntiAlias(true);
                    currentShape.setShape(new PathShape(currentPath, initialSquareSize, initialSquareSize));
                    
                    outlineView.setBackground(currentShape);
                }

                // Fill square (40-50%)
                if (progress > 0.4f && progress <= 0.5f) {
                    float fillProgress = (progress - 0.4f) / 0.1f;
                    float fillEase = interpolateEaseInOut(fillProgress);
                    
                    outlineView.setAlpha(1 - fillEase);
                    filledSquare.setAlpha(fillEase);
                }

                // Show S (50-60%)
                if (progress > 0.5f && progress <= 0.6f) {
                    float sProgress = (progress - 0.5f) / 0.1f;
                    float sEase = interpolateEaseInOut(sProgress);
                    sPathView.setAlpha(sEase);
                }

                // Move and shrink to left (60-80%)
                if (progress > 0.6f && progress <= 0.8f) {
                    float moveProgress = (progress - 0.6f) / 0.2f;
                    float moveEase = interpolateEaseInOut(moveProgress);
                    
                    float scale = 1 - (0.4f * moveEase);  // Scale from 1.0 to 0.6
                    // Calculate the distance to move left based on screen width
                    float screenCenterX = rootView.getWidth() / 2f;
                    float finalLeftPosition = dpToPx(40);  // Final position from left edge
                    float startX = 0;  // Starting from center (translation is relative to current position)
                    float targetX = -(screenCenterX - finalLeftPosition - (initialSquareSize * scale / 2)) * moveEase;
                    
                    filledSquare.setScaleX(scale);
                    filledSquare.setScaleY(scale);
                    filledSquare.setTranslationX(targetX);
                    
                    sPathView.setScaleX(scale);
                    sPathView.setScaleY(scale);
                    sPathView.setTranslationX(targetX);
                }

                // Text animation (80-90%)
                if (progress > 0.8f && progress <= 0.9f) {
                    float textProgress = (progress - 0.8f) / 0.1f;
                    float textEase = interpolateEaseOut(textProgress);
                    
                    appNameText.setAlpha(textEase);
                    appNameText.setTranslationX(dpToPx(20) * (1 - textEase));
                    
                    if (textProgress > 0.3f) {
                        float subtitleProgress = (textProgress - 0.3f) / 0.7f;
                        subtitleText.setAlpha(subtitleProgress);
                        subtitleText.setTranslationX(dpToPx(20) * (1 - subtitleProgress));
                    }
                }

                // Final fade out (90-100%)
                if (progress > 0.9f) {
                    float fadeProgress = (progress - 0.9f) / 0.1f;
                    float fadeEase = interpolateEaseInOut(fadeProgress);
                    
                    filledSquare.setAlpha(1 - fadeEase);
                    sPathView.setAlpha(1 - fadeEase);
                    appNameText.setAlpha(1 - fadeEase);
                    subtitleText.setAlpha(1 - fadeEase);
                }

                if (progress < 1f) {
                    choreographer.postFrameCallback(this);
                } else {
                    isAnimating.set(false);
                    ((ViewGroup) rootView).removeView(outlineView);
                    ((ViewGroup) rootView).removeView(filledSquare);
                    ((ViewGroup) rootView).removeView(sPathView);
                    ((ViewGroup) rootView).removeView(appNameText);
                    ((ViewGroup) rootView).removeView(subtitleText);
                    onComplete.run();
                }
            }
        };

        choreographer.postFrameCallback(frameCallback);
    }

    private void createSquareOutlinePath(Path path, int size, float radius) {
        path.moveTo(size - radius, 0);
        path.lineTo(radius, 0);
        path.quadTo(0, 0, 0, radius);
        path.lineTo(0, size - radius);
        path.quadTo(0, size, radius, size);
        path.lineTo(size - radius, size);
        path.quadTo(size, size, size, size - radius);
        path.lineTo(size, radius);
        path.quadTo(size, 0, size - radius, 0);
    }

    private float getPathLength(Path path) {
        PathMeasure measure = new PathMeasure(path, false);
        return measure.getLength();
    }

    private void getPathSegment(Path path, float start, float end, Path dst) {
        PathMeasure measure = new PathMeasure(path, false);
        dst.reset();
        measure.getSegment(start, end, dst, true);
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    private float interpolateEaseInOut(float t) {
        return t < 0.5f ? 
            4f * t * t * t : 
            1f - (float)Math.pow(-2f * t + 2f, 3f) / 2f;
    }

    private float interpolateEaseOut(float t) {
        return 1f - (float)Math.pow(1f - t, 3f);
    }

    @Override
    public void finish() {
        super.finish();
        // Remove default transition animation
        overridePendingTransition(0, 0);
    }
}