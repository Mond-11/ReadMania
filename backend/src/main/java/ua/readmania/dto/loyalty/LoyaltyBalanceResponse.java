package ua.readmania.dto.loyalty;

import java.util.ArrayList;
import java.util.List;

public class LoyaltyBalanceResponse {

    private int bonusBalance;
    private List<BonusTransactionResponse> transactions = new ArrayList<>();

    public LoyaltyBalanceResponse() {
    }

    public LoyaltyBalanceResponse(int bonusBalance, List<BonusTransactionResponse> transactions) {
        this.bonusBalance = bonusBalance;
        this.transactions = transactions != null ? transactions : new ArrayList<>();
    }

    public int getBonusBalance() {
        return bonusBalance;
    }

    public void setBonusBalance(int bonusBalance) {
        this.bonusBalance = bonusBalance;
    }

    public List<BonusTransactionResponse> getTransactions() {
        return transactions;
    }

    public void setTransactions(List<BonusTransactionResponse> transactions) {
        this.transactions = transactions != null ? transactions : new ArrayList<>();
    }
}
