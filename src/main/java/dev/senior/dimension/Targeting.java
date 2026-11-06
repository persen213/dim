package dev.senior.dimension;

import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Aiming and area helpers shared by the abilities. */
final class Targeting {
    private Targeting() { }

    /** The living entity the player is looking at within {@code range} blocks (blocked by solid blocks), or null. */
    static @Nullable LivingEntity entity(ServerPlayer p, double range) {
        Vec3 from = p.getEyePosition(), look = p.getLookAngle(), to = from.add(look.scale(range));
        ServerLevel level = p.serverLevel();
        BlockHitResult block = level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        double limitSq = block.getType() == HitResult.Type.MISS ? range * range : block.getLocation().distanceToSqr(from);
        LivingEntity best = null;
        double bestSq = limitSq;
        AABB search = p.getBoundingBox().expandTowards(look.scale(range)).inflate(1.0);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, search, x -> x != p && x.isAlive() && !x.isSpectator())) {
            AABB box = e.getBoundingBox().inflate(0.3);
            double dsq;
            if (box.contains(from)) {
                dsq = 0;
            } else {
                Optional<Vec3> hit = box.clip(from, to);
                if (hit.isEmpty()) continue;
                dsq = from.distanceToSqr(hit.get());
            }
            if (dsq < bestSq) { bestSq = dsq; best = e; }
        }
        return best;
    }

    /** The point the player is aiming at within {@code range}; aiming into open air drops down to the ground below. */
    static Vec3 point(ServerPlayer p, double range) {
        Vec3 from = p.getEyePosition(), to = from.add(p.getLookAngle().scale(range));
        ServerLevel level = p.serverLevel();
        BlockHitResult hit = level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        if (hit.getType() != HitResult.Type.MISS) return hit.getLocation();
        BlockHitResult down = level.clip(new ClipContext(to, to.add(0, -24, 0), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        return down.getType() != HitResult.Type.MISS ? down.getLocation() : to;
    }

    static Vec3 centre(Entity e) { return e.position().add(0, e.getBbHeight() / 2.0, 0); }

    /** Living entities whose centre is within {@code radius} of {@code at} (excluding {@code exclude}). */
    static List<LivingEntity> within(ServerLevel level, Vec3 at, double radius, @Nullable Entity exclude) {
        return level.getEntitiesOfClass(LivingEntity.class, new AABB(at, at).inflate(radius),
                e -> e != exclude && e.isAlive() && !e.isSpectator() && centre(e).distanceToSqr(at) <= radius * radius);
    }

    /** Bosses and other very strong entities: they resist displacement and most movement control. */
    static boolean bossLike(LivingEntity e) {
        return e instanceof EnderDragon || e instanceof WitherBoss || e instanceof Warden || e.getMaxHealth() >= 200f;
    }

    /** How strongly movement control works on this entity (1 = fully). */
    static double controlFactor(LivingEntity e) {
        if (bossLike(e)) return 0.25;
        return e.getMaxHealth() >= 100f ? 0.5 : 1.0;
    }

    /**
     * Finds a standing spot near column (x, z) around height y: loaded chunk, inside the world border, solid floor,
     * room for the entity's body, and no lava / fire / magma / cactus / berry bush / powder snow at or under the feet.
     */
    static @Nullable Vec3 safeSpot(ServerLevel level, Entity e, double x, double y, double z) {
        double cx = Math.floor(x) + 0.5, cz = Math.floor(z) + 0.5;
        BlockPos column = BlockPos.containing(cx, y, cz);
        if (!level.hasChunkAt(column) || !level.getWorldBorder().isWithinBounds(column)) return null;
        for (int dy = 4; dy >= -6; dy--) {
            BlockPos feet = BlockPos.containing(cx, y + dy, cz);
            if (feet.getY() <= level.getMinBuildHeight() + 1 || feet.getY() >= level.getMaxBuildHeight() - 3) continue;
            BlockPos below = feet.below();
            if (!level.getBlockState(below).isFaceSturdy(level, below, Direction.UP)) continue;
            if (hazard(level, feet) || hazard(level, below) || hazard(level, feet.above())) continue;
            Vec3 spot = new Vec3(cx, feet.getY(), cz);
            AABB moved = e.getBoundingBox().move(spot.x - e.getX(), spot.y - e.getY(), spot.z - e.getZ());
            if (!level.noCollision(e, moved)) continue;
            return spot;
        }
        return null;
    }

    private static boolean hazard(ServerLevel level, BlockPos pos) {
        BlockState s = level.getBlockState(pos);
        return s.getFluidState().is(FluidTags.LAVA) || s.is(BlockTags.FIRE) || s.is(Blocks.MAGMA_BLOCK) || s.is(Blocks.CACTUS)
                || s.is(Blocks.SWEET_BERRY_BUSH) || s.is(Blocks.POWDER_SNOW) || s.is(Blocks.WITHER_ROSE);
    }
}
