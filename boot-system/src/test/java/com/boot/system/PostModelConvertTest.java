package com.boot.system;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import java.util.Date;
import org.junit.jupiter.api.Test;
import com.boot.common.core.domain.QueryParam;
import com.boot.system.domain.vo.PostVo;
import com.boot.system.repo.po.Post;
import com.boot.system.domain.bo.PostBo;
import io.github.linpeilie.Converter;

/**
 * 岗位 PO/BO/VO 转换测试
 *
 * @author boot-plus
 */
class PostModelConvertTest
{
    private final Converter converter = ConverterTestSupport.getConverter();

    @Test
    void poToBoToVoKeepsPostCodeAndPostName()
    {
        Post po = new Post();
        po.setPostId(1L);
        po.setPostCode("ceo");
        po.setPostName("董事长");
        po.setPostSort(1);
        po.setStatus("0");
        po.setCreateBy("admin");
        po.setCreateTime(new Date());

        PostBo bo = converter.convert(po, PostBo.class);
        assertNotNull(bo);
        assertEquals("ceo", bo.getPostCode());
        assertEquals("董事长", bo.getPostName());
        assertFalse(QueryParam.class.isAssignableFrom(PostBo.class));

        PostVo vo = converter.convert(bo, PostVo.class);
        assertNotNull(vo);
        assertEquals(1L, vo.getPostId());
        assertEquals("ceo", vo.getPostCode());
        assertEquals("董事长", vo.getPostName());
    }
}
