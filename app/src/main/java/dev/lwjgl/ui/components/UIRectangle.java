package dev.lwjgl.ui.components;

import dev.lwjgl.ui.Colors;
import static org.lwjgl.opengl.GL11.*;

public class UIRectangle extends UIComponent {

    private float[] color;
    private float alpha = 1.0f;

    // ---------- ORIGINAL SIZE ----------
    private double originalW;
    private double originalH;

    // ---------- SHRINKING ----------
    private boolean isShrinking = false;
    private double shrinkScale = 1.0;

    public void setShrinking(boolean shrinking) {
        this.isShrinking = shrinking;
        if (shrinking) shrinkScale = 1.0;
    }
    
    public void setColor(float r, float g, float b) {
        this.color = new float[]{r, g, b};
    }

    public void setAlpha(float alpha) {
        this.alpha = alpha;
    }


    // ---------- CONSTRUCTORS ----------
    public UIRectangle(double x, double y, double w, double h) {
        this(x, y, w, h, Colors.DARK_RED);
    }

    public UIRectangle(double x, double y, double w, double h, float[] color) {
        super(x, y, w, h);
        this.color = color;

        this.originalW = w;
        this.originalH = h;
    }

    @Override
    public void render() {
        double baseW = originalW;
        double baseH = originalH;

        double scale = 1.0;
        if (isShrinking) {
            scale = shrinkScale;
            shrinkScale -= 0.05;
            if (shrinkScale <= 0) return;
        }

        double drawW = baseW * scale;
        double drawH = baseH * scale;

        // Glow rendering
        if (isGlowing) {
            int layers = 8;
            double glowPercent = 0.4;

            glEnable(GL_BLEND);
            glBlendFunc(GL_SRC_ALPHA, GL_ONE);

            float pulse = calculateGlowPulse();

            double centerX = x + baseW / 2.0;
            double centerY = y + baseH / 2.0;

            for (int i = layers; i >= 1; i--) {
                float t = i / (float) layers;
                float alpha = (glowMin + (glowMax - glowMin) * pulse) * t * t;

                double glowW = baseW * glowPercent * t * scale;
                double glowH = baseH * glowPercent * t * scale;

                glColor4f(color[0], color[1], color[2], alpha);

                renderRect(
                        centerX - (drawW / 2 + glowW),
                        centerY - (drawH / 2 + glowH),
                        drawW + glowW * 2,
                        drawH + glowH * 2
                );
            }

            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

            updateGlowState(pulse);
        }

        // ---------- CORE RECTANGLE ----------
        double finalW = drawW;
        double finalH = drawH;

        float brightness = calculateCoreBrightness();
        
        boolean needBlend = alpha < 1.0f;
        if (needBlend) {
            glEnable(GL_BLEND);
            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        }
        
        glColor4f(color[0] * brightness, color[1] * brightness, color[2] * brightness, alpha);


        renderRect(
                x - (finalW - drawW) / 2,
                y - (finalH - drawH) / 2,
                finalW,
                finalH
        );
        
        if (needBlend) {
            glDisable(GL_BLEND);
        }
    }
}
