package dev.senior.dimension;

import io.github.pinchan4273.reacademycraft.api.ActivationOutcome;
import io.github.pinchan4273.reacademycraft.api.StopReason;
import io.github.pinchan4273.reacademycraft.api.mc.AcademySafety;
import io.github.pinchan4273.reacademycraft.api.mc.ActivationContext;
import io.github.pinchan4273.reacademycraft.api.mc.SkillBehavior;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/** Spatial Compression: 4 s of pulling, heavy slowing and gradual damage on one target (more in the Domain), then a knock-back. */
final class SpatialCompression implements SkillBehavior {
    static final int DURATION = 80;

    @Override public ActivationOutcome start(ActivationContext c) {
        ServerPlayer p = c.naturalUser();
        if (p == null || SeniorState.shifted(p)) return ActivationOutcome.REFUSED_INVALID;
        var actor = c.actor();
        LivingEntity main = Targeting.entity(p, SeniorState.scaled(p, 18, 1.5));
        if (main == null) { Fx.hint(p, "No target in range"); return ActivationOutcome.REFUSED_INVALID; }
        if (!AcademySafety.mayAttack(actor, main, SeniorDim.COMPRESSION)) return ActivationOutcome.REFUSED_PERMISSION;
        List<LivingEntity> chosen = new ArrayList<>();
        chosen.add(main);
        if (SeniorState.domain(p)) {
            for (LivingEntity e : Targeting.within(p.serverLevel(), main.position(), 8, p)) {
                if (chosen.size() >= 4) break;
                if (e != main && AcademySafety.mayAttack(actor, e, SeniorDim.COMPRESSION)) chosen.add(e);
            }
        }
        if (!c.pay(Costs.COMPRESSION_CP, Costs.COMPRESSION_OVERLOAD)) return ActivationOutcome.REFUSED_RESOURCE;
        SeniorState s = SeniorState.of(p);
        s.compressed.clear();
        for (LivingEntity e : chosen) s.compressed.add(e.getUUID());
        s.compressCenter = main.position();
        Fx.sound(p.serverLevel(), main.position(), SoundEvents.RESPAWN_ANCHOR_CHARGE, 1f, 0.6f);
        return ActivationOutcome.STARTED;
    }

    @Override public boolean tick(ActivationContext c) {
        ServerPlayer p = c.naturalUser();
        SeniorState s = p == null ? null : SeniorState.peek(p);
        if (p == null || s == null || s.compressCenter == null || c.age() >= DURATION) return false;
        ServerLevel level = p.serverLevel();
        var actor = c.actor();
        Vec3 centre = s.compressCenter;
        int age = c.age();
        float damage = 1.5f + 2.0f * c.proficiency();

        Iterator<java.util.UUID> it = s.compressed.iterator();
        while (it.hasNext()) {
            Entity found = level.getEntity(it.next());
            if (!(found instanceof LivingEntity t) || !t.isAlive()) { it.remove(); continue; }
            double factor = Targeting.controlFactor(t);
            Vec3 toCentre = centre.subtract(t.position());
            Vec3 flat = new Vec3(toCentre.x, 0, toCentre.z);
            double dist = flat.length();
            Vec3 m = t.getDeltaMovement();
            if (dist > 0.6) {
                Vec3 pull = flat.normalize().scale(Math.min(0.3, dist * 0.12) * factor);
                t.setDeltaMovement(m.x * 0.4 + pull.x, m.y, m.z * 0.4 + pull.z);
            } else {
                t.setDeltaMovement(m.x * 0.3, m.y, m.z * 0.3);
            }
            t.hurtMarked = true;
            t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 6, factor < 1.0 ? 1 : 3, false, false, false));
            if (age % 10 == 0 && !AcademySafety.attack(actor, t, SeniorDim.COMPRESSION, damage)) { it.remove(); continue; }
            if (age % 2 == 0) {
                Vec3 mid = Targeting.centre(t);
                Fx.burst(level, Fx.PURPLE, mid, 3, 0.5, 0);
                Fx.burst(level, ParticleTypes.REVERSE_PORTAL, mid, 2, 0.6, 0.05);
            }
        }
        if (age % 2 == 0) {
            double radius = 3.0 - 2.2 * age / DURATION;
            Fx.ring(level, age % 4 == 0 ? Fx.BLUE : Fx.PURPLE, centre.add(0, 0.1, 0), radius, 16);
        }
        if (age % 10 == 0) c.addProficiency(0.002f);
        return !s.compressed.isEmpty();
    }

    @Override public void stop(ActivationContext c, StopReason reason) {
        ServerPlayer p = c.naturalUser();
        SeniorState s = p == null ? null : SeniorState.peek(p);
        if (p != null && s != null && s.compressCenter != null) {
            ServerLevel level = p.serverLevel();
            Vec3 centre = s.compressCenter;
            boolean release = reason == StopReason.FINISHED || reason == StopReason.SWITCHED_OFF;
            for (java.util.UUID id : s.compressed) {
                if (!release || !(level.getEntity(id) instanceof LivingEntity t) || !t.isAlive()) continue;
                Vec3 away = new Vec3(t.getX() - centre.x, 0, t.getZ() - centre.z);
                away = away.lengthSqr() < 0.01 ? new Vec3(level.random.nextDouble() - 0.5, 0, level.random.nextDouble() - 0.5).normalize() : away.normalize();
                double k = 1.1 * Targeting.controlFactor(t);
                t.setDeltaMovement(away.x * k, 0.45, away.z * k);
                t.hurtMarked = true;
            }
            if (release) {
                Fx.burst(level, ParticleTypes.FLASH, centre.add(0, 1, 0), 1, 0, 0);
                Fx.burst(level, Fx.WHITE, centre.add(0, 1, 0), 30, 0.8, 0.1);
                Fx.ring(level, Fx.PURPLE, centre.add(0, 0.2, 0), 3.0, 28);
                Fx.sound(level, centre, SoundEvents.WARDEN_SONIC_BOOM, 0.8f, 1.4f);
            }
        }
        if (s != null) { s.compressed.clear(); s.compressCenter = null; }
        c.setCooldown(18 * 20);
    }
}
