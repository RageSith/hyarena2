package de.ragesith.hyarena2.shop;

/**
 * Result of a shop purchase attempt.
 */
public enum PurchaseResult {
    SUCCESS,
    INSUFFICIENT_FUNDS,
    ALREADY_OWNED,
    ITEM_NOT_FOUND
}
