package caffeinatedandroid.dronedomination;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.view.View;

/**
 * A simple test playground, to test JoystickViews.
 * @author Christopher Bull
 */
public class TestCanvasView extends View {

    float x = 0f;
    float y = 0f;
    float radius = 100f;
    float x2 = 0f;
    float y2 = 0f;
    float angle = 0f;
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

    public void setAngle(float angle) {
        this.angle = angle;
        PointF pf = rotate_point(x, y, angle, new PointF(x, y - (float) (radius * 1.5)));
        x2 = pf.x;
        y2 = pf.y;
    }

    // Rotates by angle amount, not to the angle
    PointF rotate_point(float cx, float cy, float angle, PointF p)
    {
        float s = (float) Math.sin(angle);
        float c = (float) Math.cos(angle);

        // translate point back to origin:
        p.x -= cx;
        p.y -= cy;

        // rotate point
        float xnew = p.x * c - p.y * s;
        float ynew = p.x * s + p.y * c;

        // translate point back:
        p.x = xnew + cx;
        p.y = ynew + cy;
        return p;
    }

    public void updateXY(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public void moveSprite(float angle, float distance) {
        // Ignores angle it is facing (strafe only)
        /*float xMove = (float) Math.cos(angle)*(distance*10); // arbitrary *10, due to simple canvas (not proper 2D engine
        float yMove = (float) Math.sin(angle)*(distance*10);
        x += xMove;
        x2 += xMove;
        y += yMove;
        y2 += yMove;*/

        float xMove = (float) Math.cos(angle+this.angle)*(distance*10); // arbitrary *10, due to simple canvas (not proper 2D engine
        float yMove = (float) Math.sin(angle+this.angle)*(distance*10);
        x += xMove;
        x2 += xMove;
        y += yMove;
        y2 += yMove;

        //
        /*PointF pf = rotate_point(x, y, angle / 10, new PointF(x2, y2));
        x2 = pf.x;
        y2 = pf.y;*/

        //float pxDist = distance *10;

        // now rotate around the source point

    }

    public void moveLeft(float fraction) {
        this.x -= stepAmount * fraction;
        this.x2 -= stepAmount * fraction;
    }

    public void moveRight(float fraction) {
        this.x += stepAmount * fraction;
        this.x2 += stepAmount * fraction;
    }

    public void moveUp(float fraction) {
        this.y -= stepAmount * fraction;
        this.y2 -= stepAmount * fraction;
    }

    public void moveDown(float fraction) {
        this.y += stepAmount * fraction;
        this.y2 += stepAmount * fraction;
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
