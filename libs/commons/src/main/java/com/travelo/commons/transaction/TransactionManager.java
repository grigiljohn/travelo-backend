package com.travelo.commons.transaction;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Utility class for transaction management and post-commit callbacks.
 */
public final class TransactionManager {

    private TransactionManager() {
    }

    /**
     * Execute an action after transaction commit.
     * Useful for publishing events or sending notifications after data is committed.
     */
    public static void afterCommit(Runnable action) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            action.run();
                        }
                    }
            );
        } else {
            // No transaction active, execute immediately
            action.run();
        }
    }

    /**
     * Execute an action after transaction commit with access to result.
     */
    public static <T> void afterCommit(T result, Consumer<T> action) {
        afterCommit(() -> action.accept(result));
    }

    /**
     * Execute action after transaction completion (commit or rollback).
     */
    public static void afterCompletion(Runnable action) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCompletion(int status) {
                            action.run();
                        }
                    }
            );
        } else {
            action.run();
        }
    }

    /**
     * Execute action only after successful commit.
     * Does nothing if transaction rolls back.
     */
    public static void onCommit(Runnable action) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            action.run();
                        }
                    }
            );
        }
    }

    /**
     * Execute action only on rollback.
     */
    public static void onRollback(Runnable action) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCompletion(int status) {
                            if (status == STATUS_ROLLED_BACK) {
                                action.run();
                            }
                        }
                    }
            );
        }
    }

    /**
     * Execute in a new transaction.
     * Useful for operations that should be in a separate transaction.
     */
    @Transactional
    public static <T> T inNewTransaction(Supplier<T> action) {
        return action.get();
    }

    /**
     * Execute in a new transaction (void version).
     */
    @Transactional
    public static void inNewTransaction(Runnable action) {
        action.run();
    }
}

