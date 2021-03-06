package caffeinatedandroid.dronedomination;

import android.annotation.SuppressLint;
import android.graphics.Point;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.os.Handler;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;

import caffeinatedandroid.views.JoystickView;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 * @author Christopher Bull
 */
public class ControllerActivity extends AppCompatActivity {
    /**
     * Whether or not the system UI should be auto-hidden after
     * {@link #AUTO_HIDE_DELAY_MILLIS} milliseconds.
     */
    private static final boolean AUTO_HIDE = true;

    /**
     * If {@link #AUTO_HIDE} is set, the number of milliseconds to wait after
     * user interaction before hiding the system UI.
     */
    private static final int AUTO_HIDE_DELAY_MILLIS = 3000;

    /**
     * Some older devices needs a small delay between UI widget updates
     * and a change of the status and navigation bar.
     */
    private static final int UI_ANIMATION_DELAY = 300;
    private final Handler mHideHandler = new Handler();
    private View mContentView;
    private final Runnable mHidePart2Runnable = new Runnable() {
        @SuppressLint("InlinedApi")
        @Override
        public void run() {
            // Delayed removal of status and navigation bar

            // Note that some of these constants are new as of API 16 (Jelly Bean)
            // and API 19 (KitKat). It is safe to use them, as they are inlined
            // at compile-time and do nothing on earlier devices.
            mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }
    };
    private View mControlsView;
    private final Runnable mShowPart2Runnable = new Runnable() {
        @Override
        public void run() {
            // Delayed display of UI elements
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.show();
            }
            mControlsView.setVisibility(View.VISIBLE);
        }
    };
    private boolean mVisible;
    private final Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            hide();
        }
    };
    /**
     * Touch listener to use for in-layout UI controls to delay hiding the
     * system UI. This is to prevent the jarring behavior of controls going away
     * while interacting with activity UI.
     */
    private final View.OnTouchListener mDelayHideTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            if (AUTO_HIDE) {
                delayedHide(AUTO_HIDE_DELAY_MILLIS);
            }
            return false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_controller);

        mVisible = true;
        mControlsView = findViewById(R.id.fullscreen_content_controls);
        mContentView = findViewById(R.id.fullscreen_content);


        // Set up the user interaction to manually show or hide the system UI.
        mContentView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggle();
            }
        });

        // Upon interacting with UI controls, delay any scheduled hide()
        // operations to prevent the jarring behavior of controls going away
        // while interacting with the UI.
        findViewById(R.id.dummy_button).setOnTouchListener(mDelayHideTouchListener);

        // Get screen width/height (used for joystick placement)
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        //display.getSize(size);
        display.getRealSize(size);
        int screenWidth = size.x;
        int screenHeight = size.y;

        // Init custom Joystick controls
        final TestCanvasView canvasView = new TestCanvasView(this);
        ((FrameLayout)findViewById(R.id.controller_layout)).addView(canvasView, screenWidth, 1850);// view, width, height
        // Bottom left joystick
        JoystickView jv = new JoystickView(this);
        ((FrameLayout)findViewById(R.id.controller_layout)).addView(jv, 500, 500);// view, width, height
        jv.setX(100f);
        jv.setY(screenHeight - 100f - 500);
        jv.setJoystickMoveListener(new JoystickView.JoystickMoveListener() {
            @Override
            public void OnJoystickMove(JoystickView.JoystickMoveEvent event) {
                /*switch (event.getDirection()) {
                    case Forward:
                        canvasView.moveUp(event.getDistance());
                        break;
                    case ForwardLeft:
                        canvasView.moveUp(event.getDistance());
                        canvasView.moveLeft(event.getDistance());
                        break;
                    case ForwardRight:
                        canvasView.moveUp(event.getDistance());
                        canvasView.moveRight(event.getDistance());
                        break;
                    case Back:
                        canvasView.moveDown(event.getDistance());
                        break;
                    case BackLeft:
                        canvasView.moveDown(event.getDistance());
                        canvasView.moveLeft(event.getDistance());
                        break;
                    case BackRight:
                        canvasView.moveDown(event.getDistance());
                        canvasView.moveRight(event.getDistance());
                        break;
                    case Left:
                        canvasView.moveLeft(event.getDistance());
                        break;
                    case Right:
                        canvasView.moveRight(event.getDistance());
                        break;
                }*/
                canvasView.moveSprite(event.getAngle_Radians(), event.getDistance());
                canvasView.invalidate();
            }
        });
        // Bottom right joystick
        JoystickView jv_br = new JoystickView(this);
        ((FrameLayout)findViewById(R.id.controller_layout)).addView(jv_br, 500, 500);// view, width, height
        jv_br.setX(screenWidth - 100f - 500);
        jv_br.setY(screenHeight - 100f - 500);
        jv_br.setJoystickMoveListener(new JoystickView.JoystickMoveListener() {
            @Override
            public void OnJoystickMove(JoystickView.JoystickMoveEvent event) {
                canvasView.setAngle((float)(event.getAngle_Radians()-Math.toRadians(-90)));
                canvasView.invalidate();
            }
        });
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        delayedHide(100);
    }

    private void toggle() {
        if (mVisible) {
            hide();
        } else {
            show();
        }
    }

    private void hide() {
        // Hide UI first
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
        mControlsView.setVisibility(View.GONE);
        mVisible = false;

        // Schedule a runnable to remove the status and navigation bar after a delay
        mHideHandler.removeCallbacks(mShowPart2Runnable);
        mHideHandler.postDelayed(mHidePart2Runnable, UI_ANIMATION_DELAY);
    }

    @SuppressLint("InlinedApi")
    private void show() {
        // Show the system bar
        mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        mVisible = true;

        // Schedule a runnable to display UI elements after a delay
        mHideHandler.removeCallbacks(mHidePart2Runnable);
        mHideHandler.postDelayed(mShowPart2Runnable, UI_ANIMATION_DELAY);
    }

    /**
     * Schedules a call to hide() in [delay] milliseconds, canceling any
     * previously scheduled calls.
     */
    private void delayedHide(int delayMillis) {
        mHideHandler.removeCallbacks(mHideRunnable);
        mHideHandler.postDelayed(mHideRunnable, delayMillis);
    }
}
