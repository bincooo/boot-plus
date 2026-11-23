package com.boot.common.core.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

/**
 * QueryParam 单元测试
 *
 * @author boot-plus
 */
class QueryParamTest
{
    @Test
    void paramsIsInitializedAndSearchValueBindable()
    {
        TestParam p = new TestParam();
        assertNotNull(p.getParams());
        p.getParams().put("dataScope", "x");
        p.setSearchValue("abc");
        assertEquals("x", p.getParams().get("dataScope"));
        assertEquals("abc", p.getSearchValue());
    }

    @Test
    void getParamsLazyInitializesAfterSetNull()
    {
        TestParam p = new TestParam();
        p.setParams(null);
        assertNotNull(p.getParams());
        p.getParams().put("dataScope", "y");
        assertEquals("y", p.getParams().get("dataScope"));
    }

    static class TestParam extends QueryParam
    {
    }
}
