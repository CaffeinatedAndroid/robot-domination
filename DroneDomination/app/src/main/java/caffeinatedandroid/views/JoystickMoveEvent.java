package caffeinatedandroid.views;

/**
 * Created by chris on 24/12/2016.
 */

public class JoystickMoveEvent {
    private float angle;
    private float distance;

    JoystickMoveEvent(float distance, float angle) {
        this.angle = angle;
        this.distance = distance;
    }

    public float getAngle() {
        return angle;
    }

    public float getDistance() {
        return distance;
    }
}
