package com.travelo.commons.idempotency;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark endpoints that should support idempotency keys.
 * The idempotency key should be passed in the Idempotency-Key header.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface IdempotencyKey {
    
    /**
     * Time to live for idempotency key in seconds.
     * Default is 24 hours.
     */
    long ttlSeconds() default 86400;
    
    /**
     * Whether to require idempotency key.
     * Default is false (optional).
     */
    boolean required() default false;
}

