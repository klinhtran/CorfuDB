package org.corfudb.infrastructure.management;

import com.google.common.annotations.VisibleForTesting;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Getter;

import java.time.Duration;
import java.util.Set;

/**
 * NetworkStretcher increases or decreases the timeout intervals for polling based on
 * whether the poll was unsuccessful or successful respectively.
 */
@Builder
public class NetworkStretcher {
    /**
     * Max duration for the responseTimeouts of the routers.
     * In the worst case scenario or in case of failed servers, their response timeouts will be
     * set to a maximum value of maxPeriod.
     */
    @Getter
    @Default
    private final Duration maxPeriod = Duration.ofSeconds(5);

    /**
     * Minimum duration for the responseTimeouts of the routers.
     * Under ideal conditions the routers will have a response timeout set to this.
     */
    @Default
    private final Duration initPeriod = Duration.ofSeconds(2);

    /**
     * Response timeout for every router.
     */
    @Getter
    @Default
    private Duration currentPeriod = Duration.ofSeconds(2);

    /**
     * Poll interval between iterations in a pollRound
     */
    @Default
    private final Duration initialPollInterval = Duration.ofSeconds(1);

    /**
     * Increments in which the period moves towards the maxPeriod in every failed
     * iteration provided.
     */
    @Default
    private final Duration periodDelta = Duration.ofSeconds(1);

    /**
     * Function to increment the existing response timeout period.
     *
     * @return The new calculated timeout value.
     */
    @VisibleForTesting
    Duration getIncreasedPeriod() {
        Duration currIncreasedPeriod = currentPeriod.plus(periodDelta);
        if (currIncreasedPeriod.toMillis() < maxPeriod.toMillis()){
            return currIncreasedPeriod;
        }

        return maxPeriod;
    }

    /**
     * Function to decrement the existing response timeout period.
     *
     * @return The new calculated timeout value.
     */
    @VisibleForTesting
    Duration getDecreasedPeriod() {
        Duration currDecreasedPeriod = currentPeriod.minus(periodDelta);
        if (currDecreasedPeriod.toMillis() > maxPeriod.toMillis()) {
            return currDecreasedPeriod;
        }

        return maxPeriod;
    }

    /**
     * Tune timeouts after each poll iteration, according to list of failed and connected nodes
     *
     * @param failedNodes a set of failed nodes if any
     * @return updated poll interval
     */
    public Duration modifyIterationTimeouts(Set<String> failedNodes) {

        if (failedNodes.isEmpty()) {
            return initialPollInterval;
        }

        Duration previousPeriod = initialPollInterval;
        if (initialPollInterval.toMillis() < currentPeriod.toMillis()) {
            previousPeriod = currentPeriod;
        }

        currentPeriod = getIncreasedPeriod();

        return previousPeriod;
    }
}
