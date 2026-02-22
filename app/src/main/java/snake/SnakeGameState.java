package snake;

import dev.lwjgl.ui.components.*;
import dev.lwjgl.ui.components.controls.UIButton;
import dev.lwjgl.UIWindow;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;

/**
 * A single class that encapsulates all finite state machine logic
 * for the playing, paused, and game over phases.
 */
public class SnakeGameState {

    private enum Phase {PLAYING, PAUSED, GAME_OVER}

    private final UIWindow window;
    private final int gridW;
    private final int gridH;
    private final int cellSize;
    private final UIContainer rootView;
    private final double[] cursor = new double[2];
    private final double secPerUpdate = 1.0 / 8.0;

    private Phase phase = Phase.PLAYING;
    private SnakeModel snake;
    private UILabel scoreLabel;
    private double accumulator = 0.0;

    public SnakeGameState(UIWindow window) {
        this.window = window;
        this.gridW = window.getGridW();
        this.gridH = window.getGridH();
        this.cellSize = window.getCellSize();
        this.rootView = new UIContainer(0, 0, window.getWinW(), window.getWinH());
        transitionTo(Phase.PLAYING);
    }

    void update(double delta) {
        window.getCursorPos(cursor);
        rootView.update(cursor[0], cursor[1]);

        if (phase == Phase.PLAYING && snake != null) {
            accumulator += delta;
            while (accumulator >= secPerUpdate) {
                snake.update();
                if (snake.isGameOver()) {
                    transitionTo(Phase.GAME_OVER);
                    return;
                }
                if (scoreLabel != null) {
                    scoreLabel.setText("SCORE: " + snake.getScore());
                }
                accumulator -= secPerUpdate;
            }
        }
    }

    void render() {
        if (phase == Phase.PLAYING || phase == Phase.PAUSED) {
            glClearColor(0.06f, 0.06f, 0.06f, 1.0f);
        } else {
            glClearColor(0.08f, 0.08f, 0.08f, 1.0f);
        }
        glClear(GL_COLOR_BUFFER_BIT);

        if (phase == Phase.PLAYING || phase == Phase.PAUSED) {
            drawGridBackground();
            drawSnake();
            drawFood();
        }

        rootView.render();
    }

    void onKey(int key, int action) {
        if (key == GLFW_KEY_ESCAPE && action == GLFW_PRESS) {
            if (phase == Phase.PLAYING) {
                transitionTo(Phase.PAUSED);
            } else if (phase == Phase.PAUSED) {
                transitionTo(Phase.PLAYING);
            } else {
                window.requestClose();
            }
            return;
        }

        if (phase == Phase.PLAYING && snake != null &&
                (action == GLFW_PRESS || action == GLFW_REPEAT)) {
            switch (key) {
                case GLFW_KEY_UP:
                case GLFW_KEY_W:
                    snake.setDirection(SnakeModel.Direction.UP);
                    break;
                case GLFW_KEY_DOWN:
                case GLFW_KEY_S:
                    snake.setDirection(SnakeModel.Direction.DOWN);
                    break;
                case GLFW_KEY_LEFT:
                case GLFW_KEY_A:
                    snake.setDirection(SnakeModel.Direction.LEFT);
                    break;
                case GLFW_KEY_RIGHT:
                case GLFW_KEY_D:
                    snake.setDirection(SnakeModel.Direction.RIGHT);
                    break;
                default:
                    break;
            }
        }
    }

    void onMouse(int button, int action, double x, double y) {
        rootView.handleMouse(x, y, button, action);
    }

    private void transitionTo(Phase newPhase) {
        this.phase = newPhase;
        switch (newPhase) {
            case PLAYING -> buildPlayingUI();
            case PAUSED -> buildPauseUI();
            case GAME_OVER -> buildGameOverUI();
        }
    }

    private void buildPlayingUI() {
        window.setTitle("Snake Game - Use Arrow Keys!");
        rootView.clear();
        // Only reset game if coming from GAME_OVER, or if it's null (initial)
        if (snake == null || snake.isGameOver()) {
             this.snake = new SnakeModel(gridW, gridH);
             this.accumulator = 0.0;
        }

        this.scoreLabel = new UILabel("SCORE: " + (snake != null ? snake.getScore() : 0), 10, 10, 2);
        rootView.add(scoreLabel);
    }
    
    private void buildPauseUI() {
        window.setTitle("Snake Game - PAUSED");
        
        // Let's add the score label back
        if (snake != null) {
            this.scoreLabel = new UILabel("SCORE: " + snake.getScore(), 10, 10, 2);
            rootView.add(scoreLabel);
        }

        double winW = window.getWinW();
        double winH = window.getWinH();

        // Dim background
        UIRectangle dim = new UIRectangle(winW/2, winH/2, winW, winH);
        dim.setColor(0, 0, 0);
        dim.setAlpha(0.5f);
        rootView.add(dim);

        UILabel pauseLabel = new UILabel("PAUSED", 0, winH * 0.3, 4);
        pauseLabel.centerHorizontal(0, winW);
        rootView.add(pauseLabel);

        UIButton resumeButton = new UIButton(
                "RESUME",
                winW / 2.0 - 100,
                winH / 2.0,
                200,
                48,
                () -> transitionTo(Phase.PLAYING)
        );
        rootView.add(resumeButton);

        UIButton menuButton = new UIButton(
                "EXIT TO MENU",
                winW / 2.0 - 100,
                winH / 2.0 + 80,
                200,
                48,
                () -> window.requestClose()
        );
        rootView.add(menuButton);
    }

    private void buildGameOverUI() {
        int score = (snake != null) ? snake.getScore() : 0;
        window.setTitle("Game Over! Score: " + score);
        rootView.clear();

        double winW = window.getWinW();
        double winH = window.getWinH();

        UILabel gameOverLabel = new UILabel("GAME OVER", 0, winH * 0.25, 4);
        gameOverLabel.centerHorizontal(0, winW);
        rootView.add(gameOverLabel);

        UILabel scoreLabel = new UILabel("FINAL SCORE: " + score, 0, winH * 0.25 + 50, 3);
        scoreLabel.centerHorizontal(0, winW);
        rootView.add(scoreLabel);

        UILabel restartLabel = new UILabel("Click button to play again", 0, winH * 0.25 + 100, 2);
        restartLabel.centerHorizontal(0, winW);
        rootView.add(restartLabel);

        UIButton restartButton = new UIButton(
                "RESTART",
                winW / 2.0 - 100,
                winH / 2.0 + 48,
                200,
                48,
                () -> transitionTo(Phase.PLAYING)
        );
        rootView.add(restartButton);
        
        UIButton exitButton = new UIButton(
                "EXIT TO MENU",
                winW / 2.0 - 100,
                winH / 2.0 + 110,
                200,
                48,
                () -> window.requestClose()
        );
        rootView.add(exitButton);
    }

    private void drawGridBackground() {
        int winW = window.getWinW();
        int winH = window.getWinH();

        glColor3f(0.08f, 0.08f, 0.08f);
        glBegin(GL_LINES);
        for (int i = 0; i <= gridW; i++) {
            int x = i * cellSize;
            glVertex2i(x, 0);
            glVertex2i(x, winH);
        }
        for (int j = 0; j <= gridH; j++) {
            int y = j * cellSize;
            glVertex2i(0, y);
            glVertex2i(winW, y);
        }
        glEnd();
    }

    private void drawSnake() {
        if (snake == null) return;

        float r = 0.2f;
        float g = 0.6f;
        float b = 0.2f;
        float gIncrement = (snake.getSnakeSize() > 0) ? (0.3f / snake.getSnakeSize()) : 0;

        for (int[] segment : snake.getSnake()) {
            drawCell(segment[0], segment[1], r, g, b);
            g += gIncrement;
        }
    }

    private void drawFood() {
        if (snake == null) return;
        int[] food = snake.getFood();
        if (food != null) {
            drawCell(food[0], food[1], 1.0f, 0.35f, 0.35f);
        }
    }

    private void drawCell(int gx, int gy, float r, float g, float b) {
        int x = gx * cellSize;
        int y = gy * cellSize;
        glColor3f(r, g, b);
        glBegin(GL_QUADS);
        glVertex2i(x + 1, y + 1);
        glVertex2i(x + cellSize - 1, y + 1);
        glVertex2i(x + cellSize - 1, y + cellSize - 1);
        glVertex2i(x + 1, y + cellSize - 1);
        glEnd();
    }
}
