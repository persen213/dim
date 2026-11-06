package dev.senior.dimension;

import com.mojang.logging.LogUtils;
import io.github.pinchan4273.reacademycraft.api.AbilityId;
import io.github.pinchan4273.reacademycraft.api.ActivationKind;
import io.github.pinchan4273.reacademycraft.api.CategoryDefinition;
import io.github.pinchan4273.reacademycraft.api.Presentation;
import io.github.pinchan4273.reacademycraft.api.SkillDefinition;
import io.github.pinchan4273.reacademycraft.api.SkillRequirement;
import io.github.pinchan4273.reacademycraft.api.TreePosition;
import io.github.pinchan4273.reacademycraft.api.forge.RegisterAbilitiesEvent;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.slf4j.Logger;

/**
 * Senior - 11th-Dimensional Manipulation: an add-on category for Re:AcademyCraft 1.5.0.
 * Registers the category, its factor item, eight skills and their behaviours through the public API
 * (only io.github.pinchan4273.reacademycraft.api.* is used).
 */
@Mod(SeniorDim.MOD_ID)
public final class SeniorDim {
    public static final String MOD_ID = "seniordim";
    static final Logger LOGGER = LogUtils.getLogger();

    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MOD_ID);
    /** The item that gives the Senior category (used in the Ability Developer, like the built-in factors). */
    public static final RegistryObject<Item> SENIOR_FACTOR = ITEMS.register("senior_factor", () -> new Item(new Item.Properties().stacksTo(16)));

    static AbilityId id(String path) { return AbilityId.parse(MOD_ID + ":" + path); }
    static final AbilityId SENIOR = id("senior"),
            DISPLACEMENT = id("spatial_displacement"),
            COMPRESSION = id("spatial_compression"),
            BARRIER = id("dimensional_barrier"),
            SHIFT = id("dimensional_shift"),
            PERCEPTION = id("higher_dimensional_perception"),
            RUPTURE = id("dimensional_rupture"),
            COLLAPSE = id("spatial_collapse"),
            DOMAIN = id("eleventh_dimensional_domain");

    public SeniorDim() {
        var bus = FMLJavaModLoadingContext.get().getModEventBus();
        ITEMS.register(bus);
        bus.addListener(this::registerAbilities);
        bus.addListener(this::creativeTab);
    }

    private void creativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.INGREDIENTS) event.accept(SENIOR_FACTOR);
    }

    private static Presentation shown(AbilityId skill) {
        return new Presentation(MOD_ID + ".skill." + skill.path(), MOD_ID + ".skill." + skill.path() + ".desc", null);
    }

    private static SkillDefinition skill(AbilityId id, int level, List<SkillRequirement> requires, ActivationKind kind, float x, float y, @Nullable AbilityId parent) {
        return new SkillDefinition(id, SENIOR, level, requires, kind, shown(id), new TreePosition(x, y, parent));
    }

    private static SkillRequirement needs(AbilityId skill, float proficiency) { return new SkillRequirement(skill, proficiency); }

    /** Re:AcademyCraft fires this once during common setup; registration is only possible here. */
    private void registerAbilities(RegisterAbilitiesEvent event) {
        var abilities = event.registrar(MOD_ID);
        // Skills per level 1..5: 1, 2, 2, 1, 2 (= 8).
        abilities.category(new CategoryDefinition(SENIOR, id("senior_factor"), List.of(1, 2, 2, 1, 2),
                new Presentation(MOD_ID + ".ability.senior", null, null)));

        abilities.skill(skill(DISPLACEMENT, 1, List.of(), ActivationKind.INSTANT, 20, 55, null));
        abilities.skill(skill(COMPRESSION, 2, List.of(needs(DISPLACEMENT, .3f)), ActivationKind.TOGGLE, 65, 20, DISPLACEMENT));
        abilities.skill(skill(BARRIER, 2, List.of(needs(DISPLACEMENT, .3f)), ActivationKind.TOGGLE, 65, 90, DISPLACEMENT));
        abilities.skill(skill(SHIFT, 3, List.of(needs(COMPRESSION, .3f)), ActivationKind.TOGGLE, 110, 45, COMPRESSION));
        abilities.skill(skill(PERCEPTION, 3, List.of(needs(BARRIER, .3f)), ActivationKind.INSTANT, 110, 95, BARRIER));
        abilities.skill(skill(RUPTURE, 4, List.of(needs(COMPRESSION, .5f)), ActivationKind.TOGGLE, 155, 15, COMPRESSION));
        abilities.skill(skill(COLLAPSE, 5, List.of(needs(RUPTURE, .6f)), ActivationKind.TOGGLE, 205, 15, RUPTURE));
        abilities.skill(skill(DOMAIN, 5, List.of(needs(PERCEPTION, .5f), needs(BARRIER, .5f)), ActivationKind.TOGGLE, 205, 85, PERCEPTION));

        var behaviors = event.behaviors(MOD_ID);
        behaviors.behavior(DISPLACEMENT, new SpatialDisplacement());
        behaviors.behavior(COMPRESSION, new SpatialCompression());
        behaviors.behavior(BARRIER, new DimensionalBarrier());
        behaviors.behavior(SHIFT, new DimensionalShift());
        behaviors.behavior(PERCEPTION, new HigherPerception());
        behaviors.behavior(RUPTURE, new DimensionalRupture());
        behaviors.behavior(COLLAPSE, new SpatialCollapse());
        behaviors.behavior(DOMAIN, new DimensionalDomain());
    }
}
