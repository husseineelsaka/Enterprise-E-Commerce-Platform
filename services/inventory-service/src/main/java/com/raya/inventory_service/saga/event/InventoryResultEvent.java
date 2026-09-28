package com.raya.inventory_service.saga.event;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Reply to ReserveInventoryCommand (Session 12 — Orchestration).
 *
 * Unlike InventoryReservedEvent / InventoryReservationFailedEvent, this is not
 * an announcement to whoever cares — it is a direct answer to the orchestrator
 * that asked. Success and failure share one record so the orchestrator has a
 * single reply type per step.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
public record InventoryResultEvent(
        String orderId,
        boolean success,
        String reason
) {
}
