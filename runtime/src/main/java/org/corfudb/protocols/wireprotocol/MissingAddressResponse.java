package org.corfudb.protocols.wireprotocol;

import io.netty.buffer.ByteBuf;

import java.util.Set;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Response for missing addresses in the log unit server for a specified range.
 * Created by zlokhandwala on 2019-06-01.
 */
@Data
@AllArgsConstructor
public class MissingAddressResponse implements ICorfuPayload<MissingAddressResponse> {

    private final Set<Long> missingAddresses;

    /**
     * Deserialization Constructor from Bytebuf to MissingAddressRequest.
     *
     * @param buf The buffer to deserialize
     */
    public MissingAddressResponse(ByteBuf buf) {
        missingAddresses = ICorfuPayload.setFromBuffer(buf, Long.class);
    }

    @Override
    public void doSerialize(ByteBuf buf) {
        ICorfuPayload.serialize(buf, missingAddresses);
    }
}
