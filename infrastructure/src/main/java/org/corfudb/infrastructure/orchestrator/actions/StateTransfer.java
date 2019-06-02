package org.corfudb.infrastructure.orchestrator.actions;

import static org.corfudb.infrastructure.ServerContext.RECORDS_PER_LOG_FILE;

import com.google.common.collect.Iterables;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.ExecutionException;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import org.corfudb.protocols.wireprotocol.ILogData;
import org.corfudb.protocols.wireprotocol.LogData;
import org.corfudb.protocols.wireprotocol.Token;
import org.corfudb.runtime.CorfuRuntime;
import org.corfudb.runtime.view.Layout;
import org.corfudb.util.CFUtils;

/**
 * State transfer utility.
 * Created by zlokhandwala on 2019-02-06.
 */
@Slf4j
public class StateTransfer {

    private StateTransfer() {
        // Hide implicit public constructor.
    }

    /**
     * Fetch and propagate the trimMark to the new/healing nodes. Else, a FastLoader reading from
     * them will have to mark all the already trimmed entries as holes.
     * Transfer an address segment from a cluster to a set of specified nodes.
     * There are no cluster reconfigurations, hence no epoch change side effects.
     *
     * @param layout   layout
     * @param endpoint destination node
     * @param runtime  The runtime to read the segment from
     * @param segment  segment to transfer
     */
    public static void transfer(Layout layout,
                                @NonNull String endpoint,
                                CorfuRuntime runtime,
                                Layout.LayoutSegment segment)
            throws ExecutionException, InterruptedException {

        if (endpoint.isEmpty()) {
            log.debug("stateTransfer: No server needs to transfer for segment [{} - {}], "
                            + "skipping state transfer for this segment.",
                    segment.getStart(), segment.getEnd());
            return;
        }

        int chunkSize = runtime.getParameters().getBulkReadSize();

        long trimMark = runtime.getAddressSpaceView().getTrimMark().getSequence();
        // Send the trimMark to the new/healing nodes.
        // If this times out or fails, the Action performing the stateTransfer fails and retries.

        // TrimMark is the first address present on the log unit server.
        // Perform the prefix trim on the preceding address = (trimMark - 1).
        // Since the LU will reject trim decisions made from older epochs, we
        // need to adjust the new trim mark to have the new layout's epoch.
        Token prefixToken = new Token(layout.getEpoch(), trimMark - 1);
        CFUtils.getUninterruptibly(runtime.getLayoutView().getRuntimeLayout(layout)
                .getLogUnitClient(endpoint)
                .prefixTrim(prefixToken));

        if (trimMark > segment.getEnd()) {
            log.info("stateTransfer: Nothing to transfer, trimMark {}"
                            + "greater than end of segment {}",
                    trimMark, segment.getEnd());
            return;
        }

        // State transfer should start from segment start address or trim mark whichever is lower.
        final long segmentStart = Math.max(trimMark, segment.getStart());
        final long segmentEnd = segment.getEnd() - 1;
        log.info("stateTransfer: Total address range to transfer: [{}-{}] to node {}",
                segmentStart, segmentEnd, endpoint);

        // Create batches of RECORDS_PER_LOG_FILE addresses.
        for (long pendingWritesBatch = segmentStart; pendingWritesBatch <= segmentEnd
                ; pendingWritesBatch += RECORDS_PER_LOG_FILE) {

            long pendingWritesBatchEnd
                    = Math.min(segmentEnd, pendingWritesBatch + RECORDS_PER_LOG_FILE - 1);

            // For each batch request missing addresses in this batch.
            // This is an optimization in case the state transfer is repeated to
            // prevent redundant transfer.
            TreeSet<Long> missingEntries = new TreeSet<>(runtime.getLayoutView()
                    .getRuntimeLayout(layout)
                    .getLogUnitClient(endpoint)
                    .requestMissingAddresses(pendingWritesBatch, pendingWritesBatchEnd).get()
                    .getMissingAddresses());
            // Partition the large batch into chunks to read/write within the RPC timeout.
            Iterable<List<Long>> chunks = Iterables.partition(missingEntries, chunkSize);

            log.info("Addresses to be transferred in range [{}-{}] = {}",
                    pendingWritesBatch, pendingWritesBatchEnd, missingEntries.size());
            log.error("missingEntries: {}", missingEntries);

            // Read and write in chunks of chunkSize.
            for (List<Long> chunk : chunks) {

                long ts1 = System.currentTimeMillis();

                Map<Long, ILogData> dataMap = runtime.getAddressSpaceView().fetchAll(chunk, true);

                long ts2 = System.currentTimeMillis();

                log.info("stateTransfer: read [{}-{}] in {} ms",
                        chunk.get(0), chunk.get(chunk.size() - 1), (ts2 - ts1));

                List<LogData> entries = new ArrayList<>();
                for (long address : chunk) {
                    if (!dataMap.containsKey(address)) {
                        log.error("Missing address {} in batch {}", address, chunk);
                        throw new IllegalStateException("Missing address");
                    }
                    entries.add((LogData) dataMap.get(address));
                }

                // Write segment chunk to the new logunit
                ts1 = System.currentTimeMillis();
                boolean transferSuccess = runtime.getLayoutView().getRuntimeLayout(layout)
                        .getLogUnitClient(endpoint)
                        .writeRange(entries).get();
                ts2 = System.currentTimeMillis();

                if (!transferSuccess) {
                    log.error("stateTransfer: Failed to transfer {} to {}",
                            chunk, endpoint);
                    throw new IllegalStateException("Failed to transfer!");
                }

                log.info("stateTransfer: Transferred address chunk [{}-{}] to {} in {} ms",
                        chunk.get(0), chunk.get(chunk.size() - 1), endpoint, (ts2 - ts1));
            }

        }
    }
}
