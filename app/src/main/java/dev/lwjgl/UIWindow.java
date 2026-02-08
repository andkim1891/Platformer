package dev.lwjgl;

import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWKeyCallbackI;
import org.lwjgl.glfw.GLFWMouseButtonCallbackI;
import org.lwjgl.opengl.GL;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryUtil.NULL;

/**
 * Wrapper around GLFW window management so core game logic is
 * insulated from direct LWJGL calls.
 */
public class UIWindow {

    private final long windowHandle;
    private final GLFWErrorCallback errorCallback;
    private int winW;
    private int winH;
    private final int cellSize;
    private String title;
    private final double[] cursorX = new double[1];
    private final double[] cursorY = new double[1];

    public UIWindow(String title, int winW, int winH, int cellSize) {
        this.title = title;
        this.winW = winW;
        this.winH = winH;
        this.cellSize = cellSize;

        errorCallback = GLFWErrorCallback.createPrint(System.err);
        errorCallback.set();
        if (!glfwInit()) {
            throw new IllegalStateException("Unable to initialize GLFW");
        }

        glfwDefaultWindowHints();
        long handle = glfwCreateWindow(winW, winH, title, NULL, NULL);
        if (handle == NULL) {
            glfwTerminate();
            throw new IllegalStateException("Failed to create GLFW window");
        }
        this.windowHandle = handle;

        glfwMakeContextCurrent(windowHandle);
        glfwSwapInterval(1);
        GL.createCapabilities();

        setOrthoProjection();
    }

    private void setOrthoProjection() {
        glMatrixMode(GL_PROJECTION);
        glLoadIdentity();
        glOrtho(0, winW, winH, 0, -1, 1);
        glMatrixMode(GL_MODELVIEW);
        glLoadIdentity();
    }

    public long getHandle() {
        return windowHandle;
    }

    public void setTitle(String title) {
        this.title = title;
        glfwSetWindowTitle(windowHandle, title);
    }

    void resize(int newW, int newH) {
        this.winW = newW;
        this.winH = newH;
        glfwSetWindowSize(windowHandle, winW, winH);
        glViewport(0, 0, winW, winH);
        setOrthoProjection();
    }

    public boolean shouldClose() {
        return glfwWindowShouldClose(windowHandle);
    }

    public void requestClose() {
        glfwSetWindowShouldClose(windowHandle, true);
    }

    public void swapBuffers() {
        glfwSwapBuffers(windowHandle);
    }

    public void pollEvents() {
        glfwPollEvents();
    }

    public double getTime() {
        return glfwGetTime();
    }

    public void setKeyCallback(GLFWKeyCallbackI callback) {
        glfwSetKeyCallback(windowHandle, callback);
    }

    public void setMouseButtonCallback(GLFWMouseButtonCallbackI callback) {
        glfwSetMouseButtonCallback(windowHandle, callback);
    }

    public void getCursorPos(double[] target) {
        if (target == null || target.length < 2) {
            throw new IllegalArgumentException("Target array must have length >= 2");
        }
        glfwGetCursorPos(windowHandle, cursorX, cursorY);
        target[0] = cursorX[0];
        target[1] = cursorY[0];
    }

    public int getWinW() {
        return winW;
    }

    public int getWinH() {
        return winH;
    }

    public int getCellSize() {
        return cellSize;
    }

    public int getGridW() {
        return winW / cellSize;
    }

    public int getGridH() {
        return winH / cellSize;
    }

    public void destroy() {
        glfwDestroyWindow(windowHandle);
        glfwTerminate();
        if (errorCallback != null) {
            errorCallback.free();
        }
    }
}
