package com.matjazt.netmon2.aop;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface TimedEvent {
    String value() default ""; // Optional explicit event key

    boolean logBefore() default false; // Whether to log before method execution (default: false)

    boolean logAfter() default true; // Whether to log after method execution (default: false)
}
