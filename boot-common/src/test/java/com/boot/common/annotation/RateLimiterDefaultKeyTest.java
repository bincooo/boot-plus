package com.boot.common.annotation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * RateLimiter 注解默认值契约测试
 *
 * @author boot-plus
 */
class RateLimiterDefaultKeyTest
{
    @Test
    void defaultKeyIsEmptyToKeepProviderKeyShape() throws Exception
    {
        assertEquals("", RateLimiter.class.getMethod("key").getDefaultValue());
    }
}
