package org.corfudb.runtime.kv.core;

import lombok.AllArgsConstructor;
import org.corfudb.runtime.Api;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * Type contract representing of the key of a key-value associative mapping.
 */
public interface DbKey<T> extends Comparable<DbKey<T>>, Supplier<T> {

    @AllArgsConstructor
    class StringKey implements DbKey<String> {
        private final String key;

        @Override
        public int compareTo(DbKey<String> other) {
            return key.compareTo(other.get());
        }

        @Override
        public String get() {
            return key;
        }
    }

    @AllArgsConstructor
    class UuidKey implements DbKey<UUID> {
        private final UUID key;

        @Override
        public int compareTo(DbKey<UUID> other) {
            return key.compareTo(other.get());
        }

        @Override
        public UUID get() {
            return key;
        }
    }

    @AllArgsConstructor
    class MessageKey implements DbKey<Api.Key> {
        private final Api.Key key;

        @Override
        public int compareTo(DbKey<Api.Key> other) {
            return -1;
        }

        @Override
        public Api.Key get() {
            return key;
        }
    }
}
