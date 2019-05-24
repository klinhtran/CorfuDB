package org.corfudb.protocols.logprotocol;

import io.netty.buffer.ByteBuf;
import lombok.*;
import org.corfudb.util.serializer.ICorfuSerializable;

import java.util.Objects;


/**
 * This class is used for denote the location of an SMREntry in the global AddressSpaceView.
 * Created by Xin Li on 5/23/19.
 */
@ToString
@Data
@EqualsAndHashCode
@AllArgsConstructor
public class SMREntryLocator implements Comparable<SMREntryLocator>, ICorfuSerializable {
    /**
     * The global address the associated SMREntry resides in.
     */
    @Getter
    final private long globalAddress;

    /**
     * The per-address local locator. It is possible that multiple SMREntry resides in a same global address.
     */
    @Getter
    final private ILocalSMREntryLocator localLocator;

    @Override
    public int compareTo(SMREntryLocator other) {
        Objects.requireNonNull(other);

        int addressCompareResult = Long.compare(globalAddress, other.getGlobalAddress());
        if (addressCompareResult != 0) {
            return addressCompareResult;
        }

        return localLocator.compareTo(other.localLocator);
    }

    @Override
    public void serialize(ByteBuf b) {
        b.writeLong(globalAddress);
        localLocator.serialize(b);
    }

    public static SMREntryLocator deserialize(ByteBuf b) {
        long globalAddress = b.readLong();
        ILocalSMREntryLocator locator = AbstractLocalSMREntryLocator.deserialize(b);
        //ILocalSMREntryLocator locator = new SMREntry.LocalSMREntryLocator();
        return new SMREntryLocator(globalAddress, locator);
    }

}
