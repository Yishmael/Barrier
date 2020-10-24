package misc;

public class TimeKeeper {
    private static long now, lastTickTime, lastFpsUpdateTime;
    private static final float tbf = 1f / 60 * 1000;
    public static int fps;

    public static void update() {
        now = System.currentTimeMillis();
        if (now - lastTickTime < tbf) {
            try {
                Thread.sleep((long) (tbf - (now - lastTickTime)));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        if (System.currentTimeMillis() - lastFpsUpdateTime > 1000) {
            fps = (int) (1f / (now - lastTickTime) * 1000);
            lastFpsUpdateTime = now;
        }
        lastTickTime = now;
    }

}
