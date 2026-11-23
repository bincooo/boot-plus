package com.boot.system;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import com.boot.system.domain.vo.UserOnlineVo;

/**
 * 在线用户视图对象测试
 *
 * @author boot-plus
 */
class UserOnlineVoTest
{
    private static final Set<String> LEGACY_FIELDS = Set.of(
        "tokenId", "deptName", "userName", "ipaddr",
        "loginLocation", "browser", "os", "loginTime");

    private static final Set<String> AUDIT_FIELDS = Set.of(
        "createBy", "createTime", "updateBy", "updateTime", "remark");

    @Test
    void onlineVoHasNoAuditFields()
    {
        for (String field : AUDIT_FIELDS)
        {
            assertThrows(NoSuchFieldException.class,
                () -> UserOnlineVo.class.getDeclaredField(field),
                "UserOnlineVo should not declare audit field: " + field);
        }
    }

    @Test
    void onlineVoKeepsLegacyFieldsAndIsSerializable()
    {
        Set<String> fieldNames = Arrays.stream(UserOnlineVo.class.getDeclaredFields())
            .map(Field::getName)
            .filter(name -> !"serialVersionUID".equals(name))
            .collect(Collectors.toSet());
        assertEquals(LEGACY_FIELDS, fieldNames);
        assertTrue(Serializable.class.isAssignableFrom(UserOnlineVo.class));
    }
}
