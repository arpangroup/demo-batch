package com.arpan.demo_batch.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@NoArgsConstructor
@ToString
public class Transaction {
    private int userId;
    private String username;
    private String email;
    private double amount;

    public Transaction(int userId, double amount) {
        this.userId = userId;
        this.amount = amount;
    }
}
