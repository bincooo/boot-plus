package com.boot.system;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import com.boot.common.core.domain.bo.RoleBo;
import com.boot.common.core.domain.bo.UserBo;
import com.boot.system.repo.RoleRepository;
import com.boot.system.repo.UserRepository;
import com.boot.system.repo.mapper.DeptMapper;
import com.boot.system.repo.mapper.MenuMapper;
import com.boot.system.repo.mapper.PostMapper;
import com.boot.system.repo.mapper.RoleDeptMapper;
import com.boot.system.repo.mapper.RoleMapper;
import com.boot.system.repo.mapper.RoleMenuMapper;
import com.boot.system.repo.mapper.UserMapper;
import com.boot.system.repo.mapper.UserPostMapper;
import com.boot.system.repo.mapper.UserRoleMapper;
import com.boot.system.repo.po.Role;
import com.boot.system.repo.po.User;
import io.github.linpeilie.Converter;

/**
 * 服务端 dataScope 片段进入 MP wrapper 的回归测试（无数据库）。
 *
 * <p>用户列表的 @DataScope 别名为 sys_user，片段必须原样进入 wrapper；
 * 角色列表通过 inSql 子查询应用片段，骨架必须保留 where 1=1。</p>
 *
 * @author boot-plus
 */
class DataScopeWrapperTest
{
    /**
     * 无 Spring/MyBatis 上下文时，lambda 列解析需要预先注册实体的 TableInfo。
     */
    @BeforeAll
    static void initLambdaCache()
    {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, User.class);
        TableInfoHelper.initTableInfo(assistant, Role.class);
    }

    @Test
    @SuppressWarnings("unchecked")
    void userListAppliesDataScopeWithUserAlias()
    {
        UserMapper userMapper = mock(UserMapper.class);
        UserRepository repository = new UserRepository(userMapper, mock(UserRoleMapper.class),
                mock(UserPostMapper.class), mock(RoleMapper.class), mock(PostMapper.class),
                mock(DeptMapper.class), mock(Converter.class));

        UserBo query = new UserBo();
        query.getParams().put("dataScope", " AND (sys_user.dept_id = 100)");
        repository.selectUserList(new Page<>(1, 10), query);

        ArgumentCaptor<Wrapper<User>> captor = ArgumentCaptor.forClass(Wrapper.class);
        verify(userMapper).selectPage(any(Page.class), captor.capture());
        assertTrue(captor.getValue().getSqlSegment().contains("sys_user.dept_id"),
                "用户列表 wrapper 应保留服务端 dataScope 片段中的 sys_user.dept_id");
    }

    @Test
    @SuppressWarnings("unchecked")
    void roleListAppliesDataScopeInsideInSqlSkeleton()
    {
        RoleMapper roleMapper = mock(RoleMapper.class);
        RoleRepository repository = new RoleRepository(roleMapper, mock(RoleMenuMapper.class),
                mock(RoleDeptMapper.class), mock(UserRoleMapper.class), mock(MenuMapper.class),
                mock(DeptMapper.class), mock(Converter.class));

        RoleBo query = new RoleBo();
        query.getParams().put("dataScope", " AND (d.dept_id = 100)");
        repository.selectRoleList(new Page<>(1, 10), query);

        ArgumentCaptor<Wrapper<Role>> captor = ArgumentCaptor.forClass(Wrapper.class);
        verify(roleMapper).selectPage(any(Page.class), captor.capture());
        assertTrue(captor.getValue().getSqlSegment().contains("where 1=1"),
                "角色列表的 inSql 子查询骨架应包含 where 1=1");
    }
}