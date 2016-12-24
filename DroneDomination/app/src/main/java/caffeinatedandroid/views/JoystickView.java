package caffeinatedandroid.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.MotionEvent;
import android.view.View;

/**
 * Created by chris on 23/12/2016.
 */

public class JoystickView extends View {

    // TODO make touchable canvas area larger than the radius of the joystick (so can touch just outside the circle and still register touch event)
    // TODO preferences for joystick deadzone (optionally adjust distance measure to exclude deadzone)
    // TODO joystick that appears on touch (recentering on each ACTION_DOWN) - perhaps an app feature, not a View feature.

    public enum Type {
        Joystick,
        DPad,
        WASD
    }

    ////////////////
    // Properties //
    ////////////////

    // Type
    private Type joystickType;
    private int joystickStickiness = 0;

    // Style
    private Paint paintCircle;
    private Paint paintCircleBorder;
    private int paintCircleBorder_StrokeWidth = 10;

    // Measurements
    private float center_x = 0f;
    private float center_y = 0f;
    private float radius = 0f;
    private float radius_WithoutBorderWidth = 0f;
    private float radius_InnerJoystick = 0f;
    private float radius_NoOverlapBorderBounds = 0f;
    private float radius_PreferentiallyAdjusted = 0f;
    private float joystickInnerX = 0;
    private float joystickInnerY = 0;
    private float cachedPowerOfTwo_radius_PreferentiallyAdjusted = 0f;

    // State
    private boolean touching = false;

    // Listener
    private JoystickMoveListener listener = null;

    // Preferences
    private boolean alwaysShowJoystickPos = true;
    private boolean overlapBorderBounds = false;
    //private boolean recenterJoystickWhenTouchExternal = true; // TODO
    private boolean recenterJoystickWhenNoTouch = true;

    //////////////////
    // Constructors //
    //////////////////

    public JoystickView(Context context) {
        this(context, Type.Joystick);
    }

    public JoystickView(Context context, Type joystickType) {
        super(context);

        this.joystickType = joystickType;

        // Style
        paintCircle = new Paint();
        paintCircle.setAntiAlias(true);
        paintCircle.setColor(Color.GRAY);
        paintCircle.setAlpha(100);
        paintCircle.setStyle(Paint.Style.FILL);
        paintCircleBorder = new Paint();
        paintCircleBorder.setAntiAlias(true);
        paintCircleBorder.setColor(Color.BLACK);
        paintCircleBorder.setStyle(Paint.Style.STROKE);
        paintCircleBorder.setStrokeWidth(paintCircleBorder_StrokeWidth);
        //if(joystickType == Types.DPad){}
    }

    public JoystickView(Context context, Type joystickType, int degreeOfStickiness) {
        this(context, joystickType);
        setJoystickStickiness(degreeOfStickiness);
    }

    /////////////
    // Methods //
    /////////////

    public void setJoystickStickiness(int degreeOfStickiness) {
        joystickStickiness = degreeOfStickiness;
        // TODO
    }

    public void setOverlapBorderBounds(boolean overlap) {
        overlapBorderBounds = overlap;
        radius_PreferentiallyAdjusted = overlap ? radius : radius_NoOverlapBorderBounds; // Cached shortcut (i.e. doesn't re-evaluate which radius to use on each TouchEvent)
        cachedPowerOfTwo_radius_PreferentiallyAdjusted = (float) Math.pow(radius_PreferentiallyAdjusted, 2);
    }

    public void setJoystickMoveListener(JoystickMoveListener listener) {
        this.listener = listener;
    }

    @Override
    public void onSizeChanged(int w, int h, int oldw, int oldh) {
        center_x = ((float) w) / 2;
        center_y = ((float) h) / 2;
        radius = center_x;
        radius_WithoutBorderWidth = ((float)w - paintCircleBorder_StrokeWidth) / 2;
        radius_InnerJoystick = radius / 3;
        radius_NoOverlapBorderBounds = radius - radius_InnerJoystick;
        radius_PreferentiallyAdjusted = overlapBorderBounds ? radius : radius_NoOverlapBorderBounds;
        cachedPowerOfTwo_radius_PreferentiallyAdjusted = (float) Math.pow(radius_PreferentiallyAdjusted, 2);
        joystickInnerX = center_x;
        joystickInnerY = center_y;
    }

    @Override
    public void onDraw(Canvas canvas) {
        // Background
        canvas.drawCircle(center_x, center_y, radius, paintCircle);
        // Border
        canvas.drawCircle(center_x, center_y, radius_WithoutBorderWidth, paintCircleBorder);
        // Joystick
        if(alwaysShowJoystickPos || touching){
            canvas.drawCircle(joystickInnerX, joystickInnerY, radius_InnerJoystick, paintCircleBorder);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float touch_x = event.getX();
        float touch_y = event.getY();

        // Calculating if touch point is within the border circle
        // Algorithm: (touch_x - center_x)^2 + (touch_y - center_y)^2 < radius^2
        // http://stackoverflow.com/a/481150/508098
        if(Math.pow(touch_x - center_x, 2) + Math.pow(touch_y - center_y, 2)
                < cachedPowerOfTwo_radius_PreferentiallyAdjusted) {
            // Update inner-joystick coordinates
            joystickInnerX = touch_x;
            joystickInnerY = touch_y;
            // Set touching/ACTION_DOWN parameter
            if(event.getAction() == MotionEvent.ACTION_DOWN){
                setTouching(true);
            }else if(event.getAction() == MotionEvent.ACTION_UP){
                setTouching(false);
            }
            // Report value
            listener.OnJoystickMove(
                    new JoystickMoveEvent(
                            // Distance
                            calculateAngleDistance_AsPercent(touch_x, touch_y),
                            // Angle
                            calculateAngle_InDegrees(touch_x, touch_y)));
        }else{
            // Touch point is outside the border circle
            if(alwaysShowJoystickPos) {
                // Calculate point on circle border nearest given touch point
                //if(!recenterJoystickWhenTouchExternal) {
                    // http://stackoverflow.com/a/300894
                    double vX = touch_x - center_x;
                    double vY = touch_y - center_y;
                    double magV = Math.sqrt(vX * vX + vY * vY);
                    joystickInnerX = (float) (center_x + vX / magV * radius_PreferentiallyAdjusted);
                    joystickInnerY = (float) (center_y + vY / magV * radius_PreferentiallyAdjusted);
                //}
                // Remove inner-joystick when no longer being touched
                if(event.getAction() == MotionEvent.ACTION_UP) {
                    setTouching(false);
                }else{
                    // Report value
                    listener.OnJoystickMove(
                            new JoystickMoveEvent(
                                    // Distance
                                    calculateAngleDistance_AsPercent(touch_x, touch_y),
                                    // Angle
                                    calculateAngle_InDegrees(touch_x, touch_y)));
                }
            }else{
                setTouching(false);
            }
        }

        // Redraw
        invalidate();
        return true;
    }

    private void setTouching(boolean touching) {
        this.touching = touching;
        if(!touching && recenterJoystickWhenNoTouch) {
            joystickInnerX = center_x;
            joystickInnerY = center_y;
        }
    }

    /**
     * Calculate angle of touch point to center of joystick, in degrees (0 at top of circle)
     * @param touch_x Touch point X coordinate
     * @param touch_y Touch point Y coordinate
     * @return angle in degrees
     */
    private float calculateAngle_InDegrees(float touch_x, float touch_y) {
        float angle = (float) Math.toDegrees(Math.atan2(-(center_x - touch_x), center_y - touch_y));
        if(angle < 0) {
            angle += 360;
        }
        return angle;
    }

    private float calculateAngleDistance_AsPercent(float touch_x, float touch_y) {
        double distance = Math.hypot(center_x - touch_x, center_y - touch_y);
        return Math.min(((float)distance * 100.0f) / radius, 100f); // Percentage (100% max)
    }
}
