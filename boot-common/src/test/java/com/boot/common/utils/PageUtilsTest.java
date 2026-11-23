package com.boot.common.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

/**
 * PageUtils 单元测试
 *
 * @author boot-plus
 */
class PageUtilsTest
{
    @Test
    void convertPreservesPageMetadata()
    {
        Page<String> source = new Page<>(2, 10, 35);
        source.setRecords(List.of("aa", "bbb"));
        IPage<Integer> out = PageUtils.convert(source, String::length);
        assertEquals(35, out.getTotal());
        assertEquals(2, out.getCurrent());
        assertEquals(10, out.getSize());
        assertEquals(List.of(2, 3), out.getRecords());
    }

    @Test
    void convertPreservesOrdersIndependently()
    {
        Page<String> source = new Page<>(1, 5, 2);
        source.addOrder(OrderItem.asc("user_name"));
        source.setRecords(List.of("aa", "b"));

        IPage<Integer> out = PageUtils.convert(source, String::length);

        assertEquals(1, out.orders().size());
        assertEquals("user_name", out.orders().get(0).getColumn());
        assertTrue(out.orders().get(0).isAsc());

        // 目标页 orders 与源对象相互独立：修改目标列表不影响源列表
        out.orders().add(OrderItem.desc("dept_name"));
        out.orders().remove(0);
        assertEquals(1, source.orders().size());
        assertEquals("user_name", source.orders().get(0).getColumn());
    }

    @Test
    void convertReturnsMutableRecordsList()
    {
        Page<String> source = new Page<>(1, 5, 1);
        source.setRecords(List.of("aa"));
        IPage<Integer> out = PageUtils.convert(source, String::length);
        // 记录列表可变（如调用方需要补充或移除元素）
        out.getRecords().add(9);
        assertEquals(List.of(2, 9), out.getRecords());
        assertEquals(1, source.getRecords().size());
    }

    @Test
    void sanitizeOrdersDropsUnknownAndNormalizesCase()
    {
        Page<String> page = new Page<>(1, 10);
        page.addOrder(OrderItem.asc("userName"));
        page.addOrder(OrderItem.desc("deptName"));
        page.addOrder(OrderItem.asc("STATUS"));

        PageUtils.sanitizeOrders(page, Set.of("user_name", "status", "create_time"));

        assertEquals(2, page.orders().size());
        assertEquals("userName", page.orders().get(0).getColumn());
        assertEquals("STATUS", page.orders().get(1).getColumn());
    }

    @Test
    void sanitizeOrdersDropsAllWhenNoneAllowed()
    {
        Page<String> page = new Page<>(1, 10);
        page.addOrder(OrderItem.asc("dept_name"));

        PageUtils.sanitizeOrders(page, Set.of("user_id"));

        assertTrue(page.orders().isEmpty());
    }

    @Test
    void sanitizeOrdersIgnoresNullPage()
    {
        PageUtils.sanitizeOrders(null, Set.of("user_id"));
    }
}
