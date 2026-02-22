package platformer;

import dev.lwjgl.audio.AudioManager;

import java.util.List;

import static org.lwjgl.opengl.GL11.*;
import static platformer.PlatformerModel.*;

/**
 * Player entity with movement, jumping, dashing, and stabbing abilities.
 */
public class Player {

    private final int width = 800, height = 600;
    private final float worldWidth = PlatformerModel.WORLD_WIDTH, worldHeight = PlatformerModel.WORLD_HEIGHT;
    private final float speed = 200;
    public static boolean testing = true;
    public int whichcharacter = 2;
    public boolean skipGambling = true;
    private float invisibileFramDurration = 0.1f;
    private float invisibileFramCurrentTime;
    public static boolean isInvisibile;


    // Position and velocity
    public static float x, y;
    private float vx, vy;
    private boolean onGround;
    private boolean isfaceingRight = false;
    private boolean fall;
    private float shouldFall;

    // Dash system
    public static float dashCooldownTime = 0.6f;
    private float dashCurrentTime = dashCooldownTime;
    private boolean showDashEffect = false;
    private float dashEffectTimer = 0f;
    private final float dashEffectDuration = 0.1f;
    private float dashEffectAlpha = 0f;
    public static boolean showDashCooldownMessage = false;
    public float getDashCooldown() {
        return dashCurrentTime;
    }

    // jump system
    private float jumpSpeed = 600;
    private boolean startJumpChargeCounter;
    private int jumpChargeCounter;
    private final int jumpChargeCounterIncremants = 10;
    private boolean enableAdditionalJump;
    private int numberOfJump;
    private final int MAX_ADDITIONAL_JUMP = 3 + 1;// the +1 is because its coded so the first jump counts two times.
    private final float additionalJumpSpeed = 400; // its merged with maxJumpCounter
    public int currentGameTimeForJump;
    public static boolean canReleaseJump;
    private boolean showAdditionalJumpOval = false;//currently DON'T CHANGE
    private float jumpOvalTimer = 0f;
    private final float jumpOvalDuration = 0.3f;
    private float jumpOvalX = 0f, jumpOvalY = 0f;
    private float jumpOvalAlpha = 0f;

    // Stab system //1
    public boolean isStabbing;
    private final float stabCooldownCap = 5.0f;
    public float stabCurrentCooldown;
    public final float stabDurationCap = 0.1f;
    private float stabCurrentDuration;
    private float stabXRange = 50.0f;
    private float stabYRange = 20.0f;
    private final boolean forefield = false;
    public static boolean showStabCooldownMessage;
    public boolean initializeStabVarables;

    // Pray system
    public static boolean startPrayTimer;
    public static float prayTimer;
    public static float StartingStrikeChance = 0.001f;
    public static float strikeChance = StartingStrikeChance;
    private boolean canStrike = true;
    public static float heartsPerNTimer = 2.0f;  // 10 seconds = 1 heart
    private boolean shouldntMove;
    public static boolean renderPray;

    //Wind Burst
    public static boolean windBurst;
    public static boolean WindBurstSlow;
    public static final int windBurstSize = 100;
    public int currentGameTimeForWindBurst;





    public Player(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public void moveHoriz(int dir, boolean leftOrRight) {
        if (!PlatformerModel.freezeTime && !shouldntMove) {
            vx = dir * speed;
            isfaceingRight = leftOrRight;
        }
    }
    public void fall() {
        if (!PlatformerModel.freezeTime) {
            shouldFall = 0.1f;
        }
    }

    public void dash() {
        if (dashCurrentTime <= 0.0f && !PlatformerModel.freezeTime) {
            if (dashCooldownTime > 0 && PlatformerModel.dashLevelWasBrought) {
                dashCooldownTime -= ((PlatformerModel.dashLevel / 2) + 1.0f) * 0.01f;
                PlatformerModel.dashLevelWasBrought = false;
            }
            if (dashCooldownTime <= 0) dashCooldownTime = -0.01f;

            if (isfaceingRight) {
                x += 200;
            } else {
                x += -200;
            }
            dashCurrentTime = dashCooldownTime;
            dashEffectTimer = dashEffectDuration;
            dashEffectAlpha = dashCooldownTime;
            showDashEffect = true;
            invisibileFramCurrentTime = invisibileFramDurration;
            AudioManager.getInstance().playSFX("sfx09_electric_spark.wav");
        } else if (!PlatformerModel.freezeTime) {
            showDashCooldownMessage = true;
        }
    }

    public void stab() {
        if (this.stabCurrentCooldown <= 0.0f && !PlatformerModel.freezeTime) {
            stabCurrentCooldown = stabCooldownCap;
            stabCurrentDuration = stabDurationCap;
            isStabbing = true;
            AudioManager.getInstance().playSFX("sfx10_wood_hit.wav");
        } else if (!PlatformerModel.freezeTime) {
            showStabCooldownMessage = true;
        }
    }
    public void prayHold(){
        if (!PlatformerModel.freezeTime) {
            startPrayTimer = true;
            shouldntMove = true;
            renderPray = true;
            AudioManager.getInstance().playSFX("sfx05_power_up.wav");
        }
    }
    public void prayRelease(){
        if (!PlatformerModel.freezeTime) {
            shouldntMove = false;
            startPrayTimer = false;
            renderPray = false;
            if (canStrike) {
                if ((Math.random() < strikeChance)) {
                    strikeChance = StartingStrikeChance;
                    loseLife((int) (prayTimer / heartsPerNTimer));
                    System.out.println("You have been striked by the gods.");
                } else {
                    gainLife((int) (prayTimer / heartsPerNTimer));
                    strikeChance += Math.random() * PlatformerModel.difficulty.mobPerStar;
                    System.out.println("The gods heal you now. Do not rely on them too heavily");
                }
            } else {
                gainLife((int) (prayTimer / 1));
            }
            prayTimer = 0;
        }
    }
    public void jump() {
        if (!PlatformerModel.freezeTime) {
            if (onGround && numberOfJump == 0) {
                canReleaseJump = true;
                vy = jumpSpeed;
                startJumpChargeCounter = true;
                onGround = false;
                currentGameTimeForJump = (int) PlatformerModel.gameTime;
                AudioManager.getInstance().playSFX("sfx11_mouse_click.wav");
            }
        }
    }
    public void jumpAir() {
        enableAdditionalJump = (!onGround && numberOfJump < MAX_ADDITIONAL_JUMP && numberOfJump >= 1);
        if (enableAdditionalJump && !onGround && numberOfJump > 0) {
            vy = additionalJumpSpeed;
            this.enableAdditionalJump = false;
            // Show oval only when additional jump occurs
            if (numberOfJump >= 1) {
                showAdditionalJumpOval = true;
                jumpOvalTimer = jumpOvalDuration;
                jumpOvalX = x + 10;
                jumpOvalY = y - 5;
                jumpOvalAlpha = 0.5f;
                AudioManager.getInstance().playSFX("sfx12_mouse_click.wav");
            }
            numberOfJump++;
        }
    }
    public void jumpReleased(){
        if(numberOfJump == 0 && canReleaseJump){
            canReleaseJump = false;
            vy = jumpChargeCounter;
            startJumpChargeCounter = false;
            jumpChargeCounter = 0;
            numberOfJump++;
        }
    }
    public void windBurst(){
        windBurst = true;
        WindBurstSlow = true;
        currentGameTimeForWindBurst = (int) PlatformerModel.gameTime;
        AudioManager.getInstance().playSFX("sfx06_tv_noise.wav");
    }


    public void applyGravity(float dt) {
        if (!PlatformerModel.freezeTime && !shouldntMove) {
            vy -= 980 * dt;
        }
    }


    public void update(float dt, List<Platform> plats) {
        if (PlatformerModel.dashLevelWasBrought && dashCooldownTime > 0) {
            dashCooldownTime -= ((PlatformerModel.dashLevel / 2) * 0.1f);
            if (dashCooldownTime <= 0){
                dashCooldownTime = 0.1f;
            }
            PlatformerModel.dashLevelWasBrought = false;
        }
        if (!PlatformerModel.freezeTime) {
            if (currentGameTimeForJump + 25 <= (int) PlatformerModel.gameTime){
                jumpReleased();
            }
            if (currentGameTimeForWindBurst + 50 <= (int) PlatformerModel.gameTime){
                windBurst = false;
            }
            if (currentGameTimeForWindBurst + 80 <= (int) PlatformerModel.gameTime){
                WindBurstSlow = false;
            }
            if (onGround){
                startJumpChargeCounter = false;
                jumpChargeCounter = 0;
                numberOfJump = 0;
            }
            if (!shouldntMove) {
                x += vx * dt;
                y += vy * dt;
            }
            onGround = false;
            fall = false;
            if (y <= 25) {
                y = 20;
                onGround = true;
                vy = 0;
            }
            vx = 0;
            if (x > worldWidth - 20) x = worldWidth - 20;
            if (x < 0) x = 0;
            if (y > worldHeight - 20) y = worldHeight - 20;
            if (y < 20) y = 20;
            if(forefield && !initializeStabVarables){
                stabYRange *= 2;
                stabXRange *= 2;
                initializeStabVarables = true;
            }
            //Clocks/Timers
            if (startJumpChargeCounter){
                jumpChargeCounter += jumpChargeCounterIncremants;
                if(jumpChargeCounter > additionalJumpSpeed){
                    jumpChargeCounter = (int) additionalJumpSpeed;
                    jumpReleased();
                }
            }
            if (showAdditionalJumpOval) {
                jumpOvalTimer += dt;
                if (jumpOvalTimer <= 0.2f) {
                    jumpOvalAlpha = jumpOvalTimer / 0.2f;
                } else if (jumpOvalTimer <= 1.0f) {
                    jumpOvalAlpha = 1f - (jumpOvalTimer - 0.2f) / 0.8f;
                } else {
                    showAdditionalJumpOval = false;
                    jumpOvalAlpha = 0f;
                    jumpOvalTimer = 0f;
                }
            }
            if (showDashEffect) {
                dashEffectTimer -= dt;
                if (dashEffectTimer <= 0) {
                    showDashEffect = false;
                    dashEffectTimer = 0;
                    dashEffectAlpha = 0;
                } else {
                    dashEffectAlpha = dashEffectTimer / dashEffectDuration;
                }
            }
            if (dashCurrentTime > 0.0f) {
                dashCurrentTime -= 1.0f * dt;
                if (dashCurrentTime < 0.0f) dashCurrentTime = 0.0f;
            }

            if (stabCurrentDuration > 0.0f) {
                stabCurrentDuration -= 1.0f * dt;
                if (stabCurrentDuration <= 0.0f) {
                    stabCurrentDuration = 0.0f;
                    isStabbing = false;
                }
            }
            if (stabCurrentCooldown > 0.0f) { // countdown
                stabCurrentCooldown -= 1.0f * dt;
                if (stabCurrentCooldown <= 0.0f) {
                    stabCurrentCooldown = 0.0f;
                }
            }
            if (shouldFall > 0.0f) { // countdown
                shouldFall -= 1.0f * dt;
                fall = true;
                if (shouldFall <= 0.0f) {
                    shouldFall = 0.0f;
                    if (!onGround) AudioManager.getInstance().playSFX("sfx07_water_drop.wav");
                    fall = false;
                }
            }
            if (invisibileFramCurrentTime > 0.0f) { // countdown
                invisibileFramCurrentTime -= 1.0f * dt;
                isInvisibile = true;
                if (invisibileFramCurrentTime <= 0.0f) {
                    invisibileFramCurrentTime = 0.0f;
                    isInvisibile = false;
                }
            }
            if (startPrayTimer) { // countUp
                prayTimer += 1.0f * dt;
            }
            for (Platform p : plats) {
                if (p.x - 20 < x && x < p.x + p.w && p.y + p.h - 5 < y && y < p.y + p.h + 5 && !fall) {
                    if (vy <= -1) {
                        y = p.y + p.h;
                        onGround = true;
                        numberOfJump = 0;
                        vy = 0;
                    }
                }
            }
        }
    }

    public boolean playerCollidesWithStar(Star s) {
        return Math.hypot((x + 10) - s.x, (y + 10) - s.y) < Star.starSize;
    }

    public boolean playerCollidesWithMob(MobA m) {
        return x < m.x + m.size && x + 20 > m.x && y < m.y + m.size && y + 20 > m.y;
    }

    public boolean mobCollidesWithStab(MobA m) {
        if (!isStabbing) return false;

        double mx = m.x;
        double my = m.y;
        double ms = m.size;

        double stabX, stabWidth;

        double stabY = y - 5;
        double stabHeight = stabYRange + 10;

        if (forefield) {
            stabX = x - 10;
            stabWidth = 40;

            return stabX < mx + ms &&
                    stabX + stabWidth > mx &&
                    stabY < my + ms &&
                    stabY + stabHeight > my;
        }

        if (isfaceingRight) {
            stabX = x + 20;
            stabWidth = stabXRange;
        } else {
            stabX = x - stabXRange;
            stabWidth = stabXRange;
        }

        return stabX < mx + ms &&
                stabX + stabWidth > mx &&
                stabY < my + ms &&
                stabY + stabHeight > my;
    }


    public boolean isStarInDashTriangle(Star s) {
        if (!showDashEffect) return false;
        return Math.abs((isfaceingRight ? x - 200 : x + 200) - s.x) <= 201 && Math.abs((y + 10) - s.y) < Star.starSize;
    }


    public void renderJumpOval() {
        if (!showAdditionalJumpOval || jumpOvalAlpha <= 0f) return;
        glPushMatrix();
        glTranslatef(jumpOvalX, jumpOvalY, 0);
        glColor4f(1f, 1f, 1f, jumpOvalAlpha);
        int segments = 40;
        float radiusX = 15f;
        float radiusY = 7f;
        glBegin(GL_TRIANGLE_FAN);
        glVertex2f(0, 0);
        for (int i = 0; i <= segments; i++) {
            double angle = 2 * Math.PI * i / segments;
            float dx = (float) Math.cos(angle) * radiusX;
            float dy = (float) Math.sin(angle) * radiusY;
            glVertex2f(dx, dy);
        }
        glEnd();
        glPopMatrix();
    }

    public void renderDashEffect() {
        if (!showDashEffect || dashEffectAlpha <= 0f) return;
        glPushMatrix();
        glTranslatef(x, y, 0);

        if (jumpChargeCounter == 0) {
            glColor4f(0, 0, 1, dashEffectAlpha);
        } else {
            float red = Math.min(1.0f, jumpChargeCounter / 300f);
            glColor4f(red, 0, 1 - red, dashEffectAlpha);
        }

        glBegin(GL_TRIANGLES);
        if (isfaceingRight) {
            glVertex2f(0, 0);
            glVertex2f(0, 20);
            glVertex2f(-200, 10);
        } else {
            glVertex2f(0, 0);
            glVertex2f(0, 20);
            glVertex2f(200, 10);
        }
        glEnd();
        glPopMatrix();
    }

    public void renderStabEffect() {
        if (isStabbing && !forefield) {
            glPushMatrix();
            glTranslatef(x, y, 0);

            glBegin(GL_QUADS);
            glColor4f(1.0f, 0.0f, 0.0f, 0.5f);
            if (isfaceingRight) {
                // Rectangle extending to the right
                glVertex2f(0, 0);
                glVertex2f(0, stabYRange);
                glVertex2f(stabXRange + 10, stabYRange);
                glVertex2f(stabXRange + 10, 0);
            } else {
                // Rectangle extending to the left
                glVertex2f(0 + 10, 0);
                glVertex2f(0 + 10, stabYRange);
                glVertex2f(-stabXRange + 10, stabYRange);
                glVertex2f(-stabXRange + 10, 0);
            }

            glEnd();
            glPopMatrix();
        } else if (isStabbing){
            glPushMatrix();
            glTranslatef(x, y, 0);
            glEnable(GL_BLEND);
            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
            glColor4f(1.0f, 0.0f, 0.0f, 0.5f);
            glBegin(GL_QUADS);
            if (onGround){
                glVertex2f(-10, 0);
                glVertex2f(-10,  30);
                glVertex2f( 30,  30);
                glVertex2f( 30, 0);
            } else {
                glVertex2f(-10, -10);
                glVertex2f(-10,  30);
                glVertex2f( 30,  30);
                glVertex2f( 30, -10);
            }
            glEnd();
            glPopMatrix();

        }
    }


    public void render() {
        renderJumpOval();
        renderDashEffect();
        renderStabEffect();
        glColor3f(0, 0, 1);
        glBegin(GL_QUADS);
        glVertex2f(x, y);
        glVertex2f(x + 20, y);
        glVertex2f(x + 20, y + 20);
        glVertex2f(x, y + 20);
        glEnd();
    }
}

