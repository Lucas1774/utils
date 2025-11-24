package com.lucas.utils.orderedindexedset;

import jakarta.annotation.Nonnull;

import java.util.*;

public final class OrderedIndexedSetImpl<E> implements OrderedIndexedSet<E> {

    private final List<E> list;
    private final Map<E, Integer> map;
    private int modCount = 0;

    public OrderedIndexedSetImpl() {
        list = new ArrayList<>();
        map = new HashMap<>();
    }


    public OrderedIndexedSetImpl(Collection<? extends E> c) {
        this();
        addAll(c);
    }

    @Override
    public E get(int index) {
        return list.get(index);
    }

    @Override
    public E remove(int index) {
        E removed = list.remove(index);
        map.remove(removed);
        for (int i = index; i < list.size(); i++) {
            E elem = list.get(i);
            map.put(elem, i);
        }
        modCount++;
        return removed;
    }

    @SuppressWarnings("SuspiciousMethodCalls")
    public int indexOf(Object o) {
        Integer idx = map.get(o);
        return null == idx ? -1 : idx;
    }

    @Nonnull
    @Override
    public OrderedIndexedSet<E> subList(int fromIndex, int toIndex) {
        if (0 > fromIndex || toIndex > size() || fromIndex > toIndex)
            throw new IndexOutOfBoundsException("fromIndex=" + fromIndex + " toIndex=" + toIndex + " size=" + size());
        OrderedIndexedSet<E> result = new OrderedIndexedSetImpl<>();
        for (int i = fromIndex; i < toIndex; i++) {
            result.add(list.get(i));
        }
        return result;
    }

    @Override
    public int size() {
        return list.size();
    }

    @Override
    public boolean isEmpty() {
        return list.isEmpty();
    }

    @Override
    @SuppressWarnings("SuspiciousMethodCalls")
    public boolean contains(Object o) {
        return map.containsKey(o);
    }

    @Nonnull
    @Override
    public Iterator<E> iterator() {
        return new Itr();
    }

    @Nonnull
    @Override
    public Object[] toArray() {
        return list.toArray();
    }

    @Nonnull
    @Override
    public <T> T[] toArray(@Nonnull T[] a) {
        return list.toArray(a);
    }

    @Override
    public boolean add(E e) {
        if (map.containsKey(e)) return false;
        list.add(e);
        map.put(e, list.size() - 1);
        modCount++;
        return true;
    }

    @Override
    public boolean remove(Object o) {
        Integer idx = map.remove(o);
        if (null == idx) return false;
        list.remove(idx.intValue());
        for (int i = idx; i < list.size(); i++) {
            E elem = list.get(i);
            map.put(elem, i);
        }
        modCount++;
        return true;
    }

    @Override
    public boolean containsAll(@Nonnull Collection<?> c) {
        for (Object o : c) {
            if (!contains(o)) return false;
        }
        return true;
    }

    @Override
    public boolean addAll(@Nonnull Collection<? extends E> c) {
        Objects.requireNonNull(c);
        boolean modified = false;
        for (E e : c) {
            if (!map.containsKey(e)) {
                list.add(e);
                map.put(e, list.size() - 1);
                modified = true;
            }
        }
        if (modified) modCount++;
        return modified;
    }

    @Override
    public boolean retainAll(@Nonnull Collection<?> c) {
        Objects.requireNonNull(c);
        int originalSize = list.size();
        ArrayList<E> newList = new ArrayList<>(list.size());
        map.clear();
        for (E e : list) {
            if (c.contains(e)) {
                map.put(e, newList.size());
                newList.add(e);
            }
        }
        list.clear();
        list.addAll(newList);
        boolean changed = list.size() != originalSize;
        if (changed) modCount++;
        return changed;
    }

    @Override
    public boolean removeAll(@Nonnull Collection<?> c) {
        Objects.requireNonNull(c);
        int originalSize = list.size();
        ArrayList<E> newList = new ArrayList<>(list.size());
        map.clear();
        for (E e : list) {
            if (!c.contains(e)) {
                map.put(e, newList.size());
                newList.add(e);
            }
        }
        list.clear();
        list.addAll(newList);
        boolean changed = list.size() != originalSize;
        if (changed) modCount++;
        return changed;
    }

    @Override
    public void clear() {
        list.clear();
        map.clear();
        modCount++;
    }

    @Nonnull
    @Override
    public OrderedIndexedSet<E> reversed() {
        OrderedIndexedSet<E> result = new OrderedIndexedSetImpl<>();
        for (int i = size() - 1; 0 <= i; i--) {
            result.add(list.get(i));
        }
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Set<?> other)) return false;
        if (other.size() != size()) return false;
        return map.keySet().equals(other);
    }

    @Override
    public int hashCode() {
        int h = 0;
        for (E e : map.keySet()) h += (null == e ? 0 : e.hashCode());
        return h;
    }

    @Override
    public String toString() {
        return list.toString();
    }

    private class Itr implements Iterator<E> {

        private final ListIterator<E> it = list.listIterator();
        private int expectedModCount = modCount;
        private E lastReturned = null;

        private void checkForConcurrentModification() {
            if (expectedModCount != modCount) throw new ConcurrentModificationException();
        }

        @Override
        public boolean hasNext() {
            checkForConcurrentModification();
            return it.hasNext();
        }

        @Override
        public E next() {
            checkForConcurrentModification();
            lastReturned = it.next();
            return lastReturned;
        }

        @Override
        public void remove() {
            checkForConcurrentModification();
            if (null == lastReturned) {
                throw new IllegalStateException();
            }
            int removedIndex = it.previousIndex();
            it.remove();
            map.remove(lastReturned);
            for (int i = removedIndex; i < list.size(); i++) {
                map.put(list.get(i), i);
            }
            lastReturned = null;
            modCount++;
            expectedModCount++;
        }
    }
}
