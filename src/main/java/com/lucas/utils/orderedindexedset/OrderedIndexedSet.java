package com.lucas.utils.orderedindexedset;

import jakarta.annotation.Nonnull;

import java.util.*;
import java.util.stream.Collector;

/**
 * A {@link Set} implementation that maintains insertion order and allows indexed access to elements.
 * <p>
 * It is intended for use cases where both deduplication and insertion order must be preserved.
 * This interface combines characteristics of both a {@link List} and a {@link Set}:
 * <ul>
 *   <li>Preserves insertion order.</li>
 *   <li>Deduplicates elements (set semantics).</li>
 *   <li>Provides standard {@link Set} <code>equals</code> and <code>hashCode</code> semantics (order-insensitive).</li>
 *   <li>Adding an element always places it at the end of the collection, if not already present.</li>
 *   <li>Supports most {@link List}-like operations, except:
 *     <ul>
 *       <li>Index-based insertions. Read and removal operations supported.</li>
 *       <li><code>lastIndexOf(Object)</code> semantically incorrect for deduplicated collections.</li>
 *       <li><code>listIterator()</code> not supported as it implicitly supports index-based insertions.</li>
 *     </ul>
 *   </li>
 * </ul>
 * <p>
 * <b>Notes:</b>
 * <ul>
 *   <li>Not necessarily thread-safe.</li>
 *   <li>Intended for moderate-sized collections where simplicity and correctness are preferred.</li>
 * </ul>
 *
 * @param <E> the type of elements maintained by this set
 */
public sealed interface OrderedIndexedSet<E> extends Set<E> permits OrderedIndexedSetImpl, UnmodifiableOrderedIndexedSet {

    /**
     * Creates an {@link UnmodifiableOrderedIndexedSet} containing the given elements
     * in order of appearance. Duplicate elements are ignored. Null elements are not
     * permitted.
     *
     * @param input elements to include, in order
     * @param <E>   element type
     * @return an unmodifiable {@code OrderedIndexedSet} with the given elements
     * @throws NullPointerException if {@code input} or any element is {@code null}
     */
    @SafeVarargs
    static <E> OrderedIndexedSet<E> of(E... input) {
        for (Object o : input) {
            Objects.requireNonNull(o);
        }
        return new UnmodifiableOrderedIndexedSet<>(new OrderedIndexedSetImpl<>(Arrays.asList(input)));
    }

    /**
     * Creates an {@link UnmodifiableOrderedIndexedSet} containing the elements of {@code c}
     * in order of appearance. Duplicate elements are ignored. Null elements are not
     * permitted.
     *
     * @param c collection to copy
     * @param <E> element type
     * @return an unmodifiable {@code OrderedIndexedSet} with the elements of the given collection
     * @throws NullPointerException if {@code c} or any element is {@code null}
     */
    static <E> OrderedIndexedSet<E> copyOf(Collection<? extends E> c) {
        for (E e : c) {
            Objects.requireNonNull(e, "elements must not be null");
        }
        OrderedIndexedSetImpl<E> impl = new OrderedIndexedSetImpl<>(c);
        return new UnmodifiableOrderedIndexedSet<>(impl);
    }

    /**
     * Returns a collector that accumulates elements into a {@link OrderedIndexedSetImpl}
     * preserving encounter order and ignoring duplicates.
     *
     * @param <T> element type
     * @return a collector producing an {@code OrderedIndexedSet}
     */
    static <T> Collector<T, OrderedIndexedSet<T>, OrderedIndexedSet<T>> toOrderedIndexedSet() {
        return Collector.of(
                OrderedIndexedSetImpl::new,
                OrderedIndexedSet::add,
                (left, right) -> {
                    left.addAll(right);
                    return left;
                }
        );
    }

    /**
     * Returns a collector that accumulates elements into a {@link UnmodifiableOrderedIndexedSet}
     * preserving encounter order and ignoring duplicates.
     *
     * @param <T> element type
     * @return a collector producing an {@code OrderedIndexedSet}
     */
    static <T> Collector<T, OrderedIndexedSet<T>, OrderedIndexedSet<T>> toUnmodifiableOrderedIndexedSet() {
        return Collector.of(
                OrderedIndexedSetImpl::new,
                OrderedIndexedSet::add,
                (left, right) -> {
                    left.addAll(right);
                    return left;
                },
                UnmodifiableOrderedIndexedSet::new
        );
    }


    /**
     * Returns the element at the specified position in this set.
     *
     * @param index index of the element to return
     * @return the element at the specified position
     * @throws IndexOutOfBoundsException if the index is out of range
     */
    E get(int index);

    /**
     * Removes and returns the element at the specified position in this set.
     *
     * @param index the index of the element to remove
     * @return the element that was removed
     * @throws IndexOutOfBoundsException if the index is out of range
     */
    E remove(int index);

    /**
     * Returns the index of the specified element in this set, or {@code -1} if this set does not contain the element.
     *
     * @param o element to search for
     * @return the index of the element, or {@code -1} if not found
     */
    int indexOf(Object o);

    /**
     * Returns a new set containing the elements between the specified {@code fromIndex}, inclusive, and {@code toIndex}, exclusive.
     * <p>
     * This is not a view, meaning modifications to the returned set
     * (such as adding or removing elements) do not affect the original set, and changes to the
     * original set do not affect the returned set. Both sets will contain references to the same objects,
     * so if they are mutable themselves, modifications to them will be visible from both.
     *
     * @param fromIndex low endpoint (inclusive) of the subSet
     * @param toIndex   high endpoint (exclusive) of the subSet
     * @return a new {@code OrderedIndexedSet} containing the specified range
     * @throws IndexOutOfBoundsException if the indices are out of range
     */
    @Nonnull
    OrderedIndexedSet<E> subList(int fromIndex, int toIndex);

    /**
     * Returns a new {@code OrderedIndexedSet} with the elements in reverse order.
     * <p>
     * This is not a view, meaning modifications to the returned set
     * (such as adding or removing elements) do not affect the original set, and changes to the
     * original set do not affect the returned set. Both sets will contain references to the same objects,
     * so if they are mutable themselves, modifications to them will be visible from both.
     *
     * @return a new reversed {@code OrderedIndexedSet}
     */
    @Nonnull
    OrderedIndexedSet<E> reversed();

    /**
     * Returns a {@link Spliterator} over the elements in this set.
     * <p>
     * The spliterator is ordered, distinct, sized, and subsized.
     *
     * @return a {@code Spliterator} over the elements in this set
     */
    @Override
    default
    @Nonnull Spliterator<E> spliterator() {
        return Spliterators.spliterator(
                this,
                Spliterator.ORDERED | Spliterator.DISTINCT | Spliterator.SIZED | Spliterator.SUBSIZED
        );
    }

    /**
     * Returns the first element in this set.
     *
     * @return the first element
     * @throws NoSuchElementException if the set is empty
     */
    default E getFirst() {
        if (isEmpty()) {
            throw new NoSuchElementException();
        } else {
            return get(0);
        }
    }

    /**
     * Returns the last element in this set.
     *
     * @return the last element
     * @throws NoSuchElementException if the set is empty
     */
    default E getLast() {
        if (isEmpty()) {
            throw new NoSuchElementException();
        } else {
            return get(size() - 1);
        }
    }

    /**
     * Removes and returns the first element in this set.
     *
     * @return the removed first element
     * @throws NoSuchElementException if the set is empty
     */
    default E removeFirst() {
        if (isEmpty()) {
            throw new NoSuchElementException();
        } else {
            return remove(0);
        }
    }

    /**
     * Removes and returns the last element in this set.
     *
     * @return the removed last element
     * @throws NoSuchElementException if the set is empty
     */
    default E removeLast() {
        if (isEmpty()) {
            throw new NoSuchElementException();
        } else {
            return remove(size() - 1);
        }
    }
}
