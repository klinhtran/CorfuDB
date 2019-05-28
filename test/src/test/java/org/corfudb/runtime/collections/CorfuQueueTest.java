package org.corfudb.runtime.collections;

import com.google.common.reflect.TypeToken;
import org.corfudb.runtime.collections.CorfuQueue.CorfuQueueRecord;
import org.corfudb.runtime.view.AbstractViewTest;
import org.junit.Test;

import java.util.*;

/**
 * Created by Sundar Sridharan on May 22, 2019
 */
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Simple test of basic operations to check that insert order is preserved in the queue.
 * Created by hisundar on 05/27/2019
 */
public class CorfuQueueTest extends AbstractViewTest {

    @Test
    @SuppressWarnings("unchecked")
    public void basicQueueOrder() {
        CorfuQueue<String>
                corfuQueue = getDefaultRuntime().getObjectsView().build()
                .setTypeToken(new TypeToken<CorfuQueue<String>>() {})
                .setNeedOrderedUUIDs(true)
                .setStreamName("test")
                .open();

        UUID idC = corfuQueue.enqueue("c");
        UUID idB = corfuQueue.enqueue("b");
        UUID idA = corfuQueue.enqueue("a");


        List<CorfuQueueRecord<String>> records = corfuQueue.entryList(-1);

        assertThat(records.get(0).getID()).isEqualTo(idC);
        assertThat(records.get(1).getID()).isEqualTo(idB);
        assertThat(records.get(2).getID()).isEqualTo(idA);

        assertThat(records.get(0).getEntry()).isEqualTo("c");
        assertThat(records.get(1).getEntry()).isEqualTo("b");
        assertThat(records.get(2).getEntry()).isEqualTo("a");

        // Remove the middle entry
        corfuQueue.remove(idB);

        List<CorfuQueueRecord<String>> records2 = corfuQueue.entryList(-1);
        assertThat(records2.get(0).getEntry()).isEqualTo("c");
        assertThat(records2.get(1).getEntry()).isEqualTo("a");
    }
}
