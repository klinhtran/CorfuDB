package org.corfudb.runtime.view;

import org.corfudb.protocols.wireprotocol.TokenResponse;

import java.util.UUID;

/**
 * Ordered Globally Unique Identity (GUID) generator that returns ids
 * having a notion of a universal comparable ordering with the help of
 * a counter inside the Corfu SequencerServer.
 * Should the Sequencer restart, it will come up with a higher epoch number.
 * Load this epoch into the higher bits of the guid and load the counter
 * into the lower bits to form the globally unique ordered id.
 *
 * Created by Sundar Sridharan on 5/22/19.
 */
public class CorfuGuidGenerator implements OrderedGuidGenerator {
    // 3 bytes for epoch ~= 16.7 million epoch changes before rollover.
    private final static int MAX_EPOCH = 0xFFffFF;
    private final static int SHIFT_EPOCH = 40;
    // 5 bytes for sequence ~= 1 trillion sequence changes per epoch before rollover.
    private final static long MAX_SEQUENCE = 0xFFffFFffFFL;

    private final SequencerView sequencerView;

    public CorfuGuidGenerator(SequencerView sequencerView) {
        this.sequencerView = sequencerView;
    }

    /**
     *  ----------------------------
     * | Epoch   |      Sequence    |
     *  ----------------------------
     * <-3 bytes-><---5 bytes------->
     *
     * @return long encapsulating both epoch and sequence
     */
    @Override
    public long nextLong() {
        TokenResponse token = sequencerView.nextOrderedGuid();
        return (token.getEpoch() & MAX_EPOCH) << SHIFT_EPOCH |
               (token.getSequence() & MAX_SEQUENCE);
    }

    /**
     *  ----------------------------------
     * |    Epoch      |      Sequence    |
     *  ----------------------------------
     * <----8 bytes----><----8 bytes------>
     *
     * @return non lossy UUID carrying both epoch and sequence
     */
    @Override
    public UUID nextUUID() {
        TokenResponse token = sequencerView.nextOrderedGuid();
        return new UUID(token.getEpoch(), token.getSequence());
    }
}
