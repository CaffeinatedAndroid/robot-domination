package caffeinatedandroid.dronedomination;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.View;

/**
 * Created by chris on 24/12/2016.
 */

public class TestCanvasView extends View {

    float x = 0f;
    float y = 0f;
    float radius = 100f;
    float x2 = 0f;
    float y2 = 0f;
    Paint paint;

    private int stepAmount = 10;

    public TestCanvasView(Context context) {
        super(context);
        x = getWidth() / 2;
        y = getHeight() / 2;
        paint = new Paint();
        paint.setStyle(Paint.Style.FILL);
        paint.setStrokeWidth(3f);
    }

    @Override
    public void onSizeChanged(int w, int h, int oldw, int oldh) {
        x = w / 2;
        y = h / 2;
        x2 = x;
        y2 = y - (float) (radius * 1.5);
    }

    public void updateXY(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public void moveLeft(float fraction) {
        this.x -= stepAmount * fraction;
    }

    public void moveRight(float fraction) {
        this.x += stepAmount * fraction;
    }

    public void moveUp(float fraction) {
        this.y -= stepAmount * fraction;
    }

    public void moveDown(float fraction) {
        this.y += stepAmount * fraction;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        //paint.setColor(Color.WHITE);
        //canvas.drawPaint(paint);
        paint.setColor(Color.GREEN);
        canvas.drawLine(x, y, x2, y2, paint);
        paint.setColor(Color.RED);
        canvas.drawCircle(x, y, radius, paint);
    }
}
