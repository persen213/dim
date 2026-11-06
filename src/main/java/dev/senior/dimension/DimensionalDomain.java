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
 * 11th-Dimensional Domain (ultimate): for 15 s the caster's field (radius 25, centred on the caster) shows a purple-blue
 * boundary and floating particles. While it lasts the caster's other abilities get +50% range, radius and displacement
 * distance, extra Compression targets (up to 4) and +50% barrier durability. The boosts are read through
 * {@link SeniorState#scaled} when each ability starts.
 */
final class DimensionalDomain implements SkillBehavior {
    static final int DURATION = 300;
    static final double RADIUS = 25;

    @Override public ActivationOutcome start(ActivationContext c) {
        ServerPlayer p = c.naturalUser();
        if (p == null) return ActivationOutcome.REFUSED_INVALID;
        SeniorState s = SeniorState.of(p);
        if (s.shifted || s.domain) return ActivationOutcome.REFUSED_INVALID;
        if (!c.pay(Costs.DOMAIN_CP, Costs.DOMAIN_OVERLOAD)) return ActivationOutcome.REFUSED_RESOURCE;
        s.domain = true;
        ServerLevel level = p.serverLevel();
        Vec3 origin = p.position().add(0, 0.2, 0);
        for (int i = 1; i <= 10; i++) {
            final double r = RADIUS * i / 10.0;
            final int points = 24 + i * 8;
            final boolean blue = i % 2 == 0;
            Scheduler.later(level.getServer(), i * 2, () -> Fx.ring(level, blue ? Fx.BLUE : Fx.PURPLE, origin, r, points));
        }
        Fx.burst(level, ParticleTypes.FLASH, p.position().add(0, 1, 0), 1, 0, 0);
        Fx.sound(level, p.position(), SoundEvents.BEACON_ACTIVATE, 2f, 0.5f);
        Fx.sound(level, p.position(), SoundEvents.WARDEN_SONIC_BOOM, 1.2f, 0.7f);
        return ActivationOutcome.STARTED;
    }

    @Override public boolean tick(ActivationContext c) {
        ServerPlayer p = c.naturalUser();
        SeniorState s = p == null ? null : SeniorState.peek(p);
        if (p == null || s == null || !s.domain || c.age() >= DURATION) return false;
        ServerLevel level = p.serverLevel();
        Vec3 pos = p.position();
        int age = c.age();
        if (age % 4 == 0) Fx.ring(level, age % 8 == 0 ? Fx.PURPLE : Fx.BLUE, pos.add(0, 0.2, 0), RADIUS, 64);
        // Floating geometric motes and distortion around the caster.
        for (int i = 0; i < 6; i++) {
            double a = level.random.nextDouble() * Math.PI * 2, r = 2 + level.random.nextDouble() * 14;
            Vec3 at = pos.add(Math.cos(a) * r, 0.5 + level.random.nextDouble() * 6, Math.sin(a) * r);
            Fx.burst(level, i % 3 == 0 ? ParticleTypes.END_ROD : (i % 3 == 1 ? Fx.PURPLE : Fx.BLUE), at, 1, 0.1, 0.01);
        }
        if (age % 10 == 0) Fx.burst(level, ParticleTypes.REVERSE_PORTAL, pos.add(0, 1, 0), 10, 3.0, 0.1);
        return true;
    }

    @Override public void stop(ActivationContext c, StopReason reason) {
        ServerPlayer p = c.naturalUser();
        SeniorState s = p == null ? null : SeniorState.peek(p);
        if (s != null) s.domain = false;
        if (p != null) {
            ServerLevel level = p.serverLevel();
            Fx.ring(level, Fx.WHITE, p.position().add(0, 0.2, 0), RADIUS * 0.5, 48);
            Fx.burst(level, Fx.PURPLE, p.position().add(0, 1, 0), 40, 2.0, 0.1);
            Fx.sound(level, p.position(), SoundEvents.BEACON_DEACTIVATE, 1.5f, 0.6f);
        }
        c.setCooldown(120 * 20);
    }
}
