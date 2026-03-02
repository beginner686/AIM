package com.ailink.common.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class MethodTimeLogAspect {

    private static final Logger log = LoggerFactory.getLogger(MethodTimeLogAspect.class);

    @Around("execution(* com.ailink.module.order.service.impl..*(..))"
            + " || execution(* com.ailink.module.worker.service.impl.RunnerPaymentProfileServiceImpl.*(..))"
            + " || execution(* com.ailink.module.order.controller.PaymentNotifyController.*(..))")
    public Object logExecutionTime(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();
        try {
            return joinPoint.proceed();
        } finally {
            long cost = System.currentTimeMillis() - start;
            log.info("method={} cost={}ms", joinPoint.getSignature().toShortString(), cost);
        }
    }
}
