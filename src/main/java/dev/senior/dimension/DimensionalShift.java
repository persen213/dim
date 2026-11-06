package dev.senior.dimension;

import io.github.pinchan4273.reacademycraft.api.ActivationOutcome;
import io.github.pinchan4273.reacademycraft.api.StopReason;
import io.github.pinchan4273.reacademycraft.api.mc.ActivationContext;
import io.github.pinchan4273.reacademycraft.api.mc.SkillBehavior;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.Vec3;

/**
 * Dimensional Shift: for 4 s the caster is invisible (only the particle aura shows), cannot be hurt, and cannot attack or
 * use other Senior abilities. The blocking itself is in {@link SeniorEvents}.
 * KNOWN LIMIT: walking through solid blocks is NOT implemented. Block collision is decided on the player's own client and
 * needs client-side code that the public API does not provide.
 */
final class DimensionalShift implements SkillBehavior {
    static final int DURATION = 80;

    @Override public ActivationOutcome start(ActivationContext c) {
        ServerPlayer p = c.naturalUser();
        if (p == null) return ActivationOutcome.REFUSED_INVALID;
        SeniorState s = SeniorState.of(p);
        if (s.shifted || s.barrier) return ActivationOutcome.REFUSED_INVALID;
        if (!c.pay(Costs.SHIFT_CP, Costs.SHIFT_OVERLOAD)) return ActivationOutcome.REFUSED_RESOURCE;
        s.shifted = true;
        p.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, DURATION + 5, 0, false, false, false));
        ServerLevel level = p.serverLevel();
        Vec3 mid = p.position().add(0, 1, 0);
        Fx.burst(level, ParticleTypes.REVERSE_PORTAL, mid, 40, 0.5, 0.15);
        Fx.ring(level, Fx.PURPLE, p.position().add(0, 0.1, 0), 1.5, 20);
        Fx.sphere(level, Fx.BLUE, mid, 1.1, 20);
        Fx.sound(level, p.position(), SoundEvents.ENDERMAN_TELEPORT, 1f, 0.6f);
        return ActivationOutcome.STARTED;
    }

    @Override public boolean tick(ActivationContext c) {
        ServerPlayer p = c.naturalUser();
        if (p == null || c.age() >= DURATION) return false;
        ServerLevel level = p.serverLevel();
        Vec3 mid = p.position().add(0, 1, 0);
        Fx.sphere(level, c.age() % 2 == 0 ? Fx.PURPLE : Fx.BLUE, mid, 1.1, 6);
        if (c.age() % 4 == 0) Fx.burst(level, ParticleTypes.REVERSE_PORTAL, mid, 3, 0.4, 0.05);
        return true;
    }

    @Override public void stop(ActivationContext c, StopReason reason) {
        ServerPlayer p = c.naturalUser();
        SeniorState s = p == null ? null : SeniorState.peek(p);
        if (s != null) s.shifted = false;
        if (p != null) {
            p.removeEffect(MobEffects.INVISIBILITY);
            ServerLevel level = p.serverLevel();
            Vec3 mid = p.position().add(0, 1, 0);
            Fx.burst(level, ParticleTypes.REVERSE_PORTAL, mid, 30, 0.5, 0.15);
            Fx.ring(level, Fx.BLUE, p.position().add(0, 0.1, 0), 1.5, 20);
            Fx.sound(level, p.position(), SoundEvents.ENDERMAN_TELEPORT, 1f, 1.2f);
        }
        c.setCooldown(15 * 20);
    }
}
