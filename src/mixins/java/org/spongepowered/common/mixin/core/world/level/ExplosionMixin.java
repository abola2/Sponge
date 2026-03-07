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
package org.spongepowered.common.mixin.core.world.level;

import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.objectweb.asm.Opcodes;
import org.spongepowered.api.event.Cause;
import org.spongepowered.api.event.SpongeEventFactory;
import org.spongepowered.api.event.world.ExplosionEvent;
import org.spongepowered.api.world.explosion.Explosion;
import org.spongepowered.api.world.server.ServerLocation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.Surrogate;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.common.SpongeCommon;
import org.spongepowered.common.bridge.world.level.ExplosionBridge;
import org.spongepowered.common.event.ShouldFire;
import org.spongepowered.common.event.tracking.PhaseTracker;
import org.spongepowered.common.util.VecHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

@Mixin(net.minecraft.world.level.Explosion.class)
public abstract class ExplosionMixin implements ExplosionBridge {

    // @formatter:off
    @Shadow @Final private ObjectArrayList<BlockPos> toBlow;
    @Shadow @Final private Map<Player, Vec3> hitPlayers;
    @Shadow @Final private boolean fire;
    @Shadow @Final private net.minecraft.world.level.Level level;
    @Shadow @Final private double x;
    @Shadow @Final private double y;
    @Shadow @Final private double z;
    @Shadow @Final private Entity source;
    @Shadow @Final private float radius;
    @Shadow @Final private net.minecraft.world.level.Explosion.BlockInteraction blockInteraction;
    // @formatter:on

    private boolean impl$shouldBreakBlocks;
    private boolean impl$shouldDamageEntities;
    private boolean impl$shouldPlaySmoke;
    private int impl$resolution;
    private float impl$randomness;
    private double impl$knockback;

    @Inject(
        method = "<init>(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/damagesource/DamageSource;Lnet/minecraft/world/level/ExplosionDamageCalculator;DDDFZLnet/minecraft/world/level/Explosion$BlockInteraction;Lnet/minecraft/core/particles/ParticleOptions;Lnet/minecraft/core/particles/ParticleOptions;Lnet/minecraft/core/Holder;)V",
        at = @At("RETURN")
    )
    private void impl$onConstructed(final Level $$0, final Entity $$1, final DamageSource $$2, final ExplosionDamageCalculator $$3, final double $$4,
            final double $$5, final double $$6, final float $$7, final boolean $$8, final net.minecraft.world.level.Explosion.BlockInteraction $$9,
            final ParticleOptions $$10, final ParticleOptions $$11, final Holder<SoundEvent> $$12, final CallbackInfo ci) {
        // In Vanilla and Forge, 'damagesTerrain' controls both smoke particles and block damage
        // Sponge-created explosions will explicitly set 'impl$shouldBreakBlocks' to its proper value
        this.impl$shouldBreakBlocks = this.blockInteraction == net.minecraft.world.level.Explosion.BlockInteraction.DESTROY_WITH_DECAY || this.blockInteraction == net.minecraft.world.level.Explosion.BlockInteraction.DESTROY;
        this.impl$shouldDamageEntities = true;
        this.impl$resolution = 16;
        this.impl$randomness = 1.0F;
        this.impl$knockback = 1.0;
    }

    // (Lnet/minecraft/world/World;Lnet/minecraft/entity/Entity;Lnet/minecraft/util/DamageSource;Lnet/minecraft/world/ExplosionContext;DDDFZLnet/minecraft/world/Explosion$Mode;Lorg/spongepowered/asm/mixin/injection/callback/CallbackInfo;)V
    @Surrogate
    private void impl$onConstructed(final Level worldIn, final Entity exploderIn, final DamageSource damageSourceIn, final ExplosionDamageCalculator explosionContextIn, final double xIn, final double yIn, final double zIn, final float sizeIn, final boolean causesFireIn, final net.minecraft.world.level.Explosion.BlockInteraction modeIn, final CallbackInfo ci) {
        // In Vanilla and Forge, 'damagesTerrain' controls both smoke particles and block damage
        // Sponge-created explosions will explicitly set 'impl$shouldBreakBlocks' to its proper value
        this.impl$shouldBreakBlocks = this.blockInteraction == net.minecraft.world.level.Explosion.BlockInteraction.DESTROY_WITH_DECAY || this.blockInteraction == net.minecraft.world.level.Explosion.BlockInteraction.DESTROY;
        this.impl$shouldDamageEntities = true;
        this.impl$resolution = 16;
        this.impl$randomness = 1.0F;
        this.impl$knockback = 1.0;
    }

    @Inject(method = "explode", at = @At("HEAD"), cancellable = true)
    private void impl$onExplodeStart(final CallbackInfo ci) {
        if (this.level.isClientSide) {
            ci.cancel();
        }
    }

    @Expression("? < 16")
    @ModifyExpressionValue(method = "explode",
        at = @At(value = "MIXINEXTRAS:EXPRESSION", ordinal = 0),
        slice = @Slice(
            from = @At(value = "FIELD", target = "Lnet/minecraft/world/level/gameevent/GameEvent;EXPLODE:Lnet/minecraft/core/Holder$Reference;", opcode = Opcodes.GETSTATIC),
            to = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/ExplosionDamageCalculator;getBlockExplosionResistance(Lnet/minecraft/world/level/Explosion;Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/material/FluidState;)Ljava/util/Optional;")))
    private boolean impl$shouldIterateBlocks(final boolean original) {
        return this.impl$shouldBreakBlocks && original;
    }

    @WrapOperation(method = "explode", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;getEntities(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/AABB;)Ljava/util/List;"))
    private List<Entity> impl$filterEntitiesAndFireEvent(final Level level, final Entity source, final AABB aabb, final Operation<List<Entity>> original) {
        final List<Entity> list = this.impl$shouldDamageEntities ? original.call(level, source, aabb) : Collections.emptyList();

        if (ShouldFire.EXPLOSION_EVENT_DETONATE) {
            final List<ServerLocation> blockPositions = new ArrayList<>(this.toBlow.size());
            final List<org.spongepowered.api.entity.Entity> entities = new ArrayList<>(list.size());
            for (final BlockPos pos : this.toBlow) {
                blockPositions
                        .add(ServerLocation.of((org.spongepowered.api.world.server.ServerWorld) this.level, pos.getX(), pos.getY(), pos.getZ()));
            }
            for (final Entity entity : list) {
                // Make sure to check the entity is immune first.
                if (!entity.ignoreExplosion((net.minecraft.world.level.Explosion) (Object) this)) {
                    entities.add((org.spongepowered.api.entity.Entity) entity);
                }
            }
            final Cause cause = PhaseTracker.getInstance().currentCause();
            final ExplosionEvent.Detonate detonate = SpongeEventFactory.createExplosionEventDetonate(cause, blockPositions, entities,
                    (Explosion) this, (org.spongepowered.api.world.server.ServerWorld) this.level);
            SpongeCommon.post(detonate);
            // Clear the positions so that they can be pulled from the event
            this.toBlow.clear();
            if (detonate.isCancelled()) {
                return List.of();
            }
            if (this.impl$shouldBreakBlocks) {
                for (final ServerLocation worldLocation : detonate.affectedLocations()) {
                    this.toBlow.add(VecHelper.toBlockPos(worldLocation));
                }
            }
            // Clear the list of entities so they can be pulled from the event.
            list.clear();
            if (this.impl$shouldDamageEntities) {
                for (final org.spongepowered.api.entity.Entity entity : detonate.entities()) {
                    try {
                        list.add((Entity) entity);
                    } catch (final Exception e) {
                        // Do nothing, a plugin tried to use the wrong entity somehow.
                    }
                }
            }
        }

        return list;
    }

    @ModifyExpressionValue(method = "explode", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/ExplosionDamageCalculator;getKnockbackMultiplier(Lnet/minecraft/world/entity/Entity;)F"))
    private float impl$multiplyKnockback(final float original) {
        return (float) (original * this.impl$knockback);
    }

    @Override
    public boolean bridge$getShouldDamageBlocks() {
        return this.impl$shouldBreakBlocks;
    }

    @Override
    public boolean bridge$getShouldDamageEntities() {
        return this.impl$shouldDamageEntities;
    }

    @Override
    public void bridge$setShouldBreakBlocks(final boolean shouldBreakBlocks) {
        this.impl$shouldBreakBlocks = shouldBreakBlocks;
    }

    @Override
    public void bridge$setShouldDamageEntities(final boolean shouldDamageEntities) {
        this.impl$shouldDamageEntities = shouldDamageEntities;
    }

    @Override
    public void bridge$setResolution(final int resolution) {
        this.impl$resolution = resolution;
    }

    @Override
    public int bridge$getResolution() {
        return this.impl$resolution;
    }

    @Override
    public void bridge$setShouldPlaySmoke(final boolean shouldPlaySmoke) {
        this.impl$shouldPlaySmoke = shouldPlaySmoke;
    }

    @Override
    public boolean bridge$getShouldPlaySmoke() {
        return this.impl$shouldPlaySmoke;
    }

    @Override
    public void bridge$setRandomness(final float randomness) {
        this.impl$randomness = randomness;
    }

    @Override
    public float bridge$getRandomness() {
        return this.impl$randomness;
    }

    @Override
    public void bridge$setKnockback(final double knockback) {
        this.impl$knockback = knockback;
    }

    @Override
    public double bridge$getKnockback() {
        return this.impl$knockback;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", ExplosionMixin.class.getSimpleName() + "[", "]")
            .add("causesFire=" + this.fire)
            .add("mode=" + this.blockInteraction)
            .add("world=" + this.level)
            .add("x=" + this.x)
            .add("y=" + this.y)
            .add("z=" + this.z)
            .add("exploder=" + this.source)
            .add("size=" + this.radius)
            .add("affectedBlockPositions=" + this.toBlow)
            .add("playerKnockbackMap=" + this.hitPlayers)
            .add("shouldBreakBlocks=" + this.impl$shouldBreakBlocks)
            .add("shouldDamageEntities=" + this.impl$shouldDamageEntities)
            .add("resolution=" + this.impl$resolution)
            .add("randomness=" + this.impl$randomness)
            .add("knockback=" + this.impl$knockback)
            .toString();
    }
}
