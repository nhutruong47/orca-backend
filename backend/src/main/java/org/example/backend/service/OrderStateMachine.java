package org.example.backend.service;

import org.example.backend.entity.enums.OrderStatus;
import org.springframework.stereotype.Component;

/**
 * Centralized validator for marketplace order status transitions.
 *
 * <p>Every status mutation in {@link InterGroupOrderService} MUST go through
 * {@link #requireTransition(OrderStatus, OrderStatus)} so the state machine is
 * enforced in one place. Skipping this validator is a bug.
 */
@Component
public class OrderStateMachine {

    /** Throws if {@code from -> to} is not a legal transition. */
    public void requireTransition(OrderStatus from, OrderStatus to) {
        if (from == null) {
            throw new IllegalStateException("Current status must not be null");
        }
        if (to == null) {
            throw new IllegalStateException("Target status must not be null");
        }
        if (!from.canTransitionTo(to)) {
            throw new IllegalStateException(
                    String.format("Invalid order status transition: %s -> %s", from, to));
        }
    }

    /** Non-throwing variant for guard checks. */
    public boolean isAllowed(OrderStatus from, OrderStatus to) {
        return from != null && to != null && from.canTransitionTo(to);
    }
}