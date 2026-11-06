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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Dimensional Rupture: a crack opens at the aimed spot (25 blocks), widens for 1.5 s, then bursts in a 6-block radius
 * (bigger in the Domain). Damage is higher near the centre. Damages entities only - it never breaks blocks.
 * Pressing the key again during the charge cancels it (the energy is spent).
 */
final class DimensionalRupture implements SkillBehavior {
    static final int CHARGE = 30;

    @Override public ActivationOutcome start(ActivationContext c) {
        ServerPlayer p = c.naturalUser();
        if (p == null || SeniorState.shifted(p)) return ActivationOutcome.REFUSED_INVALID;
        Vec3 centre = Targeting.point(p, SeniorState.scaled(p, 25, 1.5));
        if (!c.pay(Costs.RUPTURE_CP, Costs.RUPTURE_OVERLOAD)) return ActivationOutcome.REFUSED_RESOURCE;
        SeniorState.of(p).ruptureCenter = centre;
        Fx.sound(p.serverLevel(), centre, SoundEvents.RESPAWN_ANCHOR_CHARGE, 1.2f, 0.5f);
        return ActivationOutcome.STARTED;
    }

    @Override public boolean tick(ActivationContext c) {
        ServerPlayer p = c.naturalUser();
        SeniorState s = p == null ? null : SeniorState.peek(p);
        if (p == null || s == null || s.ruptureCenter == null) return false;
        ServerLevel level = p.serverLevel();
        Vec3 centre = s.ruptureCenter;
        int age = c.age();
        if (age < CHARGE) {
            double progress = age / (double) CHARGE;
            double height = 0.6 + 3.2 * progress, wobble = 0.25 * (1 + 3 * progress);
            Vec3 mid = centre.add(0, 1.2, 0);
            for (int i = 0; i < 8; i++) {
                double t = level.random.nextDouble() * 2 - 1;
                Vec3 at = mid.add((level.random.nextDouble() - 0.5) * wobble, t * height, (level.random.nextDouble() - 0.5) * wobble);
                Fx.burst(level, i % 2 == 0 ? Fx.WHITE : Fx.PURPLE, at, 1, 0, 0);
            }
            Fx.burst(level, Fx.BLUE, mid, 4, 0.2 + 0.8 * progress, 0);
            if (age % 6 == 0) Fx.ring(level, Fx.PURPLE, centre.add(0, 0.1, 0), 1.0 + 3.0 * progress, 20);
            return true;
        }
        explode(c, p, level, centre);
        return false;
    }

    private static void explode(ActivationContext c, ServerPlayer p, ServerLevel level, Vec3 centre) {
        var actor = c.actor();
        double radius = SeniorState.scaled(p, 6, 1.5);
        float base = 16f + 10f * c.proficiency();
        int hits = 0;
        for (LivingEntity e : Targeting.within(level, centre, radius, p)) {
            double d = Targeting.centre(e).distanceTo(centre);
            float damage = (float) (base * (1.0 - 0.6 * Math.min(1.0, d / radius)));
            if (!AcademySafety.attack(actor, e, SeniorDim.RUPTURE, damage)) continue;
            Vec3 away = Targeting.centre(e).subtract(centre);
            away = away.lengthSqr() < 0.01 ? new Vec3(0, 1, 0) : away.normalize();
            double k = (0.5 + 1.1 * (1.0 - Math.min(1.0, d / radius))) * Targeting.controlFactor(e);
            e.setDeltaMovement(away.x * k, 0.35 + 0.3 * k, away.z * k);
            e.hurtMarked = true;
            hits++;
        }
        Vec3 mid = centre.add(0, 1.2, 0);
        Fx.burst(level, ParticleTypes.FLASH, mid, 1, 0, 0);
        Fx.burst(level, ParticleTypes.EXPLOSION_EMITTER, mid, 1, 0, 0);
        Fx.burst(level, Fx.PURPLE, mid, 60, radius * 0.4, 0.2);
        Fx.burst(level, Fx.BLUE, mid, 40, radius * 0.4, 0.2);
        Fx.burst(level, Fx.WHITE, mid, 30, radius * 0.3, 0.25);
        for (int i = 1; i <= 3; i++) Fx.ring(level, i % 2 == 0 ? Fx.BLUE : Fx.PURPLE, centre.add(0, 0.2, 0), radius * i / 3.0, 12 + i * 10);
        Fx.sound(level, centre, SoundEvents.GENERIC_EXPLODE, 1.5f, 0.6f);
        Fx.sound(level, centre, SoundEvents.WARDEN_SONIC_BOOM, 1.0f, 1.0f);
        if (hits > 0) c.addProficiency(0.01f * Math.min(hits, 4));
    }

    @Override public void stop(ActivationContext c, StopReason reason) {
        ServerPlayer p = c.naturalUser();
        SeniorState s = p == null ? null : SeniorState.peek(p);
        if (s != null) s.ruptureCenter = null;
        c.setCooldown(22 * 20);
    }
}
