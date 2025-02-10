package variousstarsectorcode.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.util.Misc;
import org.dark.shaders.distortion.DistortionShader;
import org.dark.shaders.distortion.RippleDistortion;
import org.dark.shaders.distortion.WaveDistortion;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.util.MagicRender;

import java.awt.*;

public class ExplosionEffect {
    protected Color coreColor = new Color(255, 165, 30, 255);
    protected float coreRadius = 200f;
    protected float coreDuration = 1.5f;
    protected float coreGlowRadius = 650f;
    protected float coreGlowDuration = 1f;

    protected Color mantleColor = new Color(255, 195, 40, 10);
    protected float mantleRadius = 500f;
    protected float mantleDuration = 1.5f;
    protected float mantleGlowRadius = 750f;
    protected float mantleGlowDuration = 1f;

    protected Color flashColor = new Color(240, 225, 200, 200);
    protected float flashGlowRadius = 2500f;
    protected float flashGlowDuration = 0.65f;

    public void explode(DamagingProjectileAPI projectile, CombatEngineAPI engine) {
        Vector2f point = projectile.getLocation();
        engine.spawnExplosion(point, Misc.ZERO, this.coreColor, this.coreRadius, this.coreDuration);
        engine.spawnExplosion(point, Misc.ZERO, this.mantleColor, this.mantleRadius, this.mantleDuration);
        engine.addHitParticle(point, Misc.ZERO, this.coreGlowRadius, 1f, this.coreGlowDuration, this.coreColor);
        engine.addSmoothParticle(point, Misc.ZERO, this.mantleGlowRadius, 1f, this.mantleGlowDuration, this.mantleColor);
        engine.addHitParticle(point, Misc.ZERO, this.flashGlowRadius, 1f, this.flashGlowDuration, this.flashColor);

        spawnParticles(30, point, 50f, 150f, this.coreColor, engine);
        spawnParticles(30, point, 150f, 300f, this.mantleColor, engine);

        MagicRender.battlespace(Global.getSettings().getSprite("fx", "vsc_shockwave"), point, Misc.ZERO, new Vector2f(200f, 200f), new Vector2f(1500f, 1500f), 360f * (float)Math.random(), 0f, new Color(200, 172, 119, 255), true, 0f, 0f, 0.75f);
        MagicRender.battlespace(Global.getSettings().getSprite("fx", "vsc_shockwave"), point, Misc.ZERO, new Vector2f(192f, 192f), new Vector2f(960f, 960f), 360f * (float)Math.random(), 0f, new Color(255, 175, 30, 255), true, 0f, 0.1F, 0.2f);
        MagicRender.battlespace(Global.getSettings().getSprite("fx", "vsc_shockwave"), point, Misc.ZERO, new Vector2f(256f, 256f), new Vector2f(550f, 550f), 360f * (float)Math.random(), 0f, new Color(255, 165, 0, 230), true, 0.2f, 0f, 0.35f);
        MagicRender.battlespace(Global.getSettings().getSprite("fx", "vsc_shockwave"), point, Misc.ZERO, new Vector2f(392f, 392f), new Vector2f(240f, 240f), 360f * (float)Math.random(), 0f, new Color(170, 135, 75, 255), true, 0.4f, 0f, 1.6f);
        MagicRender.battlespace(Global.getSettings().getSprite("fx", "vsc_shockwave"), point, Misc.ZERO, new Vector2f(392f, 392f), new Vector2f(240f, 240f), 360f * (float)Math.random(), 0f, new Color(170, 135, 75, 255), true, 0.4f, 0f, 2.5f);
        
        spawnDistortion(point);
        spawnRipple(point, Misc.ZERO, 530f, 5f, false, 0f, 360f, 1f, 0.1f, 0.25f, 0.5f, 0.7f, 0f);
    }

    public void spawnParticles(int count, Vector2f spawnLoc, float minVel, float maxVel, Color color, CombatEngineAPI engine) {
        for (int i = 0; i < count; i++) {
            Vector2f velocity = MathUtils.getPointOnCircumference(null, MathUtils.getRandomNumberInRange(minVel, maxVel), (float) Math.random() * 360f);
            float randSize = MathUtils.getRandomNumberInRange(4, 12);
            float randDuration = MathUtils.getRandomNumberInRange(1.5f, 5.5f);
            Vector2f offset = (Vector2f) velocity.scale(MathUtils.getRandomNumberInRange(0.05f, 1f));
            Vector2f spawnOffset = Vector2f.add(spawnLoc, offset, null);
            engine.addHitParticle(spawnOffset, velocity, randSize, 1f, randDuration, color);
        }
    }

    public void spawnDistortion(Vector2f spawnLoc) {
        WaveDistortion wave = new WaveDistortion(spawnLoc, Misc.ZERO);
        wave.setIntensity(1.5f);
        wave.setSize(750f);
        wave.flip(true);
        wave.setLifetime(0f);
        wave.fadeOutIntensity(1f);
        wave.setLocation(spawnLoc);
        DistortionShader.addDistortion(wave);
    }

    public void spawnRipple(Vector2f loc, Vector2f vel, float size, float intensity, boolean flip, float angle, float arc,
                            float edgeSmooth, float fadeIn, float last, float fadeOut, float growthTime, float shrinkTime) {
        RippleDistortion ripple = new RippleDistortion(loc, vel);
        ripple.setIntensity(intensity);
        ripple.setSize(size);
        ripple.setArc(angle - arc / 2f, angle + arc / 2f);
        if (edgeSmooth != 0f)
            ripple.setArcAttenuationWidth(edgeSmooth);
        ripple.flip(flip);
        if (fadeIn != 0f)
            ripple.fadeInIntensity(fadeIn);
        ripple.setLifetime(last);
        if (fadeOut != 0.f)
            ripple.setAutoFadeIntensityTime(fadeOut);
        if (growthTime != 0f)
            ripple.fadeInSize(growthTime);
        if (shrinkTime != 0f)
            ripple.setAutoFadeSizeTime(shrinkTime);
        ripple.setFrameRate(1f / (fadeIn + last + fadeOut) * 60f);
        DistortionShader.addDistortion(ripple);
    }
}
