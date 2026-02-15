package jvlwjgl;

import dev.lwjgl.UIWindow;
import dev.lwjgl.audio.AudioManager;
import dev.lwjgl.ui.Colors;
import dev.lwjgl.ui.components.UILabel;

import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;

/**
 * AudioDemo with UI and advanced Audio control.
 * Features:
 *  - Singleton AudioManager (SFX + BGM)
 *  - UIWindow with Labels (Last Key, Volume)
 *  - Volume Control (Arrows)
 *  - Continuous playback with gap (Debounce)
 */
public class AudioDemo {

    public static void main(String[] args) {
        AudioDemo demo = new AudioDemo();
        try {
            demo.run();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private UIWindow window;
    private UILabel statusLabel;
    private UILabel volumeLabel;
    private UILabel helpLabel;
    
    private String lastKeyName = "None";

    public void run() throws Exception {
        // 1. Init UI Window (GLFW + OpenGL)
        window = new UIWindow("AudioDemo - GUI & Advanced Audio", 600, 400, 16);
        
        // 2. Init Audio (OpenAL)
        AudioManager.getInstance().init();
        AudioManager.getInstance().setupSFX("anderson/sfx");
        AudioManager.getInstance().setupBGM("anderson/bgm");

        // 3. Setup GUI Components
        statusLabel = new UILabel("Last Key: None", 20, 300, 3.0);
        statusLabel.setColor(Colors.YELLOW);
        
        volumeLabel = new UILabel("SFX: 100% | BGM: 100%", 20, 250, 2.0);
        volumeLabel.setColor(Colors.CYAN);
        
        helpLabel = new UILabel("A/S/D/F: SFX | Arrows: Vol | M: BGM | N: Stop | R: Reset", 20, 50, 1.5);
        helpLabel.setColor(Colors.WHITE);

        // 4. Input Handler
        InputHandler input = new InputHandler(window.getHandle());
        
        // --- SFX Bindings ---
        input.bindKey(GLFW_KEY_A, () -> {
            AudioManager.getInstance().playSFX("sfx01_pickup.wav");
            updateStatus("A (Pickup)");
        });
        input.bindKey(GLFW_KEY_S, () -> {
            AudioManager.getInstance().playSFX("sfx02_explosion.wav");
            updateStatus("S (Explosion)");
        });
        input.bindKey(GLFW_KEY_D, () -> {
            AudioManager.getInstance().playSFX("sfx03_laser_shot.wav");
            updateStatus("D (Laser)");
        });
        input.bindKey(GLFW_KEY_F, () -> {
            AudioManager.getInstance().playSFX("sfx04_magic_spell.wav");
            updateStatus("F (Magic)");
        });

        // --- BGM Controls ---
        input.bindKey(GLFW_KEY_M, () -> {
            // Note: Using WAV for guaranteed playback. MP3 is supported via added SPI.
            // M4A support requires additional native libraries not present here.
            AudioManager.getInstance().playBGM("backgroundmusic1.mp3");
            updateStatus("M (Play BGM)");
        });
        input.bindKey(GLFW_KEY_N, () -> { //djartmusic-return-to-the-8-bit-past = 1 //niknet_art-retro-8bit-happy-videogame-music-246631 = 2 // cridit NiKneT_Art && DJARTMUSIC from pixabaqy
            AudioManager.getInstance().stopBGM();
            updateStatus("N (Stop BGM)");
        });

        // --- Volume Controls (Arrows) ---
        input.bindKey(GLFW_KEY_LEFT, () -> {
            float v = AudioManager.getInstance().getSFXVolume() - 0.05f;
            AudioManager.getInstance().setSFXVolume(v);
            updateStatus("Left (SFX -)");
        });
        input.bindKey(GLFW_KEY_RIGHT, () -> {
            float v = AudioManager.getInstance().getSFXVolume() + 0.05f;
            AudioManager.getInstance().setSFXVolume(v);
            updateStatus("Right (SFX +)");
        });
        input.bindKey(GLFW_KEY_UP, () -> {
            float v = AudioManager.getInstance().getBGMVolume() + 0.05f;
            AudioManager.getInstance().setBGMVolume(v);
            updateStatus("Up (BGM +)");
        });
        input.bindKey(GLFW_KEY_DOWN, () -> {
            float v = AudioManager.getInstance().getBGMVolume() - 0.05f;
            AudioManager.getInstance().setBGMVolume(v);
            updateStatus("Down (BGM -)");
        });
        
        // --- Reset ---
        input.bindKey(GLFW_KEY_R, () -> {
            AudioManager.getInstance().resetSFX("anderson/sfx");
            AudioManager.getInstance().resetBGM("anderson/bgm");
            updateStatus("R (Reset)");
        });

        System.out.println("AudioDemo running...");

        // 5. Main Loop
        while (!window.shouldClose()) {
            window.pollEvents();
            input.pollAndRun(); // Continuous polling for "keep pressing" feel

            // Update Labels
            statusLabel.setText("Last Key: " + lastKeyName);
            int sfxPct = (int)(AudioManager.getInstance().getSFXVolume() * 100);
            int bgmPct = (int)(AudioManager.getInstance().getBGMVolume() * 100);
            volumeLabel.setText(String.format("SFX: %d%% | BGM: %d%% [Loop]", sfxPct, bgmPct));

            // Render
            glClearColor(0.1f, 0.1f, 0.2f, 1.0f);
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

            statusLabel.render();
            volumeLabel.render();
            helpLabel.render();

            window.swapBuffers();
            
            // tiny sleep to yield CPU
            try { Thread.sleep(8); } catch (InterruptedException ignored) {}
        }

        // Cleanup
        // Explicit cleanup is required to prevent native crashes (0xC0000409) on Windows/LWJGL
        AudioManager.getInstance().cleanup();
        window.destroy();
    }
    
    private void updateStatus(String msg) {
        this.lastKeyName = msg;
    }

    // ---------- InputHandler ----------
    public static class InputHandler {
        private final Map<Integer, Runnable> bindings = new HashMap<>();
        private final long glfwWindow;
        private final Map<Integer, Long> lastTriggered = new HashMap<>();
        // Lower debounce for volume/SFX to feel responsive but not crazy
        private final long debounceMs = 80; 

        public InputHandler(long window) {
            this.glfwWindow = window;
        }

        public void bindKey(int glfwKeyCode, Runnable action) {
            bindings.put(glfwKeyCode, action);
        }

        public void pollAndRun() {
            long now = System.currentTimeMillis();
            for (Map.Entry<Integer, Runnable> e : bindings.entrySet()) {
                int key = e.getKey();
                if (glfwGetKey(glfwWindow, key) == GLFW_PRESS) {
                    long last = lastTriggered.getOrDefault(key, 0L);
                    if (now - last >= debounceMs) {
                        try {
                            e.getValue().run();
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                        lastTriggered.put(key, now);
                    }
                } else {
                    // Reset trigger if key released? 
                    // No, for auto-repeat we just rely on time.
                    // If we wanted "press once", we'd track release.
                    // This "continuous" style fits the request.
                }
            }
        }
    }
}
