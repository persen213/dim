package dev.senior.dimension;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/** Visual and sound helpers (vanilla particles in the purple / blue / white palette). */
final class Fx {
    private Fx() { }
    static final DustParticleOptions PURPLE = new DustParticleOptions(new Vector3f(0.62f, 0.22f, 1.0f), 1.4f);
    static final DustParticleOptions BLUE = new DustParticleOptions(new Vector3f(0.25f, 0.50f, 1.0f), 1.4f);
    static final DustParticleOptions WHITE = new DustParticleOptions(new Vector3f(0.95f, 0.90f, 1.0f), 1.2f);

    static void burst(ServerLevel level, ParticleOptions particle, Vec3 at, int count, double spread, double speed) {
        level.sendParticles(particle, at.x, at.y, at.z, count, spread, spread, spread, speed);
    }

    /** A flat horizontal ring of single particles. */
    static void ring(ServerLevel level, ParticleOptions particle, Vec3 centre, double radius, int points) {
        for (int i = 0; i < points; i++) {
            double a = Math.PI * 2 * i / points;
            level.sendParticles(particle, centre.x + Math.cos(a) * radius, centre.y, centre.z + Math.sin(a) * radius, 1, 0, 0, 0, 0);
        }
    }

    /** Random points on the surface of a sphere. */
    static void sphere(ServerLevel level, ParticleOptions particle, Vec3 centre, double radius, int count) {
        for (int i = 0; i < count; i++) {
            double x = level.random.nextGaussian(), y = level.random.nextGaussian(), z = level.random.nextGaussian();
            double len = Math.sqrt(x * x + y * y + z * z);
            if (len < 1e-6) continue;
            double k = radius / len;
            level.sendParticles(particle, centre.x + x * k, centre.y + y * k, centre.z + z * k, 1, 0, 0, 0, 0);
        }
    }

    static void sound(ServerLevel level, Vec3 at, SoundEvent sound, float volume, float pitch) {
        level.playSound(null, at.x, at.y, at.z, sound, SoundSource.PLAYERS, volume, pitch);
    }

    /** A short message above the hotbar. */
    static void hint(ServerPlayer player, String text) {
        player.displayClientMessage(Component.literal(text), true);
    }
}
