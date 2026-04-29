package com.matjazt.netmon2.aop;

import com.matjazt.tools.TimingStatistics;
import com.matjazt.tools.TimingStatistics.SingleTimingWithTimer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

/**
 * AOP aspect that automatically times every public method in all {@code @RestController} beans.
 *
 * <p>Timing data is accumulated in the shared {@link TimingStatistics} singleton and periodically
 * logged by {@link com.matjazt.netmon2.service.TimingLoggingService}.
 *
 * <p>Event type keys follow the pattern {@code ClassName.methodName}, e.g. {@code
 * DeviceController.getAllDevices}.
 */
@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class AutomaticTimingAspect {

    private final TimingStatistics timingStatistics;

    /**
     * Times every method in every class annotated with {@code @RestController}. {@code
     * pjp.getTarget()} returns the real bean (not the CGLIB proxy), so {@code
     * getClass().getSimpleName()} always yields the plain controller name.
     */
    @Around("within(@org.springframework.web.bind.annotation.RestController *)")
    public Object timeRestControllerMethod(ProceedingJoinPoint pjp) throws Throwable {
        SingleTimingWithTimer sw =
                timingStatistics.startTimer(
                        "REST."
                                + pjp.getTarget().getClass().getSimpleName()
                                + "."
                                + pjp.getSignature().getName());
        try {
            return pjp.proceed();
        } finally {
            sw.singleTiming().stopTimer(sw.timerHandle());
        }
    }

    @Around("@annotation(com.matjazt.netmon2.aop.TimedEvent)")
    public Object timeAnnotatedMethod(ProceedingJoinPoint pjp) throws Throwable {
        MethodSignature sig = (MethodSignature) pjp.getSignature();
        TimedEvent ann = sig.getMethod().getAnnotation(TimedEvent.class);
        String key =
                (ann != null && !ann.value().isBlank())
                        ? ann.value()
                        : pjp.getTarget().getClass().getSimpleName() + "." + sig.getName();

        if (ann != null && ann.logBefore()) {
            log.info("starting timed event {}", key);
        }
        var sw = timingStatistics.startTimer(key);
        try {
            return pjp.proceed();
        } finally {
            var ms = sw.singleTiming().stopTimer(sw.timerHandle());
            if (ann != null && ann.logAfter()) {
                log.info("timed event {} done in {} ms", key, ms);
            }
        }
    }
}
