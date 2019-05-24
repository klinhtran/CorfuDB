package org.corfudb.protocols.logprotocol;

import org.corfudb.util.serializer.ICorfuSerializable;

/**
 * Interface for a local SMREntry locator.
 * Created by Xin Li on 5/23/19.
 */
public interface ILocalSMREntryLocator extends ICorfuSerializable, Comparable<ILocalSMREntryLocator> {
}
