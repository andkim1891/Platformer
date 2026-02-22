package platformer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.lwjgl.glfw.GLFW;

/**
 * Manages key mapping and control randomization.
 */
public class KeyboardControlManager {
    // Singleton
    private static volatile KeyboardControlManager instance;
    public static KeyboardControlManager getInstance() {
        // Public accessor with double-checked locking
        if (instance == null) {                                                 // First check (no locking)
            synchronized (KeyboardControlManager.class) {
                if (instance == null) instance = new KeyboardControlManager();  // Second check (with lock)
            }
        }
        return instance;
    }







    private static List<Character> keys = new ArrayList<>(Arrays.asList('W', 'A', 'D', 'S', 'Q', 'E'));
    public static char getkey(int whereInList){
        return keys.get(whereInList);
    }
    private Map<String, Integer> keyMap;
    private KeyboardControlManager() {
        // Private constructor prevents external instantiation
        this.keyMap = new HashMap<>();
        keyMap.put("UP", getKeyCode(keys.get(0)));
        keyMap.put("LEFT", getKeyCode(keys.get(1)));
        keyMap.put("RIGHT", getKeyCode(keys.get(2)));
        keyMap.put("DOWN", getKeyCode(keys.get(3)));
        keyMap.put("CHARATER SKILL", getKeyCode(keys.get(4)));
        keyMap.put("CHANGE CHARATER SKILL", getKeyCode(keys.get(5)));
        keyMap.put("SUBMIT", GLFW.GLFW_KEY_ENTER);
    }
    public void randomizeKeys() {
        Collections.shuffle(keys);
        keyMap = new HashMap<>();
        keyMap.put("UP", getKeyCode(keys.get(0)));
        keyMap.put("LEFT", getKeyCode(keys.get(1)));
        keyMap.put("RIGHT", getKeyCode(keys.get(2)));
        keyMap.put("DOWN", getKeyCode(keys.get(3)));
        keyMap.put("CHARATER SKILL", getKeyCode(keys.get(4)));
        keyMap.put("CHANGE CHARATER SKILL", getKeyCode(keys.get(5)));
        keyMap.put("SUBMIT", GLFW.GLFW_KEY_ENTER);
        didRandomize = true;
    }
    private boolean didRandomize;
    public boolean didRandomize() { return didRandomize; }

    private int getKeyCode(char c) {
        return switch (c) {
            case 'Q' -> GLFW.GLFW_KEY_Q;
            case 'A' -> GLFW.GLFW_KEY_A;
            case 'W' -> GLFW.GLFW_KEY_W;
            case 'S' -> GLFW.GLFW_KEY_S;
            case 'E' -> GLFW.GLFW_KEY_E;
            case 'D' -> GLFW.GLFW_KEY_D;
            default -> GLFW.GLFW_KEY_SPACE;
        };
    }



    public void resetKeys() {
        keys = new ArrayList<>(Arrays.asList('W', 'A', 'D', 'S', 'Q', 'E'));
        keyMap = new HashMap<>();
        keyMap.put("UP", getKeyCode(keys.get(0)));
        keyMap.put("LEFT", getKeyCode(keys.get(1)));
        keyMap.put("RIGHT", getKeyCode(keys.get(2)));
        keyMap.put("DOWN", getKeyCode(keys.get(3)));
        keyMap.put("CHARATER SKILL", getKeyCode(keys.get(4)));
        keyMap.put("CHANGE CHARATER SKILL", getKeyCode(keys.get(5)));
        keyMap.put("SUBMIT", GLFW.GLFW_KEY_ENTER);
        didRandomize = false;
    }

    public Map<String, Integer> getKeyMap() {
        return keyMap;
    }

    public static List<Character> getKeys() {
        return keys;
    }

}

