package platformer;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.lwjgl.opengl.GL11.GL_ALL_ATTRIB_BITS;
import static org.lwjgl.opengl.GL11.GL_BLEND;
import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_MODELVIEW;
import static org.lwjgl.opengl.GL11.GL_ONE_MINUS_SRC_ALPHA;
import static org.lwjgl.opengl.GL11.GL_PROJECTION;
import static org.lwjgl.opengl.GL11.GL_QUADS;
import static org.lwjgl.opengl.GL11.GL_SRC_ALPHA;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.glBegin;
import static org.lwjgl.opengl.GL11.glBindTexture;
import static org.lwjgl.opengl.GL11.glBlendFunc;
import static org.lwjgl.opengl.GL11.glClear;
import static org.lwjgl.opengl.GL11.glColor3f;
import static org.lwjgl.opengl.GL11.glColor4f;
import static org.lwjgl.opengl.GL11.glDisable;
import static org.lwjgl.opengl.GL11.glEnable;
import static org.lwjgl.opengl.GL11.glEnd;
import static org.lwjgl.opengl.GL11.glLoadIdentity;
import static org.lwjgl.opengl.GL11.glMatrixMode;
import static org.lwjgl.opengl.GL11.glOrtho;
import static org.lwjgl.opengl.GL11.glPopAttrib;
import static org.lwjgl.opengl.GL11.glPushAttrib;
import static org.lwjgl.opengl.GL11.glTexCoord2f;
import static org.lwjgl.opengl.GL11.glVertex2f;
import static org.lwjgl.opengl.GL11C.GL_TRIANGLES;
import static org.lwjgl.opengl.GL11.*;


import dev.lwjgl.TextureLoader;
import dev.lwjgl.ui.MessageSystem;
import dev.lwjgl.ui.Colors;
import dev.lwjgl.ui.components.*;

/**
 * Core game model that encapsulates all game logic and state.
 */
public class PlatformerModel {
    // World constants
    public static final float WORLD_WIDTH = 2400f;
    public static final float WORLD_HEIGHT = 600f;
    
    public static float getWorldWidth() {
        return WORLD_WIDTH;
    }

    // Game state
    public static long gameTime;
    public static long secondGameTime;
    public static boolean freezeTime = false;
    public static float dashLevel = 1.0f;
    public static boolean dashLevelWasBrought = false;
    public static int gameState;

    // Difficulty
    public enum Difficulty {
        EASY(10),
        NORMAL(5),
        HARD(1),
        IMPOSSIBLE(0.5f),
        NIGHTMARE(0.25f);

        public final float mobPerStar;
        Difficulty(float mobPerStar) {
            this.mobPerStar = mobPerStar;
        }
    }

    public static Difficulty difficulty = Difficulty.NORMAL;
    private int totalMobsA = 0;
    private int pastScore = 0;
    private double mobsThatIsLeftover = 0;
    private boolean addMobsA = false;
    private final boolean mobsEnabled = true;
    private final float mapCenterX = WORLD_WIDTH / 2.0f;
    private final float mapCenterY = WORLD_HEIGHT / 2.0f;
    private final float pctWidth = WORLD_WIDTH * 0.8f;
    private final float pctHeight = WORLD_HEIGHT * 0.8f;

    // Spawn areas
    private final float[] starRectSpawnArea = {mapCenterX, mapCenterY, pctWidth, pctHeight};
    private final float[] mobsRectsSpanArea = {mapCenterX, mapCenterY, pctWidth, pctHeight};

    // Game entities
    private Player player;
    private GameCamera camera;
    private List<Platform> platforms;
    private List<Star> stars;
    private List<MobA> mobs;
    public static List<UIPolygon> polygons = new ArrayList<>();



    // Systems
    private final MessageSystem messageSystem;
    private final ShopSystem shopSystem;

    // Game stats
    // Assume these are class variables:
    private double lifeWidth = 15;
    private double lifeHeight = 15;
    private double lifeSpacing = 25;
    private double lifeYOffset = 25;

    private static int currentLives = 5;  // starting lives
    private static int maxLives = 10;     // maximum lives
    private static List<UIRectangle> livesRectangles;

    public void initLivesUI() {
        livesRectangles = new ArrayList<>();

           for (int i = 0; i < maxLives; i++) {
            UIRectangle life = new UIRectangle(
                    10 + i * lifeSpacing,
                    screenHeight - lifeYOffset,
                    lifeWidth,
                    lifeHeight
            );
            
            // Hide lives beyond the starting amount
            if (i >= currentLives) {
                life.setShrinking(true);
            }

            livesRectangles.add(life);
        }
    }

    private int score = 0;

    // UI
    private int[] digitTextures = null;
    private final int screenWidth;
    private final int screenHeight;

    public PlatformerModel(int screenWidth, int screenHeight) {
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
        this.messageSystem = new MessageSystem(screenWidth, screenHeight);
        this.shopSystem = new ShopSystem();

        
        initialize();
    }

    public void showKeys(boolean isInitial) {
        KeyboardControlManager keyManager = KeyboardControlManager.getInstance();
        showMessage("Key Mapping:");
        showMessage("UP    = " + keyManager.getKeys().get(0));
        showMessage("LEFT  = " + keyManager.getKeys().get(1));
        showMessage("RIGHT = " + keyManager.getKeys().get(2));
        showMessage("DOWN  = " + keyManager.getKeys().get(3));
        showMessage("CHARATER SKILL = " + keyManager.getKeys().get(4));
        if (!isInitial) return;
        showMessage("Press Enter For Shop");
    }

    public void initialize() {
        gameTime = 0;
        secondGameTime = 0;
        freezeTime = false;
        dashLevel = 1.0f;
        dashLevelWasBrought = false;
        score = 0;
        pastScore = 0;
        totalMobsA = 0;
        mobsThatIsLeftover = 0;
        addMobsA = false;
        currentLives = 5;

        camera = new GameCamera(screenWidth, screenHeight, WORLD_WIDTH);
        player = new Player(100, 50);
        generatePlatforms();
        spawnStars(5);
        mobs = new ArrayList<>();

        KeyboardControlManager keyManager = KeyboardControlManager.getInstance();
        keyManager.resetKeys();
        if (difficulty == Difficulty.NIGHTMARE) keyManager.randomizeKeys();
        showKeys(true);
        initLivesUI();
    }

    public void update(float dt) {
        gameTime++;
        if(gameTime == Long.MAX_VALUE){
            gameTime = 0;
            secondGameTime++;
        }
        // Clear shop delay after first frame when shop is open
        if (shopSystem.isShopOpened() && shopSystem.isDelay()) {
            shopSystem.setDelay(false);
        }

        KeyboardControlManager keyManager = KeyboardControlManager.getInstance();
        if ((gameTime % 500) == 0 && keyManager.didRandomize()) showKeys(false);
        
        player.applyGravity(dt);
        player.update(dt, platforms);
        camera.follow(player);

        // Collect stars
        stars.removeIf(s -> {
            if (player.playerCollidesWithStar(s) || player.isStarInDashTriangle(s)) {
                score++;
                return true;
            }
            return false;
        });
        
        if (stars.isEmpty()) {
            spawnStars(5);
            if (difficulty == Difficulty.NIGHTMARE) KeyboardControlManager.getInstance().randomizeKeys();
        }

        // Spawn mobs
        if ((score - pastScore) >= difficulty.mobPerStar) {
            addMobsA = true;
        }

        if (addMobsA && mobsEnabled) {
            float growth = 1f + (score * 0.05f);
            float base = 1f / difficulty.mobPerStar;
            double mobCount = (score - pastScore) * (growth * base) + mobsThatIsLeftover;
            int wholenumbersMobs = (int) mobCount;
            mobsThatIsLeftover = mobCount - wholenumbersMobs;
            spawnMobsA(wholenumbersMobs);
            pastScore = score;
            addMobsA = false;
        }
        // Update mobs
        if (mobs != null) {
            for (MobA m : mobs) m.update(dt);
            mobs.removeIf(m -> {
                if (player.playerCollidesWithMob(m)) {
                    loseLife(1);
                    totalMobsA--;
                    return true;
                } else if (player.mobCollidesWithStab(m)) {
                    totalMobsA--;
                    return true;
                }
                return false;
            });
        }

        if (Player.showDashCooldownMessage) {
            showMessage("Dash is on cooldown! Wait for " + String.format("%.1f", player.getDashCooldown()) + " seconds.");
            Player.showDashCooldownMessage = false;
        }

        if (Player.showStabCooldownMessage) {
            showMessage("Stab is on cooldown! Wait for " + String.format("%.1f", player.stabCurrentCooldown) + " seconds.");
            Player.showStabCooldownMessage = false;
        }
        updatePolygons();
    }

    // Input handling is now done in PlatformerGameState

    // Input handling is now done in PlatformerGameState


    public void performGambling() {
        Random rnd = new Random();
        if (!shopSystem.isDealarPayedThisRound() && !Player.testing) {
            if (rnd.nextBoolean() && rnd.nextBoolean()) {
                shopSystem.setCanGamble(false);
                score += score;
                showMessage("You won!");
                showMessage("Your score has been doubled");
                showMessage("Your new score is " + score);
            } else {
                shopSystem.setCanGamble(false);
                score = 0;
                showMessage("You lost!");
                showMessage("The house always wins");
                showMessage("Better luck next time and also buy the game pass");
            }
        } else if (!Player.testing) {
            if (rnd.nextBoolean() && rnd.nextBoolean()) {
                shopSystem.setCanGamble(false);
                shopSystem.setDealarPayedThisRound(false);
                score = 0;
                showMessage("You lost!");
                showMessage("The house always wins");
                showMessage("Better luck next time and also buy the game pass");
            } else {
                shopSystem.setCanGamble(false);
                shopSystem.setDealarPayedThisRound(false);
                score += score;
                showMessage("You won!");
                showMessage("Your score has been doubled");
                showMessage("Your new score is " + score);
            }
        } else {
            score += score;
            showMessage("You won!");
            showMessage("Your score has been doubled");
            showMessage("Your new score is " + score);
        }
    }




    // Call when losing a life
    public static void loseLife(int heartsLoss) {
        while (heartsLoss > 0 && currentLives > 0 && !Player.isInvisibile && !freezeTime) {
            currentLives--;
            UIRectangle lost = livesRectangles.get(currentLives);
            lost.setShrinking(true); // triggers shrinking
            lost.setGlowing(false);  // stop any glow
            heartsLoss--;
            if (currentLives > 0){
                heartsLoss = 0;
            }
        }
    }


    public static void gainLife(int heartsGain) {
        while (heartsGain > 0 && currentLives < livesRectangles.size() && !freezeTime) {
            UIRectangle gained = livesRectangles.get(currentLives);
            gained.setShrinking(false);    // make it visible
            gained.setGlowing(true);       // glow the new life
            gained.setMaxGlowPulses(1);    // optional: single pulse pop
            currentLives++;
            heartsGain--;
            if (currentLives == maxLives){
                heartsGain = 0;
            }
        }
    }



    private void generatePlatforms() {
        platforms = new ArrayList<>();
        platforms.add(new Platform(0, 0, WORLD_WIDTH, 20));
        platforms.add(new Platform(400, 90, 200, 20));
        platforms.add(new Platform(800, 140, 150, 20));
        platforms.add(new Platform(1400, 290, 200, 20));
        platforms.add(new Platform(2000, 190, 200, 20));
    }

    private void spawnStars(int count) {
        stars = new ArrayList<>();
        Random rnd = new Random();
        for (int i = 0; i < count; i++) {
            float x = starRectSpawnArea[0] - starRectSpawnArea[2] / 2.0f + rnd.nextFloat() * starRectSpawnArea[2];
            float y = starRectSpawnArea[1] - starRectSpawnArea[3] / 2.0f + rnd.nextFloat() * starRectSpawnArea[3];
            stars.add(new Star(x, y));
        }
    }

    private void spawnMobsA(int count) {
        if (mobs == null) return;
        Random rnd = new Random();
        for (int i = 0; i < count; i++) {
            float x = mobsRectsSpanArea[0] - mobsRectsSpanArea[2] / 2.0f + rnd.nextFloat() * mobsRectsSpanArea[2];
            float y = mobsRectsSpanArea[1] - mobsRectsSpanArea[3] / 2.0f + rnd.nextFloat() * mobsRectsSpanArea[3];
            mobs.add(new MobA(x, y));
        }
        totalMobsA += count;
    }

    public void render() {
        glClear(GL_COLOR_BUFFER_BIT);
        camera.begin();

        for (Platform p : platforms) p.render();
        for (Star s : stars) s.render();
        player.render();
        if (mobs != null) {
            for (MobA m : mobs) m.render();
        }

        camera.end();
    }

    public void renderUI() {
        glMatrixMode(GL_PROJECTION);
        glLoadIdentity();
        glOrtho(0, screenWidth, 0, screenHeight, -1, 1);
        glMatrixMode(GL_MODELVIEW);
        glLoadIdentity();

        // Lives
        glColor3f(1, 0, 0);
        for (UIRectangle rect : livesRectangles) {
            rect.render();
        }

        // Render pray polygons
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        renderPolygons();
        glDisable(GL_BLEND);

        // Score
        if (digitTextures == null) {
            digitTextures = new int[10];
            for (int i = 0; i < 10; i++) {
                Path path = Paths.get("anderson", "number_" + i + ".png");
                digitTextures[i] = loadTexture(path);
            }
        }

        String scoreStr = Integer.toString(score);
        float sx = screenWidth / 2f - (scoreStr.length() * 20) / 2f;
        float sy = screenHeight - 30;
        float dx = 0;

        glColor3f(0.5f, 0.8f, 1f);
        glBegin(GL_QUADS);
        glVertex2f(sx - 10, sy - 5);
        glVertex2f(sx + scoreStr.length() * 20 + 10, sy - 5);
        glVertex2f(sx + scoreStr.length() * 20 + 10, sy + 25);
        glVertex2f(sx - 10, sy - 5);
        glEnd();

        glPushAttrib(GL_ALL_ATTRIB_BITS);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glEnable(GL_TEXTURE_2D);

        for (char c : scoreStr.toCharArray()) {
            int digit = c - '0';
            if (digit < 0 || digit > 9) continue;
            glBindTexture(GL_TEXTURE_2D, digitTextures[digit]);
            glColor4f(1, 1, 1, 1);
            glBegin(GL_QUADS);
            glTexCoord2f(0, 0);
            glVertex2f(sx + dx, sy);
            glTexCoord2f(1, 0);
            glVertex2f(sx + dx + 20, sy);
            glTexCoord2f(1, 1);
            glVertex2f(sx + dx + 20, sy + 20);
            glTexCoord2f(0, 1);
            glVertex2f(sx + dx, sy + 20);
            glEnd();
            dx += 20;
        }
        glBindTexture(GL_TEXTURE_2D, 0);
        glDisable(GL_TEXTURE_2D);
        glDisable(GL_BLEND);
        glPopAttrib();

        messageSystem.render(freezeTime);
    }

    private int loadTexture(Path resourcePath) {
        return TextureLoader.loadTexture(resourcePath, getClass());
    }

    /**
     * Updates pray polygons every frame to stay centered on the player.
     */
    private void updatePolygons() {
        // Create polygons once when they're needed
        if (Player.renderPray) {
            if (!polygons.stream().anyMatch(p -> "prayPolygon".equals(p.getName()))) {
                // Convert player world position to screen position
                float screenX = camera.worldToScreenX(player.x + 10);
                float screenY = camera.worldToScreenY(player.y + 10);

                // Create polygons centered on player
                addPolygon("prayPolygon", "null",5, screenX + 10, screenY + 10, 50, 0, true, true, 1);
                addPolygon("prayPolygon", "null",5, screenX + 10, screenY + 10, 50, 180, true, true, 1);

            } else {
                // Update existing polygon positions to follow player
                float screenX = camera.worldToScreenX(player.x + 10);
                float screenY = camera.worldToScreenY(player.y + 10);

                for (UIPolygon p : polygons) {
                    p.xCenter = screenX;
                    p.yCenter = screenY;
                }
            }
        } else {
            polygons.removeIf(p -> "prayPolygon".equals(p.getName()));
        }
        if (Player.windBurst) {
            if (!polygons.stream().anyMatch(p -> "WindBurst".equals(p.getName()))) {
                addPolygon("WindBurst", "yellow", 100, Player.x, Player.y, Player.windBurstSize, 0, false, true, 0.5f);
            } else {
                // Update existing polygon positions to follow player
                float screenX = camera.worldToScreenX(player.x + 10);
                float screenY = camera.worldToScreenY(player.y + 10);

                for (UIPolygon p : polygons) {
                    p.xCenter = screenX;
                    p.yCenter = screenY;
                }
            }
        } else {
            polygons.removeIf(p -> "WindBurst".equals(p.getName()));
        }
    }

    public static void addPolygon(
            String name,
            String colorName,
            int sides,
            float x,
            float y,
            float size,
            float rotation,
            boolean isRotating,
            boolean isGlowing,
            float alpha
    ) {
        // Convert color string to float[]
        float[] color = Colors.getColorByName(colorName);

        UIPolygon polygon;

        if (color == null) {
            // default color if null
            polygon = new UIPolygon(name, sides, x + 10, y + 10, size, rotation, alpha);
        } else {
            polygon = new UIPolygon(name, sides, x + 10, y + 10, size, rotation, color, alpha);
        }

        // Set effects
        if (isRotating) polygon.setRotating(true);
        if (isGlowing) polygon.setGlowing(true);

        // Add to list
        polygons.add(polygon);
    }

    private void renderPolygons() {
        for (UIPolygon p : polygons) {
            p.render();
        }
    }

    // Getters
    public Player getPlayer() { return player; }
    public GameCamera getCamera() { return camera; }
    public List<Platform> getPlatforms() { return platforms; }
    public List<Star> getStars() { return stars; }
    public List<MobA> getMobs() { return mobs; }
    public MessageSystem getMessageSystem() { return messageSystem; }
    public ShopSystem getShopSystem() { return shopSystem; }
    public int getScore() { return score; }
    public void setScore(int score) { this.score = score; }
    public void showMessage(String text) { messageSystem.showMessage(text); }
    public void cleanup() { messageSystem.cleanup(); }
}

