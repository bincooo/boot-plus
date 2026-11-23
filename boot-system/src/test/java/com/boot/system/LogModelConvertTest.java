package com.boot.system;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.Date;
import org.junit.jupiter.api.Test;
import com.boot.common.core.domain.QueryParam;
import com.boot.system.domain.vo.CacheVo;
import com.boot.system.domain.vo.LogininforVo;
import com.boot.system.domain.vo.OperLogVo;
import com.boot.system.repo.po.Logininfor;
import com.boot.system.repo.po.OperLog;
import com.boot.system.domain.bo.LogininforBo;
import com.boot.system.domain.bo.OperLogBo;
import io.github.linpeilie.Converter;

/**
 * 日志 PO/BO/VO 转换测试
 *
 * @author boot-plus
 */
class LogModelConvertTest
{
    private final Converter converter = ConverterTestSupport.getConverter();

    @Test
    void logininforPoToBoToVoKeepsFields()
    {
        Logininfor po = new Logininfor();
        po.setInfoId(100L);
        po.setUserName("admin");
        po.setStatus("0");
        po.setIpaddr("127.0.0.1");
        po.setLoginLocation("内网");
        po.setBrowser("Chrome");
        po.setOs("Windows 10");
        po.setMsg("登录成功");
        po.setLoginTime(new Date());

        LogininforBo bo = converter.convert(po, LogininforBo.class);
        assertNotNull(bo);
        assertEquals("admin", bo.getUserName());
        assertEquals("0", bo.getStatus());
        assertEquals("127.0.0.1", bo.getIpaddr());
        assertEquals("内网", bo.getLoginLocation());
        assertEquals("Chrome", bo.getBrowser());
        assertEquals("Windows 10", bo.getOs());
        assertEquals("登录成功", bo.getMsg());

        LogininforVo vo = converter.convert(bo, LogininforVo.class);
        assertNotNull(vo);
        assertEquals(100L, vo.getInfoId());
        assertEquals("admin", vo.getUserName());
        assertEquals("0", vo.getStatus());
        assertEquals("127.0.0.1", vo.getIpaddr());
        assertEquals("内网", vo.getLoginLocation());
        assertEquals("Chrome", vo.getBrowser());
        assertEquals("Windows 10", vo.getOs());
        assertEquals("登录成功", vo.getMsg());
    }

    @Test
    void operLogPoToBoToVoKeepsFields()
    {
        OperLog po = new OperLog();
        po.setOperId(100L);
        po.setTitle("用户管理");
        po.setBusinessType(2);
        po.setMethod("com.boot.web.controller.system.UserController.edit()");
        po.setRequestMethod("PUT");
        po.setOperatorType(1);
        po.setOperName("admin");
        po.setDeptName("研发部门");
        po.setOperUrl("/system/user");
        po.setOperIp("127.0.0.1");
        po.setOperLocation("内网");
        po.setOperParam("{}");
        po.setJsonResult("{\"code\":200}");
        po.setStatus(0);
        po.setErrorMsg("");
        po.setOperTime(new Date());
        po.setCostTime(128L);

        OperLogBo bo = converter.convert(po, OperLogBo.class);
        assertNotNull(bo);
        assertEquals("用户管理", bo.getTitle());
        assertEquals(2, bo.getBusinessType());
        assertEquals("com.boot.web.controller.system.UserController.edit()", bo.getMethod());
        assertEquals("PUT", bo.getRequestMethod());
        assertEquals(1, bo.getOperatorType());
        assertEquals("admin", bo.getOperName());
        assertEquals("研发部门", bo.getDeptName());
        assertEquals(0, bo.getStatus());
        assertEquals(128L, bo.getCostTime());

        OperLogVo vo = converter.convert(bo, OperLogVo.class);
        assertNotNull(vo);
        assertEquals(100L, vo.getOperId());
        assertEquals("用户管理", vo.getTitle());
        assertEquals(2, vo.getBusinessType());
        assertEquals("PUT", vo.getRequestMethod());
        assertEquals("admin", vo.getOperName());
        assertEquals("研发部门", vo.getDeptName());
        assertEquals(0, vo.getStatus());
        assertEquals(128L, vo.getCostTime());
    }

    @Test
    void logininforBoAndOperLogBoExtendQueryParam()
    {
        assertTrue(QueryParam.class.isAssignableFrom(LogininforBo.class));
        assertTrue(QueryParam.class.isAssignableFrom(OperLogBo.class));
        assertNotNull(new LogininforBo().getParams());
        assertNotNull(new OperLogBo().getParams());
    }

    @Test
    void operLogBoKeepsQueryOnlyBusinessTypes()
    {
        OperLogBo bo = new OperLogBo();
        bo.setBusinessTypes(new Integer[] { 1, 2, 3 });
        assertEquals(3, bo.getBusinessTypes().length);
        assertThrows(NoSuchFieldException.class, () -> OperLog.class.getDeclaredField("businessTypes"));
    }

    @Test
    void logVosHaveNoQueryParamsOrDelFlag()
    {
        assertThrows(NoSuchFieldException.class, () -> LogininforVo.class.getDeclaredField("params"));
        assertThrows(NoSuchFieldException.class, () -> LogininforVo.class.getDeclaredField("searchValue"));
        assertThrows(NoSuchFieldException.class, () -> LogininforVo.class.getDeclaredField("delFlag"));
        assertThrows(NoSuchFieldException.class, () -> OperLogVo.class.getDeclaredField("params"));
        assertThrows(NoSuchFieldException.class, () -> OperLogVo.class.getDeclaredField("searchValue"));
        assertThrows(NoSuchFieldException.class, () -> OperLogVo.class.getDeclaredField("delFlag"));
    }

    @Test
    void logPosDeclareOnlyTableColumns()
    {
        assertThrows(NoSuchFieldException.class, () -> Logininfor.class.getDeclaredField("params"));
        assertThrows(NoSuchFieldException.class, () -> Logininfor.class.getDeclaredField("searchValue"));
        assertThrows(NoSuchFieldException.class, () -> Logininfor.class.getDeclaredField("delFlag"));
        assertThrows(NoSuchFieldException.class, () -> OperLog.class.getDeclaredField("params"));
        assertThrows(NoSuchFieldException.class, () -> OperLog.class.getDeclaredField("searchValue"));
        assertThrows(NoSuchFieldException.class, () -> OperLog.class.getDeclaredField("delFlag"));
    }

    @Test
    void sysCacheVoKeepsCacheFieldSetWithoutAuditFields()
    {
        assertDoesNotThrow(() -> {
            assertNotNull(CacheVo.class.getDeclaredField("cacheName"));
            assertNotNull(CacheVo.class.getDeclaredField("cacheKey"));
            assertNotNull(CacheVo.class.getDeclaredField("cacheValue"));
            assertNotNull(CacheVo.class.getDeclaredField("remark"));
        });

        assertThrows(NoSuchFieldException.class, () -> CacheVo.class.getDeclaredField("createBy"));
        assertThrows(NoSuchFieldException.class, () -> CacheVo.class.getDeclaredField("createTime"));
        assertThrows(NoSuchFieldException.class, () -> CacheVo.class.getDeclaredField("updateBy"));
        assertThrows(NoSuchFieldException.class, () -> CacheVo.class.getDeclaredField("updateTime"));
        assertThrows(NoSuchFieldException.class, () -> CacheVo.class.getDeclaredField("delFlag"));
        assertThrows(NoSuchFieldException.class, () -> CacheVo.class.getDeclaredField("params"));
        assertThrows(NoSuchFieldException.class, () -> CacheVo.class.getDeclaredField("searchValue"));
    }

    @Test
    void sysCacheVoConstructorsPreserveOldBehavior()
    {
        CacheVo named = new CacheVo("login_tokens:", "用户信息");
        assertEquals("login_tokens:", named.getCacheName());
        assertEquals("用户信息", named.getRemark());
        assertEquals("", named.getCacheKey());
        assertEquals("", named.getCacheValue());

        CacheVo value = new CacheVo("login_tokens", "login_tokens:uuid:1", "token-value");
        assertEquals("login_tokens", value.getCacheName());
        assertEquals(":uuid:1", value.getCacheKey());
        assertEquals("token-value", value.getCacheValue());
    }
}
