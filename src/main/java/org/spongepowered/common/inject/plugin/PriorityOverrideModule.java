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
package org.spongepowered.common.inject.plugin;

import com.google.inject.Binding;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.PrivateModule;
import com.google.inject.spi.Element;
import com.google.inject.spi.Elements;
import com.google.inject.spi.PrivateElements;
import com.google.inject.util.Modules;

import java.util.Iterator;
import java.util.List;

public final class PriorityOverrideModule extends PrivateModule {

    private final List<Module> modules;

    PriorityOverrideModule(final List<Module> modules) {
        this.modules = modules;
    }

    @Override
    protected void configure() {
        final Iterator<Module> iterator = this.modules.iterator();
        if (!iterator.hasNext()) {
            return;
        }

        Module current = this.process(iterator.next());

        while (iterator.hasNext()) {
            final Module next = this.process(iterator.next());

            current = Modules.override(current).with(next);
        }

        this.install(current);
    }

    private Module process(final Module module) {
        final List<Element> elements = Elements.getElements(this.currentStage(), module);

        for (final Element element : elements) {
            if (element instanceof final Binding<?> binding) {
                this.binder().withSource(binding.getSource()).expose(binding.getKey());
            } else if (element instanceof final PrivateElements privateElements) {
                for (final Key<?> exposedKey : privateElements.getExposedKeys()) {
                    this.binder().withSource(privateElements.getExposedSource(exposedKey)).expose(exposedKey);
                }

                return Elements.getModule(privateElements.getElements());
            }
        }

        return Elements.getModule(elements);
    }
}
