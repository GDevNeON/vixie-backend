package com.neong.vixie.exceptions;

/**
 * Thrown when a user attempts a purchase without sufficient coin balance.
 */
public class InsufficientFundsException extends RuntimeException {

    private final int currentBalance;
    private final int requiredAmount;

    public InsufficientFundsException(int currentBalance, int requiredAmount) {
        super("Insufficient funds: have " + currentBalance + " coins, need " + requiredAmount);
        this.currentBalance = currentBalance;
        this.requiredAmount = requiredAmount;
    }

    public int getCurrentBalance() {
        return currentBalance;
    }

    public int getRequiredAmount() {
        return requiredAmount;
    }

    public int getShortfall() {
        return requiredAmount - currentBalance;
    }
}
