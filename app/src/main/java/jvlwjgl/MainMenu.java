package jvlwjgl;

import dev.lwjgl.UIWindow;
import dev.lwjgl.ui.components.UIContainer;
import dev.lwjgl.ui.components.UILabel;
import dev.lwjgl.ui.components.UIPolygon;
import dev.lwjgl.ui.components.UIRectangle;
import dev.lwjgl.ui.components.UIStar;
import dev.lwjgl.ui.components.controls.UIButton;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;

public class MainMenu {

    public enum Selection {
        EXIT,
        PLATFORMER,
        SNAKE
    }

    private Selection selection = Selection.EXIT;
    private UIWindow window;
    private UIContainer uiRoot;

    public Selection run() {
        // Reset selection in case of reuse
        selection = Selection.EXIT;
        
        window = new UIWindow("Anderson Arcade - Main Menu", 800, 600, 20);
        uiRoot = new UIContainer(0, 0, window.getWinW(), window.getWinH());
        setupUI();

        while (!window.shouldClose() && selection == Selection.EXIT) {
            window.pollEvents();
            
            double[] cursor = new double[2];
            window.getCursorPos(cursor);
            uiRoot.update(cursor[0], cursor[1]);

            // Dark grey background to match Snake style
            glClearColor(0.08f, 0.08f, 0.08f, 1.0f);
            glClear(GL_COLOR_BUFFER_BIT);
            
            uiRoot.render();
            
            window.swapBuffers();
            
            try { Thread.sleep(16); } catch (Exception e) {}
        }

        window.destroy();
        return selection;
    }

    private void setupUI() {
        double w = window.getWinW();
        double h = window.getWinH();

        // --- Decorative Elements (from Snake Menu) ---
        int n = 5;
        
        // Glowing Rectangle
        UIRectangle rectangle = new UIRectangle(w / 2, h * 0.25, 200, 100); // Near top-ish
        rectangle.setGlowing(true);
        uiRoot.add(rectangle);

        // Rotating Polygon
        UIPolygon polygon = new UIPolygon("poly", n, w / 2 - 100 , h * 0.2, 100, 0, 1f);
        polygon.setRotating(true);
        polygon.setGlowing(true);
        uiRoot.add(polygon);

        // Glowing Star
        UIStar star = new UIStar(n, w / 2 + 100, h * 0.8, 100, 0); // Near bottom
        star.setGlowing(true);
        uiRoot.add(star);

        // Motif Text
        UILabel motif = new UILabel("abcdefghijklmnopqrstuvwxyz", 0, h * 0.75, 4);
        motif.centerHorizontal(0, w);
        uiRoot.add(motif);

        // --- Main Title & Buttons ---
        
        UILabel title = new UILabel("ANDERSON ARCADE", 0, h * 0.15, 3);
        title.centerHorizontal(0, w);
        uiRoot.add(title);

        UIButton platformerBtn = new UIButton("PLATFORMER GAME", w/2 - 150, h * 0.4, 300, 50, () -> {
            selection = Selection.PLATFORMER;
            glfwSetWindowShouldClose(window.getHandle(), true);
        });
        uiRoot.add(platformerBtn);

        UIButton snakeBtn = new UIButton("SNAKE GAME", w/2 - 150, h * 0.5, 300, 50, () -> {
            selection = Selection.SNAKE;
            glfwSetWindowShouldClose(window.getHandle(), true);
        });
        uiRoot.add(snakeBtn);

        UIButton exitBtn = new UIButton("EXIT", w/2 - 150, h * 0.6, 300, 50, () -> {
            selection = Selection.EXIT;
            glfwSetWindowShouldClose(window.getHandle(), true);
        });
        uiRoot.add(exitBtn);

        // Input handling
        window.setMouseButtonCallback((win, button, action, mods) -> {
            double[] cursor = new double[2];
            window.getCursorPos(cursor);
            uiRoot.handleMouse(cursor[0], cursor[1], button, action);
        });
        
        window.setKeyCallback((win, key, scancode, action, mods) -> {
             if (key == GLFW_KEY_ESCAPE && action == GLFW_PRESS) {
                 selection = Selection.EXIT;
                 glfwSetWindowShouldClose(window.getHandle(), true);
             }
        });
    }
}
