package org.corfudb.runtime.kv.core;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * Type contract representing an instance of associative-mapping.
 *
 * @see DbKey
 * @see DbValue
 * @see DbVersion
 */
@AllArgsConstructor
@EqualsAndHashCode
@Getter
public final class Record<Key extends DbKey, Value extends DbValue, Version extends DbVersion> {
    private final Key key;
    private final Value value;
    private final Version version;
}
