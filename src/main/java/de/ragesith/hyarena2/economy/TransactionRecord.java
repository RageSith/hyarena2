package de.ragesith.hyarena2.economy;

/**
 * Single audit log entry for economy transactions.
 * Appended to data/players/<uuid>_transactions.json.
 */
public class TransactionRecord {
    private String type;      // "AP_EARN", "AP_SPEND", "HONOR_EARN", "HONOR_DECAY", "PURCHASE"
    private double amount;
    private String reason;
    private long timestamp;

    public TransactionRecord() {}

    public TransactionRecord(String type, double amount, String reason) {
        this.type = type;
        this.amount = amount;
        this.reason = reason;
        this.timestamp = System.currentTimeMillis();
    }

    public String getType() {
        return type;
    }

    public double getAmount() {
        return amount;
    }

    public String getReason() {
        return reason;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
