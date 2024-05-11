package com.bgsoftware.wildchests.nms.v1_20_4.utils;

import com.google.common.base.Function;
import com.google.common.collect.ForwardingList;
import com.google.common.collect.Lists;
import net.minecraft.core.NonNullList;

import java.util.List;
import java.util.RandomAccess;

public class TransformingNonNullList<T> extends NonNullList<T> {

    public static <E, T> NonNullList<T> transform(List<E> delegate, T initialElement, Function<? super E, ? extends T> transformer) {
        return new TransformingNonNullList<>(Lists.transform(new RandomAccessNonNullList<>(delegate), transformer), initialElement);
    }

    private TransformingNonNullList(List<T> delegate, T initialElement) {
        super(delegate, initialElement);
    }

    /* Patch for Lists#transform to detect NonNullList as a RandomAccess list */
    private static class RandomAccessNonNullList<E> extends ForwardingList<E> implements RandomAccess {

        private final List<E> delegate;

        public RandomAccessNonNullList(List<E> delegate) {
            this.delegate = delegate;
        }

        @Override
        protected List<E> delegate() {
            return this.delegate;
        }

    }

}
