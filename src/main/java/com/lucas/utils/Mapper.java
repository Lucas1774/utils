package com.lucas.utils;

import com.lucas.utils.exception.MappingException;

/**
 * Functional interface for mapping a key to a value throwing a checked exception on error
 *
 * @param <K> the input type
 * @param <V> the output type
 */
@SuppressWarnings("unused")
public interface Mapper<K, V> {

    /**
     * Maps the given key to a value.
     *
     * @param key the input to map
     * @return the mapped value
     * @throws MappingException if the mapping cannot be performed
     */
    @SuppressWarnings("RedundantThrows")
    V map(K key) throws MappingException;
}
