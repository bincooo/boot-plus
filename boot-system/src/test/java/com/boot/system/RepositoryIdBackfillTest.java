package com.boot.system;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import com.baomidou.mybatisplus.extension.toolkit.Db;
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
import com.boot.system.repo.po.RoleMenu;
import com.boot.system.repo.po.User;
import com.boot.system.repo.po.UserPost;
import com.boot.system.repo.po.UserRole;
import io.github.linpeilie.Converter;

/**
 * 用户/角色仓库生成主键回填回归测试。
 *
 * <p>主表插入后必须把数据库生成的主键回填到 BO，并使用该主键写入关联表。</p>
 *
 * @author boot-plus
 */
@ExtendWith(MockitoExtension.class)
class RepositoryIdBackfillTest
{
    @Mock
    private UserMapper userMapper;

    @Mock
    private UserRoleMapper userRoleMapper;

    @Mock
    private UserPostMapper userPostMapper;

    @Mock
    private RoleMapper roleMapper;

    @Mock
    private PostMapper postMapper;

    @Mock
    private RoleMenuMapper roleMenuMapper;

    @Mock
    private RoleDeptMapper roleDeptMapper;

    @Mock
    private MenuMapper menuMapper;

    @Mock
    private DeptMapper deptMapper;

    @Mock
    private Converter converter;

    @InjectMocks
    private UserRepository userRepository;

    @InjectMocks
    private RoleRepository roleRepository;

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void insertUserBackfillsGeneratedIdAndUsesItForBridgeRows()
    {
        UserBo bo = new UserBo();
        bo.setUserName("alice");
        bo.setRoleIds(new Long[] { 5L });
        bo.setPostIds(new Long[] { 7L });

        User po = new User();
        when(converter.convert(bo, User.class)).thenReturn(po);
        when(userMapper.insert(po)).thenAnswer(invocation ->
        {
            po.setUserId(99L);
            return 1;
        });
        ArgumentCaptor<List> batchCaptor = ArgumentCaptor.forClass(List.class);

        int rows;
        try (MockedStatic<Db> dbMock = mockStatic(Db.class))
        {
            dbMock.when(() -> Db.saveBatch(anyCollection())).thenReturn(true);
            rows = userRepository.insertUser(bo);
            dbMock.verify(() -> Db.saveBatch(batchCaptor.capture()), times(2));
        }

        assertEquals(1, rows);
        assertEquals(99L, bo.getUserId());
        List<UserPost> postRows = batchCaptor.getAllValues().get(0);
        List<UserRole> roleRows = batchCaptor.getAllValues().get(1);
        assertEquals(7L, postRows.get(0).getPostId());
        assertEquals(99L, postRows.get(0).getUserId());
        assertEquals(5L, roleRows.get(0).getRoleId());
        assertEquals(99L, roleRows.get(0).getUserId());
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void insertRoleBackfillsGeneratedIdAndUsesItForMenuRows()
    {
        RoleBo bo = new RoleBo();
        bo.setRoleName("ops");
        bo.setMenuIds(new Long[] { 11L, 12L });

        Role po = new Role();
        when(converter.convert(bo, Role.class)).thenReturn(po);
        when(roleMapper.insert(po)).thenAnswer(invocation ->
        {
            po.setRoleId(77L);
            return 1;
        });
        ArgumentCaptor<List> menuCaptor = ArgumentCaptor.forClass(List.class);

        int rows;
        try (MockedStatic<Db> dbMock = mockStatic(Db.class))
        {
            dbMock.when(() -> Db.saveBatch(anyCollection())).thenReturn(true);
            rows = roleRepository.insertRole(bo);
            dbMock.verify(() -> Db.saveBatch(menuCaptor.capture()), times(1));
        }

        assertEquals(2, rows);
        assertEquals(77L, bo.getRoleId());
        List<RoleMenu> menuRows = menuCaptor.getAllValues().get(0);
        assertEquals(77L, menuRows.get(0).getRoleId());
        assertEquals(11L, menuRows.get(0).getMenuId());
        assertEquals(77L, menuRows.get(1).getRoleId());
        assertEquals(12L, menuRows.get(1).getMenuId());
    }

    @Test
    void insertUserWithoutRelationsReturnsMainInsertResult()
    {
        UserBo bo = new UserBo();
        User po = new User();
        when(converter.convert(bo, User.class)).thenReturn(po);
        when(userMapper.insert(po)).thenReturn(1);

        try (MockedStatic<Db> dbMock = mockStatic(Db.class))
        {
            dbMock.when(() -> Db.saveBatch(anyCollection())).thenReturn(true);
            assertEquals(1, userRepository.insertUser(bo));
            dbMock.verifyNoInteractions();
        }
        verifyNoInteractions(userRoleMapper, userPostMapper);
    }
}
