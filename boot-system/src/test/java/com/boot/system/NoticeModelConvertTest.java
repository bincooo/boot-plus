package com.boot.system;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.Date;
import org.junit.jupiter.api.Test;
import com.boot.common.core.domain.QueryParam;
import com.boot.system.domain.vo.NoticeReadUserVo;
import com.boot.system.domain.vo.NoticeVo;
import com.boot.system.repo.po.NoticeReadUserRow;
import com.boot.system.repo.po.Notice;
import com.boot.system.domain.bo.NoticeBo;
import io.github.linpeilie.Converter;
import tools.jackson.databind.ObjectMapper;

/**
 * 公告 PO/BO/VO 转换与 JSON 契约测试
 *
 * @author boot-plus
 */
class NoticeModelConvertTest
{
    private final Converter converter = ConverterTestSupport.getConverter();

    @Test
    void noticeVoSerializesIsReadKey()
    {
        NoticeVo vo = new NoticeVo();
        vo.setNoticeId(1L);
        vo.setNoticeTitle("系统通知");
        vo.setIsRead(true);

        String json = new ObjectMapper().writeValueAsString(vo);

        assertTrue(json.contains("\"isRead\""), json);
        assertFalse(json.contains("\"read\""), json);
    }

    @Test
    void noticePoToBoToVoKeepsFieldsAndReadFlag()
    {
        Notice po = new Notice();
        po.setNoticeId(1L);
        po.setNoticeTitle("系统通知");
        po.setNoticeType("1");
        po.setNoticeContent("内容");
        po.setStatus("0");
        po.setCreateBy("admin");
        po.setCreateTime(new Date());
        po.setIsRead(true);

        NoticeBo bo = converter.convert(po, NoticeBo.class);
        assertNotNull(bo);
        assertEquals(1L, bo.getNoticeId());
        assertEquals("系统通知", bo.getNoticeTitle());
        assertEquals("1", bo.getNoticeType());
        assertEquals("0", bo.getStatus());
        assertEquals("admin", bo.getCreateBy());
        assertTrue(bo.getIsRead());

        NoticeVo vo = converter.convert(bo, NoticeVo.class);
        assertNotNull(vo);
        assertEquals(1L, vo.getNoticeId());
        assertEquals("系统通知", vo.getNoticeTitle());
        assertEquals("内容", vo.getNoticeContent());
        assertEquals("0", vo.getStatus());
        assertEquals("admin", vo.getCreateBy());
        assertTrue(vo.getIsRead());
    }

    @Test
    void noticeBoDoesNotExtendQueryParam()
    {
        assertFalse(QueryParam.class.isAssignableFrom(NoticeBo.class));
    }

    @Test
    void noticeReadUserRowToVoKeepsFields()
    {
        NoticeReadUserRow row = new NoticeReadUserRow();
        row.setUserId(1L);
        row.setUserName("admin");
        row.setNickName("管理员");
        row.setDeptName("研发部门");
        row.setPhonenumber("13800138000");
        Date readTime = new Date();
        row.setReadTime(readTime);

        NoticeReadUserVo vo = converter.convert(row, NoticeReadUserVo.class);
        assertNotNull(vo);
        assertEquals(1L, vo.getUserId());
        assertEquals("admin", vo.getUserName());
        assertEquals("管理员", vo.getNickName());
        assertEquals("研发部门", vo.getDeptName());
        assertEquals("13800138000", vo.getPhonenumber());
        assertEquals(readTime, vo.getReadTime());
    }

    @Test
    void noticeVoHasNoQueryParamsOrDelFlag()
    {
        assertThrows(NoSuchFieldException.class, () -> NoticeVo.class.getDeclaredField("params"));
        assertThrows(NoSuchFieldException.class, () -> NoticeVo.class.getDeclaredField("delFlag"));
    }

    @Test
    void noticePoDeclaresOnlyTableColumns()
    {
        assertThrows(NoSuchFieldException.class, () -> Notice.class.getDeclaredField("params"));
        assertThrows(NoSuchFieldException.class, () -> Notice.class.getDeclaredField("searchValue"));
        assertThrows(NoSuchFieldException.class, () -> Notice.class.getDeclaredField("delFlag"));
    }
}
