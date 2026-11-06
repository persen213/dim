package dev.senior.dimension;

import io.github.pinchan4273.reacademycraft.api.ActivationOutcome;
import io.github.pinchan4273.reacademycraft.api.StopReason;
import io.github.pinchan4273.reacademycraft.api.mc.AcademySafety;
import io.github.pinchan4273.reacademycraft.api.mc.ActivationContext;
import io.github.pinchan4273.reacademycraft.api.mc.SkillBehavior;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Spatial Collapse (ultimate): a 10-block-radius area (bigger in the Domain) is pulled toward a point for 3 s, then
 * releases a heavy shockwave and throws survivors outward. Damages entities only - it never destroys terrain.
 */
final class SpatialCollapse implements SkillBehavior {
    static final int CHARGE = 60;

    @Override public ActivationOutcome start(ActivationContext c) {
        ServerPlayer p = c.naturalUser();
        if (p == null || SeniorState.shifted(p)) return ActivationOutcome.REFUSED_INVALID;
        Vec3 centre = Targeting.point(p, SeniorState.scaled(p, 25, 1.5));
        if (!c.pay(Costs.COLLAPSE_CP, Costs.COLLAPSE_OVERLOAD)) return ActivationOutcome.REFUSED_RESOURCE;
        SeniorState s = SeniorState.of(p);
        s.collapseCenter = centre;
        s.collapseTargets.clear();
        Fx.sound(p.serverLevel(), centre, SoundEvents.RESPAWN_ANCHOR_CHARGE, 1.5f, 0.4f);
        return ActivationOutcome.STARTED;
    }

    @Override public boolean tick(ActivationContext c) {
        ServerPlayer p = c.naturalUser();
        SeniorState s = p == null ? null : SeniorState.peek(p);
        if (p == null || s == null || s.collapseCenter == null) return false;
        ServerLevel level = p.serverLevel();
        var actor = c.actor();
        Vec3 centre = s.collapseCenter;
        double radius = SeniorState.scaled(p, 10, 1.5);
        int age = c.age();
        if (age >= CHARGE) {
            shockwave(c, p, level, centre, radius);
            return false;
        }
        double progress = age / (double) CHARGE;
        // Re-check who may be pulled a few times a second (permission checks are not free).
        if (age % 5 == 0) {
            s.collapseTargets.clear();
            for (LivingEntity e : Targeting.within(level, centre, radius, p))
                if (AcademySafety.mayAttack(actor, e, SeniorDim.COLLAPSE)) s.collapseTargets.add(e.getUUID());
        }
        Vec3 pullPoint = centre.add(0, 1.0, 0);
        for (java.util.UUID id : s.collapseTargets) {
            Entity found = level.getEntity(id);
            if (!(found instanceof LivingEntity t) || !t.isAlive()) continue;
            Vec3 to = pullPoint.subtract(Targeting.centre(t));
            double dist = to.length();
            if (dist < 0.8) continue;
            double strength = Math.min((0.08 + 0.18 * progress) * Targeting.controlFactor(t), dist * 0.2);
            Vec3 m = t.getDeltaMovement().scale(0.6).add(to.normalize().scale(strength));
            t.setDeltaMovement(m);
            t.hurtMarked = true;
        }
        // Inward spiral and a shrinking boundary.
        for (int arm = 0; arm < 4; arm++) {
            double a = age * 0.35 + arm * Math.PI / 2;
            double r = radius * (1.0 - progress * 0.85) * (0.4 + 0.6 * ((arm + age) % 5) / 4.0);
            Vec3 at = centre.add(Math.cos(a) * r, 0.3 + progress * 2.0, Math.sin(a) * r);
            Fx.burst(level, arm % 2 == 0 ? Fx.PURPLE : Fx.BLUE, at, 2, 0.1, 0);
        }
        Fx.burst(level, ParticleTypes.REVERSE_PORTAL, centre.add(0, 1, 0), 4, 0.5 + progress, 0.05);
        if (age % 4 == 0) Fx.ring(level, Fx.PURPLE, centre.add(0, 0.1, 0), radius * (1.0 - 0.7 * progress), 36);
        return true;
    }

    private static void shockwave(ActivationContext c, ServerPlayer p, ServerLevel level, Vec3 centre, double radius) {
        var actor = c.actor();
        float base = 30f + 14f * c.proficiency();
        int hits = 0;
        for (LivingEntity e : Targeting.within(level, centre, radius, p)) {
            double d = Targeting.centre(e).distanceTo(centre);
            float damage = (float) (base * (1.0 - 0.5 * Math.min(1.0, d / radius)));
            if (!AcademySafety.attack(actor, e, SeniorDim.COLLAPSE, damage)) continue;
            Vec3 away = new Vec3(e.getX() - centre.x, 0, e.getZ() - centre.z);
            away = away.lengthSqr() < 0.01 ? new Vec3(level.random.nextDouble() - 0.5, 0, level.random.nextDouble() - 0.5).normalize() : away.normalize();
            double k = 1.6 * Targeting.controlFactor(e);
            e.setDeltaMovement(away.x * k, 0.6 * Math.max(0.4, Targeting.controlFactor(e)), away.z * k);
            e.hurtMarked = true;
            hits++;
        }
        Vec3 mid = centre.add(0, 1.0, 0);
        Fx.burst(level, ParticleTypes.FLASH, mid, 1, 0, 0);
        Fx.burst(level, ParticleTypes.EXPLOSION_EMITTER, mid, 3, radius * 0.25, 0);
        Fx.burst(level, Fx.PURPLE, mid, 120, radius * 0.5, 0.3);
        Fx.burst(level, Fx.BLUE, mid, 90, radius * 0.5, 0.3);
        Fx.burst(level, Fx.WHITE, mid, 60, radius * 0.4, 0.35);
        for (int i = 1; i <= 5; i++) Fx.ring(level, i % 2 == 0 ? Fx.BLUE : Fx.PURPLE, centre.add(0, 0.2, 0), radius * i / 5.0, 16 + i * 10);
        Fx.sound(level, centre, SoundEvents.GENERIC_EXPLODE, 2.0f, 0.5f);
        Fx.sound(level, centre, SoundEvents.WARDEN_SONIC_BOOM, 1.5f, 0.8f);
        if (hits > 0) c.addProficiency(0.015f * Math.min(hits, 4));
    }

    @Override public void stop(ActivationContext c, StopReason reason) {
        ServerPlayer p = c.naturalUser();
        SeniorState s = p == null ? null : SeniorState.peek(p);
        if (s != null) { s.collapseCenter = null; s.collapseTargets.clear(); }
        c.setCooldown(60 * 20);
    }
}
