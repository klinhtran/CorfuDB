package org.corfudb.runtime.crud;

import lombok.Builder;
import lombok.NonNull;
import org.corfudb.runtime.collections.CorfuTable;
import org.corfudb.runtime.kv.core.DbKey;
import org.corfudb.runtime.kv.core.DbValue;
import org.corfudb.runtime.kv.core.DbVersion;
import org.corfudb.runtime.kv.core.OperationResult;
import org.corfudb.runtime.kv.core.Record;
import org.corfudb.runtime.kv.service.Crud;

@Builder
public class CrudService<Key extends DbKey, Value extends DbValue, Version extends DbVersion>
        implements Crud {

    @NonNull
    private final CorfuTable<Key, Record<Key, Value, Version>> table;

    CrudOp<OperationResult<Record<Key, Value, Version>>> create(Key key, Value value) {

        return new CrudOp<OperationResult<Record<Key, Value, Version>>>() {

            @Override
            public CrudOpType opType() {
                return CrudOpType.CREATE;
            }

            @Override
            public OperationResult<Record<Key, Value, Version>> apply() {
                return OperationResult.of(() -> {
                    Record<Key, Value, Version> record = new Record<>(key, value, null);
                    table.put(key, record);
                    return record;
                });
            }
        };
    }

    CrudOp<OperationResult<Record<Key, Value, Version>>> get(Key key) {
        return new CrudOp<OperationResult<Record<Key, Value, Version>>>() {
            @Override
            public CrudOpType opType() {
                return CrudOpType.GET;
            }

            @Override
            public OperationResult<Record<Key, Value, Version>> apply() {
                return OperationResult.of(() -> table.get(key));
            }
        };
    }
}