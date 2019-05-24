package org.corfudb.protocols.logprotocol;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.corfudb.runtime.view.Address;
import org.junit.Test;

import java.util.UUID;

import static org.junit.Assert.assertEquals;

/**
 * Created by Xin Li on 05/23/19.
 */
public class SMREntryLocatorTest {

    @Test
    public void testSMREntrySerializeDeserialize() {

        long globalAddress = Address.getMinAddress();
        SMREntry.LocalSMREntryLocator locator = new SMREntry.LocalSMREntryLocator();
        SMREntryLocator smrEntryLocator = new SMREntryLocator(globalAddress, locator);

        ByteBuf buf = Unpooled.buffer();
        smrEntryLocator.serialize(buf);
        SMREntryLocator deserializedSMREntryLocator = SMREntryLocator.deserialize(buf);

        assertEquals(smrEntryLocator, deserializedSMREntryLocator);
    }

    @Test
    public void testMultiSMREntrySerializeDeserialize() {

        long globalAddress = Address.getMinAddress();
        int index = 0;
        MultiSMREntry.LocalMultiSMREntryLocator locator =
                new MultiSMREntry.LocalMultiSMREntryLocator(index);
        SMREntryLocator smrEntryLocator = new SMREntryLocator(globalAddress, locator);

        ByteBuf buf = Unpooled.buffer();
        smrEntryLocator.serialize(buf);
        SMREntryLocator deserializedSMREntryLocator = SMREntryLocator.deserialize(buf);

        assertEquals(smrEntryLocator, deserializedSMREntryLocator);
    }

    @Test
    public void testMultiObjectSMREntrySerializeDeserialize() {

        long globalAddress = Address.getMinAddress();
        UUID steramId = UUID.randomUUID();
        int index = 0;
        MultiObjectSMREntry.LocalMultiObjectSMREntryLocator locator =
                new MultiObjectSMREntry.LocalMultiObjectSMREntryLocator(steramId, index);
        SMREntryLocator smrEntryLocator = new SMREntryLocator(globalAddress, locator);

        ByteBuf buf = Unpooled.buffer();
        smrEntryLocator.serialize(buf);
        SMREntryLocator deserializedSMREntryLocator = SMREntryLocator.deserialize(buf);

        assertEquals(smrEntryLocator, deserializedSMREntryLocator);
    }
}
