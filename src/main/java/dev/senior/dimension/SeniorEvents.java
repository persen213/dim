package dev.senior.dimension;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.ServerLifecycleHooks;

/** Forge-event side of Dimensional Shift (untouchable, cannot attack) and Dimensional Barrier (damage absorption). */
@Mod.EventBusSubscriber(modid = SeniorDim.MOD_ID)
public final class SeniorEvents {
    private SeniorEvents() { }

    /** Shifted players take no damage (except the void / kill-style damage) and deal none. */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onAttacked(LivingAttackEvent event) {
        if (event.getEntity().level().isClientSide()) return;
        if (event.getEntity() instanceof ServerPlayer victim && SeniorState.shifted(victim)
                && !event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            event.setCanceled(true);
            return;
        }
        if (event.getSource().getEntity() instanceof ServerPlayer attacker && SeniorState.shifted(attacker)) event.setCanceled(true);
    }

    /** Melee attacks by a shifted player are cancelled before they start. */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onPlayerAttack(AttackEntityEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && SeniorState.shifted(player)) event.setCanceled(true);
    }

    /** The barrier takes 70% of each hit; hits of 30+ damage wear it down twice as fast. */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        SeniorState s = SeniorState.peek(player);
        if (s == null || !s.barrier || s.barrierHp <= 0f) return;
        if (event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return;
        float amount = event.getAmount();
        float absorbed = amount * 0.7f;
        float wear = absorbed * (amount >= 30f ? 2f : 1f);
        if (wear > s.barrierHp) {
            absorbed *= s.barrierHp / wear;
            wear = s.barrierHp;
        }
        s.barrierHp -= wear;
        if (s.barrierHp < 0.01f) s.barrierHp = 0f; // the barrier behaviour sees this on its next tick and collapses
        event.setAmount(amount - absorbed);
        var level = player.serverLevel();
        var mid = player.position().add(0, 1, 0);
        Fx.sphere(level, Fx.WHITE, mid, 2.0, 14);
        Fx.sphere(level, Fx.PURPLE, mid, 2.0, 10);
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) Scheduler.run(server);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        SeniorState.forget(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        SeniorState.clearAll();
        Scheduler.clear();
    }
}
