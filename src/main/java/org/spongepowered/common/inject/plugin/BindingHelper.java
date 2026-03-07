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

import com.google.inject.Binder;
import com.google.inject.Binding;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Provider;
import com.google.inject.spi.ElementSource;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

final class BindingHelper {

    private final Binder binder;
    private final Map<Key<?>, Provider<?>> combinedKeys;

    BindingHelper(final Binder binder) {
        this.binder = binder;
        this.combinedKeys = new HashMap<>();
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    void bind() {
        for (final Map.Entry<Key<?>, Provider<?>> entry : this.combinedKeys.entrySet()) {
            this.binder.bind((Key) entry.getKey()).toProvider(entry.getValue());
        }
    }

    void bindFrom(final Injector fromInjector) {
        for (final Binding<?> binding : fromInjector.getBindings().values()) {
            if (!(binding.getSource() instanceof ElementSource elementSource) || elementSource.getDeclaringSource() == BindingHelper.class) {
                continue;
            }

            this.bind(fromInjector, binding);
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void bind(final Injector fromInjector, final Binding<?> binding) {
        final Key key = binding.getKey();
        final Class<?> clazz = key.getTypeLiteral().getRawType();
        if (Iterable.class.isAssignableFrom(clazz)) {
            if (Set.class.isAssignableFrom(clazz)) {
                this.getBindData(key, () -> new CollectionProviderCombiner<>(HashSet::new)).add(fromInjector.getProvider(key));
            } else {
                this.getBindData(key, () -> new CollectionProviderCombiner<>(ArrayList::new)).add(fromInjector.getProvider(key));
            }
        } else {
            this.binder.bind(key).toProvider(fromInjector.getProvider(key));
        }
    }

    @SuppressWarnings("unchecked")
    private <T, P extends Provider<T>> P getBindData(final Key<T> key, final Supplier<P> newValueSupplier) {
        return (P) this.combinedKeys.computeIfAbsent(key, $ -> newValueSupplier.get());
    }

    private static final class CollectionProviderCombiner<T> implements Provider<Collection<T>> {

        private final List<Provider<Collection<T>>> providers = new ArrayList<>();
        private final Supplier<Collection<T>> collectionSupplier;

        CollectionProviderCombiner(final Supplier<Collection<T>> collectionSupplier) {
            this.collectionSupplier = collectionSupplier;
        }

        @Override
        public Collection<T> get() {
            final Collection<T> collection = this.collectionSupplier.get();
            this.providers.forEach(p -> collection.addAll(p.get()));
            return collection;
        }

        void add(final Provider<Collection<T>> provider) {
            this.providers.add(provider);
        }
    }
}
