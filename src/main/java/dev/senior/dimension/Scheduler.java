package dev.senior.dimension;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.MinecraftServer;

/** Tiny "run this N ticks from now" helper (used for expanding visual rings). Server thread only. */
final class Scheduler {
    private Scheduler() { }
    private record Task(long at, Runnable run) { }
    private static final List<Task> TASKS = new ArrayList<>();

    static void later(MinecraftServer server, int delayTicks, Runnable run) {
        TASKS.add(new Task(server.getTickCount() + Math.max(0, delayTicks), run));
    }

    static void run(MinecraftServer server) {
        if (TASKS.isEmpty()) return;
        long now = server.getTickCount();
        List<Task> due = new ArrayList<>();
        for (Task t : TASKS) if (t.at <= now) due.add(t);
        TASKS.removeAll(due);
        for (Task t : due) {
            try {
                t.run.run();
            } catch (RuntimeException e) {
                SeniorDim.LOGGER.error("A scheduled Senior effect failed", e);
            }
        }
    }

    static void clear() { TASKS.clear(); }
}
