package org.corfudb.runtime.kv.core;

import lombok.AllArgsConstructor;
import org.corfudb.runtime.Api;

import java.util.NoSuchElementException;
import java.util.function.Supplier;

/**
 * Type contract representing a data-containing entity.
 */
@FunctionalInterface
public interface DbValue<T> extends Supplier<T> {

    /**
     * Implementation of {@link DbValue} that represents an "empty" value.
     * This class should carry the same connotation as the return value of
     * {@linkplain java.util.Optional#empty() Optional&lt;Value&gt;.empty()}.
     */
    final class NoneValue implements DbValue<Void> {
        /**
         * Static singleton instance of {@link NoneValue}.
         */
        private static final DbValue<?> INSTANCE = new NoneValue();

        private NoneValue() {
        }

        @Override
        public Void get() {
            throw new NoSuchElementException("No value present");
        }

        @Override
        public Type type() {
            return Type.NONE;
        }

        @Override
        public String toString() {
            return "Value{None}";
        }
    }

    @AllArgsConstructor
    class MessageValue implements DbValue<Api.Value> {
        private final Api.Value value;

        @Override
        public Api.Value get() {
            return value;
        }
    }

    /**
     * Enumeration of possible value types.
     * All types should have value identifier sized exactly 4-bytes as type signature.
     */
    enum Type {
        /**
         * Special type denoting that the associated {@link DbValue} instance is not a value.
         */
        NONE("NONE"),
        /**
         * Data is in unspecified encoding.
         */
        BINARY("BNRY");

        private final String val;

        public String value() {
            return val;
        }

        Type(String type) {
            this.val = type;
        }

        public static Type from(final String type) {
            for (Type item : Type.values()) {
                if (type.equals(item.val)) {
                    return item;
                }
            }
            throw new IllegalArgumentException(type + " is not a valid type");
        }
    }

    /**
     * Data type.
     *
     * @return the type of data as {@link Type}.
     */
    default Type type() {
        return DbValue.Type.BINARY;
    }

    /**
     * Returns a {@link DbValue} instance that does not represent any value.
     *
     * @return an instance of {@link DbValue}.
     */
    static <T> DbValue<T> none() {
        return (DbValue<T>) NoneValue.INSTANCE;
    }
}

