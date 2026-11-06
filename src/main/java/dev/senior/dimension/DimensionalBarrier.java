package dev.senior.dimension;

import io.github.pinchan4273.reacademycraft.api.ActivationOutcome;
import io.github.pinchan4273.reacademycraft.api.StopReason;
import io.github.pinchan4273.reacademycraft.api.mc.ActivationContext;
import io.github.pinchan4273.reacademycraft.api.mc.SkillBehavior;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.phys.Vec3;

/**
 * Dimensional Barrier: for 6 s a spherical boundary takes 70% of every hit against the caster until its durability is
 * used up (hits of 30+ damage wear it down twice as fast). The damage handling itself is in {@link SeniorEvents}.
 */
final class DimensionalBarrier implements SkillBehavior {
    static final int DURATION = 120;

    @Override public ActivationOutcome start(ActivationContext c) {
        ServerPlayer p = c.naturalUser();
        if (p == null) return ActivationOutcome.REFUSED_INVALID;
        SeniorState s = SeniorState.of(p);
        if (s.shifted || s.barrier) return ActivationOutcome.REFUSED_INVALID;
        if (!c.pay(Costs.BARRIER_CP, Costs.BARRIER_OVERLOAD)) return ActivationOutcome.REFUSED_RESOURCE;
        s.barrier = true;
        s.barrierHp = (float) SeniorState.scaled(p, 60, 1.5);
        ServerLevel level = p.serverLevel();
        Vec3 mid = p.position().add(0, 1, 0);
        Fx.sphere(level, Fx.BLUE, mid, 2.0, 40);
        Fx.sound(level, mid, SoundEvents.BEACON_ACTIVATE, 0.8f, 1.6f);
        return ActivationOutcome.STARTED;
    }

    @Override public boolean tick(ActivationContext c) {
        ServerPlayer p = c.naturalUser();
        SeniorState s = p == null ? null : SeniorState.peek(p);
        if (p == null || s == null || !s.barrier || c.age() >= DURATION) return false;
        ServerLevel level = p.serverLevel();
        Vec3 mid = p.position().add(0, 1, 0);
        Fx.sphere(level, c.age() % 2 == 0 ? Fx.PURPLE : Fx.BLUE, mid, 2.0, 8);
        if (c.age() % 8 == 0) Fx.sphere(level, Fx.WHITE, mid, 2.0, 4);
        return true;
    }

    @Override public void stop(ActivationContext c, StopReason reason) {
        ServerPlayer p = c.naturalUser();
        SeniorState s = p == null ? null : SeniorState.peek(p);
        boolean broken = s != null && s.barrierHp <= 0.01f;
        if (s != null) { s.barrier = false; s.barrierHp = 0; }
        if (p != null) {
            ServerLevel level = p.serverLevel();
            Vec3 mid = p.position().add(0, 1, 0);
            if (broken) {
                // The sphere fractures into fragments.
                Fx.burst(level, ParticleTypes.END_ROD, mid, 40, 1.4, 0.15);
                Fx.sphere(level, Fx.PURPLE, mid, 2.0, 40);
                Fx.sphere(level, Fx.WHITE, mid, 2.0, 24);
                Fx.sound(level, mid, SoundEvents.SHIELD_BREAK, 1f, 0.6f);
            } else {
                Fx.sphere(level, Fx.BLUE, mid, 2.0, 24);
                Fx.sound(level, mid, SoundEvents.BEACON_DEACTIVATE, 0.6f, 1.6f);
            }
        }
        c.setCooldown(28 * 20);
    }
}
