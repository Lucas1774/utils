package com.lucas.utils;

import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class OrderedIndexedSetTest {

    @Test
    void addAndIterationOrder() {
        OrderedIndexedSet<String> s = new OrderedIndexedSet<>();
        assertTrue(s.add("a"));
        assertTrue(s.add("b"));
        assertTrue(s.add("c"));
        assertFalse(s.add("b")); // duplicate

        assertEquals(3, s.size());
        assertArrayEquals(new Object[]{"a", "b", "c"}, s.toArray());

        Iterator<String> it = s.iterator();
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
        OrderedIndexedSet<String> s = new OrderedIndexedSet<>();
        s.add("a");
        s.add("b");
        s.add("c");

        Iterator<String> it = s.iterator();
        assertEquals("a", it.next());
        assertEquals("b", it.next());
        it.remove(); // removes "b"
        assertEquals(2, s.size());
        assertEquals(0, s.indexOf("a"));
        assertEquals(1, s.indexOf("c"));
        assertFalse(s.contains("b"));
    }

    @Test
    void iteratorRemoveWithoutNextThrows() {
        OrderedIndexedSet<String> s = new OrderedIndexedSet<>();
        s.add("x");
        Iterator<String> it = s.iterator();
        assertThrows(IllegalStateException.class, it::remove);
    }

    @Test
    void iteratorFailFastOnExternalModification() {
        OrderedIndexedSet<String> s = new OrderedIndexedSet<>();
        s.add("a");
        s.add("b");
        Iterator<String> it = s.iterator();
        s.add("c"); // structural modification after iterator creation
        assertThrows(ConcurrentModificationException.class, it::next);
    }

    @Test
    void toArrayPreservesOrderGeneric() {
        OrderedIndexedSet<String> s = new OrderedIndexedSet<>();
        s.add("one");
        s.add("two");
        s.add("three");

        String[] arr = s.toArray(new String[0]);
        assertArrayEquals(new String[]{"one", "two", "three"}, arr);
    }

    @Test
    void equalsAndHashCodeOrderInsensitive() {
        OrderedIndexedSet<String> s1 = new OrderedIndexedSet<>();
        OrderedIndexedSet<String> s2 = new OrderedIndexedSet<>();

        s1.add("a");
        s1.add("b");
        s1.add("c");
        s2.add("c");
        s2.add("a");
        s2.add("b");

        assertEquals(s1.size(), s2.size());
        assertEquals(s1, s2);
        assertEquals(s1.hashCode(), s2.hashCode());
    }

    @Test
    void addAllWithDuplicates() {
        OrderedIndexedSet<String> s = new OrderedIndexedSet<>();
        s.add("a");
        boolean modified = s.addAll(Arrays.asList("a", "b", "b", "c"));
        assertTrue(modified);
        assertEquals(3, s.size());
        assertEquals(0, s.indexOf("a"));
        assertEquals(1, s.indexOf("b"));
        assertEquals(2, s.indexOf("c"));
    }

    @Test
    void removeByIndex() {
        OrderedIndexedSet<String> s = new OrderedIndexedSet<>();
        s.add("a");
        s.add("b");
        s.add("c");

        String removed = s.remove(1);
        assertEquals("b", removed);
        assertEquals(2, s.size());
        assertEquals(1, s.indexOf("c"));
        assertFalse(s.contains("b"));
    }

    @Test
    void getFirstLastAndEmptyExceptions() {
        OrderedIndexedSet<Integer> s = new OrderedIndexedSet<>();
        assertThrows(NoSuchElementException.class, s::getFirst);
        assertThrows(NoSuchElementException.class, s::getLast);

        s.add(10);
        s.add(20);
        assertEquals(10, s.getFirst());
        assertEquals(20, s.getLast());
    }

    @Test
    void subListReturnsNewSet() {
        OrderedIndexedSet<String> s = new OrderedIndexedSet<>();
        s.add("a");
        s.add("b");
        s.add("c");
        s.add("d");

        OrderedIndexedSet<String> sub = s.subList(1, 3);
        assertEquals(2, sub.size());
        assertArrayEquals(new Object[]{"b", "c"}, sub.toArray());

        // original unchanged
        assertEquals(4, s.size());
    }

    @Test
    void retainAllAndRemoveAllBehavior() {
        OrderedIndexedSet<String> s = new OrderedIndexedSet<>();
        s.add("a");
        s.add("b");
        s.add("c");
        s.add("d");

        boolean changed = s.retainAll(Arrays.asList("b", "d", "x"));
        assertTrue(changed);
        assertEquals(2, s.size());
        assertTrue(s.contains("b"));
        assertTrue(s.contains("d"));

        boolean removed = s.removeAll(List.of("d"));
        assertTrue(removed);
        assertEquals(1, s.size());
        assertFalse(s.contains("d"));
    }

    @SuppressWarnings("ConstantValue")
    @Test
    void clearEmptiesSet() {
        OrderedIndexedSet<String> s = new OrderedIndexedSet<>();
        s.add("a");
        s.add("b");
        s.clear();
        assertTrue(s.isEmpty());
        assertEquals(0, s.size());
    }

    @Test
    void nullElementSupport() {
        OrderedIndexedSet<String> s = new OrderedIndexedSet<>();
        assertTrue(s.add(null));
        assertTrue(s.contains(null));
        assertEquals(0, s.indexOf(null));
        assertTrue(s.remove(null));
        assertFalse(s.contains(null));
        assertEquals(-1, s.indexOf(null));
    }

    @Test
    void removeReturnsTrueButElementGone() {
        OrderedIndexedSet<String> s = new OrderedIndexedSet<>();
        s.add(null);
        assertTrue(s.remove(null));
        assertFalse(s.contains(null));
        assertEquals(-1, s.indexOf(null));
    }

    @Test
    void testAddAtIndex() {
        OrderedIndexedSet<String> set = new OrderedIndexedSet<>();
        set.add("a");
        set.add("b");
        set.add("c");

        // Insert at middle
        set.add(1, "x");
        assertEquals(4, set.size());
        assertEquals("a", set.get(0));
        assertEquals("x", set.get(1));
        assertEquals("b", set.get(2));
        assertEquals("c", set.get(3));

        // Indices via indexOf
        assertEquals(0, set.indexOf("a"));
        assertEquals(1, set.indexOf("x"));
        assertEquals(2, set.indexOf("b"));
        assertEquals(3, set.indexOf("c"));

        // Insert at start
        set.add(0, "start");
        assertEquals(5, set.size());
        assertEquals("start", set.get(0));
        assertEquals("a", set.get(1));
        assertEquals("x", set.get(2));
        assertEquals("b", set.get(3));
        assertEquals("c", set.get(4));

        // Insert at end
        set.add(set.size(), "end");
        assertEquals(6, set.size());
        assertEquals("end", set.get(5));

        // Duplicate insertion should be ignored
        set.add(2, "a");
        assertEquals(6, set.size()); // size unchanged
        assertEquals(1, set.indexOf("a")); // index unchanged

        assertThrows(IndexOutOfBoundsException.class, () -> set.add(7, "z"));
    }

    @Test
    void testRemoveAtIndex() {
        OrderedIndexedSet<String> set = new OrderedIndexedSet<>();
        set.add("a");
        set.add("b");
        set.add("c");
        set.add("d");

        // Remove middle element
        String removed = set.remove(1); // "b"
        assertEquals("b", removed);
        assertEquals(3, set.size());
        assertEquals("a", set.get(0));
        assertEquals("c", set.get(1));
        assertEquals("d", set.get(2));

        // Check indexOf reflects shift
        assertEquals(0, set.indexOf("a"));
        assertEquals(1, set.indexOf("c"));
        assertEquals(2, set.indexOf("d"));

        // Remove first element
        removed = set.remove(0); // "a"
        assertEquals("a", removed);
        assertEquals(2, set.size());
        assertEquals("c", set.get(0));
        assertEquals("d", set.get(1));

        // Remove last element
        removed = set.remove(set.size() - 1); // "d"
        assertEquals("d", removed);
        assertEquals(1, set.size());
        assertEquals("c", set.get(0));

        // Remove only element left
        removed = set.remove(0); // "c"
        assertEquals("c", removed);
        assertTrue(set.isEmpty());

        // Out of bounds removal
        assertThrows(IndexOutOfBoundsException.class, () -> set.remove(0));
    }

    @Test
    void testReversed() {
        OrderedIndexedSet<String> set = new OrderedIndexedSet<>();
        set.add("a");
        set.add("b");
        set.add("c");

        OrderedIndexedSet<String> reversed = set.reversed();

        // Original unchanged
        assertEquals(3, set.size());
        assertEquals("a", set.get(0));
        assertEquals("b", set.get(1));
        assertEquals("c", set.get(2));

        // Reversed order
        assertEquals(3, reversed.size());
        assertEquals("c", reversed.get(0));
        assertEquals("b", reversed.get(1));
        assertEquals("a", reversed.get(2));

        // Check indexOf
        assertEquals(0, reversed.indexOf("c"));
        assertEquals(1, reversed.indexOf("b"));
        assertEquals(2, reversed.indexOf("a"));

        // Single-element set
        OrderedIndexedSet<String> single = new OrderedIndexedSet<>();
        single.add("x");
        OrderedIndexedSet<String> singleReversed = single.reversed();
        assertEquals(1, singleReversed.size());
        assertEquals("x", singleReversed.get(0));

        // Empty set
        OrderedIndexedSet<String> empty = new OrderedIndexedSet<>();
        OrderedIndexedSet<String> emptyReversed = empty.reversed();
        assertTrue(emptyReversed.isEmpty());
    }

    @SuppressWarnings("OverwrittenKey")
    @Test
    void testReversedDuplicatesIgnored() {
        OrderedIndexedSet<String> set = new OrderedIndexedSet<>();
        set.add("a");
        set.add("b");
        set.add("a"); // duplicate
        OrderedIndexedSet<String> reversed = set.reversed();

        // Duplicate "a" only appears once
        assertEquals(2, reversed.size());
        assertEquals("b", reversed.get(0));
        assertEquals("a", reversed.get(1));
    }
}
