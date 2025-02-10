package variousstarsectorcode.weapons;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.impl.campaign.ids.StarTypes;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.combat.CombatViewport;
import com.fs.starfarer.combat.entities.terrain.Planet;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.EnumSet;

public class Large2Effect implements EveryFrameWeaponEffectPlugin, OnFireEffectPlugin, OnHitEffectPlugin {
    public CombatEntityAPI miniSunEntity;
    public MiniSunEffect miniSunEffect;

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        boolean charging = weapon.getChargeLevel() > 0 && weapon.getCooldownRemaining() <= 0;
        if (charging && this.miniSunEntity == null) {
            this.miniSunEffect = new MiniSunEffect(weapon, engine);
            this.miniSunEntity = engine.addLayeredRenderingPlugin(this.miniSunEffect);
        } else if (!charging && this.miniSunEntity != null) {
            engine.removeEntity(this.miniSunEntity);
            this.miniSunEffect = null;
            this.miniSunEntity = null;
        }
    }

    @Override
    public void onFire(DamagingProjectileAPI projectile, WeaponAPI weapon, CombatEngineAPI engine) {
        if (this.miniSunEffect != null) {
            projectile.getLocation().set(this.miniSunEntity.getLocation().x, this.miniSunEntity.getLocation().y);
            this.miniSunEffect.projectile = projectile;
            this.miniSunEffect = null;
            this.miniSunEntity = null;
        }
    }

    @Override
    public void onHit(DamagingProjectileAPI projectile, CombatEntityAPI target, Vector2f point, boolean shieldHit, ApplyDamageResultAPI damageResult, CombatEngineAPI engine) {
        new ExplosionEffect().explode(projectile, engine);
    }

    public static class MiniSunEffect extends BaseCombatLayeredRenderingPlugin {
        public CombatEngineAPI engine;
        public WeaponAPI weapon;
        public IntervalUtil arcInterval = new IntervalUtil(0.09f, 0.11f);
        public Planet planet;
        public DamagingProjectileAPI projectile = null;
        public float maxSize = 50f;

        public MiniSunEffect(WeaponAPI weapon, CombatEngineAPI engine) {
            this.engine = engine;
            this.weapon = weapon;
            this.planet = new Planet(StarTypes.RED_GIANT, 0f, 0f, this.weapon.getFirePoint(0));
            this.planet.setRenderingBackground(false);
            this.planet.setCollisionClass(CollisionClass.NONE);
            this.planet.isAdditiveBlend();
        }

        @Override
        public void advance(float amount) {
            if (this.engine.isPaused()) {
                return;
            }

            float chargeLevel = this.weapon.getChargeLevel();
            boolean charging = this.weapon.getChargeLevel() > 0 && this.weapon.getCooldownRemaining() <= 0;

            if (charging) {
                this.arcInterval.advance(amount);
                if (this.arcInterval.intervalElapsed()) {
                    spawnArc();
                }
            }

            if (this.projectile != null) {
                Vector2f projLoc = this.projectile.getLocation();
                this.entity.getLocation().set(projLoc.x, projLoc.y);
                this.planet.getLocation().set(projLoc.x, projLoc.y);
            } else {
                Vector2f barrelLoc = MathUtils.getPointOnCircumference(this.weapon.getFirePoint(0),  (20f + this.maxSize) * chargeLevel, this.weapon.getCurrAngle());
                this.entity.getLocation().set(barrelLoc.x, barrelLoc.y);
                this.planet.getLocation().set(barrelLoc.x, barrelLoc.y);
                this.planet.setRadius(chargeLevel * this.maxSize);
            }

            this.planet.advance(amount * 100f);
        }

        @Override
        public void render(CombatEngineLayers layer, ViewportAPI viewport) {
            this.planet.renderSphere((CombatViewport) viewport);
            this.planet.renderStarGlow((CombatViewport) viewport);
        }

        @Override
        public boolean isExpired() {
            if (this.projectile != null) {
                return !this.engine.isEntityInPlay(this.projectile) || this.projectile.isExpired() || this.projectile.didDamage();
            }
            return false;
        }

        @Override
        public EnumSet<CombatEngineLayers> getActiveLayers() {
            return EnumSet.of(CombatEngineLayers.ABOVE_SHIPS_AND_MISSILES_LAYER);
        }

        @Override
        public float getRenderRadius() {
            return Float.MAX_VALUE;
        }

        public void spawnArc() {
            float thickness = 20f;
            float coreWidthMult = 0.67f;
            float minAngle = this.weapon.getShip().getFacing() - 180f + 90f;
            float maxAngle = this.weapon.getShip().getFacing() - 180f - 90f;
            float angle = MathUtils.getRandomNumberInRange(minAngle, maxAngle);
            Vector2f arcTargetLoc = MathUtils.getPointOnCircumference(this.planet.getLocation(), this.planet.getRadius(), angle);
            EmpArcEntityAPI arc = this.engine.spawnEmpArcVisual(
                    this.weapon.getFirePoint(0),
                    this.weapon.getShip(),
                    arcTargetLoc,
                    this.planet,
                    thickness,
                    this.planet.getSpec().atmosphereColor,
                    Color.WHITE
            );
            arc.setCoreWidthOverride(thickness * coreWidthMult);
            arc.setSingleFlickerMode();
        }
    }
}
