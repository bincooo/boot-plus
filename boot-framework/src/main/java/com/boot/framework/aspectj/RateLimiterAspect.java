package com.boot.framework.aspectj;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import com.boot.common.annotation.RateLimiter;
import com.boot.common.constant.CacheConstants;
import com.boot.common.core.cache.CacheProvider;
import com.boot.common.enums.LimitType;
import com.boot.common.exception.ServiceException;
import com.boot.common.utils.ip.IpUtils;

/**
 * 限流处理
 *
 * @author boot-plus
 */
@Aspect
@Component
@RequiredArgsConstructor
public class RateLimiterAspect
{
    private static final Logger log = LoggerFactory.getLogger(RateLimiterAspect.class);

    private final CacheProvider cacheProvider;

    @Before("@annotation(rateLimiter)")
    public void doBefore(JoinPoint point, RateLimiter rateLimiter) throws Throwable
    {
        int time = rateLimiter.time();
        int count = rateLimiter.count();

        String combineKey = getCombineKey(rateLimiter, point);
        try
        {
            long number = cacheProvider.increment(CacheConstants.RATE_LIMIT_CACHE, combineKey, time, TimeUnit.SECONDS);
            if (number > count)
            {
                throw new ServiceException("访问过于频繁，请稍候再试");
            }
            log.info("限制请求'{}',当前请求'{}',缓存key'{}'", count, number, combineKey);
        }
        catch (ServiceException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            throw new RuntimeException("服务器限流异常，请稍候再试");
        }
    }

    public String getCombineKey(RateLimiter rateLimiter, JoinPoint point)
    {
        StringBuffer stringBuffer = new StringBuffer();
        if (!rateLimiter.key().isEmpty())
        {
            stringBuffer.append(rateLimiter.key());
        }
        if (rateLimiter.limitType() == LimitType.IP)
        {
            stringBuffer.append(IpUtils.getIpAddr()).append("-");
        }
        MethodSignature signature = (MethodSignature) point.getSignature();
        Method method = signature.getMethod();
        Class<?> targetClass = method.getDeclaringClass();
        stringBuffer.append(targetClass.getName()).append("-").append(method.getName());
        return stringBuffer.toString();
    }
}
