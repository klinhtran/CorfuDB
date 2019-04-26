package org.corfudb.infrastructure.management;

import com.google.common.collect.ImmutableSet;
import org.junit.Test;

import java.time.Duration;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class NetworkStretcherTest {
    @Test
    public void testIncreasedPeriod() {
        NetworkStretcher ns = NetworkStretcher.builder().build();
        assertThat(ns.getIncreasedPeriod()).isEqualTo(Duration.ofSeconds(3));
    }

    /**
     * Tests that timeout does increase (stretch) by a second on failures.
     * Tests that timeout can't go above 5 second max.
     */
    @Test
    public void modifyIterationTimeouts() {
        NetworkStretcher ns = NetworkStretcher.builder().build();
        Set<String> failedNodes = ImmutableSet.of("a");
        assertThat(ns.modifyIterationTimeouts(failedNodes)).isEqualTo(Duration.ofSeconds(2));
        assertThat(ns.modifyIterationTimeouts(failedNodes)).isEqualTo(Duration.ofSeconds(3));
        assertThat(ns.modifyIterationTimeouts(failedNodes)).isEqualTo(Duration.ofSeconds(4));
        assertThat(ns.modifyIterationTimeouts(failedNodes)).isEqualTo(Duration.ofSeconds(5));
        assertThat(ns.modifyIterationTimeouts(failedNodes)).isEqualTo(Duration.ofSeconds(5));
    }
}

