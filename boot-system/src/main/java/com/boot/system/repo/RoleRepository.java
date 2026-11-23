package com.boot.system.repo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.toolkit.Db;
import com.boot.common.core.domain.bo.RoleBo;
import com.boot.common.utils.DateUtils;
import com.boot.common.utils.PageUtils;
import com.boot.system.repo.mapper.DeptMapper;
import com.boot.system.repo.mapper.MenuMapper;
import com.boot.system.repo.mapper.RoleDeptMapper;
import com.boot.system.repo.mapper.RoleMapper;
import com.boot.system.repo.mapper.RoleMenuMapper;
import com.boot.system.repo.mapper.UserRoleMapper;
import com.boot.system.repo.po.Dept;
import com.boot.system.repo.po.Menu;
import com.boot.system.repo.po.Role;
import com.boot.system.repo.po.RoleDept;
import com.boot.system.repo.po.RoleMenu;
import com.boot.system.repo.po.UserRole;
import com.boot.system.domain.bo.UserRoleBo;
import io.github.linpeilie.Converter;
import org.springframework.stereotype.Repository;
import lombok.RequiredArgsConstructor;

/**
 * 角色信息 聚合仓库
 *
 * @author boot-plus
 */
@Repository
@RequiredArgsConstructor
public class RoleRepository
{
    private final RoleMapper roleMapper;

    private final RoleMenuMapper roleMenuMapper;

    private final RoleDeptMapper roleDeptMapper;

    private final UserRoleMapper userRoleMapper;

    private final MenuMapper menuMapper;

    private final DeptMapper deptMapper;

    private final Converter converter;

    /**
     * 根据条件分页查询角色数据
     *
     * @param page 分页参数
     * @param query 角色查询条件
     * @return 角色数据集合信息
     */
    public IPage<RoleBo> selectRoleList(IPage<RoleBo> page, RoleBo query)
    {
        // 原 join 的关联表排序列已不在单表 wrapper 上，仅保留基表排序列
        PageUtils.sanitizeOrders(page, Set.of("role_id", "role_name", "role_key", "role_sort", "data_scope",
                "status", "create_time", "update_time"));
        LambdaQueryWrapper<Role> wrapper = roleVoColumns().eq(Role::getDelFlag, "0");
        applyRoleFilters(wrapper, query);
        wrapper.orderByAsc(Role::getRoleSort);
        // 数据范围过滤：原 SQL 通过 sys_user_role/sys_user/sys_dept join 过滤角色，
        // 这里用 inSql 子查询等价表达（d/u 别名与原 @DataScope(deptAlias = "d") 片段一致）。
        // 片段由 DataScopeAspect 服务端生成（dataScope 进入前已清空），仅拼接服务端 SQL，无请求字符串。
        Object dataScope = query.getParams().get("dataScope");
        if (dataScope != null && !dataScope.toString().isEmpty())
        {
            wrapper.inSql(Role::getRoleId, "select ur.role_id from sys_user_role ur "
                    + "left join sys_user u on u.user_id = ur.user_id "
                    + "left join sys_dept d on u.dept_id = d.dept_id "
                    + "where 1=1 " + dataScope);
        }
        if (page == null)
        {
            return convertAll(roleMapper.selectList(wrapper));
        }
        Page<Role> poPage = new Page<>(page.getCurrent(), page.getSize());
        poPage.setOrders(page.orders());
        roleMapper.selectPage(poPage, wrapper);
        return PageUtils.convert(poPage, po -> converter.convert(po, RoleBo.class));
    }

    /**
     * 查询所有角色
     *
     * <p>复刻原 selectRoleAll 语义：原 SQL 为 selectRoleVo join 的 distinct 查询，
     * role_id 为主键且 left join 不剔除 sys_role 行，distinct 折叠后恰为 sys_role 全表
     * 的 11 列投影，与单表查询完全等价。</p>
     *
     * @return 角色列表
     */
    public List<RoleBo> selectRoleAll()
    {
        return PageUtils.convert(roleMapper.selectList(roleVoColumns()),
                po -> converter.convert(po, RoleBo.class));
    }

    /**
     * 根据用户ID查询角色权限
     *
     * <p>复刻原 selectRolePermissionByUserId join 语义：取 sys_user_role 关联行，
     * 角色需存在且 r.del_flag='0'（孤儿关联行随 in 条件丢弃）。</p>
     *
     * @param userId 用户ID
     * @return 角色列表
     */
    public List<RoleBo> selectRolePermissionByUserId(Long userId)
    {
        List<Long> roleIds = userRoleMapper.selectList(Wrappers.lambdaQuery(UserRole.class)
                .select(UserRole::getRoleId)
                .eq(UserRole::getUserId, userId))
                .stream().map(UserRole::getRoleId).collect(Collectors.toList());
        if (roleIds.isEmpty())
        {
            return new ArrayList<>();
        }
        return PageUtils.convert(roleMapper.selectList(roleVoColumns()
                .in(Role::getRoleId, roleIds)
                .eq(Role::getDelFlag, "0")),
                po -> converter.convert(po, RoleBo.class));
    }

    /**
     * 根据用户ID获取角色选择框列表
     *
     * <p>复刻原 selectRoleListByUserId join 语义：关联行需在 sys_role 中存在，
     * 无 del_flag 过滤（与原 SQL 一致）。</p>
     *
     * @param userId 用户ID
     * @return 选中角色ID列表
     */
    public List<Long> selectRoleIdsByUserId(Long userId)
    {
        List<Long> roleIds = userRoleMapper.selectList(Wrappers.lambdaQuery(UserRole.class)
                .select(UserRole::getRoleId)
                .eq(UserRole::getUserId, userId))
                .stream().map(UserRole::getRoleId).collect(Collectors.toList());
        if (roleIds.isEmpty())
        {
            return roleIds;
        }
        return roleMapper.selectList(Wrappers.lambdaQuery(Role.class)
                .select(Role::getRoleId)
                .in(Role::getRoleId, roleIds))
                .stream().map(Role::getRoleId).collect(Collectors.toList());
    }

    /**
     * 通过角色ID查询角色（仅主表）
     *
     * <p>复刻原 selectRoleById 语义：role_id 为主键，原 join + distinct 恰为单行投影，
     * 与单表查询完全等价。</p>
     *
     * @param roleId 角色ID
     * @return 角色对象信息
     */
    public RoleBo selectRoleById(Long roleId)
    {
        Role role = roleMapper.selectOne(roleVoColumns().eq(Role::getRoleId, roleId));
        return converter.convert(role, RoleBo.class);
    }

    /**
     * 通过角色ID查询角色详情（含菜单组、部门组）
     *
     * @param roleId 角色ID
     * @return 角色对象信息
     */
    public RoleBo selectRoleDetailById(Long roleId)
    {
        RoleBo role = selectRoleById(roleId);
        if (role != null)
        {
            role.setMenuIds(selectMenuIdsByRoleId(roleId, role.isMenuCheckStrictly()).toArray(new Long[0]));
            role.setDeptIds(selectDeptIdsByRoleId(roleId, role.isDeptCheckStrictly()).toArray(new Long[0]));
        }
        return role;
    }

    /**
     * 校验角色名称是否唯一
     *
     * @param roleName 角色名称
     * @return 角色信息
     */
    public RoleBo selectRoleNameUnique(String roleName)
    {
        Role role = roleMapper.selectOne(Wrappers.lambdaQuery(Role.class)
                .select(Role::getRoleId, Role::getRoleName, Role::getRoleKey, Role::getRoleSort,
                        Role::getDataScope, Role::isMenuCheckStrictly, Role::isDeptCheckStrictly,
                        Role::getStatus, Role::getDelFlag, Role::getCreateTime, Role::getRemark)
                .eq(Role::getRoleName, roleName)
                .eq(Role::getDelFlag, "0")
                .last("limit 1"));
        return converter.convert(role, RoleBo.class);
    }

    /**
     * 校验角色权限是否唯一
     *
     * @param roleKey 角色权限
     * @return 角色信息
     */
    public RoleBo selectRoleKeyUnique(String roleKey)
    {
        Role role = roleMapper.selectOne(Wrappers.lambdaQuery(Role.class)
                .select(Role::getRoleId, Role::getRoleName, Role::getRoleKey, Role::getRoleSort,
                        Role::getDataScope, Role::isMenuCheckStrictly, Role::isDeptCheckStrictly,
                        Role::getStatus, Role::getDelFlag, Role::getCreateTime, Role::getRemark)
                .eq(Role::getRoleKey, roleKey)
                .eq(Role::getDelFlag, "0")
                .last("limit 1"));
        return converter.convert(role, RoleBo.class);
    }

    /**
     * 新增角色信息（含菜单关联）
     *
     * @param role 角色信息
     * @return 结果
     */
    public int insertRole(RoleBo role)
    {
        Role po = converter.convert(role, Role.class);
        po.setCreateTime(DateUtils.getNowDate());
        roleMapper.insert(po);
        role.setRoleId(po.getRoleId());
        return insertRoleMenu(po.getRoleId(), role.getMenuIds());
    }

    /**
     * 修改角色信息（含菜单关联，先删后插）
     *
     * @param role 角色信息
     * @return 结果
     */
    public int updateRole(RoleBo role)
    {
        updateRoleByWrapper(converter.convert(role, Role.class));
        // 删除角色与菜单关联
        roleMenuMapper.delete(Wrappers.lambdaQuery(RoleMenu.class).eq(RoleMenu::getRoleId, role.getRoleId()));
        return insertRoleMenu(role.getRoleId(), role.getMenuIds());
    }

    /**
     * 修改角色基本信息（仅主表）
     *
     * @param role 角色信息
     * @return 结果
     */
    public int updateRoleBasic(RoleBo role)
    {
        return updateRoleByWrapper(converter.convert(role, Role.class));
    }

    /**
     * 修改角色数据权限（含部门关联，先删后插）
     *
     * @param role 角色信息
     * @return 结果
     */
    public int updateRoleDataScope(RoleBo role)
    {
        updateRoleByWrapper(converter.convert(role, Role.class));
        // 删除角色与部门关联
        roleDeptMapper.delete(Wrappers.lambdaQuery(RoleDept.class).eq(RoleDept::getRoleId, role.getRoleId()));
        // 新增角色和部门信息（数据权限）
        return insertRoleDept(role.getRoleId(), role.getDeptIds());
    }

    /**
     * 修改角色信息（镜像原 XML updateRole 的动态 <if> 条件；menuCheckStrictly/deptCheckStrictly
     * 为原生 boolean，原 OGNL {@code != null} 恒成立，故无条件 set）
     *
     * @param po 角色PO
     * @return 结果
     */
    private int updateRoleByWrapper(Role po)
    {
        return roleMapper.update(null, Wrappers.lambdaUpdate(Role.class)
                .set(po.getRoleName() != null && !po.getRoleName().isEmpty(), Role::getRoleName, po.getRoleName())
                .set(po.getRoleKey() != null && !po.getRoleKey().isEmpty(), Role::getRoleKey, po.getRoleKey())
                .set(po.getRoleSort() != null, Role::getRoleSort, po.getRoleSort())
                .set(po.getDataScope() != null && !po.getDataScope().isEmpty(), Role::getDataScope, po.getDataScope())
                .set(Role::isMenuCheckStrictly, po.isMenuCheckStrictly())
                .set(Role::isDeptCheckStrictly, po.isDeptCheckStrictly())
                .set(po.getStatus() != null && !po.getStatus().isEmpty(), Role::getStatus, po.getStatus())
                .set(po.getRemark() != null, Role::getRemark, po.getRemark())
                .set(po.getUpdateBy() != null && !po.getUpdateBy().isEmpty(), Role::getUpdateBy, po.getUpdateBy())
                .set(Role::getUpdateTime, DateUtils.getNowDate())
                .eq(Role::getRoleId, po.getRoleId()));
    }

    /**
     * 通过角色ID删除角色（含菜单/部门关联）
     *
     * @param roleId 角色ID
     * @return 结果
     */
    public int deleteRoleById(Long roleId)
    {
        // 删除角色与菜单关联
        roleMenuMapper.delete(Wrappers.lambdaQuery(RoleMenu.class).eq(RoleMenu::getRoleId, roleId));
        // 删除角色与部门关联
        roleDeptMapper.delete(Wrappers.lambdaQuery(RoleDept.class).eq(RoleDept::getRoleId, roleId));
        // 软删除角色
        return roleMapper.update(null, Wrappers.lambdaUpdate(Role.class)
                .set(Role::getDelFlag, "2")
                .eq(Role::getRoleId, roleId));
    }

    /**
     * 批量删除角色信息（含菜单/部门关联）
     *
     * @param roleIds 需要删除的角色ID
     * @return 结果
     */
    public int deleteRoleByIds(Long[] roleIds)
    {
        // 删除角色与菜单关联
        roleMenuMapper.delete(Wrappers.lambdaQuery(RoleMenu.class).in(RoleMenu::getRoleId, Arrays.asList(roleIds)));
        // 删除角色与部门关联
        roleDeptMapper.delete(Wrappers.lambdaQuery(RoleDept.class).in(RoleDept::getRoleId, Arrays.asList(roleIds)));
        // 软删除角色
        return roleMapper.update(null, Wrappers.lambdaUpdate(Role.class)
                .set(Role::getDelFlag, "2")
                .in(Role::getRoleId, Arrays.asList(roleIds)));
    }

    /**
     * 通过角色ID查询角色使用数量
     *
     * @param roleId 角色ID
     * @return 结果
     */
    public int countUserRoleByRoleId(Long roleId)
    {
        Long count = userRoleMapper.selectCount(Wrappers.lambdaQuery(UserRole.class).eq(UserRole::getRoleId, roleId));
        return count.intValue();
    }

    /**
     * 取消授权用户角色
     *
     * @param userRole 用户和角色关联信息
     * @return 结果
     */
    public int deleteAuthUser(UserRoleBo userRole)
    {
        UserRole po = converter.convert(userRole, UserRole.class);
        return userRoleMapper.delete(Wrappers.lambdaQuery(UserRole.class)
                .eq(UserRole::getUserId, po.getUserId())
                .eq(UserRole::getRoleId, po.getRoleId()));
    }

    /**
     * 批量取消授权用户角色
     *
     * @param roleId 角色ID
     * @param userIds 需要取消授权的用户数据ID
     * @return 结果
     */
    public int deleteAuthUsers(Long roleId, Long[] userIds)
    {
        return userRoleMapper.delete(Wrappers.lambdaQuery(UserRole.class)
                .eq(UserRole::getRoleId, roleId)
                .in(UserRole::getUserId, Arrays.asList(userIds)));
    }

    /**
     * 批量选择授权用户角色
     *
     * @param roleId 角色ID
     * @param userIds 需要授权的用户数据ID
     * @return 结果
     */
    public int insertAuthUsers(Long roleId, Long[] userIds)
    {
        List<UserRole> list = new ArrayList<>(userIds.length);
        for (Long userId : userIds)
        {
            UserRole ur = new UserRole();
            ur.setUserId(userId);
            ur.setRoleId(roleId);
            list.add(ur);
        }
        // 原 XML 为 foreach 多行 insert；Db.saveBatch 以 BATCH 会话等价写入（参与当前 Spring 事务）
        return Db.saveBatch(list) ? list.size() : 0;
    }

    /**
     * 新增角色菜单关联
     *
     * @param roleId 角色ID
     * @param menuIds 菜单组
     * @return 结果
     */
    private int insertRoleMenu(Long roleId, Long[] menuIds)
    {
        int rows = 1;
        if (menuIds != null && menuIds.length > 0)
        {
            List<RoleMenu> list = new ArrayList<>(menuIds.length);
            for (Long menuId : menuIds)
            {
                RoleMenu rm = new RoleMenu();
                rm.setRoleId(roleId);
                rm.setMenuId(menuId);
                list.add(rm);
            }
            // 原 XML 为 foreach 多行 insert；Db.saveBatch 以 BATCH 会话等价写入
            rows = Db.saveBatch(list) ? list.size() : 0;
        }
        return rows;
    }

    /**
     * 新增角色部门关联（数据权限）
     *
     * @param roleId 角色ID
     * @param deptIds 部门组
     * @return 结果
     */
    private int insertRoleDept(Long roleId, Long[] deptIds)
    {
        int rows = 1;
        if (deptIds != null && deptIds.length > 0)
        {
            List<RoleDept> list = new ArrayList<>(deptIds.length);
            for (Long deptId : deptIds)
            {
                RoleDept rd = new RoleDept();
                rd.setRoleId(roleId);
                rd.setDeptId(deptId);
                list.add(rd);
            }
            // 原 XML 为 foreach 多行 insert；Db.saveBatch 以 BATCH 会话等价写入
            rows = Db.saveBatch(list) ? list.size() : 0;
        }
        return rows;
    }

    /**
     * 应用角色列表动态条件（复刻原 XML selectRoleList 的 if 条件）
     */
    private void applyRoleFilters(LambdaQueryWrapper<Role> wrapper, RoleBo query)
    {
        if (query.getRoleId() != null && query.getRoleId() != 0)
        {
            wrapper.eq(Role::getRoleId, query.getRoleId());
        }
        if (query.getRoleName() != null && !query.getRoleName().isEmpty())
        {
            wrapper.like(Role::getRoleName, query.getRoleName());
        }
        if (query.getStatus() != null && !query.getStatus().isEmpty())
        {
            wrapper.eq(Role::getStatus, query.getStatus());
        }
        if (query.getRoleKey() != null && !query.getRoleKey().isEmpty())
        {
            wrapper.like(Role::getRoleKey, query.getRoleKey());
        }
        Object beginTime = query.getParams().get("beginTime");
        if (beginTime != null && !"".equals(beginTime))
        {
            // {0} 为 MP 预编译参数绑定，非字符串拼接
            wrapper.apply("cast(create_time as date) >= cast({0} as date)", beginTime);
        }
        Object endTime = query.getParams().get("endTime");
        if (endTime != null && !"".equals(endTime))
        {
            wrapper.apply("cast(create_time as date) <= cast({0} as date)", endTime);
        }
    }

    /**
     * 查询角色关联的菜单 ID（复刻原 selectMenuListByRoleId）
     *
     * <p>外层等多价 IN 子查询按 parent_id, order_num 排序；strict 时用与原 SQL 完全相同的
     * NOT IN 子查询减去父菜单，保留原 SQL 对 NULL parent_id 的 NOT IN 语义。
     * 子查询仅拼接 Long 角色 ID，无请求字符串。</p>
     */
    private List<Long> selectMenuIdsByRoleId(Long roleId, boolean menuCheckStrictly)
    {
        if (roleId == null)
        {
            return new ArrayList<>();
        }
        LambdaQueryWrapper<Menu> wrapper = Wrappers.lambdaQuery(Menu.class)
                .select(Menu::getMenuId)
                .inSql(Menu::getMenuId, "select rm.menu_id from sys_role_menu rm where rm.role_id = " + roleId)
                .orderByAsc(Menu::getParentId, Menu::getOrderNum);
        if (menuCheckStrictly)
        {
            wrapper.notInSql(Menu::getMenuId, "select m.parent_id from sys_menu m "
                    + "inner join sys_role_menu rm on m.menu_id = rm.menu_id and rm.role_id = " + roleId);
        }
        return menuMapper.selectList(wrapper).stream().map(Menu::getMenuId).collect(Collectors.toList());
    }

    /**
     * 查询角色关联的部门 ID（复刻原 selectDeptListByRoleId）
     *
     * <p>外层等多价 IN 子查询按 parent_id, order_num 排序；strict 时用与原 SQL 完全相同的
     * NOT IN 子查询减去父部门，保留原 SQL 对 NULL parent_id 的 NOT IN 语义。
     * 子查询仅拼接 Long 角色 ID，无请求字符串。</p>
     */
    private List<Long> selectDeptIdsByRoleId(Long roleId, boolean deptCheckStrictly)
    {
        if (roleId == null)
        {
            return new ArrayList<>();
        }
        LambdaQueryWrapper<Dept> wrapper = Wrappers.lambdaQuery(Dept.class)
                .select(Dept::getDeptId)
                .inSql(Dept::getDeptId, "select rd.dept_id from sys_role_dept rd where rd.role_id = " + roleId)
                .orderByAsc(Dept::getParentId, Dept::getOrderNum);
        if (deptCheckStrictly)
        {
            wrapper.notInSql(Dept::getDeptId, "select d.parent_id from sys_dept d "
                    + "inner join sys_role_dept rd on d.dept_id = rd.dept_id and rd.role_id = " + roleId);
        }
        return deptMapper.selectList(wrapper).stream().map(Dept::getDeptId).collect(Collectors.toList());
    }

    /**
     * 构建与原 selectRoleVo 相同列清单的查询壳（11 列，无 create_by/update_by/update_time），
     * 供本仓库与 {@link UserRepository} 共用
     */
    static LambdaQueryWrapper<Role> roleVoColumns()
    {
        return Wrappers.lambdaQuery(Role.class)
                .select(Role::getRoleId, Role::getRoleName, Role::getRoleKey, Role::getRoleSort,
                        Role::getDataScope, Role::isMenuCheckStrictly, Role::isDeptCheckStrictly,
                        Role::getStatus, Role::getDelFlag, Role::getCreateTime, Role::getRemark);
    }

    /**
     * 将不分页的 PO 列表包装为分页 BO
     */
    private IPage<RoleBo> convertAll(List<Role> records)
    {
        Page<Role> poPage = new Page<>(1, records.size(), records.size());
        poPage.setRecords(records);
        return PageUtils.convert(poPage, po -> converter.convert(po, RoleBo.class));
    }
}
