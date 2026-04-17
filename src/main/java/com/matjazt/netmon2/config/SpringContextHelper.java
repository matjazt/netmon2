package com.matjazt.netmon2.config;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.stereotype.Component;

/**
 * Helper class to access Spring beans from non-Spring managed objects.
 *
 * <p>This is particularly useful for Logback appenders, which are instantiated outside the Spring
 * container but need access to Spring beans.
 *
 * <p>Usage: {@code AppLogDbWriterService writer =
 * SpringContextHelper.getBean(AppLogDbWriterService.class);}
 *
 * <p>WARNING: Only use this after Spring context is fully initialized. Calling {@code getBean()}
 * during application startup may fail.
 */
@Component
@Slf4j
public class SpringContextHelper
        implements ApplicationContextAware, ApplicationListener<ContextClosedEvent> {

    private static ApplicationContext context;

    /**
     * Called by Spring during startup to inject the ApplicationContext.
     *
     * @param applicationContext the Spring application context
     * @throws BeansException if context cannot be set
     */
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        context = applicationContext;
        log.info("SpringContextHelper initialized with application context");
    }

    @Override
    public void onApplicationEvent(ContextClosedEvent event) {
        context = null; // stop all bean lookups during/after shutdown
    }

    /**
     * Retrieves a Spring bean by its type.
     *
     * <p>Returns null if the Spring context is not yet initialized or if the bean doesn't exist.
     * Never throws exceptions to avoid breaking non-Spring code.
     *
     * @param <T> the bean type
     * @param beanClass the class of the bean to retrieve
     * @return the bean instance, or null if not available
     */
    public static <T> T getBean(Class<T> beanClass) {
        try {
            if (context == null
                    || (context instanceof ConfigurableApplicationContext cac && !cac.isActive())) {
                // This happens a lot during application startup before Spring is ready
                return null;
            }
            var bean = context.getBean(beanClass);
            log.debug(
                    "Successfully retrieved bean {} from Spring context",
                    beanClass.getSimpleName());
            return bean;
        } catch (Exception e) {
            log.warn("Failed to retrieve bean {}: {}", beanClass.getSimpleName(), e.getMessage());
            return null;
        }
    }

    /**
     * Checks if the Spring context has been initialized.
     *
     * @return true if context is available, false otherwise
     */
    public static boolean isContextInitialized() {
        return context != null;
    }
}
