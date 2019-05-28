The class `CorfuQueue` implements a persisted queue over the abstraction of a CorfuTable. Currently CorfuTable uses a HashMap<> to represent the materialized view of a distributed map. A HashMap does not carry a notion of ordering natively. However when implemented over the abstraction of a distributed shared log, the elements added to the map do, in fact, have an ordering imposed by their append operations into the global log. The class `CorfuQueue` attempts to expose this inherent ordering as an immutable logical FIFO Queue interface with three simple apis:

###  1. `UUID enqueue(E)`
Since a map has a key and value, where key is a conflict parameter, enqueue simply generates a non-conflicting UUID as the key and inserts the Entry as a value into a CorfuTable. The generated UUID is returned for the caller.

### 2. `List<CorfuQueueRecord<Object>> entryList()`
Returns a list of all the entries along with their ids, sorted by their realized stream addresses (thus logically a FIFO queue). Assuming insertion order is preserved, CorfuTable can materialize its state as a [LinkedHashMap](https://docs.oracle.com/javase/8/docs/api/java/util/LinkedHashMap.html) instead of a HashMap and return the view as a list.

### 3. `E remove(UUID id)`
Instead of a `dequeue()`, the returned id from `enqueue()` or the `entryList()` api can be used to remove entries in any order from the persisted queue.

Assuming checkpointing and garbage collection also work in the same insertion order, this proposal is to implement the abstraction of a logical immutable queue using nothing more than a [LinkedHashMap](https://docs.oracle.com/javase/8/docs/api/java/util/LinkedHashMap.html).

### A Note about the UUIDs
The UUIDs are the keys of the underlying [LinkedHashMap](https://docs.oracle.com/javase/8/docs/api/java/util/LinkedHashMap.html). These UUIDs will need to have be globally unique and also comparable to facilitate easy processing of the queue elements. The unique UUIDs can be simply generated using a epoch aware counter inside the Sequencer.
