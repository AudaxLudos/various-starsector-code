package variousstarsectorcode.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.util.Misc;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Large1Effect implements EveryFrameWeaponEffectPlugin, OnFireEffectPlugin {
    // Fires 10 arcs
    // Each arc can ricochet to another target
    // Each arc can only ricochet 2 times
    // Arcs can pierce shields based on target's hard flux level
    // Arcs will prioritize other targets
    // Arcs will prioritize ships before missiles
    public static final int MAX_TARGETS = 10;
    public static final float ARC = 45;
    public List<ArcTarget> primaryTargets = new ArrayList<>();
    public List<ArcTarget> secondaryTargets = new ArrayList<>();
    public List<ArcTarget> tertiaryTargets = new ArrayList<>();
    public List<CombatEntityAPI> usedTargets = new ArrayList<>();

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        Iterator<ArcTarget> primaryItr = this.primaryTargets.iterator();
        while (primaryItr.hasNext()) {
            ArcTarget data = primaryItr.next();
            data.spawnArc();
            CombatEntityAPI target = findTarget(data.arcEntity.getTargetLocation(), weapon);
            ArcTarget arcTarget = new ArcTarget(data.projectile, engine, data.arcEntity.getTargetLocation(), data.arcEntity, target);
            this.secondaryTargets.add(arcTarget);
            primaryItr.remove();
        }

        Iterator<ArcTarget> secondaryItr = this.secondaryTargets.iterator();
        while (secondaryItr.hasNext()) {
            ArcTarget data = secondaryItr.next();
            data.spawnArc();
            CombatEntityAPI target = findTarget(data.arcEntity.getTargetLocation(), weapon);
            ArcTarget arcTarget = new ArcTarget(data.projectile, engine, data.arcEntity.getTargetLocation(), data.arcEntity, target);
            this.tertiaryTargets.add(arcTarget);
            secondaryItr.remove();
        }

        Iterator<ArcTarget> tertiaryItr = this.tertiaryTargets.iterator();
        while (tertiaryItr.hasNext()) {
            ArcTarget data = tertiaryItr.next();
            data.spawnArc();
            tertiaryItr.remove();
        }

        this.usedTargets.clear();
    }

    @Override
    public void onFire(DamagingProjectileAPI projectile, WeaponAPI weapon, CombatEngineAPI engine) {
        for (int i = 0; i < MAX_TARGETS; i++) {
            CombatEntityAPI target = findTarget(projectile.getLocation(), weapon);
            ArcTarget arcTarget = new ArcTarget(projectile, engine, weapon.getFirePoint(0), null, target);
            this.primaryTargets.add(arcTarget);
        }
    }

    public CombatEntityAPI findTarget(Vector2f center, WeaponAPI weapon) {
        float range = weapon.getRange() * 0.5f;

        Iterator<Object> itr = Global.getCombatEngine().getAllObjectGrid().getCheckIterator(center, range * 2f, range * 2f);
        int owner = weapon.getShip().getOwner();
        CombatEntityAPI bestNotUsed = null;
        float bestNotUsedDist = Float.MAX_VALUE;
        CombatEntityAPI bestUsed = null;
        float bestUsedDist = Float.MAX_VALUE;

        ShipAPI ship = weapon.getShip();
        boolean ignoreFlares = ship != null && ship.getMutableStats().getDynamic().getValue(Stats.PD_IGNORES_FLARES, 0) >= 1;
        ignoreFlares |= weapon.hasAIHint(WeaponAPI.AIHints.IGNORES_FLARES);

        while (itr.hasNext()) {
            Object o = itr.next();
            if (!(o instanceof MissileAPI) && !(o instanceof ShipAPI)) {
                continue;
            }
            CombatEntityAPI other = (CombatEntityAPI) o;
            if (other.getOwner() == owner) {
                continue;
            }

            if (other instanceof ShipAPI) {
                ShipAPI otherShip = (ShipAPI) other;
                if (otherShip.isHulk()) {
                    continue;
                }
                if (otherShip.isPhased()) {
                    continue;
                }
            }

            if (other.getCollisionClass() == CollisionClass.NONE) {
                continue;
            }

            if (ignoreFlares && other instanceof MissileAPI) {
                MissileAPI missile = (MissileAPI) other;
                if (missile.isFlare()) {
                    continue;
                }
            }

            float radius = Misc.getTargetingRadius(center, other, false);
            float dist = Misc.getDistance(center, other.getLocation()) - radius;
            if (dist > range) {
                continue;
            }

            if (!Misc.isInArc(weapon.getCurrAngle(), ARC, center, other.getLocation())) {
                continue;
            }

            if (!this.usedTargets.contains(other) && other instanceof ShipAPI) {
                if (dist < bestNotUsedDist) {
                    bestNotUsedDist = dist;
                    bestNotUsed = other;
                }
            } else {
                if (dist < bestUsedDist) {
                    bestUsedDist = dist;
                    bestUsed = other;
                }
            }
        }

        CombatEntityAPI best = bestNotUsed;
        if (best == null) {
            best = bestUsed;
        }

        this.usedTargets.add(best);
        return best;
    }

    public static class ArcTarget {
        public DamagingProjectileAPI projectile;
        public WeaponAPI weapon;
        public CombatEngineAPI engine;
        public ShipAPI source;
        public CombatEntityAPI anchor;
        public CombatEntityAPI target;
        public Vector2f arcSpawnLoc;
        public EmpArcEntityAPI arcEntity;

        public ArcTarget(DamagingProjectileAPI projectile, CombatEngineAPI engine, Vector2f arcSpawnLoc, CombatEntityAPI anchor, CombatEntityAPI target) {
            this.projectile = projectile;
            this.engine = engine;
            this.arcSpawnLoc = arcSpawnLoc;
            this.anchor = anchor;
            this.target = target;
            this.weapon = projectile.getWeapon();
            this.source = projectile.getSource();
        }

        public void spawnArc() {
            float thickness = 20f;
            float coreWidthMult = 0.67f;
            Color color = this.weapon.getSpec().getGlowColor();
            if (this.target != null) {
                float pierceChance = 0f;
                if (this.target instanceof ShipAPI) {
                    pierceChance = ((ShipAPI) this.target).getHardFluxLevel() - 0.1f;
                    pierceChance *= ((ShipAPI) this.target).getMutableStats().getDynamic().getValue(Stats.SHIELD_PIERCED_MULT);
                }
                if (Math.random() < pierceChance) {
                    this.arcEntity = this.engine.spawnEmpArcPierceShields(
                            null,
                            this.arcSpawnLoc,
                            this.anchor,
                            this.target,
                            DamageType.ENERGY,
                            this.projectile.getEmpAmount(),
                            this.projectile.getDamageAmount(),
                            this.weapon.getRange(),
                            "tachyon_lance_emp_impact",
                            thickness,
                            color,
                            Color.WHITE
                    );
                } else {
                    this.arcEntity = this.engine.spawnEmpArc(
                            null,
                            this.arcSpawnLoc,
                            this.anchor,
                            this.target,
                            DamageType.ENERGY,
                            this.projectile.getEmpAmount(),
                            this.projectile.getDamageAmount(),
                            this.weapon.getRange(),
                            "tachyon_lance_emp_impact",
                            thickness,
                            color,
                            Color.WHITE
                    );
                }
            } else {
                Vector2f from = new Vector2f(this.arcSpawnLoc);
                Vector2f to = pickNoTargetDest();
                this.arcEntity = this.engine.spawnEmpArcVisual(
                        from,
                        null,
                        to,
                        null,
                        thickness,
                        color,
                        Color.WHITE);
            }
            this.arcEntity.setCoreWidthOverride(thickness * coreWidthMult);
            this.arcEntity.setSingleFlickerMode();
        }

        public Vector2f pickNoTargetDest() {
            float minAngle = this.weapon.getCurrAngle() - ARC * 0.5f;
            float maxAngle = this.weapon.getCurrAngle() + ARC * 0.5f;
            float range = this.weapon.getRange() * 0.34f;
            return MathUtils.getPointOnCircumference(this.arcSpawnLoc, range, MathUtils.getRandomNumberInRange(minAngle, maxAngle));
        }
    }
}
