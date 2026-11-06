package dev.senior.dimension;

import io.github.pinchan4273.reacademycraft.api.ActivationOutcome;
import io.github.pinchan4273.reacademycraft.api.TargetRelation;
import io.github.pinchan4273.reacademycraft.api.mc.AcademySafety;
import io.github.pinchan4273.reacademycraft.api.mc.ActivationContext;
import io.github.pinchan4273.reacademycraft.api.mc.SkillBehavior;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Higher-Dimensional Perception: an expanding pulse (35 blocks) that gives players and hostile mobs the vanilla Glowing
 * outline for 5 s (visible through walls) and shows the nearest ones' direction and distance above the hotbar.
 * NOTE: vanilla Glowing is visible to everybody, not only to the caster.
 */
final class HigherPerception implements SkillBehavior {
    private static final String[] DIRECTIONS = {"ahead", "ahead-right", "right", "behind-right", "behind", "behind-left", "left", "ahead-left"};

    @Override public ActivationOutcome start(ActivationContext c) {
        ServerPlayer p = c.naturalUser();
        if (p == null || SeniorState.shifted(p)) return ActivationOutcome.REFUSED_INVALID;
        if (!c.pay(Costs.PERCEPTION_CP, Costs.PERCEPTION_OVERLOAD)) return ActivationOutcome.REFUSED_RESOURCE;
        var actor = c.actor();
        ServerLevel level = p.serverLevel();
        double radius = SeniorState.scaled(p, 35, 1.5);

        List<LivingEntity> found = new ArrayList<>();
        for (LivingEntity e : Targeting.within(level, p.position(), radius, p)) {
            if (!(e instanceof Player || e instanceof Enemy)) continue;
            TargetRelation relation = AcademySafety.relation(actor, e);
            if (relation == TargetRelation.ALLY || relation == TargetRelation.SELF || relation == TargetRelation.NONE) continue;
            found.add(e);
        }
        for (LivingEntity e : found) {
            e.addEffect(new MobEffectInstance(MobEffects.GLOWING, 100, 0, false, false, false));
            Fx.burst(level, Fx.PURPLE, Targeting.centre(e), 6, 0.4, 0);
            Fx.burst(level, Fx.BLUE, Targeting.centre(e), 4, 0.5, 0);
        }

        // The expanding pulse: 8 rings out to the full radius over 16 ticks.
        Vec3 origin = p.position().add(0, 0.2, 0);
        for (int i = 1; i <= 8; i++) {
            final double r = radius * i / 8.0;
            final int points = 24 + i * 6;
            final boolean blue = i % 2 == 0;
            Scheduler.later(level.getServer(), i * 2, () -> Fx.ring(level, blue ? Fx.BLUE : Fx.PURPLE, origin, r, points));
        }
        Fx.sound(level, p.position(), SoundEvents.AMETHYST_BLOCK_CHIME, 1.5f, 0.5f);

        if (found.isEmpty()) {
            Fx.hint(p, "Perception: nothing detected within " + (int) radius + " blocks");
        } else {
            found.sort(Comparator.comparingDouble(e -> e.distanceToSqr(p)));
            StringBuilder text = new StringBuilder("Perception: " + found.size() + " detected | ");
            for (int i = 0; i < Math.min(3, found.size()); i++) {
                if (i > 0) text.append(" | ");
                text.append(describe(p, found.get(i)));
            }
            Fx.hint(p, text.toString());
        }
        c.addProficiency(0.004f);
        c.setCooldown(20 * 20);
        return ActivationOutcome.PERFORMED;
    }

    private static String describe(ServerPlayer p, LivingEntity e) {
        Vec3 look = p.getLookAngle();
        double lx = look.x, lz = look.z, ll = Math.hypot(lx, lz);
        if (ll < 1e-4) { lx = 0; lz = 1; } else { lx /= ll; lz /= ll; }
        Vec3 to = e.position().subtract(p.position());
        double tl = Math.hypot(to.x, to.z), tx = tl < 1e-4 ? lx : to.x / tl, tz = tl < 1e-4 ? lz : to.z / tl;
        double angle = Math.toDegrees(Math.atan2(lx * tz - lz * tx, lx * tx + lz * tz));
        int index = ((int) Math.round(angle / 45.0) % 8 + 8) % 8;
        String vertical = to.y > 3 ? ", above" : to.y < -3 ? ", below" : "";
        return (int) Math.round(to.length()) + "m " + DIRECTIONS[index] + vertical;
    }
}
