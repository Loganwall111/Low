package net.mcsm.extras;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

/** Places the new converted NBT blueprints through Minecraft's own
 * StructureTemplateManager instead of the broken legacy .schematic loader. */
public final class McsmTemplateSummoner {
    public static final String MODID = "dabywitherstormmod";
    public static final String SKY_CITY = "sky_city";
    public static final String BEACONTOWN = "beacontown";

    private McsmTemplateSummoner() {}

    public static String[] keys() {
        return new String[] { BEACONTOWN, SKY_CITY, "world", "all" };
    }

    public static int summon(ServerPlayer player, String key) {
        if (player == null || player.level() == null) {
            return 0;
        }
        ServerLevel level = (ServerLevel) player.level();
        BlockPos anchor = player.blockPosition();
        return summon(level, anchor, key);
    }

    public static int summon(ServerLevel level, BlockPos anchor, String key) {
        if (level == null || anchor == null || key == null) {
            return 0;
        }
        String k = key.trim().toLowerCase(java.util.Locale.ROOT);
        if ("world".equals(k) || "all".equals(k)) {
            int n = 0;
            n += placeCentered(level, BEACONTOWN, anchor);
            // Sky City belongs high above/near the spawn story world, not
            // replacing Beacon Town at ground level.
            n += placeCentered(level, SKY_CITY, anchor.offset(0, 4200, 900));
            return n;
        }
        if ("beacon_town".equals(k)) {
            k = BEACONTOWN;
        }
        if ("skycity".equals(k) || "sky_city".equals(k) || "skyland".equals(k)) {
            k = SKY_CITY;
        }
        return placeCentered(level, k, anchor);
    }

    public static int placeCentered(ServerLevel level, String name, BlockPos center) {
        try {
            Identifier id = Identifier.fromNamespaceAndPath(MODID, name);
            StructureTemplate template = (StructureTemplate) level.getStructureManager().get(id).orElse(null);
            if (template == null) {
                return 0;
            }
            Vec3i size = template.getSize();
            BlockPos origin = new BlockPos(center.getX() - size.getX() / 2,
                    center.getY(), center.getZ() - size.getZ() / 2);
            template.placeInWorld(level, origin, origin, new StructurePlaceSettings(), level.getRandom(), 2);
            return 1;
        } catch (Throwable t) {
            return 0;
        }
    }
}
