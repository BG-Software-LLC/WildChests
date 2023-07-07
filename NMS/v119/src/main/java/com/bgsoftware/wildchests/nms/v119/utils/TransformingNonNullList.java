package com.bgsoftware.wildchests.nms.v119.utils;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import net.minecraft.core.NonNullList;

import java.util.List;

public class TransformingNonNullList<T> extends NonNullList<T> {

    public static <E, T> NonNullList<T> transform(NonNullList<E> delegate, T initialElement, Function<? super E, ? extends T> transformer) {
        return new TransformingNonNullList<>(Lists.transform(delegate, transformer), initialElement);
    }

    private TransformingNonNullList(List<T> delegate, T initialElement) {
        super(delegate, initialElement);
    }

}
