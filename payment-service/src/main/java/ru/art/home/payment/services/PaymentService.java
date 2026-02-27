package ru.art.home.payment.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import ru.art.home.payment.model.Balance;
import ru.art.home.payment.model.Transaction;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
@Slf4j
public class PaymentService {

    private final AtomicLong balance;
    private final Map<String, Transaction> transactions = new ConcurrentHashMap<>();

    public PaymentService(@Value("${payment.service.initial-balance:100000}") Long initialBalance) {
        this.balance = new AtomicLong(initialBalance);
        log.info("Payment service initialized with balance: {} RUB", initialBalance / 100.0);
    }

    public Mono<Balance> getBalance() {
        return Mono.fromCallable(() -> {
            Balance balanceObj = new Balance();
            balanceObj.setAmount(balance.get());
            return balanceObj;
        });
    }

    public Mono<Transaction> processPayment(Long orderId, Long amount, String description) {
        return Mono.fromCallable(() -> {
            log.info("Processing payment for order {}: amount={}", orderId, amount);

            if (amount <= 0) {
                throw new IllegalArgumentException("Amount must be positive");
            }

            while (true) {
                long currentBalance = balance.get();

                if (currentBalance < amount) {
                    Transaction failedTx = Transaction.createFailed(
                            orderId,
                            amount,
                            currentBalance,
                            Transaction.TransactionStatus.FAILED_INSUFFICIENT_FUNDS,
                            "Insufficient funds"
                    );
                    transactions.put(failedTx.getId(), failedTx);

                    log.warn("Payment failed for order {}: insufficient funds (balance={}, required={})",
                            orderId, currentBalance, amount);

                    return failedTx;
                }

                long newBalance = currentBalance - amount;

                if (balance.compareAndSet(currentBalance, newBalance)) {

                    Transaction successTx = Transaction.createSuccess(
                            orderId,
                            amount,
                            currentBalance,
                            newBalance,
                            description != null
                                    ? description
                                    : "Payment for order #" + orderId
                    );

                    transactions.put(successTx.getId(), successTx);

                    log.info("Payment successful for order {}: new balance={}",
                            orderId, newBalance);

                    return successTx;
                }
            }
        });
    }

    public Mono<Transaction> getTransaction(String transactionId) {
        return Mono.justOrEmpty(transactions.get(transactionId));
    }

    public Mono<Balance> deposit(Long amount) {
        if (amount <= 0) {
            return Mono.error(new IllegalArgumentException("Amount must be positive"));
        }
        long newBalance = balance.addAndGet(amount);
        return getBalance();
    }
}