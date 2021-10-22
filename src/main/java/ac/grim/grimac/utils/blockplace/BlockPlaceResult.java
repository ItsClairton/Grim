package ac.grim.grimac.utils.blockplace;

import ac.grim.grimac.utils.anticheat.Version;
import ac.grim.grimac.utils.blockdata.WrappedBlockData;
import ac.grim.grimac.utils.blockdata.types.WrappedBlockDataValue;
import ac.grim.grimac.utils.blockdata.types.WrappedDoor;
import ac.grim.grimac.utils.blockdata.types.WrappedSlab;
import ac.grim.grimac.utils.blockdata.types.WrappedSnow;
import ac.grim.grimac.utils.blockstate.BaseBlockState;
import ac.grim.grimac.utils.blockstate.FlatBlockState;
import ac.grim.grimac.utils.blockstate.helper.BlockFaceHelper;
import ac.grim.grimac.utils.blockstate.helper.BlockStateHelper;
import ac.grim.grimac.utils.collisions.CollisionData;
import ac.grim.grimac.utils.collisions.datatypes.CollisionBox;
import ac.grim.grimac.utils.nmsImplementations.Materials;
import ac.grim.grimac.utils.nmsImplementations.XMaterial;
import io.github.retrooper.packetevents.utils.player.Direction;
import io.github.retrooper.packetevents.utils.vector.Vector3i;
import org.bukkit.Axis;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.type.*;
import org.bukkit.util.Vector;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public enum BlockPlaceResult {

    // If the block only has directional data
    // TODO: Add skulls to this
    ANVIL((player, place) -> {
        if (Version.isFlat()) {
            Directional data = (Directional) place.getMaterial().createBlockData();
            data.setFacing(BlockFaceHelper.getClockWise(place.getPlayerFacing()));
            place.set(new FlatBlockState(data));
        }
    }, XMaterial.ANVIL.parseMaterial(), XMaterial.CHIPPED_ANVIL.parseMaterial(), XMaterial.DAMAGED_ANVIL.parseMaterial()),

    // The client only predicts one of the individual bed blocks, interestingly
    BED((player, place) -> {
        BlockFace facing = place.getPlayerFacing();
        if (place.isBlockFaceOpen(facing)) {
            place.set(place.getMaterial());
        }
    }, Arrays.stream(Material.values()).filter(mat -> mat.name().contains("BED") && !mat.name().contains("ROCK")).toArray(Material[]::new)),

    SNOW((player, place) -> {
        Vector3i against = place.getPlacedAgainstBlockLocation();
        WrappedBlockDataValue blockState = place.getPlacedAgainstData();
        int layers = 0;
        if (blockState instanceof WrappedSnow) {
            layers = ((WrappedSnow) blockState).getLayers() + 1; // convert to bukkit indexing at 1
        }

        BaseBlockState below = place.getBelowState();
        if (!Materials.checkFlag(below.getMaterial(), Materials.ICE_BLOCKS) && below.getMaterial() != Material.BARRIER) {
            if (below.getMaterial() != Material.HONEY_BLOCK && below.getMaterial() != Material.SOUL_SAND) {
                if (place.isFullFace(BlockFace.DOWN)) { // Vanilla also checks for 8 layers of snow but that's redundant...
                    Snow snow = (Snow) Material.SNOW.createBlockData();
                    snow.setLayers(layers + 1);
                    place.set(against, new FlatBlockState(snow));
                }
            } else { // Honey and soul sand are exempt from this full face check
                Snow snow = (Snow) Material.SNOW.createBlockData();
                snow.setLayers(layers + 1);
                place.set(against, new FlatBlockState(snow));
            }
        }

    }, XMaterial.SNOW.parseMaterial()),

    SLAB((player, place) -> {
        Vector clickedPos = place.getClickedLocation();
        Slab slabData = (Slab) place.getMaterial().createBlockData();
        WrappedBlockDataValue existing = place.getPlacedAgainstData();

        boolean clickedTop = clickedPos.getY() > 0.5;

        if (existing instanceof WrappedSlab && place.isFaceVertical()) {
            slabData.setType(Slab.Type.DOUBLE);
            place.set(place.getPlacedAgainstBlockLocation(), new FlatBlockState(slabData));
        } else {
            slabData.setType(clickedTop ? Slab.Type.TOP : Slab.Type.BOTTOM);
            place.set(new FlatBlockState(slabData));
        }

    }, Arrays.stream(Material.values()).filter(mat -> (mat.name().contains("_SLAB") || mat.name().contains("STEP"))
            && !mat.name().contains("DOUBLE")).toArray(Material[]::new)),

    STAIRS((player, place) -> {
        BlockFace direction = place.getBlockFace();
        Stairs stair = (Stairs) place.getMaterial().createBlockData();
        stair.setFacing(place.getPlayerFacing());

        Bisected.Half half = (direction != BlockFace.DOWN && (direction == BlockFace.UP || place.getClickedLocation().getY() < 0.5D)) ? Bisected.Half.BOTTOM : Bisected.Half.TOP;
        stair.setHalf(half);
        place.set(stair);
    }, Arrays.stream(Material.values()).filter(mat -> mat.name().endsWith("_STAIRS"))
            .toArray(Material[]::new)),

    END_ROD((player, place) -> {
        Directional endRod = (Directional) place.getMaterial().createBlockData();
        endRod.setFacing(place.getBlockFace());
        place.set(endRod);
    }, XMaterial.END_ROD.parseMaterial(), XMaterial.LIGHTNING_ROD.parseMaterial()),

    LADDER((player, place) -> {
        // Horizontal ladders aren't a thing
        if (place.isFaceVertical()) return;
        if (!place.isFullFace(place.getBlockFace())) return;

        Directional ladder = (Directional) place.getMaterial().createBlockData();
        ladder.setFacing(place.getBlockFace());
        place.set(ladder);
    }, XMaterial.LADDER.parseMaterial()),

    FARM_BLOCK((player, place) -> {
        // I need brightness to know whether this block place was successful
        // I also need heightmaps
        // Probably just mark this as a desync'd block and ignore medium sized offsets until it is resync'd
        place.set(place.getMaterial());
        // What we also need to check:
        // BlockState blockstate = p_53273_.getBlockState(p_53274_.above());
        // return !blockstate.getMaterial().isSolid() || blockstate.getBlock() instanceof FenceGateBlock || blockstate.getBlock() instanceof MovingPistonBlock;
    }, XMaterial.FARMLAND.parseMaterial()),

    // 1.13+ only blocks from here below!  No need to write everything twice
    AMETHYST_CLUSTER((player, place) -> {
        AmethystCluster amethyst = (AmethystCluster) place.getMaterial().createBlockData();
        amethyst.setFacing(place.getBlockFace());
        if (place.isFullFace(place.getBlockFace().getOppositeFace())) place.set(amethyst);
    }, XMaterial.AMETHYST_CLUSTER.parseMaterial()),

    BAMBOO((player, place) -> {
        Vector3i clicked = place.getPlacedAgainstBlockLocation();
        if (player.compensatedWorld.getFluidLevelAt(clicked.getX(), clicked.getY(), clicked.getZ()) > 0) return;

        BaseBlockState below = place.getBelowState();
        if (Tag.BAMBOO_PLANTABLE_ON.isTagged(below.getMaterial())) {
            if (below.getMaterial() == Material.BAMBOO_SAPLING || below.getMaterial() == Material.BAMBOO) {
                place.set(Material.BAMBOO);
            } else {
                BaseBlockState above = place.getBelowState();
                if (above.getMaterial() == Material.BAMBOO_SAPLING || above.getMaterial() == Material.BAMBOO) {
                    place.set(Material.BAMBOO);
                } else {
                    place.set(Material.BAMBOO_SAPLING);
                }
            }
        }
    }, XMaterial.BAMBOO.parseMaterial(), XMaterial.BAMBOO_SAPLING.parseMaterial()),

    BELL((player, place) -> {
        Direction direction = place.getDirection();
        Bell bell = (Bell) place.getMaterial().createBlockData();

        boolean canSurvive = !Materials.checkFlag(place.getPlacedAgainstMaterial(), Materials.GATE);
        // This is exempt from being able to place on
        if (!canSurvive) return;

        if (place.isFaceVertical()) {
            if (direction == Direction.DOWN) {
                bell.setAttachment(Bell.Attachment.CEILING);
                canSurvive = place.isFaceFullCenter(BlockFace.UP);
            }
            if (direction == Direction.UP) {
                bell.setAttachment(Bell.Attachment.FLOOR);
                canSurvive = place.isFullFace(BlockFace.DOWN);
            }
            bell.setFacing(place.getPlayerFacing());
        } else {
            boolean flag = place.isXAxis()
                    && place.isFullFace(BlockFace.EAST)
                    && place.isFullFace(BlockFace.WEST)

                    || place.isZAxis()
                    && place.isFullFace(BlockFace.SOUTH)
                    && place.isFullFace(BlockFace.NORTH);

            bell.setFacing(place.getBlockFace().getOppositeFace());
            bell.setAttachment(flag ? Bell.Attachment.DOUBLE_WALL : Bell.Attachment.SINGLE_WALL);
            canSurvive = place.isFullFace(place.getBlockFace().getOppositeFace());

            if (canSurvive) {
                place.set(bell);
                return;
            }

            boolean flag1 = place.isFullFace(BlockFace.DOWN);
            bell.setAttachment(flag1 ? Bell.Attachment.FLOOR : Bell.Attachment.CEILING);
            canSurvive = place.isFullFace(flag1 ? BlockFace.DOWN : BlockFace.UP);
        }
        if (canSurvive) place.set(bell);
    }, XMaterial.BELL.parseMaterial()),

    CANDLE((player, place) -> {
        BlockData existing = place.getExistingBlockBlockData();
        Candle candle = (Candle) place.getMaterial().createBlockData();

        if (existing instanceof Candle) {
            Candle existingCandle = (Candle) existing;
            // Max candles already exists
            if (existingCandle.getMaximumCandles() == existingCandle.getCandles()) return;
            candle.setCandles(existingCandle.getCandles() + 1);
        }

        if (place.isFaceFullCenter(BlockFace.DOWN)) {
            place.set(candle);
        }
    }, Arrays.stream(Material.values()).filter(mat -> mat.name().endsWith("CANDLE")).toArray(Material[]::new)),

    // Sea pickles refuse to overwrite any collision... but... that's already checked.  Unsure what Mojang is doing.
    SEA_PICKLE((player, place) -> {
        BlockData existing = place.getExistingBlockBlockData();
        SeaPickle pickle = (SeaPickle) place.getMaterial().createBlockData();

        if (existing instanceof SeaPickle) {
            SeaPickle existingPickle = (SeaPickle) existing;
            // Max pickels already exist
            if (existingPickle.getMaximumPickles() == existingPickle.getPickles()) return;
            pickle.setPickles(existingPickle.getPickles() + 1);
        }

        place.set(pickle);
    }, XMaterial.SEA_PICKLE.parseMaterial()),

    CHAIN((player, place) -> {
        Chain chain = (Chain) place.getMaterial().createBlockData();
        BlockFace face = place.getBlockFace();

        switch (face) {
            case EAST:
            case WEST:
                chain.setAxis(Axis.X);
                break;
            case NORTH:
            case SOUTH:
                chain.setAxis(Axis.Z);
                break;
            case UP:
            case DOWN:
                chain.setAxis(Axis.Y);
                break;
        }

        place.set(chain);
    }, XMaterial.CHAIN.parseMaterial()),

    COCOA((player, place) -> {
        for (BlockFace face : place.getNearestPlacingDirections()) {
            if (BlockFaceHelper.isFaceVertical(face)) continue;
            Material mat = place.getDirectionalState(face).getMaterial();
            if (mat == Material.JUNGLE_LOG || mat == Material.STRIPPED_JUNGLE_LOG) {
                Cocoa data = (Cocoa) place.getMaterial().createBlockData();
                data.setFacing(face);
                place.set(face, new FlatBlockState(data));
                break;
            }
        }
    }, XMaterial.COCOA.parseMaterial()),

    DIRT_PATH((player, place) -> {
        BaseBlockState state = place.getDirectionalState(BlockFace.UP);
        // If there is a solid block above the dirt path, it turns to air.  This does not include fence gates
        if (!Materials.checkFlag(state.getMaterial(), Materials.SOLID_BLACKLIST) || Materials.checkFlag(state.getMaterial(), Materials.GATE)) {
            place.set(place.getMaterial());
        } else {
            place.set(Material.DIRT);
        }
    }, XMaterial.DIRT_PATH.parseMaterial()),

    HOPPER((player, place) -> {
        BlockFace opposite = place.getPlayerFacing().getOppositeFace();
        Hopper hopper = (Hopper) place.getMaterial().createBlockData();
        hopper.setFacing(place.isFaceVertical() ? BlockFace.DOWN : opposite);
    }, XMaterial.HOPPER.parseMaterial()),

    LANTERN((player, place) -> {
        for (BlockFace face : place.getNearestPlacingDirections()) {
            if (BlockFaceHelper.isFaceHorizontal(face)) continue;
            Lantern lantern = (Lantern) place.getMaterial().createBlockData();

            boolean isHanging = face == BlockFace.UP;
            lantern.setHanging(isHanging);

            boolean canSurvive = place.isFaceFullCenter(isHanging ? BlockFace.UP : BlockFace.DOWN) && !Materials.checkFlag(place.getPlacedAgainstMaterial(), Materials.GATE);
            if (!canSurvive) continue;

            place.set(new FlatBlockState(lantern));
            return;
        }
    }, XMaterial.LANTERN.parseMaterial(), XMaterial.SOUL_LANTERN.parseMaterial()),

    POINTED_DRIPSTONE((player, place) -> {
        // To explain what Mojang is doing, take the example of placing on top face
        BlockFace primaryDirection = place.getNearestVerticalDirection().getOppositeFace(); // The player clicked downwards, so use upwards
        BlockData typePlacingOn = place.getDirectionalFlatState(primaryDirection.getOppositeFace()).getBlockData(); // Block we are placing on

        // Check to see if we can place on the block or there is dripstone on the block that we are placing on also pointing upwards
        boolean primarySameType = typePlacingOn instanceof PointedDripstone && ((PointedDripstone) typePlacingOn).getVerticalDirection() == primaryDirection;
        boolean primaryValid = place.isFullFace(primaryDirection.getOppositeFace()) || primarySameType;

        // Try to use the opposite direction, just to see if switching directions makes it valid.
        if (!primaryValid) {
            BlockFace secondaryDirection = primaryDirection.getOppositeFace(); // See if placing it DOWNWARDS is valid
            BlockData secondaryType = place.getDirectionalFlatState(secondaryDirection.getOppositeFace()).getBlockData(); // Get the block above us
            // Check if the dripstone above us is also facing downwards
            boolean secondarySameType = secondaryType instanceof PointedDripstone && ((PointedDripstone) secondaryType).getVerticalDirection() == secondaryDirection;

            primaryDirection = secondaryDirection;
            typePlacingOn = secondaryType;
            // Update block survivability
            primaryValid = place.isFullFace(secondaryDirection.getOppositeFace()) || secondarySameType;
        }

        // No valid locations
        if (!primaryValid) return;

        PointedDripstone toPlace = (PointedDripstone) Material.POINTED_DRIPSTONE.createBlockData();
        toPlace.setVerticalDirection(primaryDirection); // This block is facing UPWARDS as placed on the top face

        // We then have to calculate the thickness of the dripstone
        //
        // PrimaryDirection should be the direction that the current dripstone being placed will face
        // oppositeType should be the opposite to the direction the dripstone is facing, what it is pointing into
        //
        // If the dripstone is -> <- pointed at one another

        // If check the blockstate that is above now with the direction of DOWN
        BlockData oppositeToUs = place.getDirectionalFlatState(primaryDirection).getBlockData();

        // TODO: This is block update code and we must now run this for all 6 directions around us.
        if (oppositeToUs instanceof PointedDripstone && ((PointedDripstone) oppositeToUs).getVerticalDirection() == primaryDirection.getOppositeFace()) {
            PointedDripstone dripstone = (PointedDripstone) oppositeToUs;
            // Use tip if the player is sneaking, or if it already is merged (somehow)
            PointedDripstone.Thickness thick = place.isSecondaryUse() && dripstone.getThickness() != PointedDripstone.Thickness.TIP_MERGE ?
                    PointedDripstone.Thickness.TIP : PointedDripstone.Thickness.TIP_MERGE;

            toPlace.setThickness(thick);
        } else {
            BlockData sameDirectionToUs = place.getDirectionalFlatState(primaryDirection).getBlockData();

            // Check if the blockstate air does not have the direction of UP already (somehow)
            if (!(sameDirectionToUs instanceof PointedDripstone) || ((PointedDripstone) sameDirectionToUs).getVerticalDirection() != primaryDirection) {
                toPlace.setThickness(PointedDripstone.Thickness.TIP);
            } else {
                if (typePlacingOn instanceof PointedDripstone &&
                        ((PointedDripstone) typePlacingOn).getThickness() != PointedDripstone.Thickness.TIP &&
                        ((PointedDripstone) typePlacingOn).getThickness() != PointedDripstone.Thickness.TIP_MERGE) {
                    // Look downwards
                    PointedDripstone dripstone = (PointedDripstone) typePlacingOn;
                    PointedDripstone.Thickness toSetThick = dripstone.getVerticalDirection() == primaryDirection ? PointedDripstone.Thickness.BASE : PointedDripstone.Thickness.MIDDLE;
                    toPlace.setThickness(toSetThick);

                } else {
                    toPlace.setThickness(PointedDripstone.Thickness.FRUSTUM);
                }
            }
        }

        place.set(toPlace);
    }, XMaterial.POINTED_DRIPSTONE.parseMaterial()),

    CACTUS((player, place) -> {
        for (BlockFace face : place.getHorizontalFaces()) {
            if (place.isSolid(face) || place.isLava(face)) {
                return;
            }
        }

        if (place.isOn(Material.CACTUS, Material.SAND, Material.RED_SAND) && !place.isLava(BlockFace.UP)) {
            place.set();
        }
    }, XMaterial.CACTUS.parseMaterial()),

    CAKE((player, place) -> {
        if (place.isSolid(BlockFace.DOWN)) {
            place.set();
        }
    }, XMaterial.CAKE.parseMaterial(), XMaterial.CANDLE_CAKE.parseMaterial()),

    PISTON_BASE((player, place) -> {
        Piston piston = (Piston) place.getMaterial().createBlockData();
        piston.setFacing(place.getNearestVerticalDirection().getOppositeFace());
    }, XMaterial.PISTON.parseMaterial(), XMaterial.STICKY_PISTON.parseMaterial()),

    AZALEA((player, place) -> {
        BaseBlockState below = place.getBelowState();
        if (place.isOnDirt() || below.getMaterial() == Material.FARMLAND || below.getMaterial() == Material.CLAY) {
            place.set(place.getMaterial());
        }
    }, XMaterial.AZALEA.parseMaterial()),

    CROP((player, place) -> {
        BaseBlockState below = place.getBelowState();
        if (below.getMaterial() == Material.FARMLAND) {
            place.set(place.getMaterial());
        }
    }, XMaterial.CARROTS.parseMaterial(), XMaterial.BEETROOTS.parseMaterial(), XMaterial.POTATOES.parseMaterial(),
            XMaterial.PUMPKIN_STEM.parseMaterial(), XMaterial.MELON_STEM.parseMaterial(), XMaterial.WHEAT.parseMaterial()),

    SUGARCANE((player, place) -> {
        if (place.isOnDirt() || place.isOn(Material.SAND, Material.RED_SAND)) {
            Vector3i pos = place.getPlacedBlockPos();
            pos.setY(pos.getY() - 1);

            for (BlockFace direction : place.getHorizontalFaces()) {
                Vector3i toSearchPos = pos.clone();
                toSearchPos.setX(toSearchPos.getX() + direction.getModX());
                toSearchPos.setZ(toSearchPos.getZ() + direction.getModZ());

                BaseBlockState directional = player.compensatedWorld.getWrappedBlockStateAt(toSearchPos);
                if (Materials.isWater(player.getClientVersion(), directional) || directional.getMaterial() == Material.FROSTED_ICE) {
                    place.set();
                    return;
                }
            }
        }
    }, XMaterial.SUGAR_CANE.parseMaterial()),

    CARPET((player, place) -> {
        if (Materials.checkFlag(place.getBelowState().getMaterial(), Materials.AIR)) {
            place.set();
        }
    }, Arrays.stream(Material.values()).filter(mat -> mat.name().contains("CARPET")).toArray(Material[]::new)),

    CHORUS_FLOWER((player, place) -> {
        BaseBlockState blockstate = place.getBelowState();
        if (blockstate.getMaterial() != Material.CHORUS_PLANT && blockstate.getMaterial() != Material.END_STONE) {
            if (Materials.checkFlag(blockstate.getMaterial(), Materials.AIR)) {
                boolean flag = false;

                for (BlockFace direction : place.getHorizontalFaces()) {
                    BaseBlockState blockstate1 = place.getDirectionalState(direction);
                    if (blockstate1.getMaterial() == Material.CHORUS_PLANT) {
                        if (flag) {
                            return;
                        }

                        flag = true;
                    } else if (Materials.checkFlag(blockstate1.getMaterial(), Materials.AIR)) {
                        return;
                    }
                }

                if (flag) {
                    place.set();
                }
            }
        } else {
            place.set();
        }
    }, XMaterial.CHORUS_FLOWER.parseMaterial()),

    CHORUS_PLANT((player, place) -> {
        BaseBlockState blockstate = place.getBelowState();
        boolean flag = !Materials.checkFlag(place.getAboveState().getMaterial(), Materials.AIR) &&
                !Materials.checkFlag(blockstate.getMaterial(), Materials.AIR);

        for (BlockFace direction : place.getHorizontalFaces()) {
            BaseBlockState blockstate1 = place.getDirectionalState(direction);
            if (blockstate1.getMaterial() == Material.CHORUS_PLANT) {
                if (flag) {
                    return;
                }

                Vector3i placedPos = place.getPlacedBlockPos();
                placedPos.setY(placedPos.getY() - 1);
                placedPos.setX(placedPos.getX() + direction.getModX());
                placedPos.setX(placedPos.getZ() + direction.getModZ());

                BaseBlockState blockstate2 = player.compensatedWorld.getWrappedBlockStateAt(placedPos);
                if (blockstate2.getMaterial() == Material.CHORUS_PLANT || blockstate2.getMaterial() == Material.END_STONE) {
                    place.set();
                }
            }
        }

        if (blockstate.getMaterial() == Material.CHORUS_PLANT || blockstate.getMaterial() == Material.END_STONE) {
            place.set();
        }
    }, XMaterial.CHORUS_PLANT.parseMaterial()),

    DEAD_BUSH((player, place) -> {
        BaseBlockState below = place.getBelowState();
        if (below.getMaterial() == Material.SAND || below.getMaterial() == Material.RED_SAND ||
                below.getMaterial().name().contains("TERRACOTTA") || place.isOnDirt()) {
            place.set(place.getMaterial());
        }
    }, XMaterial.DEAD_BUSH.parseMaterial()),

    DIODE((player, place) -> {
        if (place.isFaceRigid(BlockFace.DOWN)) {
            place.set();
        }
    }, Materials.matchLegacy("LEGACY_DIODE_BLOCK_OFF"), Materials.matchLegacy("LEGACY_DIODE_BLOCK_ON"),
            Materials.matchLegacy("LEGACY_REDSTONE_COMPARATOR_ON"), Materials.matchLegacy("LEGACY_REDSTONE_COMPARATOR_OFF"),
            XMaterial.REPEATER.parseMaterial(), XMaterial.COMPARATOR.parseMaterial(),
            XMaterial.REDSTONE_WIRE.parseMaterial()),

    FUNGUS((player, place) -> {
        if (place.isOn(Material.CRIMSON_NYLIUM, Material.WARPED_NYLIUM, Material.MYCELIUM, Material.SOUL_SOIL, Material.FARMLAND) || place.isOnDirt()) {
            place.set();
        }
    }, XMaterial.CRIMSON_FUNGUS.parseMaterial(), XMaterial.WARPED_FUNGUS.parseMaterial()),

    SPROUTS((player, place) -> {
        if (place.isOn(Material.CRIMSON_NYLIUM, Material.WARPED_NYLIUM, Material.SOUL_SOIL, Material.FARMLAND) || place.isOnDirt()) {
            place.set();
        }
    }, XMaterial.NETHER_SPROUTS.parseMaterial(), XMaterial.WARPED_ROOTS.parseMaterial(), XMaterial.CRIMSON_ROOTS.parseMaterial()),

    NETHER_WART((player, place) -> {
        if (place.isOn(Material.SOUL_SAND)) {
            place.set();
        }
    }, XMaterial.NETHER_WART.parseMaterial()),

    WATERLILY((player, place) -> {
        BaseBlockState below = place.getDirectionalState(BlockFace.DOWN);
        if (!place.isInLiquid() && (Materials.isWater(player.getClientVersion(), below) || place.isOn(Material.ICE, Material.FROSTED_ICE))) {
            place.set();
        }
    }, XMaterial.LILY_PAD.parseMaterial()),

    WITHER_ROSE((player, place) -> {
        if (place.isOn(Material.NETHERRACK, Material.SOUL_SAND, Material.SOUL_SOIL, Material.FARMLAND) || place.isOnDirt()) {
            place.set();
        }
    }, XMaterial.WITHER_ROSE.parseMaterial()),

    // Blocks that have both wall and standing states
    TORCH_OR_HEAD((player, place) -> {
        // type doesn't matter to grim, same hitbox.
        // If it's a torch, create a wall torch
        // Otherwise, it's going to be a head.  The type of this head also doesn't matter
        Directional dir;
        boolean isTorch = place.getMaterial().name().contains("TORCH");
        boolean isHead = place.getMaterial().name().contains("HEAD") || place.getMaterial().name().contains("SKULL");
        boolean isWallSign = !isTorch && !isHead;

        if (isTorch) {
            dir = (Directional) Material.WALL_TORCH.createBlockData();
        } else if (place.getMaterial().name().contains("HEAD") || place.getMaterial().name().contains("SKULL")) {
            dir = (Directional) Material.PLAYER_HEAD.createBlockData();
        } else {
            dir = (Directional) Material.OAK_WALL_SIGN.createBlockData();
        }

        for (BlockFace face : place.getNearestPlacingDirections()) {
            // Torches need solid faces
            // Heads have no special preferences - place them anywhere
            // Signs need solid - exempts chorus flowers and a few other strange cases
            if (face != BlockFace.UP) {
                if (BlockFaceHelper.isFaceHorizontal(face)) {
                    boolean canPlace = isHead || ((isWallSign || place.isFullFace(face)) && (isTorch || place.isSolid(face)));
                    if (canPlace && face != BlockFace.UP) { // center requires nothing (head), full face (torch), or solid (sign)
                        dir.setFacing(face.getOppositeFace());
                        place.set(dir);
                        return;
                    }
                } else if (place.isFaceFullCenter(BlockFace.DOWN)) {
                    boolean canPlace = isHead || ((isWallSign || place.isFaceFullCenter(face)) && (isTorch || place.isSolid(face)));
                    if (canPlace) {
                        place.set(place.getMaterial());
                        return;
                    }
                }
            }
        }
        // First add all torches
    }, Arrays.stream(Material.values()).filter(mat -> mat.name().contains("TORCH") // Find all torches
                    || (mat.name().contains("HEAD") || mat.name().contains("SKULL")) && !mat.name().contains("PISTON") // Skulls
                    || mat.name().contains("SIGN")) // And signs
            .toArray(Material[]::new)),

    FACE_ATTACHED_HORIZONTAL_DIRECTIONAL((player, place) -> {
        for (BlockFace face : place.getNearestPlacingDirections()) {
            if (place.isFullFace(face)) {
                place.set(place.getMaterial());
                return;
            }
        }
    }, Arrays.stream(Material.values()).filter(mat -> mat.name().contains("BUTTON") // Find all buttons
                    || mat.name().contains("LEVER") // And levers
                    || mat.name().contains("LICHEN")) // Add lichen too
            .toArray(Material[]::new)),

    GRINDSTONE((player, place) -> { // Grindstones do not have special survivability requirements
        Grindstone stone = (Grindstone) place.getMaterial().createBlockData();
        stone.setFacing(place.getBlockFace());
        place.set(stone);
    }, Arrays.stream(Material.values()).filter(mat -> mat.name().contains("GRINDSTONE")) // GRINDSTONE
            .toArray(Material[]::new)),

    // Blocks that have both wall and standing states
    // Banners
    BANNER((player, place) -> {
        for (BlockFace face : place.getNearestPlacingDirections()) {
            if (place.isSolid(face) && face != BlockFace.UP) {
                if (BlockFaceHelper.isFaceHorizontal(face)) {
                    // type doesn't matter to grim, same hitbox.
                    // If it's a torch, create a wall torch
                    // Otherwise, it's going to be a head.  The type of this head also doesn't matter.
                    Directional dir = (Directional) Material.BLACK_WALL_BANNER.createBlockData();
                    dir.setFacing(face.getOppositeFace());
                    place.set(dir);
                } else {
                    place.set(place.getMaterial());
                }
                break;
            }
        }
    }, Arrays.stream(Material.values()).filter(mat -> (mat.name().contains("BANNER")))
            .toArray(Material[]::new)),

    BIG_DRIPLEAF((player, place) -> {
        BlockData existing = place.getDirectionalFlatState(BlockFace.DOWN).getBlockData();
        if (place.isFullFace(BlockFace.DOWN) || existing.getMaterial() == Material.BIG_DRIPLEAF || existing.getMaterial() == Material.BIG_DRIPLEAF_STEM) {
            place.set(place.getMaterial());
        }
    }, XMaterial.BIG_DRIPLEAF.parseMaterial()),

    SMALL_DRIPLEAF((player, place) -> {
        BlockData existing = place.getDirectionalFlatState(BlockFace.DOWN).getBlockData();
        if (place.isBlockFaceOpen(BlockFace.UP) && Tag.SMALL_DRIPLEAF_PLACEABLE.isTagged(existing.getMaterial()) || (place.isInWater() && (place.isOnDirt() || existing.getMaterial() == Material.FARMLAND))) {
            place.set(place.getMaterial());
        }
    }, XMaterial.SMALL_DRIPLEAF.parseMaterial()),

    SEAGRASS((player, place) -> {
        BlockData existing = place.getDirectionalFlatState(BlockFace.DOWN).getBlockData();
        if (place.isInWater() && place.isFullFace(BlockFace.DOWN) && existing.getMaterial() != Material.MAGMA_BLOCK) {
            place.set(place.getMaterial());
        }
    }, XMaterial.SEAGRASS.parseMaterial()),

    HANGING_ROOT((player, place) -> {
        if (place.isFullFace(BlockFace.UP)) {
            place.set(place.getMaterial());
        }
    }, XMaterial.HANGING_ROOTS.parseMaterial()),

    SPORE_BLOSSOM((player, place) -> {
        if (place.isFullFace(BlockFace.UP) && !place.isInWater()) {
            place.set();
        }
    }, XMaterial.SPORE_BLOSSOM.parseMaterial()),

    FIRE((player, place) -> {
        boolean byFlammable = false;
        for (BlockFace face : BlockFace.values()) {
            if (place.getDirectionalState(face).getMaterial().isFlammable()) byFlammable = true;
        }
        if (byFlammable || place.isFullFace(BlockFace.DOWN)) {
            place.set(place.getMaterial());
        }
    }, XMaterial.FIRE.parseMaterial(), XMaterial.SOUL_FIRE.parseMaterial()), // soul fire isn't directly placeable

    TRIPWIRE_HOOK((player, place) -> {
        if (place.isFaceHorizontal() && place.isFullFace(place.getBlockFace())) {
            place.set(place.getMaterial());
        }
    }, XMaterial.TRIPWIRE_HOOK.parseMaterial()),

    CORAL_PLANT((player, place) -> {
        if (place.isFullFace(BlockFace.DOWN)) {
            place.set(place.getMaterial());
        }
    }, Arrays.stream(Material.values()).filter(mat -> (mat.name().contains("CORAL")
                    && !mat.name().contains("BLOCK") && !mat.name().contains("FAN")))
            .toArray(Material[]::new)),

    CORAL_FAN((player, place) -> {
        for (BlockFace face : place.getNearestPlacingDirections()) {
            if (BlockFaceHelper.isFaceHorizontal(face) && place.isFullFace(face) && face != BlockFace.UP) {
                // type doesn't matter to grim, same hitbox.
                // If it's a torch, create a wall torch
                // Otherwise, it's going to be a head.  The type of this head also doesn't matter.
                Directional dir = (Directional) place.getMaterial().createBlockData();
                dir.setFacing(face.getOppositeFace());
                place.set(dir);

                break;
            }
        }
    }, Arrays.stream(Material.values()).filter(mat -> (mat.name().contains("CORAL")
                    && !mat.name().contains("BLOCK") && mat.name().contains("FAN")))
            .toArray(Material[]::new)),

    PRESSURE_PLATE((player, place) -> {
        if (place.isFullFace(BlockFace.DOWN) || place.isFaceFullCenter(BlockFace.DOWN)) {
            place.set();
        }
    }, Arrays.stream(Material.values()).filter(mat -> (mat.name().contains("PLATE")))
            .toArray(Material[]::new)),

    RAIL((player, place) -> {
        if (place.isFaceRigid(BlockFace.DOWN)) {
            place.set(place.getMaterial());
        }
    }, Arrays.stream(Material.values()).filter(mat -> mat.name().contains("RAIL")).toArray(Material[]::new)),

    KELP((player, place) -> {
        Material below = place.getDirectionalFlatState(BlockFace.DOWN).getMaterial();
        if (below != Material.MAGMA_BLOCK && (place.isFullFace(BlockFace.DOWN) || below == Material.KELP || below == Material.KELP_PLANT) && place.isInWater()) {
            place.set(place.getMaterial());
        }
    }, XMaterial.KELP.parseMaterial()),

    CAVE_VINE((player, place) -> {
        Material below = place.getDirectionalFlatState(BlockFace.UP).getMaterial();
        if (place.isFullFace(BlockFace.DOWN) || below == Material.CAVE_VINES || below == Material.CAVE_VINES_PLANT) {
            place.set(place.getMaterial());
        }
    }, XMaterial.CAVE_VINES.parseMaterial()),

    WEEPING_VINE((player, place) -> {
        Material below = place.getDirectionalFlatState(BlockFace.UP).getMaterial();
        if (place.isFullFace(BlockFace.DOWN) || below == Material.WEEPING_VINES || below == Material.WEEPING_VINES_PLANT) {
            place.set(place.getMaterial());
        }
    }, XMaterial.WEEPING_VINES.parseMaterial()),

    TWISTED_VINE((player, place) -> {
        Material below = place.getDirectionalFlatState(BlockFace.DOWN).getMaterial();
        if (place.isFullFace(BlockFace.UP) || below == Material.TWISTING_VINES || below == Material.TWISTING_VINES_PLANT) {
            place.set(place.getMaterial());
        }
    }, XMaterial.TWISTING_VINES.parseMaterial()),

    // Vine logic
    // If facing up, then there is a face facing up.
    // Checks for solid faces in the direction that it is in
    // Also checks for vines with the same directional above itself
    // However, as all vines have the same hitbox (to collisions and climbing)
    // As long as one of these properties is met, it is good enough for grim!
    VINE((player, place) -> {
        if (place.getAboveState().getMaterial() == Material.VINE) {
            place.set();
            return;
        }

        for (BlockFace face : place.getHorizontalFaces()) {
            if (place.isSolid(face)) {
                place.set();
                return;
            }
        }
    }, XMaterial.VINE.parseMaterial()),

    // TODO: This isn't allowed on 1.8 clients, they use different trapdoor placing logic
    TRAPDOOR((player, place) -> {
        TrapDoor door = (TrapDoor) place.getMaterial().createBlockData();

        BlockFace direction = place.getBlockFace();
        if (!place.replaceClicked() && BlockFaceHelper.isFaceHorizontal(direction)) {
            door.setFacing(direction);
            boolean clickedTop = place.getClickedLocation().getY() > 0.5;
            Bisected.Half half = clickedTop ? Bisected.Half.TOP : Bisected.Half.BOTTOM;
            door.setHalf(half);
        } else {
            door.setFacing(place.getPlayerFacing().getOppositeFace());
            Bisected.Half half = direction == BlockFace.UP ? Bisected.Half.BOTTOM : Bisected.Half.TOP;
            door.setHalf(half);
        }

        // TODO: We must check for block power.
        place.set(door);
    }, Arrays.stream(Material.values()).filter(mat -> mat.name().contains("TRAP_DOOR") || mat.name().contains("TRAPDOOR")).toArray(Material[]::new)),

    DOOR((player, place) -> {
        if (place.isFullFace(BlockFace.DOWN) && place.isBlockFaceOpen(BlockFace.UP)) {
            Door door = (Door) place.getMaterial().createBlockData();
            door.setFacing(place.getPlayerFacing());

            // Get the hinge
            BlockFace playerFacing = place.getPlayerFacing();

            BlockFace ccw = BlockFaceHelper.getCounterClockwise(playerFacing);
            BaseBlockState ccwState = place.getDirectionalState(ccw);
            CollisionBox ccwBox = CollisionData.getData(ccwState.getMaterial()).getMovementCollisionBox(player, player.getClientVersion(), ccwState);

            Vector aboveCCWPos = place.getClickedLocation().add(ccw.getDirection()).add(new Vector(0, 1, 0));
            BaseBlockState aboveCCWState = player.compensatedWorld.getWrappedBlockStateAt(aboveCCWPos);
            CollisionBox aboveCCWBox = CollisionData.getData(aboveCCWState.getMaterial()).getMovementCollisionBox(player, player.getClientVersion(), aboveCCWState);

            BlockFace cw = BlockFaceHelper.getClockWise(playerFacing);
            BaseBlockState cwState = place.getDirectionalState(cw);
            CollisionBox cwBox = CollisionData.getData(cwState.getMaterial()).getMovementCollisionBox(player, player.getClientVersion(), cwState);

            Vector aboveCWPos = place.getClickedLocation().add(cw.getDirection()).add(new Vector(0, 1, 0));
            BaseBlockState aboveCWState = player.compensatedWorld.getWrappedBlockStateAt(aboveCWPos);
            CollisionBox aboveCWBox = CollisionData.getData(aboveCWState.getMaterial()).getMovementCollisionBox(player, player.getClientVersion(), aboveCWState);

            int i = (ccwBox.isFullBlock() ? -1 : 0) + (aboveCCWBox.isFullBlock() ? -1 : 0) + (cwBox.isFullBlock() ? 1 : 0) + (aboveCWBox.isFullBlock() ? 1 : 0);

            boolean isCCWLower = false;
            WrappedBlockDataValue ccwValue = WrappedBlockData.getMaterialData(ccwState).getData(ccwState);
            if (ccwValue instanceof WrappedDoor) isCCWLower = ((WrappedDoor) ccwValue).isBottom();

            boolean isCWLower = false;
            WrappedBlockDataValue cwValue = WrappedBlockData.getMaterialData(ccwState).getData(ccwState);
            if (cwValue instanceof WrappedDoor) isCWLower = ((WrappedDoor) cwValue).isBottom();

            Door.Hinge hinge;
            if ((!isCCWLower || isCWLower) && i <= 0) {
                if ((!isCWLower || isCCWLower) && i >= 0) {
                    int j = playerFacing.getModX();
                    int k = playerFacing.getModZ();
                    Vector vec3 = place.getClickedLocation();
                    double d0 = vec3.getX();
                    double d1 = vec3.getY();
                    hinge = (j >= 0 || d1 >= 0.5D) && (j <= 0 || d1 <= 0.5D) && (k >= 0 || d0 <= 0.5D) && (k <= 0 || d0 >= 0.5D) ? Door.Hinge.LEFT : Door.Hinge.RIGHT;
                } else {
                    hinge = Door.Hinge.LEFT;
                }
            } else {
                hinge = Door.Hinge.RIGHT;
            }
            door.setHinge(hinge);

            place.set(door);

            door.setHalf(Bisected.Half.TOP);
            place.setAbove(new FlatBlockState(door));
        }
    }, Arrays.stream(Material.values()).filter(mat -> mat.name().contains("DOOR")).toArray(Material[]::new)),

    DOUBLE_PLANT((player, place) -> {
        if (place.isBlockFaceOpen(BlockFace.UP) && place.isOnDirt() || place.isOn(Material.FARMLAND)) {
            place.set();
            place.setAbove(); // Client predicts block above
        }
    }, XMaterial.TALL_GRASS.parseMaterial(), XMaterial.LARGE_FERN.parseMaterial(), XMaterial.SUNFLOWER.parseMaterial(),
            XMaterial.LILAC.parseMaterial(), XMaterial.ROSE_BUSH.parseMaterial(), XMaterial.PEONY.parseMaterial()),

    BUSH_BLOCK_TYPE((player, place) -> {
        if (place.isOnDirt() || place.isOn(Material.FARMLAND)) {
            place.set();
        }
    }, XMaterial.SPRUCE_SAPLING.parseMaterial(), XMaterial.ACACIA_SAPLING.parseMaterial(),
            XMaterial.BIRCH_SAPLING.parseMaterial(), XMaterial.DARK_OAK_SAPLING.parseMaterial(),
            XMaterial.OAK_SAPLING.parseMaterial(), XMaterial.JUNGLE_SAPLING.parseMaterial(),
            XMaterial.SWEET_BERRY_BUSH.parseMaterial(), XMaterial.DANDELION.parseMaterial(),
            XMaterial.POPPY.parseMaterial(), XMaterial.BLUE_ORCHID.parseMaterial(),
            XMaterial.ALLIUM.parseMaterial(), XMaterial.AZURE_BLUET.parseMaterial(),
            XMaterial.RED_TULIP.parseMaterial(), XMaterial.ORANGE_TULIP.parseMaterial(),
            XMaterial.WHITE_TULIP.parseMaterial(), XMaterial.PINK_TULIP.parseMaterial(),
            XMaterial.OXEYE_DAISY.parseMaterial(), XMaterial.CORNFLOWER.parseMaterial(),
            XMaterial.LILY_OF_THE_VALLEY.parseMaterial()),

    NO_DATA((player, place) -> {
        place.set(BlockStateHelper.create(place.getMaterial()));
    }, XMaterial.AIR.parseMaterial());

    private static final BlockPlaceResult[] lookup = new BlockPlaceResult[Material.values().length];

    static {
        for (BlockPlaceResult data : values()) {
            for (Material mat : data.materials) lookup[mat.ordinal()] = data;
        }
    }

    private final BlockPlaceFactory data;
    private final Material[] materials;

    BlockPlaceResult(BlockPlaceFactory data, Material... materials) {
        this.data = data;
        Set<Material> mList = new HashSet<>(Arrays.asList(materials));
        mList.remove(null); // Sets can contain one null
        this.materials = mList.toArray(new Material[0]);
    }

    public static BlockPlaceFactory getMaterialData(Material placed) {
        BlockPlaceResult data = lookup[placed.ordinal()];

        return data == null ? NO_DATA.data : data.data;
    }
}
