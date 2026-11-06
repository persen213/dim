package dev.senior.dimension;

import io.github.pinchan4273.reacademycraft.api.ActivationOutcome;
import io.github.pinchan4273.reacademycraft.api.mc.AcademySafety;
import io.github.pinchan4273.reacademycraft.api.mc.ActivationContext;
import io.github.pinchan4273.reacademycraft.api.mc.SkillBehavior;
import javax.annotation.Nullable;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/** Spatial Displacement: relocate one aimed-at enemy 8-12 blocks to a checked, safe spot. */
final class SpatialDisplacement implements SkillBehavior {
    @Override public ActivationOutcome start(ActivationContext c) {
        ServerPlayer p = c.naturalUser();
        if (p == null || SeniorState.shifted(p)) return ActivationOutcome.REFUSED_INVALID;
        var actor = c.actor();
        LivingEntity target = Targeting.entity(p, SeniorState.scaled(p, 20, 1.5));
        if (target == null) { Fx.hint(p, "No target in range"); return ActivationOutcome.REFUSED_INVALID; }
        if (Targeting.bossLike(target)) { Fx.hint(p, "The target resists displacement"); return ActivationOutcome.REFUSED_INVALID; }
        Vec3 destination = destination(p, target);
        if (destination == null) { Fx.hint(p, "No safe place to send the target"); return ActivationOutcome.REFUSED_INVALID; }
        Vec3 checked = AcademySafety.checkTeleport(actor, target, destination, SeniorDim.DISPLACEMENT);
        if (checked == null) return ActivationOutcome.REFUSED_PERMISSION;
        if (!c.pay(Costs.DISPLACEMENT_CP, Costs.DISPLACEMENT_OVERLOAD)) return ActivationOutcome.REFUSED_RESOURCE;

        ServerLevel level = actor.level();
        Vec3 from = Targeting.centre(target);
        Fx.ring(level, Fx.PURPLE, target.position().add(0, 0.1, 0), 1.2, 18);
        Fx.sphere(level, Fx.BLUE, from, 1.0, 16);
        Fx.burst(level, ParticleTypes.REVERSE_PORTAL, from, 30, 0.4, 0.1);
        Fx.sound(level, from, SoundEvents.ENDERMAN_TELEPORT, 1f, 0.7f);
        if (AcademySafety.teleport(actor, target, checked, SeniorDim.DISPLACEMENT)) {
            Vec3 to = Targeting.centre(target);
            Fx.ring(level, Fx.BLUE, target.position().add(0, 0.1, 0), 1.0, 18);
            Fx.burst(level, Fx.PURPLE, to, 20, 0.4, 0.05);
            Fx.burst(level, ParticleTypes.END_ROD, to, 12, 0.5, 0.08);
            Fx.sound(level, to, SoundEvents.ENDERMAN_TELEPORT, 1f, 1.1f);
        }
        c.addProficiency(0.01f);
        c.setCooldown(16 * 20);
        return ActivationOutcome.PERFORMED;
    }

    /** A random safe spot 8-12 blocks (x1.5 in the Domain) from the target's current position. */
    private static @Nullable Vec3 destination(ServerPlayer p, LivingEntity target) {
        ServerLevel level = p.serverLevel();
        double min = SeniorState.scaled(p, 8, 1.5), max = SeniorState.scaled(p, 12, 1.5);
        for (int i = 0; i < 24; i++) {
            double angle = level.random.nextDouble() * Math.PI * 2;
            double distance = min + level.random.nextDouble() * (max - min);
            Vec3 spot = Targeting.safeSpot(level, target, target.getX() + Math.cos(angle) * distance, target.getY(), target.getZ() + Math.sin(angle) * distance);
            if (spot != null) return spot;
        }
        return null;
    }
}
