package org.corfudb.runtime.kv.core;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import java.util.UUID;

class DbKeyTest {

    @Test
    public void testStringKey(){
        DbKey.StringKey strKeyA = new DbKey.StringKey("a");
        DbKey.StringKey strKeyB = new DbKey.StringKey("b");

        assertEquals(1, strKeyA.compareTo(strKeyB));
    }

    @Test
    public void testUuidKey(){
        DbKey.UuidKey key1 = new DbKey.UuidKey(UUID.fromString("1"));
        DbKey.UuidKey key2 = new DbKey.UuidKey(UUID.fromString("2"));

        assertEquals(1, key1.compareTo(key2));
    }

}