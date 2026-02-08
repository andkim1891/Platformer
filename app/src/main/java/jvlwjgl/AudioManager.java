package jvlwjgl;

import com.google.common.reflect.ClassPath;
import org.lwjgl.BufferUtils;
import org.lwjgl.openal.AL;
import org.lwjgl.openal.ALC;
import org.lwjgl.system.MemoryUtil;

import javax.sound.sampled.*;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static org.lwjgl.openal.AL10.*;
import static org.lwjgl.openal.ALC10.*;

/**
 * Singleton AudioManager for handling OpenAL audio playback.
 * <p>
 * strictly separates SFX and BGM:
 * - SFX: Pre-loaded into memory (good for low latency, frequent use).
 * - BGM: Lazy-loaded on demand (good for memory efficiency).
 * </p>
 */
public class AudioManager {

    private long device = MemoryUtil.NULL;
    private long context = MemoryUtil.NULL;
    private boolean initialized = false;

    // --- SFX Data ---
    private final Map<String, Integer> sfxBuffers = new ConcurrentHashMap<>();
    private int[] sfxSources;
    private int nextSfxIdx = 0;
    private boolean sfxDirSet = false;
    private float sfxVolume = 1.0f;
    // Playback rate limiting (gap) per sound
    private final Map<String, Long> lastPlayedTime = new ConcurrentHashMap<>();
    private static final long MIN_PLAY_GAP_MS = 50; 

    // --- BGM Data ---
    // We map filename -> resource path for lazy loading
    private final Map<String, String> bgmPaths = new ConcurrentHashMap<>();
    // We cache loaded BGM buffers: filename -> bufferId
    private final Map<String, Integer> bgmBuffers = new ConcurrentHashMap<>();
    private int bgmSource = 0;
    private String currentBgmName = null;
    private boolean bgmDirSet = false;
    private float bgmVolume = 1.0f;

    private AudioManager() {
        // Private constructor
    }

    public static AudioManager getInstance() {
        return Holder.INSTANCE;
    }

    private static class Holder {
        private static final AudioManager INSTANCE = new AudioManager();
    }

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    public synchronized void init() {
        if (initialized) return;

        device = alcOpenDevice((ByteBuffer) null);
        if (device == MemoryUtil.NULL) throw new IllegalStateException("Failed to open default OpenAL device");

        context = alcCreateContext(device, (IntBuffer) null);
        if (context == MemoryUtil.NULL) throw new IllegalStateException("Failed to create OpenAL context");

        alcMakeContextCurrent(context);
        AL.createCapabilities(ALC.createCapabilities(device));
        
        createSfxPool(16);
        createBgmSource();
        
        initialized = true;
        System.out.println("AudioManager: OpenAL initialized.");
    }

    public synchronized void cleanup() {
        // Stop everything
        destroySfxSources();
        destroyBgmSource();

        // Unload all buffers
        unloadSfxBuffers();
        unloadBgmBuffers();

        if (context != MemoryUtil.NULL) {
            alcDestroyContext(context);
            context = MemoryUtil.NULL;
        }
        if (device != MemoryUtil.NULL) {
            alcCloseDevice(device);
            device = MemoryUtil.NULL;
        }
        initialized = false;
        sfxDirSet = false;
        bgmDirSet = false;
        System.out.println("AudioManager: Cleaned up.");
    }

    // -------------------------------------------------------------------------
    // SFX Handling (Pre-load)
    // -------------------------------------------------------------------------

    /**
     * Sets the SFX directory and pre-loads all found audio files.
     * @param directoryPath Classpath directory (e.g. "anderson/sfx")
     */
    public synchronized void setupSFX(String directoryPath) {
        if (!initialized) throw new IllegalStateException("Init first.");
        unloadSfxBuffers();
        sfxDirSet = false;

        System.out.println("AudioManager: Loading SFX from " + directoryPath);
        Map<String, String> found = scanResources(directoryPath);
        for (Map.Entry<String, String> entry : found.entrySet()) {
            int bufferId = loadAudioToBuffer(entry.getValue());
            if (bufferId != 0) {
                sfxBuffers.put(entry.getKey(), bufferId);
            }
        }
        sfxDirSet = true;
        System.out.println("AudioManager: Loaded " + sfxBuffers.size() + " SFX files.");
    }

    public synchronized void resetSFX(String newDirectory) {
        // For SFX reset, we might want to stop playing sounds?
        // Yes, stop sources, destroy buffers, reload.
        destroySfxSources();
        unloadSfxBuffers();
        
        createSfxPool(16); // recreate sources
        setupSFX(newDirectory);
    }

    public synchronized void playSFX(String filename) {
        if (!sfxDirSet) return;
        
        Integer buf = sfxBuffers.get(filename);
        if (buf == null) {
            System.err.println("AudioManager: SFX not found: " + filename);
            return;
        }

        // Debounce / Gap
        long now = System.currentTimeMillis();
        long last = lastPlayedTime.getOrDefault(filename, 0L);
        if (now - last < MIN_PLAY_GAP_MS) return;
        lastPlayedTime.put(filename, now);

        if (sfxSources == null || sfxSources.length == 0) return;

        int src = sfxSources[nextSfxIdx];
        alSourceStop(src);
        alSourcei(src, AL_BUFFER, buf);
        alSourcef(src, AL_GAIN, sfxVolume);
        alSourcei(src, AL_LOOPING, AL_FALSE);
        alSourcePlay(src);

        nextSfxIdx = (nextSfxIdx + 1) % sfxSources.length;
    }

    // -------------------------------------------------------------------------
    // BGM Handling (Lazy Load)
    // -------------------------------------------------------------------------

    /**
     * Sets the BGM directory. Scans files but does NOT load them yet.
     * @param directoryPath Classpath directory (e.g. "anderson/bgm")
     */
    public synchronized void setupBGM(String directoryPath) {
        if (!initialized) throw new IllegalStateException("Init first.");
        // If we are changing directory, should we stop current BGM? Yes.
        stopBGM();
        unloadBgmBuffers(); // Unload cache
        bgmPaths.clear();
        bgmDirSet = false;

        System.out.println("AudioManager: Scanning BGM from " + directoryPath);
        Map<String, String> found = scanResources(directoryPath);
        bgmPaths.putAll(found);
        
        bgmDirSet = true;
        System.out.println("AudioManager: Found " + bgmPaths.size() + " BGM files (Lazy load).");
    }

    public synchronized void resetBGM(String newDirectory) {
        stopBGM();
        destroyBgmSource();
        unloadBgmBuffers();
        bgmPaths.clear();

        createBgmSource();
        setupBGM(newDirectory);
    }

    public synchronized void playBGM(String filename) {
        if (!bgmDirSet) return;
        
        // Check if currently playing this exact file
        if (currentBgmName != null && currentBgmName.equals(filename)) {
            int state = alGetSourcei(bgmSource, AL_SOURCE_STATE);
            if (state == AL_PLAYING) return; 
        }

        // 1. Check if loaded in cache
        Integer buf = bgmBuffers.get(filename);
        if (buf == null) {
            // 2. Not in cache, try to find path and load
            String path = bgmPaths.get(filename);
            if (path == null) {
                System.err.println("AudioManager: BGM file not found in scan: " + filename);
                return;
            }
            // Load now
            System.out.println("AudioManager: Lazy loading BGM... " + filename);
            buf = loadAudioToBuffer(path);
            if (buf == 0) {
                System.err.println("AudioManager: Failed to load BGM " + filename);
                return;
            }
            bgmBuffers.put(filename, buf);
        }

        // 3. Play
        alSourceStop(bgmSource);
        alSourcei(bgmSource, AL_BUFFER, buf);
        alSourcef(bgmSource, AL_GAIN, bgmVolume);
        alSourcei(bgmSource, AL_LOOPING, AL_TRUE);
        alSourcePlay(bgmSource);

        currentBgmName = filename;
        System.out.println("AudioManager: Playing BGM " + filename);
    }

    public synchronized void stopBGM() {
        if (bgmSource != 0) {
            alSourceStop(bgmSource);
            // We do NOT detach buffer necessarily, but good practice if we want to release it?
            // For now, keep it attached or just stopped.
            alSourcei(bgmSource, AL_BUFFER, 0); 
        }
        currentBgmName = null;
    }

    // -------------------------------------------------------------------------
    // Volume
    // -------------------------------------------------------------------------

    public void setSFXVolume(float volume) {
        this.sfxVolume = Math.max(0.0f, Math.min(1.0f, volume));
        if (sfxSources != null) {
            for (int s : sfxSources) alSourcef(s, AL_GAIN, sfxVolume);
        }
    }

    public void setBGMVolume(float volume) {
        this.bgmVolume = Math.max(0.0f, Math.min(1.0f, volume));
        if (bgmSource != 0) alSourcef(bgmSource, AL_GAIN, bgmVolume);
    }
    
    public float getSFXVolume() { return sfxVolume; }
    public float getBGMVolume() { return bgmVolume; }


    // -------------------------------------------------------------------------
    // Internals
    // -------------------------------------------------------------------------

    private void createSfxPool(int size) {
        if (size <= 0) size = 8;
        sfxSources = new int[size];
        for (int i = 0; i < size; i++) {
            sfxSources[i] = alGenSources();
            alSourcef(sfxSources[i], AL_GAIN, sfxVolume);
            alSourcef(sfxSources[i], AL_PITCH, 1.0f);
            alSourcei(sfxSources[i], AL_LOOPING, AL_FALSE);
        }
        nextSfxIdx = 0;
    }

    private void destroySfxSources() {
        if (sfxSources != null) {
            for (int s : sfxSources) {
                if (s != 0) {
                    alSourceStop(s);
                    alDeleteSources(s);
                }
            }
            sfxSources = null;
        }
    }

    private void unloadSfxBuffers() {
        for (Integer b : sfxBuffers.values()) {
            if (b != 0) alDeleteBuffers(b);
        }
        sfxBuffers.clear();
        lastPlayedTime.clear();
    }

    private void createBgmSource() {
        bgmSource = alGenSources();
        alSourcef(bgmSource, AL_GAIN, bgmVolume);
        alSourcef(bgmSource, AL_PITCH, 1.0f);
        alSourcei(bgmSource, AL_LOOPING, AL_TRUE);
    }

    private void destroyBgmSource() {
        if (bgmSource != 0) {
            alSourceStop(bgmSource);
            alDeleteSources(bgmSource);
            bgmSource = 0;
        }
    }

    private void unloadBgmBuffers() {
        for (Integer b : bgmBuffers.values()) {
            if (b != 0) alDeleteBuffers(b);
        }
        bgmBuffers.clear();
    }

    private Map<String, String> scanResources(String directoryPath) {
        Map<String, String> results = new HashMap<>();
        try {
            ClassPath classPath = ClassPath.from(Thread.currentThread().getContextClassLoader());
            String searchPath = directoryPath.replace('\\', '/');
            if (searchPath.startsWith("/")) searchPath = searchPath.substring(1);
            if (!searchPath.endsWith("/")) searchPath = searchPath + "/";

            for (ClassPath.ResourceInfo info : classPath.getResources()) {
                String resourceName = info.getResourceName();
                if (resourceName.startsWith(searchPath)) {
                    String fileName = resourceName.substring(resourceName.lastIndexOf('/') + 1);
                    if (!fileName.isEmpty()) {
                        results.put(fileName, resourceName);
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("AudioManager: Scan failed: " + e.getMessage());
        }
        return results;
    }

    private int loadAudioToBuffer(String resourcePath) {
        String p = resourcePath.startsWith("/") ? resourcePath : "/" + resourcePath;
        try (InputStream raw = AudioManager.class.getResourceAsStream(p)) {
             InputStream streamToUse = raw;
             if (streamToUse == null) {
                 streamToUse = Thread.currentThread().getContextClassLoader().getResourceAsStream(resourcePath);
             }
             if (streamToUse == null) {
                 System.err.println("AudioManager: Resource not found: " + resourcePath);
                 return 0;
             }
             return loadFromStream(streamToUse);
        } catch (IOException e) {
            System.err.println("AudioManager: I/O error loading: " + resourcePath + " -> " + e.getMessage());
        }
        return 0;
    }

    private int loadFromStream(InputStream raw) {
        try (AudioInputStream ais = AudioSystem.getAudioInputStream(new BufferedInputStream(raw))) {
            AudioFormat baseFormat = ais.getFormat();
            // Convert to PCM 16bit Signed
            AudioFormat pcmFormat = new AudioFormat(
                    AudioFormat.Encoding.PCM_SIGNED,
                    baseFormat.getSampleRate(),
                    16,
                    baseFormat.getChannels(),
                    baseFormat.getChannels() * 2,
                    baseFormat.getSampleRate(),
                    false
            );

            try (AudioInputStream din = AudioSystem.getAudioInputStream(pcmFormat, ais)) {
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
        } catch (UnsupportedAudioFileException e) {
             System.err.println("AudioManager: Unsupported audio format: " + e.getMessage() + " (Supported: WAV, MP3, OGG. M4A/AAC requires native libraries)");
             // Note: standard Java Sound does not support MP3/M4A without SPIs.
        } catch (Exception e) {
            System.err.println("AudioManager: Error loading audio stream: " + e.getMessage());
            e.printStackTrace();
        }
        return 0;
    }
}