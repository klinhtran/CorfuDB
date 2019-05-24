package org.corfudb.protocols.logprotocol;

import io.netty.buffer.ByteBuf;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Abstract class for a local SMREntry locator.
 * Created by Xin Li on 5/23/19.
 */
public abstract class AbstractLocalSMREntryLocator implements ILocalSMREntryLocator {

    static private final Map<Byte, LocalLocatorType> typeMap =
            Arrays.stream(LocalLocatorType.values())
                    .collect(Collectors.toMap(LocalLocatorType::asByte, Function.identity()));

    @Setter
    @Getter
    private LocalLocatorType type;

    @Override
    public void serialize(ByteBuf buf) {
        buf.writeByte(type.asByte());
    }

    protected abstract void deserializeBuffer(ByteBuf buf);

    public static ILocalSMREntryLocator deserialize(ByteBuf buf) {
        try {
            LocalLocatorType type = typeMap.get(buf.readByte());
            AbstractLocalSMREntryLocator locator = type.entryType.newInstance();
            locator.setType(type);
            locator.deserializeBuffer(buf);
            return locator;
        } catch (InstantiationException | IllegalAccessException ex) {
            throw new RuntimeException("Error deserializing Local SMREntryLocator", ex);
        }
    }

    @AllArgsConstructor
    public enum LocalLocatorType {
        SMREntryLocator (0, SMREntry.LocalSMREntryLocator.class),
        MultiSMREntryLocator (1, MultiSMREntry.LocalMultiSMREntryLocator.class),
        MultiObjectSMREntryLocator (2, MultiObjectSMREntry.LocalMultiObjectSMREntryLocator.class);

        public final int type;
        public final Class<? extends AbstractLocalSMREntryLocator> entryType;

        public byte asByte() {
            return (byte) type;
        }
    }
}
