package com.lucas.utils.orderedindexedset;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Spliterator;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderedIndexedSetTest {

    @Test
    void addAndIterationOrder() {
        OrderedIndexedSet<String> set = new OrderedIndexedSetImpl<>();
        assertTrue(set.add("a"));
        assertTrue(set.add("b"));
        assertTrue(set.add("c"));
        assertFalse(set.add("b"));

        assertEquals(3, set.size());
        assertArrayEquals(new Object[]{"a", "b", "c"}, set.toArray());

        Iterator<String> it = set.iterator();
        assertTrue(it.hasNext());
        assertEquals("a", it.next());
        assertTrue(it.hasNext());
        assertEquals("b", it.next());
        assertTrue(it.hasNext());
        assertEquals("c", it.next());
        assertFalse(it.hasNext());
    }

    @Test
    void iteratorRemoveAndReindex() {
        OrderedIndexedSet<String> set = new OrderedIndexedSetImpl<>();
        set.add("a");
        set.add("b");
        set.add("c");

        Iterator<String> it = set.iterator();
        assertEquals("a", it.next());
        assertEquals("b", it.next());
        it.remove();
        assertEquals(2, set.size());
        assertEquals(0, set.indexOf("a"));
        assertEquals(1, set.indexOf("c"));
        assertFalse(set.contains("b"));
    }

    @Test
    void iteratorRemoveWithoutNextThrows() {
        @SuppressWarnings("MismatchedQueryAndUpdateOfCollection") OrderedIndexedSet<String> set =
                new OrderedIndexedSetImpl<>();
        set.add("x");
        Iterator<String> it = set.iterator();
        assertThrows(IllegalStateException.class, it::remove);
    }

    @Test
    void iteratorFailFastOnExternalModification() {
        @SuppressWarnings("MismatchedQueryAndUpdateOfCollection") OrderedIndexedSet<String> set =
                new OrderedIndexedSetImpl<>();
        set.add("a");
        set.add("b");
        Iterator<String> it = set.iterator();
        set.add("c");
        assertThrows(ConcurrentModificationException.class, it::next);
    }

    @Test
    void toArrayPreservesOrderGeneric() {
        OrderedIndexedSet<String> set = new OrderedIndexedSetImpl<>();
        set.add("one");
        set.add("two");
        set.add("three");

        String[] arr = set.toArray(new String[0]);
        assertArrayEquals(new String[]{"one", "two", "three"}, arr);
    }

    @Test
    void equalsAndHashCodeOrderInsensitive() {
        OrderedIndexedSet<String> set1 = new OrderedIndexedSetImpl<>();
        set1.add("a");
        set1.add("b");
        set1.add("c");

        OrderedIndexedSet<String> set2 = new OrderedIndexedSetImpl<>();
        set2.add("c");
        set2.add("a");
        set2.add("b");

        assertEquals(set1.size(), set2.size());
        assertEquals(set1, set2);
        assertEquals(set1.hashCode(), set2.hashCode());
    }

    @Test
    void addAllWithoutDuplicates() {
        OrderedIndexedSet<String> set = new OrderedIndexedSetImpl<>();
        set.add("a");
        boolean modified = set.addAll(Arrays.asList("b", "c", "d"));
        assertTrue(modified);
        assertEquals(4, set.size());
        assertEquals(0, set.indexOf("a"));
        assertEquals(1, set.indexOf("b"));
        assertEquals(2, set.indexOf("c"));
        assertEquals(3, set.indexOf("d"));
    }

    @Test
    void addAllWithDuplicates() {
        OrderedIndexedSet<String> set = new OrderedIndexedSetImpl<>();
        set.add("a");
        boolean modified = set.addAll(Arrays.asList("a", "b", "b", "c"));
        assertTrue(modified);
        assertEquals(3, set.size());
        assertEquals(0, set.indexOf("a"));
        assertEquals(1, set.indexOf("b"));
        assertEquals(2, set.indexOf("c"));
    }

    @Test
    void removeAtIndex() {
        OrderedIndexedSet<String> set = new OrderedIndexedSetImpl<>();
        set.add("a");
        set.add("b");
        set.add("c");

        String removed = set.remove(1);
        assertEquals("b", removed);
        assertEquals(2, set.size());
        assertEquals(1, set.indexOf("c"));
        assertFalse(set.contains("b"));
    }

    @Test
    void getFirstLastAndEmptyExceptions() {
        OrderedIndexedSet<Integer> set = new OrderedIndexedSetImpl<>();
        assertThrows(NoSuchElementException.class, set::getFirst);
        assertThrows(NoSuchElementException.class, set::getLast);

        set.add(10);
        set.add(20);
        assertEquals(10, set.getFirst());
        assertEquals(20, set.getLast());
    }

    @Test
    void removeFirstLastAndEmptyExceptions() {
        OrderedIndexedSet<Integer> set = new OrderedIndexedSetImpl<>();
        assertThrows(NoSuchElementException.class, set::removeFirst);
        assertThrows(NoSuchElementException.class, set::removeLast);

        set.add(10);
        set.add(20);
        assertEquals(10, set.removeFirst());
        assertEquals(20, set.removeLast());
        assertTrue(set.isEmpty());
    }

    @Test
    void subListReturnsNewSet() {
        OrderedIndexedSet<String> set = new OrderedIndexedSetImpl<>();
        set.add("a");
        set.add("b");
        set.add("c");
        set.add("d");

        OrderedIndexedSet<String> sub = set.subList(1, 3);
        assertEquals(2, sub.size());
        assertArrayEquals(new Object[]{"b", "c"}, sub.toArray());

        assertEquals(4, set.size());
    }

    @Test
    void retainAllAndRemoveAllBehavior() {
        OrderedIndexedSet<String> set = new OrderedIndexedSetImpl<>();
        set.add("a");
        set.add("b");
        set.add("c");
        set.add("d");

        boolean changed = set.retainAll(Arrays.asList("b", "d", "x"));
        assertTrue(changed);
        assertEquals(2, set.size());
        assertTrue(set.contains("b"));
        assertTrue(set.contains("d"));

        boolean removed = set.removeAll(List.of("d"));
        assertTrue(removed);
        assertEquals(1, set.size());
        assertFalse(set.contains("d"));
    }

    @SuppressWarnings("ConstantValue")
    @Test
    void clearEmptiesSet() {
        OrderedIndexedSet<String> set = new OrderedIndexedSetImpl<>();
        set.add("a");
        set.add("b");
        set.clear();
        assertTrue(set.isEmpty());
        assertEquals(0, set.size());
    }

    @Test
    void nullElementSupport() {
        OrderedIndexedSet<String> set = new OrderedIndexedSetImpl<>();
        assertTrue(set.add(null));
        assertTrue(set.contains(null));
        assertEquals(0, set.indexOf(null));
        assertTrue(set.remove(null));
        assertFalse(set.contains(null));
        assertEquals(-1, set.indexOf(null));
    }

    @Test
    void removeReturnsTrueButElementGone() {
        OrderedIndexedSet<String> set = new OrderedIndexedSetImpl<>();
        set.add(null);
        assertTrue(set.remove(null));
        assertFalse(set.contains(null));
        assertEquals(-1, set.indexOf(null));
    }

    @Test
    void removeAtIndexMultipleOperations() {
        OrderedIndexedSet<String> set = new OrderedIndexedSetImpl<>();
        set.add("a");
        set.add("b");
        set.add("c");
        set.add("d");

        String removed = set.remove(1);
        assertEquals("b", removed);
        assertEquals(3, set.size());
        assertEquals("a", set.get(0));
        assertEquals("c", set.get(1));
        assertEquals("d", set.get(2));
        assertEquals(0, set.indexOf("a"));
        assertEquals(1, set.indexOf("c"));
        assertEquals(2, set.indexOf("d"));

        removed = set.remove(0);
        assertEquals("a", removed);
        assertEquals(2, set.size());
        assertEquals("c", set.get(0));
        assertEquals("d", set.get(1));

        removed = set.remove(set.size() - 1);
        assertEquals("d", removed);
        assertEquals(1, set.size());
        assertEquals("c", set.get(0));

        removed = set.remove(0);
        assertEquals("c", removed);
        assertTrue(set.isEmpty());

        assertThrows(IndexOutOfBoundsException.class, () -> set.remove(0));
    }

    @Test
    void reversed() {
        OrderedIndexedSet<String> set = new OrderedIndexedSetImpl<>();
        set.add("a");
        set.add("b");
        set.add("c");

        assertEquals(3, set.size());
        assertEquals("a", set.get(0));
        assertEquals("b", set.get(1));
        assertEquals("c", set.get(2));

        OrderedIndexedSet<String> reversed = set.reversed();
        assertEquals(3, reversed.size());
        assertEquals("c", reversed.get(0));
        assertEquals("b", reversed.get(1));
        assertEquals("a", reversed.get(2));
        assertEquals(0, reversed.indexOf("c"));
        assertEquals(1, reversed.indexOf("b"));
        assertEquals(2, reversed.indexOf("a"));

        OrderedIndexedSet<String> single = new OrderedIndexedSetImpl<>();
        single.add("x");
        OrderedIndexedSet<String> singleReversed = single.reversed();
        assertEquals(1, singleReversed.size());
        assertEquals("x", singleReversed.get(0));

        OrderedIndexedSet<String> empty = new OrderedIndexedSetImpl<>();
        OrderedIndexedSet<String> emptyReversed = empty.reversed();
        assertTrue(emptyReversed.isEmpty());
    }

    @SuppressWarnings("OverwrittenKey")
    @Test
    void reversedDuplicatesIgnored() {
        OrderedIndexedSet<String> set = new OrderedIndexedSetImpl<>();
        set.add("a");
        set.add("b");
        set.add("a");
        OrderedIndexedSet<String> reversed = set.reversed();

        assertEquals(2, reversed.size());
        assertEquals("b", reversed.get(0));
        assertEquals("a", reversed.get(1));
    }

    @Test
    void ofCreatesUnmodifiableSet() {
        OrderedIndexedSet<String> set = OrderedIndexedSet.of("a", "b", "a");

        assertEquals(2, set.size());
        assertEquals("a", set.get(0));
        assertEquals("b", set.get(1));
        assertTrue(set.contains("a"));
        assertEquals(0, set.indexOf("a"));
        assertEquals(1, set.indexOf("b"));

        Iterator<String> it = set.iterator();
        assertTrue(it.hasNext());
        assertEquals("a", it.next());
        assertTrue(it.hasNext());
        assertEquals("b", it.next());
        assertFalse(it.hasNext());

        assertThrows(UnsupportedOperationException.class, () -> set.add("c"));
        assertThrows(UnsupportedOperationException.class, () -> set.remove("a"));
        assertThrows(UnsupportedOperationException.class, set::clear);

        Iterator<String> it2 = set.iterator();
        it2.next();
        assertThrows(UnsupportedOperationException.class, it2::remove);
    }

    @Test
    void collectorModifiable() {
        List<String> input = List.of("one", "two", "one", "three", "two");

        OrderedIndexedSet<String> set = input.stream().collect(OrderedIndexedSet.toOrderedIndexedSet());

        assertEquals(3, set.size());
        assertEquals("one", set.get(0));
        assertEquals("two", set.get(1));
        assertEquals("three", set.get(2));

        assertTrue(set.add("four"));
        assertEquals(4, set.size());
        assertEquals("four", set.get(3));
    }

    @Test
    void collectorUnmodifiable() {
        List<String> input = List.of("one", "two", "one", "three", "two");

        OrderedIndexedSet<String> set = input.stream().collect(OrderedIndexedSet.toUnmodifiableOrderedIndexedSet());

        assertEquals(3, set.size());
        assertEquals("one", set.get(0));
        assertEquals("two", set.get(1));
        assertEquals("three", set.get(2));

        assertThrows(UnsupportedOperationException.class, () -> set.add("four"));
    }

    @Test
    void spliteratorCharacteristicsAndParallelCollection() {
        OrderedIndexedSet<String> set =
                Stream.of("one", "two", "three").collect(OrderedIndexedSet.toOrderedIndexedSet());
        set.add("four");

        Spliterator<String> sp = set.spliterator();
        int ch = sp.characteristics();
        assertNotEquals(0, ch & Spliterator.ORDERED);
        assertNotEquals(0, ch & Spliterator.DISTINCT);
        assertNotEquals(0, ch & Spliterator.SIZED);
        assertNotEquals(0, ch & Spliterator.SUBSIZED);

        List<String> fromSpliterator = new ArrayList<>();
        sp.forEachRemaining(fromSpliterator::add);
        assertEquals(List.of("one", "two", "three", "four"), fromSpliterator);

        OrderedIndexedSet<String> fromParallel =
                Stream.of("a", "b", "a", "c").parallel().collect(OrderedIndexedSet.toOrderedIndexedSet());
        assertEquals(3, fromParallel.size());
        assertEquals("a", fromParallel.get(0));
        assertEquals("b", fromParallel.get(1));
        assertEquals("c", fromParallel.get(2));
    }

    @Test
    void copyOfCreatesUnmodifiableCopy() {
        List<String> input = List.of("a", "b", "c");
        OrderedIndexedSet<String> copy = OrderedIndexedSet.copyOf(input);

        assertEquals(3, copy.size());
        assertEquals("a", copy.get(0));
        assertEquals("b", copy.get(1));
        assertEquals("c", copy.get(2));

        assertThrows(UnsupportedOperationException.class, () -> copy.add("d"));
        assertThrows(UnsupportedOperationException.class, () -> copy.remove("a"));
    }
}
