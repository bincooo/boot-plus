package com.boot.system;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import io.github.linpeilie.Converter;
import io.github.linpeilie.mapstruct.MapstructAutoConfiguration;

/**
 * 转换器测试支持：启动一个只注册 MapStruct Plus 自动配置的 Spring 上下文
 *
 * @author boot-plus
 */
public final class ConverterTestSupport
{
    private static final AnnotationConfigApplicationContext CONTEXT;

    static
    {
        CONTEXT = new AnnotationConfigApplicationContext();
        CONTEXT.register(MapstructAutoConfiguration.class);
        CONTEXT.refresh();
    }

    private ConverterTestSupport()
    {
    }

    public static Converter getConverter()
    {
        return CONTEXT.getBean(Converter.class);
    }
}
