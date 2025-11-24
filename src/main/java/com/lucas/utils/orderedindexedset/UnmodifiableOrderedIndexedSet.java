package com.lucas.utils.orderedindexedset;

import jakarta.annotation.Nonnull;

import java.util.Collection;
import java.util.Iterator;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Unmodifiable wrapper around an {@link OrderedIndexedSet}.
 * All read operations delegate to the provided instance. All mutation
 * operations throw {@link UnsupportedOperationException}.
 */
public final class UnmodifiableOrderedIndexedSet<E> implements OrderedIndexedSet<E> {

    private final OrderedIndexedSet<E> delegate;

    public UnmodifiableOrderedIndexedSet(@Nonnull OrderedIndexedSet<E> delegate) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
    }

    @Override
    public E get(int index) {
        return delegate.get(index);
    }

    @Override
    public E remove(int index) {
        throw new UnsupportedOperationException("This set is unmodifiable");
    }

    @Override
    public int indexOf(Object o) {
        return delegate.indexOf(o);
    }

    @Nonnull
    @Override
    public OrderedIndexedSet<E> subList(int fromIndex, int toIndex) {
        return new UnmodifiableOrderedIndexedSet<>(delegate.subList(fromIndex, toIndex));
    }

    @Override
    public int size() {
        return delegate.size();
    }

    @Override
    public boolean isEmpty() {
        return delegate.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return delegate.contains(o);
    }

    @Nonnull
    @Override
    public Iterator<E> iterator() {
        return new UnmodifiableIterator<>(delegate.iterator());
    }

    @Nonnull
    @Override
    public Object[] toArray() {
        return delegate.toArray();
    }

    @Nonnull
    @Override
    public <T> T[] toArray(@Nonnull T[] a) {
        return delegate.toArray(a);
    }

    @Override
    public boolean add(E e) {
        throw new UnsupportedOperationException("This set is unmodifiable");
    }

    @Override
    public boolean remove(Object o) {
        throw new UnsupportedOperationException("This set is unmodifiable");
    }

    @Override
    public boolean containsAll(@Nonnull Collection<?> c) {
        return delegate.containsAll(c);
    }

    @Override
    public boolean addAll(@Nonnull Collection<? extends E> c) {
        throw new UnsupportedOperationException("This set is unmodifiable");
    }

    @Override
    public boolean retainAll(@Nonnull Collection<?> c) {
        throw new UnsupportedOperationException("This set is unmodifiable");
    }

    @Override
    public boolean removeAll(@Nonnull Collection<?> c) {
        throw new UnsupportedOperationException("This set is unmodifiable");
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException("This set is unmodifiable");
    }

    @Nonnull
    @Override
    public OrderedIndexedSet<E> reversed() {
        return new UnmodifiableOrderedIndexedSet<>(delegate.reversed());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (null == o || getClass() != o.getClass()) return false;
        return delegate.equals(o);
    }

    @Override
    public int hashCode() {
        return delegate.hashCode();
    }

    @Override
    public String toString() {
        return delegate.toString();
    }

    private record UnmodifiableIterator<E>(Iterator<E> it) implements Iterator<E> {

        private UnmodifiableIterator(Iterator<E> it) {
            this.it = Objects.requireNonNull(it);
        }

        @Override
        public boolean hasNext() {
            return it.hasNext();
        }

        @Override
        public E next() {
            return it.next();
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("This iterator is unmodifiable");
        }

        @Override
        public void forEachRemaining(Consumer<? super E> action) {
            it.forEachRemaining(action);
        }
    }
}
