package org.corfudb.runtime.kv.service;

/**
 * caching - mvcc
 * we can keep operations history
 * strongly typed - transparent types
 * flexibly
 * friendly api - autocomplete/intellisence
 */
public interface Crud {

    enum CrudOpType {
        CREATE, GET, UPDATE, DELETE
    }

    interface CrudOp<Result> {
        CrudOpType opType();

        Result apply();
    }
}
