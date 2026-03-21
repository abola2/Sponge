/*
 * This file is part of Sponge, licensed under the MIT License (MIT).
 *
 * Copyright (c) SpongePowered <https://www.spongepowered.org>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.spongepowered.common.event.tracking.phase.player;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.chunk.LevelChunk;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.block.transaction.BlockTransactionReceipt;
import org.spongepowered.api.event.cause.entity.SpawnType;
import org.spongepowered.api.event.cause.entity.SpawnTypes;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.common.bridge.world.TrackedWorldBridge;
import org.spongepowered.common.bridge.world.level.TrackableBlockEventDataBridge;
import org.spongepowered.common.bridge.world.level.chunk.LevelChunkBridge;
import org.spongepowered.common.entity.PlayerTracker;
import org.spongepowered.common.event.tracking.IPhaseState;
import org.spongepowered.common.event.tracking.PhaseTracker;
import org.spongepowered.common.event.tracking.PooledPhaseState;
import org.spongepowered.common.event.tracking.TrackingUtil;
import org.spongepowered.common.event.tracking.phase.packet.BasicPacketContext;
import org.spongepowered.common.world.BlockChange;

import java.util.function.Supplier;

public final class PlayerInteractPhase extends PooledPhaseState<PlayerInteractContext> {

    @Override
    protected PlayerInteractContext createNewContext(final PhaseTracker tracker) {
        return new PlayerInteractContext(this, tracker);
    }

    @Override
    public void unwind(final PlayerInteractContext context) {
        TrackingUtil.processBlockCaptures(context);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public void postBlockTransactionApplication(PlayerInteractContext context, BlockChange blockChange, BlockTransactionReceipt receipt) {
        if (context.forward() != null && context.forwardContext() != null) {
            ((IPhaseState) context.forward()).postBlockTransactionApplication(context.forwardContext(), blockChange, receipt);
            return;
        }
        // When there is no forwarding state, apply tracker association directly from the context
        context.getCreator().ifPresent(uuid -> TrackingUtil.associateTrackerToTarget(blockChange, receipt, uuid));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public void associateNeighborStateNotifier(
        final PlayerInteractContext context, final @Nullable BlockPos sourcePos, final Block block, final BlockPos notifyPos,
        final ServerLevel minecraftWorld, final PlayerTracker.Type notifier
    ) {
        if (context.forward() != null && context.forwardContext() != null) {
            ((IPhaseState) context.forward()).associateNeighborStateNotifier(context.forwardContext(), sourcePos, block, notifyPos, minecraftWorld, notifier);
            return;
        }
        context.getCreator().ifPresent(uuid -> {
            final LevelChunk chunk = minecraftWorld.getChunkAt(notifyPos);
            ((LevelChunkBridge) chunk).bridge$setBlockNotifier(notifyPos, uuid);
        });
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public void appendNotifierToBlockEvent(
        final PlayerInteractContext context, final TrackedWorldBridge mixinWorldServer, final BlockPos pos,
        final TrackableBlockEventDataBridge blockEvent
    ) {
        if (context.forward() != null && context.forwardContext() != null) {
            ((IPhaseState) context.forward()).appendNotifierToBlockEvent(context.forwardContext(), mixinWorldServer, pos, blockEvent);
        }
    }

    @Override
    public Supplier<SpawnType> getSpawnTypeForTransaction(
        final PlayerInteractContext context, final Entity entityToSpawn
    ) {
        if (context.forwardContext() instanceof BasicPacketContext bpc) {
            final ItemStack itemStack = bpc.getItemUsed();
            return itemStack.type() instanceof SpawnEggItem ? SpawnTypes.SPAWN_EGG : SpawnTypes.PLACEMENT;
        }
        return super.getSpawnTypeForTransaction(context, entityToSpawn);
    }
}
