package cn.local.quickrecorder;

/** Pure Java: two short complete presses, 450 ms between downs, no repeats. */
public final class DoublePress {
    private long first = -1, downAt = -1, lastTrigger = -2000;
    private boolean down;
    public boolean event(boolean isDown, int repeats, long time) {
        if (isDown) {
            if (repeats > 0 || down) { first = -1; return false; }
            down = true; downAt = time; return false;
        }
        if (!down) return false;
        down = false;
        if (time < downAt || time - downAt > 250) { first = -1; return false; }
        if (first >= 0 && downAt - first <= 450 && downAt >= first && time - lastTrigger >= 1000) {
            first = -1; lastTrigger = time; return true;
        }
        first = downAt; return false;
    }
    public void reset() { first = -1; down = false; }
}
