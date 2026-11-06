package dev.senior.dimension;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/** Per-player runtime state of the Senior abilities. Server thread only; never saved (every ability is short-lived). */
final class SeniorState {
    private static final Map<UUID, SeniorState> ALL = new HashMap<>();

    boolean shifted, barrier, domain;
    float barrierHp;
    final List<UUID> compressed = new ArrayList<>();
    @Nullable Vec3 compressCenter;
    @Nullable Vec3 ruptureCenter;
    @Nullable Vec3 collapseCenter;
    final Set<UUID> collapseTargets = new HashSet<>();

    static SeniorState of(Entity player) { return ALL.computeIfAbsent(player.getUUID(), id -> new SeniorState()); }
    static @Nullable SeniorState peek(Entity player) { return ALL.get(player.getUUID()); }
    static void forget(UUID id) { ALL.remove(id); }
    static void clearAll() { ALL.clear(); }

    static boolean shifted(Entity player) { var s = peek(player); return s != null && s.shifted; }
    static boolean domain(Entity player) { var s = peek(player); return s != null && s.domain; }
    /** {@code base}, or {@code base * domainMultiplier} while the player's 11th-Dimensional Domain is active. */
    static double scaled(Entity player, double base, double domainMultiplier) { return domain(player) ? base * domainMultiplier : base; }
}
