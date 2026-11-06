package dev.senior.dimension;

/**
 * Starting energy costs (CP, overload) for each ability. These are UNTUNED first guesses: the spec only says
 * "high / very high / extremely high". Change a number here, rebuild, and re-test. Re:AcademyCraft also lets server
 * owners scale every skill's cost/damage in config/academy-addon-seniordim.toml without rebuilding.
 */
final class Costs {
    private Costs() { }
    static final float DISPLACEMENT_CP = 250, DISPLACEMENT_OVERLOAD = 60;
    static final float COMPRESSION_CP = 400, COMPRESSION_OVERLOAD = 80;
    static final float BARRIER_CP = 450, BARRIER_OVERLOAD = 90;
    static final float SHIFT_CP = 500, SHIFT_OVERLOAD = 100;
    static final float PERCEPTION_CP = 200, PERCEPTION_OVERLOAD = 40;
    static final float RUPTURE_CP = 900, RUPTURE_OVERLOAD = 180;
    static final float COLLAPSE_CP = 2000, COLLAPSE_OVERLOAD = 400;
    static final float DOMAIN_CP = 3000, DOMAIN_OVERLOAD = 600;
}
