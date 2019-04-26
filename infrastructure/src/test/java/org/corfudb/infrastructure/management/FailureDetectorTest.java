package org.corfudb.infrastructure.management;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.corfudb.infrastructure.management.ClusterStateContext.HeartbeatCounter;
import org.corfudb.protocols.wireprotocol.SequencerMetrics;
import org.corfudb.runtime.clients.IClientRouter;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class FailureDetectorTest {

    @Test
    public void testPollRound() {
        FailureDetector failureDetector = new FailureDetector(
                new HeartbeatCounter(), "a"
        );

        long epoch = 1;
        ImmutableSet<String> allServers = ImmutableSet.of("a", "b", "c");
        Map<String, IClientRouter> routerMap = new HashMap<>();
        SequencerMetrics metrics = SequencerMetrics.READY;
        ImmutableList<String> responsiveServers = ImmutableList.of("a", "b");

        PollReport report = failureDetector.pollRound(
                epoch, allServers, routerMap, metrics, responsiveServers
        );

        assertThat(report.getReachableNodes()).isEmpty();
        assertThat(report.getFailedNodes()).containsExactly("a", "b", "c");
    }
}