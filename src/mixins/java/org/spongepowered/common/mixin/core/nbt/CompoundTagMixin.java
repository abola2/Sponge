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
package org.spongepowered.common.mixin.core.nbt;

import com.google.common.base.Function;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import org.apache.logging.log4j.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.common.SpongeCommon;
import org.spongepowered.common.util.PrettyPrinter;

import java.util.Map;

@Mixin(CompoundTag.class)
public abstract class CompoundTagMixin {

    // @formatter:off
    @Shadow @Final private Map<String, Tag> tags;
    // @formatter:on

    @ModifyArg(method = "copy()Lnet/minecraft/nbt/CompoundTag;", at = @At(value = "INVOKE", target = "Lcom/google/common/collect/Maps;transformValues(Ljava/util/Map;Lcom/google/common/base/Function;)Ljava/util/Map;"))
    private Function<Tag, Tag> impl$checkForOverflowOnCopy(final Function<Tag, Tag> copy) {
        return (tag) -> {
            try {
                return copy.apply(tag);
            } catch (final StackOverflowError e) {
                final PrettyPrinter printer = new PrettyPrinter(60)
                    .add("StackOverflow from trying to copy this compound")
                    .centre()
                    .hr();
                printer.addWrapped(70, "Sponge caught a stack overflow error, printing out some special"
                    + " handling and printouts to assist in finding out where this"
                    + " recursion is coming from.");
                printer.add();
                try {
                    printer.addWrapped(80, "%s : %s", "This compound", this);
                } catch (final Throwable error) {
                    printer.addWrapped(80, "Unable to get the string of this compound. Printing out some of the entries to better assist");

                    for (final Map.Entry<String, Tag> entry : this.tags.entrySet()) {
                        try {
                            printer.addWrapped(80, "%s : %s", entry.getKey(), entry.getValue());
                        } catch (final Throwable throwable) {
                            printer.add();
                            printer.addWrapped(80, "The offending key entry is belonging to " + entry.getKey());
                            break;
                        }
                    }
                }
                printer.add();
                printer.log(SpongeCommon.logger(), Level.ERROR);
                throw e;
            }
        };
    }
}
