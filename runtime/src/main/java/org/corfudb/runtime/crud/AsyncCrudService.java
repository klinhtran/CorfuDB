package org.corfudb.runtime.crud;

import lombok.Builder;
import lombok.NonNull;
import org.corfudb.runtime.collections.CorfuTable;
import org.corfudb.runtime.kv.core.DbKey;
import org.corfudb.runtime.kv.core.DbValue;
import org.corfudb.runtime.kv.core.DbVersion;
import org.corfudb.runtime.kv.core.Record;
import org.corfudb.runtime.kv.service.Crud;

import java.util.concurrent.CompletableFuture;

@Builder
public class AsyncCrudService<Key extends DbKey, Value extends DbValue, Version extends DbVersion>
        implements Crud {

    @NonNull
    private final CorfuTable<Key, Record<Key, Value, Version>> table;

    CrudOp<CompletableFuture<Record<Key, Value, Version>>> create(Key key, Value value) {

        return new CrudOp<CompletableFuture<Record<Key, Value, Version>>>() {

            @Override
            public CrudOpType opType() {
                return CrudOpType.CREATE;
            }

            @Override
            public CompletableFuture<Record<Key, Value, Version>> apply() {
                return CompletableFuture.supplyAsync(() -> {
                    Record<Key, Value, Version> record = new Record<>(key, value, null);
                    table.put(key, record);
                    return record;
                });
            }
        };
    }

    CrudOp<CompletableFuture<Record<Key, Value, Version>>> get(Key key) {
        return new CrudOp<CompletableFuture<Record<Key, Value, Version>>>() {
            @Override
            public CrudOpType opType() {
                return CrudOpType.GET;
            }

            @Override
            public CompletableFuture<Record<Key, Value, Version>> apply() {
                return CompletableFuture.supplyAsync(() -> table.get(key));
            }
        };
    }
}