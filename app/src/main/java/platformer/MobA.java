package platformer;

import static org.lwjgl.opengl.GL11.glColor3f;
import dev.lwjgl.ShapeRenderer;

public class MobA {

    public float x, y;
    public float size = 20f;

    // tuning values
    private static final float MIN_SPEED = 1f;
    private static final float MAX_SPEED = 3f;
    private static final float SPEED_RAMP_DISTANCE = 400f;
    private static final float WIND_STRENGTH = 1f;

    public MobA(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public void update(float dt) {
        if (PlatformerModel.freezeTime) return;

        // -------------------------------------------------
        // Centers
        float px = Player.x + 10;
        float py = Player.y + 10;
        float mx = x + size * 0.5f;
        float my = y + size * 0.5f;

        float dx = px - mx;
        float dy = py - my;

        float distance = (float) Math.sqrt(dx * dx + dy * dy);
        if (distance == 0f) return;

        dx /= distance;
        dy /= distance;

        // -------------------------------------------------
        // Speed ramp
        float t = Math.min(distance / SPEED_RAMP_DISTANCE, 1f);
        float speed = MIN_SPEED + (MAX_SPEED - MIN_SPEED) * t;

        float pushAmount = Math.max(0f, (Player.windBurstSize + size) - distance);

        // -------------------------------------------------
        // Movement modifiers
        float slowMultiplier = 1f;   // 1 = normal speed

        // -------------------------------------------------
        // WIND BURST ZONES
        if (Player.windBurst && distance < Player.windBurstSize) {
            // Inner zone: strong knockback
            float knockback = pushAmount * WIND_STRENGTH;
            x -= dx * knockback;
            y -= dy * knockback;
            slowMultiplier = 0f; // fully stop while being blasted

        } else if (Player.WindBurstSlow && distance < Player.windBurstSize + 20) {
            // Outer zone: slow only (NO knockback)
            slowMultiplier = 0.25f; // tweak: 0.2–0.4 feels good
        }

        // -------------------------------------------------
        // BASE BEHAVIOR (after modifiers decided)
        if (!Player.isInvisibile) {
            // Normal chase, affected by slow
            x += dx * speed * slowMultiplier;
            y += dy * speed * slowMultiplier;
        } else {
            // Invisible repel (NOT using wind strength)
            float repelForce = pushAmount * 0.15f; // tuned constant
            x -= dx * repelForce;
            y -= dy * repelForce;
        }

        // -------------------------------------------------
        // World bounds
        x = Math.max(0, Math.min(x, PlatformerModel.WORLD_WIDTH - size));
        y = Math.max(20, Math.min(y, PlatformerModel.WORLD_HEIGHT - size));
    }


    public void render() {
        glColor3f(0.4f, 0.0f, 0.4f);
        ShapeRenderer.renderRect(x, y, size, size);
    }
}
