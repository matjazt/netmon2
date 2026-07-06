package com.matjazt.netmon2.config;

import org.springframework.transaction.annotation.Transactional;

import java.lang.annotation.*;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
@Transactional(rollbackFor = Exception.class) // Bundles the global rule here
public @interface ApplicationTransactional {
    // You can optionally alias other Transactional properties if needed
}
