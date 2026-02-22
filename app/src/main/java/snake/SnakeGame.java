package snake;


import dev.lwjgl.UIWindow;

/**
 * Entry point for the refactored LWJGL Snake game.
 * The Game class (SnakeGame) owns the lifecycle and delegates
 * logic to a single GameState instance while UIWindow encapsulates
 * all LWJGL window interactions.
 */
public class SnakeGame {

    private UIWindow window;
    private SnakeGameState state;

    public static void main(String[] args) {
        new SnakeGame().run();
    }

    public void run() {
        try {
            init();
            loop();
        } finally {
            if (window != null) {
                window.destroy();
            }
        }
    }

    private void init() {
        this.window = new UIWindow("Snake Game", 640, 480, 20);
        this.state = new SnakeGameState(window);

        window.setKeyCallback((win, key, scancode, action, mods) -> state.onKey(key, action));
        window.setMouseButtonCallback((win, button, action, mods) -> {
            double[] cursor = new double[2];
            window.getCursorPos(cursor);
            state.onMouse(button, action, cursor[0], cursor[1]);
        });
    }

    private void loop() {
        double lastTime = window.getTime();

        while (!window.shouldClose()) {
            double now = window.getTime();
            double delta = now - lastTime;
            lastTime = now;

            state.update(delta);
            state.render();

            window.swapBuffers();
            window.pollEvents();
        }
    }
}
