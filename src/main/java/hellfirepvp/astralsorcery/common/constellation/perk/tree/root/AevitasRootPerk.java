/*******************************************************************************
 * HellFirePvP / Astral Sorcery 2018
 *
 * All rights reserved.
 * The source code is available on github: https://github.com/HellFirePvP/AstralSorcery
 * For further details, see the License file there.
 ******************************************************************************/

package hellfirepvp.astralsorcery.common.constellation.perk.tree.root;

import hellfirepvp.astralsorcery.common.data.research.PlayerProgress;
import hellfirepvp.astralsorcery.common.data.research.ResearchManager;
import hellfirepvp.astralsorcery.common.lib.Constellations;
import hellfirepvp.astralsorcery.common.util.MiscUtils;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

import java.util.*;

/**
 * This class is part of the Astral Sorcery Mod
 * The complete source code for this mod can be found on github.
 * Class: AevitasRootPerk
 * Created by HellFirePvP
 * Date: 16.07.2018 / 15:45
 */
public class AevitasRootPerk extends RootPerk {

    //TODO clear caches

    private static final int trackLength = 20;
    private Map<UUID, Queue<BlockPos>> plInteractMap = new HashMap<>();
    private Map<UUID, Deque<IBlockState>> plDimReturns = new HashMap<>();

    public AevitasRootPerk(int x, int y) {
        super("aevitas", Constellations.aevitas, x, y);
    }

    @Override
    public void removePerkLogic(EntityPlayer player, Side side) {
        super.removePerkLogic(player, side);

        if (side == Side.SERVER) {
            plInteractMap.remove(player.getUniqueID());
            plDimReturns.remove(player.getUniqueID());
        }
    }

    @SubscribeEvent
    public void onPlace(BlockEvent.PlaceEvent event) {
        EntityPlayer player = event.getPlayer();
        Side side = player.world.isRemote ? Side.CLIENT : Side.SERVER;
        if (side != Side.SERVER) return;

        PlayerProgress prog = ResearchManager.getProgress(player, side);
        if (prog == null || !prog.hasPerkUnlocked(this)) {
            return;
        }

        Deque<IBlockState> dim = plDimReturns.computeIfAbsent(player.getUniqueID(), u -> new LinkedList<>());
        while (dim.size() >= trackLength) {
            dim.pollLast();
        }
        int used = 0;
        for (IBlockState placed : dim) {
            if (MiscUtils.matchStateExact(event.getPlacedBlock(), placed)) {
                used++;
            }
        }
        float same;
        if (dim.size() <= 0) {
            same = 1F;
        } else {
            same = 0.15F + (1F - ((float) (used / dim.size()))) * 0.85F;
        }
        dim.addFirst(event.getPlacedBlock());

        BlockPos pos = event.getPos();
        Queue<BlockPos> tracked = plInteractMap.computeIfAbsent(player.getUniqueID(), u -> new ArrayDeque<>(trackLength));
        if (!tracked.contains(pos)) {
            tracked.add(pos);

            float xp = 2.5F * Math.max(event.getPlacedBlock().getBlockHardness(event.getWorld(), event.getPos()) / 20F, 1);
            xp *= expMultiplier;
            xp *= same;
            ResearchManager.modifyExp(player, xp);
        }

    }

    @SubscribeEvent
    public void onBreak(BlockEvent.BreakEvent event) {
        EntityPlayer player = event.getPlayer();
        Side side = player.world.isRemote ? Side.CLIENT : Side.SERVER;
        if (side != Side.SERVER) return;

        PlayerProgress prog = ResearchManager.getProgress(player, side);
        if (prog == null || !prog.hasPerkUnlocked(this)) {
            return;
        }

        Queue<BlockPos> tracked = plInteractMap.computeIfAbsent(player.getUniqueID(), u -> new ArrayDeque<>(trackLength));
        if (tracked.contains(event.getPos())) {
            return;
        }
        while (tracked.size() >= trackLength) {
            tracked.poll();
        }
        tracked.add(event.getPos());
    }

}
