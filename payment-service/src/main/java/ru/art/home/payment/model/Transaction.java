package ru.art.home.payment.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {
    private String id;
    private Long orderId;
    private Long amount;
    private Long balanceBefore;
    private Long balanceAfter;
    private TransactionStatus status;
    private LocalDateTime createdAt;
    private String description;

    public enum TransactionStatus {
        SUCCESS, FAILED_INSUFFICIENT_FUNDS, FAILED_OTHER
    }

    public static Transaction createSuccess(Long orderId, Long amount,
                                            Long balanceBefore, Long balanceAfter,
                                            String description) {
        return new Transaction(
                UUID.randomUUID().toString(),
                orderId,
                amount,
                balanceBefore,
                balanceAfter,
                TransactionStatus.SUCCESS,
                LocalDateTime.now(),
                description
        );
    }

    public static Transaction createFailed(Long orderId, Long amount,
                                           Long balanceBefore,
                                           TransactionStatus status,
                                           String description) {
        return new Transaction(
                UUID.randomUUID().toString(),
                orderId,
                amount,
                balanceBefore,
                balanceBefore,
                status,
                LocalDateTime.now(),
                description
        );
    }
}