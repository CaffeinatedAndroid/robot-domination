package caffeinatedandroid.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.MotionEvent;
import android.view.View;

/**
 * Virtual Joystick. This bespoke joystick view is customisable, and is capable of reporting various
 * values. Default values for a joystick movement event are angle (in degress), and distance from
 * centre (in percentage).
 * @author Christopher Bull
 */
public class JoystickView extends View implements Runnable {

    // TODO make bool preferences settable in constructor and/or setters
    // TODO make touchable canvas area larger than the radius of the joystick (so can touch just outside the circle and still register touch event)
    // TODO preferences for joystick dead-zone (optionally adjust distance measure to exclude dead-zone)
    // TODO joystick that appears on touch (re-centering on each ACTION_DOWN) - perhaps an app feature, not a View feature.
    // TODO add customisable tolerance value to stop small repetitive events (e.g. if touch coords don't move by x amount)
    // TODO optionally paint line (canvas.drawLine) between centre and touch point.
    // TODO add separate listener with simple forward/backward/left/right
    // TODO optional flexible center (define center_x/y on each initial press) - fixed by default.

    public enum Type {
        Joystick,
        DPad,
        WASD
    }

    public enum Direction {
        Forward,
        ForwardRight,
        Right,
        BackRight,
        Back,
        BackLeft,
        Left,
        ForwardLeft,
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

    // State
    private boolean touching = false;

    // Listener
    private JoystickMoveListener moveListener = null;

    // Preferences
    private boolean alwaysShowJoystickPos = true;
    private boolean drawTouchLine = true;
    private boolean overlapBorderBounds = false;
    //private boolean recenterJoystickWhenTouchExternal = true; // TODO
    private boolean recenterJoystickWhenNoTouch = true;
    private boolean polling = true;

    // Vars for polling
    /** Fast poll interval: 17 milliseconds (16.67ms == 60fps) */
    public static long POLL_INTERVAL_FAST = 17;
    /** Medium poll interval: 33 milliseconds (33.33ms == 30fps) */
    public static long POLL_INTERVAL_MEDIUM = 33;
    /** Slow poll interval: 100 milliseconds (100ms == 10fps) */
    public static long POLL_INTERVAL_SLOW = 100;
    private long PollInterval = POLL_INTERVAL_FAST;
    private float touch_x_cached = 0f;
    private float touch_y_cached = 0f;
    private float center_x_cached = 0f;
    private float center_y_cached = 0f;
    private float radius_cached = 0f;
    private JoystickMoveEvent moveEvent_cached = null;
    private Thread tPoll = new Thread(this);

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
        if(alwaysShowJoystickPos || touching) {
            // Joystick - circle
            canvas.drawCircle(joystickInnerX, joystickInnerY, radius_InnerJoystick, paintCircleBorder);
            // Joystick - line
            if(drawTouchLine) {
                canvas.drawLine(center_x, center_y, joystickInnerX, joystickInnerY, paintCircleBorder);
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float touch_x = event.getX();
        float touch_y = event.getY();

        float distFromCenter = (float) calculateDistance(touch_x, touch_y, center_x, center_y);

        // Set touching state (for drawing) and manage the polling thread
        if(distFromCenter <= radius && (event.getAction() == MotionEvent.ACTION_DOWN
                // OR, A touch move event has returned to inside the circle
                || (!touching && event.getAction() == MotionEvent.ACTION_MOVE))) {
            // Touch action occurred within the joystick circle (i.e. not just in the containing rectangle).
            setTouching(true);
        } else if(event.getAction() == MotionEvent.ACTION_UP) {
            setTouching(false); // Remove inner-joystick when no longer being touched
        }

        // Calculating if touch point is within the border circle (minus any expected overlap)
        if(distFromCenter <= radius_NoOverlapBorderBounds) {
            // Update inner-joystick coordinates
            joystickInnerX = touch_x;
            joystickInnerY = touch_y;
        } else { // Touch point is at circle's border (or beyond)
            //if(alwaysShowJoystickPos) {
                // Calculate point on circle border nearest given touch point
                // http://stackoverflow.com/a/300894
                double vX = touch_x - center_x;
                double vY = touch_y - center_y;
                double magV = Math.sqrt(vX * vX + vY * vY);
                joystickInnerX = (float) (center_x + vX / magV * radius_PreferentiallyAdjusted);
                joystickInnerY = (float) (center_y + vY / magV * radius_PreferentiallyAdjusted);
            //} else {
            //if(!alwaysShowJoystickPos) {
            //    setTouching(false);
            //}
        }

        // Report now, or wait for reporting poll
        if(polling) { // Let runnable lazily calculate and report at the poll interval/rate
            touch_x_cached = joystickInnerX;
            touch_y_cached = joystickInnerY;
            center_x_cached = center_x;
            center_y_cached = center_y;
            radius_cached = radius;
        } else if(touching && moveListener != null) { // Report event straight away.
            moveListener.OnJoystickMove(new JoystickMoveEvent(
                    // Raw X/Y
                    joystickInnerX, joystickInnerY,
                    // Distance
                    calculateDistance_AsDecimalFraction(joystickInnerX, joystickInnerY, center_x, center_y, radius),
                    // Angle
                    calculateAngle_InDegrees(joystickInnerX, joystickInnerY, center_x, center_y)));
        }

        // Re-center joysticks after processing the rest of Touch Event.
        // JoystickMoveEvents will have previous X/Y coords, but will be centered on next draw call.
        if(!touching && recenterJoystickWhenNoTouch) {
            joystickInnerX = center_x;
            joystickInnerY = center_y;
        }

        // Redraw
        invalidate();
        return true;
    }

    /**
     * Caches the touching/not-touching state of the Joystick.
     * Optionally (with 'recenterJoystickWhenNoTouch'), re-centers the inner-joystick to the center.
     * @param touching Whether the joystick is currently being touched.
     */
    private void setTouching(boolean touching) {
        this.touching = touching;
        if(touching) {
            // Kill any existing threads
            if(tPoll != null && tPoll.isAlive()) {
                tPoll.interrupt();
            }
            // Start new thread
            tPoll = new Thread(this);
            tPoll.start();
        } else {
            tPoll.interrupt(); // quit thread
        }
    }

    @Override
    public void run() {
        while(!Thread.interrupted()) {
            if (moveListener != null) {
                post(new Runnable() {
                    @Override
                    public void run() {
                        // Don't re-calculate MoveEvent if no moving has occurred.
                        if(isCachedMoveEventCurrent()) {
                            moveListener.OnJoystickMove(moveEvent_cached);
                        } else {
                            moveListener.OnJoystickMove(moveEvent_cached = new JoystickMoveEvent(
                                    // Raw X/Y
                                    touch_x_cached, touch_y_cached,
                                    // Distance
                                    calculateDistance_AsDecimalFraction(
                                            touch_x_cached,
                                            touch_y_cached,
                                            center_x_cached,
                                            center_y_cached,
                                            radius_cached),
                                    // Angle
                                    calculateAngle_InDegrees(
                                            touch_x_cached,
                                            touch_y_cached,
                                            center_x_cached,
                                            center_y_cached)));
                        }
                    }
                });
            }
            //  Sleep for Poll Interval
            try {
                Thread.sleep(PollInterval);
            } catch(InterruptedException e) {
                break;
            }
        }
    }

    private boolean isCachedMoveEventCurrent() {
        return moveEvent_cached != null
                && moveEvent_cached.touchX == touch_x_cached
                && moveEvent_cached.touchY == touch_y_cached;
    }

    //////////////////////
    // Calculate Output //
    //////////////////////

    /**
     * Calculate angle of touch point to center of joystick, in degrees (0 at top of circle)
     * @param touch_x Touch point X coordinate
     * @param touch_y Touch point Y coordinate
     * @param center_x Centre point X coordinate
     * @param center_y Centre point Y coordinate
     * @return angle in degrees
     */
    private float calculateAngle_InDegrees(float touch_x, float touch_y, float center_x, float center_y) {
        float angle = (float) Math.toDegrees(Math.atan2(-(center_x - touch_x), center_y - touch_y));
        if(angle < 0) {
            angle += 360;
        }
        return angle;
    }

    /**
     * Calculates the distance between the touch point and the center of the Joystick.
     * @param touch_x Touch point X coordinate
     * @param touch_y Touch point Y coordinate
     * @param center_x Centre point X coordinate
     * @param center_y Centre point Y coordinate
     * @return distance as an absolute value
     */
    private double calculateDistance(float touch_x, float touch_y, float center_x, float center_y) {
        return Math.hypot(center_x - touch_x, center_y - touch_y);
    }

    /**
     * Calculates the distance between the touch point and the center of the Joystick. Returns value
     * as a decimal fraction (i.e. between 0 and 1).
     * @param touch_x Touch point X coordinate
     * @param touch_y Touch point Y coordinate
     * @param center_x Centre point X coordinate
     * @param center_y Centre point Y coordinate
     * @param radius The radius of the joystick
     * @return the distance as a fraction
     */
    private float calculateDistance_AsDecimalFraction(float touch_x, float touch_y, float center_x, float center_y, float radius) {
        double distance = calculateDistance(touch_x, touch_y, center_x, center_y);
        return Math.min((float)distance / radius, 1f); // Max return value: 1.0
    }

    /**
     * Calculates a simplified enum value for the Joystick's direction based on an angle input
     * @param angle The 360 degree angle of the joystick (0 at the top/front)
     * @return A simplified enum description of the Joystick's angle.
     */
    public static Direction calculateDirection(float angle) {
        if(angle >= 337.5 || angle <= 22.5) {
            return Direction.Forward;
        } else if(angle <= 67.5) {
            return Direction.ForwardRight;
        } else if(angle <= 112.5) {
            return Direction.Right;
        } else if(angle <= 157.5) {
            return Direction.BackRight;
        } else if(angle <= 202.5) {
            return Direction.Back;
        } else if(angle <= 247.5) {
            return Direction.BackLeft;
        } else if(angle <= 292.5) {
            return Direction.Left;
        } else {
            return Direction.ForwardLeft;
        }
    }

    ////////////////////
    // Listener/Event //
    ////////////////////

    /**
     * Attaches a moveListener object to this View, which will be notified upon each
     * Joystick movement event.
     * @param listener An instance of the Listener interface to attach to this View
     */
    public void setJoystickMoveListener(JoystickMoveListener listener) {
        this.moveListener = listener;
    }

    /**
     * Interface definition for a callback to be invoked when a Joystick view is moved.
     */
    public interface JoystickMoveListener {
        /**
         * Called when a Joystick is moved.
         * @param event Contains information about the joystick movement event.
         */
        void OnJoystickMove(JoystickMoveEvent event);
    }

    /**
     * Event object that contains information about a Joystick movement event.
     */
    public class JoystickMoveEvent {
        public final float touchX;
        public final float touchY;
        private final float angle;
        private Direction direction;
        private final float distance;

        /**
         * Initialises the Joystick move event object.
         * Use the get*() methods to retrieve event information.
         * @param distance distance from center
         * @param angle angle from top
         */
        JoystickMoveEvent(float touchX, float touchY, float distance, float angle) {
            this.touchX = touchX;
            this.touchY = touchY;
            this.angle = angle;
            this.distance = distance;
        }

        /**
         * Retrieves the angle of the joystick in degrees; 0 is the top of the joystick.
         * @return The angle of the joystick
         */
        public float getAngle() {
            return angle;
        }

        /**
         * Retrieves a enum value that represents a simplified direction (e.g. 'Forward')
         * @return The direction of the Joystick angle, of type JoystickView.Direction
         */
        public Direction getDirection() {
            if(direction == null) {
                direction = JoystickView.calculateDirection(angle);
            }
            return direction;
        }

        /**
         * Retrieves the distance of the joystick from it's centre. Value is a percentage.
         * @return Percentage distance from the centre.
         */
        public float getDistance() {
            return distance;
        }
    }
}
