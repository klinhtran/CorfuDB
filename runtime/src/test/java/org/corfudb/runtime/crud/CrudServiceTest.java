package org.corfudb.runtime.crud;

import static org.corfudb.runtime.kv.core.DbKey.MessageKey;
import static org.corfudb.runtime.kv.core.DbValue.MessageValue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.protobuf.ByteString;
import org.corfudb.runtime.Api;
import org.corfudb.runtime.collections.CorfuTable;
import org.corfudb.runtime.kv.core.DbVersion;
import org.corfudb.runtime.kv.core.Record;
import org.junit.jupiter.api.Test;

class CrudServiceTest {

    @Test
    public void testGet() {
        ByteString keyName = ByteString.copyFromUtf8("key");
        ByteString data = ByteString.copyFromUtf8("data");

        Api.Key protoKey = Api.Key.newBuilder()
                .setKeyName(keyName)
                .build();

        MessageKey key = new MessageKey(protoKey);

        CorfuTable<MessageKey, Record<MessageKey, MessageValue, DbVersion>> table = null;

        CrudService<MessageKey, MessageValue, DbVersion> crud = new CrudService<>(table);
        AsyncCrudService<MessageKey, MessageValue, DbVersion> asyncCrud = new AsyncCrudService<>(table);

        Record<MessageKey, MessageValue, DbVersion> record = crud.get(key).apply().get();

        assertEquals(keyName, record.getKey().get().getKeyName());
        assertEquals(data, record.getValue().get());

        record = asyncCrud.get(key).apply().join();
        assertEquals(keyName, record.getKey().get().getKeyName());
        assertEquals(data, record.getValue().get().getData());
    }
}
