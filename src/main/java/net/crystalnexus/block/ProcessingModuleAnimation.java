package net.crystalnexus.block;

import java.util.Objects;

/** Client animation time follows recipe snapshots without restarting at tick or packet boundaries. */
public final class ProcessingModuleAnimation {
    private static final float[] CNC_X = {0, -.09F, .09F, .09F, -.09F, -.09F, .09F, 0};
    private static final float[] CNC_Z = {0, -.08F, -.08F, 0, 0, .08F, .08F, 0};
    private String recipe;
    private int lastProgress;
    private int duration;
    private double lastTime;
    private double visualTicks;

    public float progress(String recipe, int progress, int duration, boolean working, double time) {
        if (recipe == null) {
            this.recipe = null;
            visualTicks = 0;
            return 0;
        }
        duration = Math.max(1, duration);
        if (!Objects.equals(this.recipe, recipe) || progress < lastProgress || this.duration != duration) {
            visualTicks = Math.max(0, progress - 1);
        } else if (working) {
            double elapsed = Math.max(0, time - lastTime);
            double speed = Math.clamp(1 + (progress - visualTicks) * .15, .8, 1.2);
            // Predict at most one tick ahead, correcting packet jitter through speed rather than position jumps.
            visualTicks = Math.min(progress + 1, visualTicks + elapsed * speed);
        }
        this.recipe = recipe;
        this.duration = duration;
        lastProgress = progress;
        lastTime = time;
        return (float) Math.clamp(visualTicks / duration, 0, 1);
    }

    public static float pressStroke(float progress) {
        return ease(progress / .35F) * (1 - ease((progress - .55F) / .35F));
    }

    public static float drillRotation(float progress) {
        return ease(progress / .9F) * 1440;
    }

    public static float cncStroke(float progress) {
        return ease(progress / .18F) * (1 - ease((progress - .82F) / .14F));
    }

    public static float cncOffsetX(float progress) { return cncPath(CNC_X, progress); }
    public static float cncOffsetZ(float progress) { return cncPath(CNC_Z, progress); }

    private static float cncPath(float[] positions, float progress) {
        float path = Math.clamp((progress - .22F) / .58F, 0, 1) * (positions.length - 1);
        int segment = Math.min((int) path, positions.length - 2);
        float fraction = ease(path - segment);
        return positions[segment] + (positions[segment + 1] - positions[segment]) * fraction;
    }

    public static float fabricationStroke(float progress) {
        return ease(progress / .18F) * (1 - ease((progress - .84F) / .12F));
    }

    public static float weldingStroke(float progress) {
        return ease(progress / .20F) * (1 - ease((progress - .80F) / .15F));
    }

    public static float weldingGlow(float progress) {
        float active = ease((progress - .20F) / .04F) * (1 - ease((progress - .76F) / .04F));
        double pulse = Math.sin(progress * Math.PI * 40);
        return active * (float) (.7 + .3 * pulse * pulse);
    }

    public static float fabricationBeam(float progress) {
        float active = ease((progress - .18F) / .06F) * (1 - ease((progress - .80F) / .04F));
        float pulse = ease((progress - .72F) / .04F) * (1 - ease((progress - .78F) / .04F));
        return active * (1 + pulse);
    }

    public static float fabricationSweep(float progress) {
        float scan = ease((progress - .24F) / .48F);
        return (float) Math.sin(scan * Math.PI * 4) * .085F;
    }

    private static float ease(float value) {
        float t = Math.clamp(value, 0, 1);
        return t * t * (3 - 2 * t);
    }
}
