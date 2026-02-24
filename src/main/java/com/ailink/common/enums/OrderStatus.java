package com.ailink.common.enums;

import java.util.Map;
import java.util.Set;

public enum OrderStatus {
    CREATED,
    ESCROWED,
    IN_PROGRESS,
    COMPLETED,
    DISPUTED,
    CLOSED;

    private static final Map<OrderStatus, Set<OrderStatus>> TRANSITIONS = Map.of(
            CREATED, Set.of(ESCROWED, CLOSED),
            ESCROWED, Set.of(IN_PROGRESS, CLOSED, DISPUTED),
            IN_PROGRESS, Set.of(COMPLETED, DISPUTED, CLOSED),
            DISPUTED, Set.of(COMPLETED, CLOSED),
            COMPLETED, Set.of(CLOSED),
            CLOSED, Set.of()
    );

    public boolean canTransitTo(OrderStatus target) {
        return TRANSITIONS.getOrDefault(this, Set.of()).contains(target);
    }
}
