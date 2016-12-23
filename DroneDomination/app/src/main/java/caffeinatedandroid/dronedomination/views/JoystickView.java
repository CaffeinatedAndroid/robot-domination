package caffeinatedandroid.dronedomination.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

/**
 * Created by chris on 23/12/2016.
 */

public class JoystickView extends View {

    public enum Type {
        Joystick,
        DPad,
        WASD
    }

    ////////////////
    // Properties //
    ////////////////

    private Type joystickType;
    private int joystickStickiness = 0;

    private Paint paintCircle = new Paint();
    private Paint paintCircleBorder = new Paint();
    private int paintCircleBorder_StrokeWidth = 10;

    private float joystickInnerX = 0;
    private float joystickInnerY = 0;

    private boolean alwaysShowJoystickPos = false;
    private boolean touching = false;

    //////////////////
    // Constructors //
    //////////////////

    public JoystickView(Context context) {
        this(context, Type.Joystick);
    }

    public JoystickView(Context context, Type joystickType) {
        super(context);

        this.joystickType = joystickType;

        // Colours
        paintCircle.setAntiAlias(true);
        paintCircle.setColor(Color.GREEN);
        paintCircle.setStyle(Paint.Style.FILL);
        paintCircleBorder.setAntiAlias(true);
        paintCircleBorder.setColor(Color.BLACK);
        paintCircleBorder.setStyle(Paint.Style.STROKE);
        paintCircleBorder.setStrokeWidth(paintCircleBorder_StrokeWidth);
        //if(joystickType == Types.DPad){}

        // TODO
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

    @Override
    public void onDraw(Canvas canvas) {
        // Background
        canvas.drawCircle((float)getWidth() / 2, (float)getHeight() / 2, (float)getWidth() / 2, paintCircle);
        // Border
        canvas.drawCircle((float)getWidth() / 2, (float)getHeight() / 2, ((float)getWidth() - paintCircleBorder_StrokeWidth) / 2, paintCircleBorder);
        // Joystick
        if(alwaysShowJoystickPos || touching){
            canvas.drawCircle(joystickInnerX, joystickInnerY, (((float)getWidth() / 2) / 3), paintCircleBorder);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        // Calculating if touch point is within the border circle
        // Algorithm: (touch_x - center_x)^2 + (touch_y - center_y)^2 < radius^2
        float radius = (float)getWidth() / 2;
        float center_x = (float)getWidth() / 2;
        float center_y = (float)getHeight() / 2;
        float innerJoystickRadius = (((float)getWidth() / 2) / 3);
        //if(Math.pow(x - center_x, 2) + Math.pow(y - center_y, 2) < Math.pow(radius, 2)){ // joystick overlaps border
        if(Math.pow(x - center_x, 2) + Math.pow(y - center_y, 2) < Math.pow(radius-innerJoystickRadius, 2)){ // joystick remains inside border
            Log.d("tag", "in");
            joystickInnerX = x;
            joystickInnerY = y;

            // Set touching/ACTION_DOWN parameter
            if(event.getAction() == MotionEvent.ACTION_DOWN){
                touching = true;
            }
            else if(event.getAction() == MotionEvent.ACTION_UP){
                touching = false;
            }
        }
        else{

            joystickInnerX = (float) Math.pow(x - center_x, 2);
            joystickInnerY = (float) Math.pow(y - center_y, 2);

            // Don't redraw - unless joystick currently showing (remove it)
            if(touching && event.getAction() == MotionEvent.ACTION_UP){
                touching = false;
            }
        }

        // Redraw
        invalidate();
        return true;
    }
}
