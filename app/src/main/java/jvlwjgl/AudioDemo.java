package jvlwjgl;

import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.*;
import org.lwjgl.openal.*;
import org.lwjgl.system.MemoryUtil;

import javax.sound.sampled.*;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.openal.AL10.*;
import static org.lwjgl.openal.ALC10.*;
import static org.lwjgl.system.MemoryUtil.NULL;

/**
 * AudioDemo with clearer design:
 *  - AudioManager (Singleton) manages OpenAL lifecycle, loading, playing, volume, cleanup.
 *  - InputHandler maps keys to Runnable commands (easy to extend).
 *
 * Resource WAVs are expected under: src/main/resources/anderson/sfx/
 * Example names used below: sfx_a.wav, sfx_s.wav, sfx_d.wav, sfx_f.wav
 *
 * NOTE: Keep the LWJGL/OpenAL/GLFW dependencies in your Gradle setup (same as previous snippet).
 */
public class AudioDemo {

    // ---------- Main / lifecycle ----------
    public static void main(String[] args) {
        AudioDemo demo = new AudioDemo();
        try {
            demo.run();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private long window;

    public void run() throws Exception {
        initGLFWWindow();
        AudioManager.INSTANCE.init();               // Singleton init
        AudioManager.INSTANCE.createSourcePool(16);  // pool for overlapping sounds

        // Load sounds from resources/anderson/sfx/
        AudioManager.INSTANCE.load("A", "anderson/sfx/sfx_a.wav");
        AudioManager.INSTANCE.load("S", "anderson/sfx/sfx_s.wav");
        AudioManager.INSTANCE.load("D", "anderson/sfx/sfx_d.wav");
        AudioManager.INSTANCE.load("F", "anderson/sfx/sfx_f.wav");

        // Input handler binds keys to actions (command pattern-ish)
        InputHandler input = new InputHandler(window);
        input.bindKey(GLFW_KEY_A, () -> AudioManager.INSTANCE.play("A"));
        input.bindKey(GLFW_KEY_S, () -> AudioManager.INSTANCE.play("S"));
        input.bindKey(GLFW_KEY_D, () -> AudioManager.INSTANCE.play("D"));
        input.bindKey(GLFW_KEY_F, () -> AudioManager.INSTANCE.play("F"));

        System.out.println("Press A S D F to play sounds. Close the window to exit.");

        // Main loop
        while (!glfwWindowShouldClose(window)) {
            glfwPollEvents();
            input.pollAndRun();
            // tiny sleep to avoid a busy-loop
            try { Thread.sleep(8); } catch (InterruptedException ignored) {}
        }

        // cleanup
        AudioManager.INSTANCE.cleanup();
        glfwDestroyWindow(window);
        glfwTerminate();
        GLFWErrorCallback cb = glfwSetErrorCallback(null);
        if (cb != null) cb.free();
    }

    // ---------- GLFW helper ----------
    private void initGLFWWindow() {
        GLFWErrorCallback.createPrint(System.err).set();
        if (!glfwInit()) throw new IllegalStateException("Unable to initialize GLFW");

        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_FALSE);

        window = glfwCreateWindow(360, 120, "AudioDemo (A S D F)", NULL, NULL);
        if (window == NULL) throw new RuntimeException("Failed to create GLFW window");

        // center and show
        GLFWVidMode vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());
        if (vidmode != null) {
            glfwSetWindowPos(window,
                    (vidmode.width() - 360) / 2,
                    (vidmode.height() - 120) / 2);
        }
        glfwShowWindow(window);
        // an OpenGL context helps ensure keyboard input works cross-platform
        glfwMakeContextCurrent(window);
    }

    // ---------- AudioManager: Singleton, Facade for audio operations ----------
    public enum AudioManager {
        INSTANCE;

        private long device = MemoryUtil.NULL;
        private long context = MemoryUtil.NULL;

        // name -> OpenAL buffer id
        private final Map<String, Integer> buffers = new HashMap<>();

        // source pool for overlapping playback
        private int[] sources = new int[8];
        private int nextSourceIdx = 0;

        private float masterGain = 1.0f; // simple volume control (0..1)

        // initialize OpenAL device/context
        public void init() {
            device = alcOpenDevice((ByteBuffer) null);
            if (device == MemoryUtil.NULL) throw new IllegalStateException("Failed to open default OpenAL device");

            context = alcCreateContext(device, (IntBuffer) null);
            if (context == MemoryUtil.NULL) throw new IllegalStateException("Failed to create OpenAL context");

            alcMakeContextCurrent(context);
            AL.createCapabilities(ALC.createCapabilities(device));
            System.out.println("AudioManager: OpenAL initialized.");
        }

        // create a pool of sources (call after init)
        public void createSourcePool(int size) {
            if (size <= 0) size = 8;
            sources = new int[size];
            for (int i = 0; i < size; i++) {
                sources[i] = alGenSources();
                alSourcef(sources[i], AL_GAIN, masterGain);
                alSourcef(sources[i], AL_PITCH, 1.0f);
                alSourcei(sources[i], AL_LOOPING, AL_FALSE);
            }
            nextSourceIdx = 0;
            System.out.println("AudioManager: source pool created (" + size + ")");
        }

        // load a WAV from classpath (robust converter using Java Sound)
        public void load(String name, String classpathResource) {
            if (buffers.containsKey(name)) {
                System.out.println("AudioManager: sound '" + name + "' already loaded.");
                return;
            }
            int bufferId = loadWavToALBuffer(classpathResource);
            if (bufferId != 0) {
                buffers.put(name, bufferId);
                System.out.println("AudioManager: loaded '" + name + "' -> buffer " + bufferId);
            } else {
                System.err.println("AudioManager: failed to load '" + name + "' from " + classpathResource);
            }
        }

        // play by name (no-op if not loaded)
        public void play(String name) {
            Integer buf = buffers.get(name);
            if (buf == null) {
                System.err.println("AudioManager: sound not found: " + name);
                return;
            }
            if (sources.length == 0) {
                System.err.println("AudioManager: no sources available");
                return;
            }
            int src = sources[nextSourceIdx];
            // stop & detach previous buffer before reusing source
            alSourceStop(src);
            alSourcei(src, AL_BUFFER, buf);
            alSourcef(src, AL_GAIN, masterGain);
            alSourcePlay(src);
            nextSourceIdx = (nextSourceIdx + 1) % sources.length;
        }

        public void setMasterGain(float gain) {
            masterGain = Math.max(0f, Math.min(1f, gain));
            for (int s : sources) {
                alSourcef(s, AL_GAIN, masterGain);
            }
        }

        // cleanup AL buffers & sources & context & device
        public void cleanup() {
            // delete sources
            for (int s : sources) {
                if (s != 0) alDeleteSources(s);
            }
            // delete buffers
            for (Integer b : buffers.values()) {
                if (b != null && b != 0) alDeleteBuffers(b);
            }
            buffers.clear();

            if (context != MemoryUtil.NULL) {
                alcDestroyContext(context);
                context = MemoryUtil.NULL;
            }
            if (device != MemoryUtil.NULL) {
                alcCloseDevice(device);
                device = MemoryUtil.NULL;
            }
            System.out.println("AudioManager: cleaned up.");
        }

        // ---------- internal WAV loader ----------
        private int loadWavToALBuffer(String resourcePath) {
            try (InputStream raw = openResource(resourcePath)) {
                if (raw == null) {
                    System.err.println("AudioManager: resource not found: " + resourcePath);
                    return 0;
                }
                try (AudioInputStream ais = AudioSystem.getAudioInputStream(new BufferedInputStream(raw))) {
                    AudioFormat baseFormat = ais.getFormat();
                    AudioFormat pcmFormat = new AudioFormat(
                            AudioFormat.Encoding.PCM_SIGNED,
                            baseFormat.getSampleRate(),
                            16,
                            baseFormat.getChannels(),
                            baseFormat.getChannels() * 2,
                            baseFormat.getSampleRate(),
                            false // little-endian
                    );
                    try (AudioInputStream din = AudioSystem.getAudioInputStream(pcmFormat, ais)) {
                        // read all bytes
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        byte[] buf = new byte[4096];
                        int read;
                        while ((read = din.read(buf, 0, buf.length)) != -1) {
                            baos.write(buf, 0, read);
                        }
                        byte[] audioBytes = baos.toByteArray();

                        ByteBuffer data = BufferUtils.createByteBuffer(audioBytes.length);
                        data.put(audioBytes);
                        data.flip();

                        int alBuffer = alGenBuffers();
                        int channels = pcmFormat.getChannels();
                        int format = (channels == 1) ? AL_FORMAT_MONO16 : AL_FORMAT_STEREO16;
                        int sampleRate = (int) pcmFormat.getSampleRate();

                        alBufferData(alBuffer, format, data, sampleRate);
                        return alBuffer;
                    }
                }
            } catch (UnsupportedAudioFileException e) {
                System.err.println("AudioManager: Unsupported audio file: " + resourcePath + " -> " + e.getMessage());
            } catch (IOException e) {
                System.err.println("AudioManager: I/O error loading: " + resourcePath + " -> " + e.getMessage());
            } catch (Exception e) {
                System.err.println("AudioManager: Unexpected error: " + e.getMessage());
                e.printStackTrace();
            }
            return 0;
        }

        // helper to open classpath resource (leading slash optional)
        private InputStream openResource(String resourcePath) {
            String p = resourcePath.startsWith("/") ? resourcePath : "/" + resourcePath;
            InputStream s = AudioDemo.class.getResourceAsStream(p);
            if (s == null) s = AudioDemo.class.getClassLoader().getResourceAsStream(resourcePath.startsWith("/") ? resourcePath.substring(1) : resourcePath);
            return s;
        }
    }

    // ---------- InputHandler: binds GLFW keys to Runnable actions ----------
    public static class InputHandler {
        private final Map<Integer, Runnable> bindings = new HashMap<>();
        private final long glfwWindow;
        // small debounce to reduce retriggers while key is held
        private final Map<Integer, Long> lastTriggered = new HashMap<>();
        private final long debounceMs = 120;

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
                }
            }
        }
    }
}
