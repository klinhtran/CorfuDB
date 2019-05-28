package org.corfudb.runtime.collections;

import com.google.common.reflect.TypeToken;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.corfudb.annotations.Accessor;
import org.corfudb.annotations.ConflictParameter;
import org.corfudb.annotations.CorfuObject;
import org.corfudb.annotations.DontInstrument;
import org.corfudb.annotations.Mutator;
import org.corfudb.annotations.MutatorAccessor;
import org.corfudb.runtime.view.OrderedGuidGenerator;

import java.util.*;

/**
 * Persisted Queue supported by CorfuDB using distributed State Machine Replication.
 * Entries enqueued are backed by a LinkedHashMap where each entry is mapped to a unique and
 * globally ordered generated id that is returned upon successful <b>enqueue()</b>.
 * <b>entryList()</b> returns the enqueued entries along with their ids sorted by
 * the order in which their <b>enqueue()</b> operations materialized.
 * Instead of a dequeue() this Queue supports a <b>remove()</b> which accepts the id of the element.
 * Entries cannot be modified in-place and but can be removed from anywhere in the queue.
 *
 * Created by hisundar on 5/8/19.
 *
 * @param <E>   Type of the entry to be enqueued into the persisted queue
 */
@Slf4j
@CorfuObject
public class CorfuQueue<E> implements ISMRObject {
    /** The "main" linked map which contains the primary key-value mappings. */
    private final Map<UUID, E> mainMap = new LinkedHashMap<>();

    private final OrderedGuidGenerator guidGenerator;

    public CorfuQueue(OrderedGuidGenerator guidGenerator) {
        this.guidGenerator = guidGenerator;
    }

    /** Returns the size of the queue at a given point in time. */
    @Accessor
    public int size() {
        return mainMap.size();
    }

    /**
     * Appends the specified element at the end of this unbounded queue.
     * In a distributed system, the order of insertions cannot be guaranteed
     * unless a transaction is used.
     * Capacity restrictions and backoffs must be implemented outside this
     * interface. Consider validating the size of the queue against a high
     * watermark before enqueue.
     *
     * @param e the element to add
     * @throws NullPointerException if the specified element is null and this
     *         queue does not permit null elements
     * @throws IllegalArgumentException if some property of the specified
     *         element prevents it from being added to this queue
     * @return Globally ordered and unique ID representing this entry in the persistent queue
     */
    public UUID enqueue(E e) {
        UUID id = guidGenerator.nextUUID();
        put(id, e);
        return id;
    }

    /** CorfuQueueRecord encapsulates each entry enqueued into CorfuQueue with its unique ID.
     * It is a read-only type returned by the entryList() method.
     * The ID returned here can be used for both point get()s as well as remove() operations
     * on this Queue.
     *
     * @param <E>
     */
    public static class CorfuQueueRecord<E> {
        /**
         * This ID represents the entry and its order in the Queue.
         * This implies that it is unique and comparable with other IDs
         * returned from CorfuQueue methods with respect to its enqueue order.
         */
        @Getter
        final UUID ID;

        @Getter
        final E entry;

        CorfuQueueRecord(UUID id, E entry) {
            this.ID = id;
            this.entry = entry;
        }
    }

    /**
     * Returns a List of CorfuQueueRecords sorted by the order in which the enqueue materialized.
     *
     * {@inheritDoc}
     *
     * <p>This function currently does not return a view like the java.util implementation,
     * and changes to the entryList will *not* be reflected in the map. </p>
     *
     * @param maxEntries - Limit the number of entries returned from start of the queue
     *                   -1 implies all entries
     * @return List of Entries sorted by their enqueue order
     */
    public List<CorfuQueueRecord<E>> entryList(int maxEntries) {
        if (maxEntries == -1) {
            maxEntries = Integer.MAX_VALUE;
        }
        List<CorfuQueueRecord<E>> copy = new ArrayList<>(Math.min(mainMap.size(), maxEntries));
        for (Map.Entry<UUID, E> entry : mainMap.entrySet()) {
            copy.add(new CorfuQueueRecord<>(entry.getKey(),
                    entry.getValue()));
            if (--maxEntries == 0) {
                break;
            }
        }
        return copy;
    }

    @Accessor
    public boolean isEmpty() {
        return mainMap.isEmpty();
    }

    /** {@inheritDoc} */
    @Accessor
    public boolean containsKey(@ConflictParameter UUID key) {
        return mainMap.containsKey(key);
    }

    /** {@inheritDoc} */
    @Accessor
    public E get(@ConflictParameter UUID key) {
        return mainMap.get(key);
    }

    /**
     * Removes a specific element identified by the ID returned via entryList()'s CorfuQueueRecord.
     *
     * {@inheritDoc}
     *
     * @throws NoSuchElementException if this queue did not contain this element
     * @return The entry that was successfully removed
     */
    @MutatorAccessor(name = "remove", undoFunction = "undoRemove",
                                undoRecordFunction = "undoRemoveRecord")
    @SuppressWarnings("unchecked")
    public E remove(@ConflictParameter UUID key) {
        E previous =  mainMap.remove(key);
        return previous;
    }

    /**
     * Remove all entries from the Queue.
     */
    @Mutator(name = "clear", reset = true)
    public void clear() {
        mainMap.clear();
    }

    /** Helper function to get a Corfu Queue.
     *
     * @param <E>                   Queue entry type
     * @return                      A type token to pass to the builder.
     */
    static <E>
    TypeToken<CorfuQueue<E>>
        getQueueType() {
            return new TypeToken<CorfuQueue<E>>() {};
    }

    /** {@inheritDoc} */
    @MutatorAccessor(name = "put", undoFunction = "undoPut", undoRecordFunction = "undoPutRecord")
    protected E put(@ConflictParameter UUID key, E value) {
        E previous = mainMap.put(key, value);
        return previous;
    }

    @DontInstrument
    protected E undoPutRecord(CorfuQueue<E> queue, UUID key, E value) {
        return queue.mainMap.get(key);
    }

    @DontInstrument
    protected void undoPut(CorfuQueue<E> queue, E undoRecord, UUID key, E entry) {
        // Same as undoRemove (restore previous value)
        undoRemove(queue, undoRecord, key);
    }

    enum UndoNullable {
        NULL;
    }

    @DontInstrument
    protected E undoRemoveRecord(CorfuQueue<E> table, UUID key) {
        return table.mainMap.get(key);
    }

    @DontInstrument
    protected void undoRemove(CorfuQueue<E> queue, E undoRecord, UUID key) {
        if (undoRecord == null) {
            queue.mainMap.remove(key);
        } else {
            queue.mainMap.put(key, undoRecord);
        }
    }
}
