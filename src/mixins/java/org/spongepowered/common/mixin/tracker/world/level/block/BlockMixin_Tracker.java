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
package org.spongepowered.common.mixin.tracker.world.level.block;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.common.bridge.RegistryBackedTrackableBridge;
import org.spongepowered.common.bridge.world.level.block.TrackableBlockBridge;
import org.spongepowered.common.config.SpongeGameConfigs;
import org.spongepowered.common.config.tracker.TrackerCategory;
import org.spongepowered.common.event.tracking.PhaseContext;
import org.spongepowered.common.event.tracking.PhaseTracker;
import org.spongepowered.common.event.tracking.context.transaction.EffectTransactor;
import org.spongepowered.common.util.ReflectionUtil;

@Mixin(Block.class)
public abstract class BlockMixin_Tracker implements TrackableBlockBridge, RegistryBackedTrackableBridge<Block> {

    private final boolean tracker$hasNeighborLogicOverridden = ReflectionUtil.isNeighborChangedDeclared(this.getClass());
    private final boolean tracker$hasEntityInsideLogicOverridden = ReflectionUtil.isEntityInsideDeclared(this.getClass());

    @Override
    public boolean bridge$overridesNeighborNotificationLogic() {
        return this.tracker$hasNeighborLogicOverridden;
    }

    @Override
    public boolean bridge$hasEntityInsideLogic() {
        return this.tracker$hasEntityInsideLogicOverridden;
    }

    @WrapMethod(method = "dropResources(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)V")
    private static void tracker$captureBlockProposedToBeSpawningDrops(final BlockState state, final Level worldIn,
        final BlockPos pos, final Operation<Void> original) {
        BlockMixin_Tracker.tracker$logBlockDrops(worldIn, pos, state, null, () -> original.call(state, worldIn, pos));
    }

    @WrapMethod(method = "dropResources(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/entity/BlockEntity;)V")
    private static void tracker$captureBlockProposedToBeSpawningDrops(
        final BlockState state, final LevelAccessor worldIn,
        final BlockPos pos, final @Nullable BlockEntity tileEntity, final Operation<Void> original
    ) {
        if (!(worldIn instanceof final Level level)) {
            return; // In the name of my father, and his father before him, I cast you out!
        }

        BlockMixin_Tracker.tracker$logBlockDrops(level, pos, state, tileEntity, () -> original.call(state, worldIn, pos, tileEntity));
    }

    @WrapMethod(method = "dropResources(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/entity/BlockEntity;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/item/ItemStack;)V")
    private static void tracker$captureBlockProposedToBeSpawningDrops(final BlockState state, final Level worldIn,
        final BlockPos pos, final @Nullable BlockEntity tileEntity, final Entity entity, final ItemStack itemStack,
        final Operation<Void> original) {
        BlockMixin_Tracker.tracker$logBlockDrops(worldIn, pos, state, tileEntity, () -> original.call(state, worldIn, pos, tileEntity, entity, itemStack));
    }

    private static void tracker$logBlockDrops(final Level worldIn, final BlockPos pos, final BlockState state,
            final @Nullable BlockEntity tileEntity, final Runnable runnable) {
        final PhaseTracker tracker = PhaseTracker.SERVER;
        if (tracker.onSidedThread()) {
            if (tracker.getPhaseContext().isRestoring()) {
                return;
            }

            final PhaseContext<@NonNull ?> context = tracker.getPhaseContext();
            try (final EffectTransactor ignored = context.getTransactor().logBlockDrops(worldIn, pos, state, tileEntity)) {
                runnable.run();
            }
        } else {
            runnable.run();
        }
    }

    @Override
    public TrackerCategory bridge$trackerCategory() {
        return SpongeGameConfigs.getTracker().get().block;
    }

    @Override
    public Registry<Block> bridge$trackerRegistryBacking() {
        return BuiltInRegistries.BLOCK;
    }

    @Override
    public void bridge$saveTrackerConfig() {
        SpongeGameConfigs.getTracker().save();
    }
}
