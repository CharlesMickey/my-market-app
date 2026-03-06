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

    private final Map<String, AtomicLong> clientBalances = new ConcurrentHashMap<>();
    private final Map<String, Map<String, Transaction>> clientTransactions = new ConcurrentHashMap<>();
    private final Long initialBalance;

    public PaymentService(@Value("${payment.service.initial-balance:100000}") Long initialBalance) {
        this.initialBalance = initialBalance;
        log.info("Payment service initialized with initial balance: {} RUB", initialBalance / 100.0);
    }

    private AtomicLong getClientBalance(String clientId) {
        return clientBalances.computeIfAbsent(clientId,
                k -> new AtomicLong(initialBalance));
    }

    private Map<String, Transaction> getClientTransactions(String clientId) {
        return clientTransactions.computeIfAbsent(clientId,
                k -> new ConcurrentHashMap<>());
    }

    public Mono<Balance> getBalance(String clientId) {
        return Mono.fromCallable(() -> {
            Balance balanceObj = new Balance();
            balanceObj.setAmount(getClientBalance(clientId).get());
            return balanceObj;
        });
    }

    public Mono<Transaction> processPayment(String clientId, Long orderId, Long amount, String description) {
        return Mono.fromCallable(() -> {
            log.info("Processing payment for client {} order {}: amount={}", clientId, orderId, amount);

            if (amount <= 0) {
                throw new IllegalArgumentException("Amount must be positive");
            }

            AtomicLong balance = getClientBalance(clientId);
            Map<String, Transaction> transactions = getClientTransactions(clientId);

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

                    log.warn("Payment failed for client {} order {}: insufficient funds (balance={}, required={})",
                            clientId, orderId, currentBalance, amount);

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

                    log.info("Payment successful for client {} order {}: new balance={}",
                            clientId, orderId, newBalance);

                    return successTx;
                }
            }
        });
    }

    public Mono<Transaction> getTransaction(String clientId, String transactionId) {
        return Mono.justOrEmpty(getClientTransactions(clientId).get(transactionId));
    }

    public Mono<Balance> deposit(String clientId, Long amount) {
        if (amount <= 0) {
            return Mono.error(new IllegalArgumentException("Amount must be positive"));
        }
        long newBalance = getClientBalance(clientId).addAndGet(amount);
        return getBalance(clientId);
    }
}