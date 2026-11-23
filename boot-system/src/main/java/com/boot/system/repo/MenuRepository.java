package com.boot.system.repo;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.boot.common.core.domain.bo.MenuBo;
import com.boot.common.utils.DateUtils;
import com.boot.common.utils.PageUtils;
import com.boot.system.repo.mapper.MenuMapper;
import com.boot.system.repo.mapper.RoleMapper;
import com.boot.system.repo.mapper.RoleMenuMapper;
import com.boot.system.repo.mapper.UserMapper;
import com.boot.system.repo.mapper.UserRoleMapper;
import com.boot.system.repo.po.Menu;
import com.boot.system.repo.po.Role;
import com.boot.system.repo.po.RoleMenu;
import com.boot.system.repo.po.UserRole;
import io.github.linpeilie.Converter;
import org.springframework.stereotype.Repository;
import lombok.RequiredArgsConstructor;

/**
 * 菜单信息 聚合仓库
 *
 * @author boot-plus
 */
@Repository
@RequiredArgsConstructor
public class MenuRepository
{
    private final MenuMapper menuMapper;

    private final RoleMenuMapper roleMenuMapper;

    private final RoleMapper roleMapper;

    private final UserRoleMapper userRoleMapper;

    private final UserMapper userMapper;

    private final Converter converter;

    /**
     * 查询系统菜单列表
     *
     * <p>复刻原 selectMenuList（管理员单表查询）语义：selectMenuVo 列子集，
     * 动态条件 menu_name like / visible = / status =，按 parent_id, order_num 排序；
     * 原 ifnull(perms,'') 由查询后补空串等价实现。</p>
     *
     * @param query 菜单查询条件
     * @return 菜单列表
     */
    public List<MenuBo> selectMenuList(MenuBo query)
    {
        LambdaQueryWrapper<Menu> wrapper = menuVoColumnWrapper();
        if (query.getMenuName() != null && !query.getMenuName().isEmpty())
        {
            wrapper.like(Menu::getMenuName, query.getMenuName());
        }
        if (query.getVisible() != null && !query.getVisible().isEmpty())
        {
            wrapper.eq(Menu::getVisible, query.getVisible());
        }
        if (query.getStatus() != null && !query.getStatus().isEmpty())
        {
            wrapper.eq(Menu::getStatus, query.getStatus());
        }
        wrapper.orderByAsc(Menu::getParentId, Menu::getOrderNum);
        return convertList(applyMenuVoPerms(menuMapper.selectList(wrapper)));
    }

    /**
     * 根据用户查询系统菜单列表
     *
     * @param query 菜单查询条件
     * @return 菜单列表
     */
    public List<MenuBo> selectMenuListByUserId(MenuBo query)
    {
        // 原 SQL：where ur.user_id = #{params.userId}（params 为 QueryParam.params Map，userId 由服务层写入）
        Object userIdValue = query.getParams().get("userId");
        if (userIdValue == null)
        {
            // 原等值条件 ur.user_id = null 永不命中
            return new ArrayList<>();
        }
        List<Long> roleIds = userRoleMapper.selectList(Wrappers.lambdaQuery(UserRole.class)
                .select(UserRole::getRoleId)
                .eq(UserRole::getUserId, userIdValue))
                .stream().map(UserRole::getRoleId).collect(Collectors.toList());
        if (roleIds.isEmpty())
        {
            return new ArrayList<>();
        }
        List<Long> menuIds = selectMenuIdsByRoleIds(roleIds);
        if (menuIds.isEmpty())
        {
            return new ArrayList<>();
        }
        LambdaQueryWrapper<Menu> wrapper = menuVoColumnWrapper().in(Menu::getMenuId, menuIds);
        if (query.getMenuName() != null && !query.getMenuName().isEmpty())
        {
            wrapper.like(Menu::getMenuName, query.getMenuName());
        }
        if (query.getVisible() != null && !query.getVisible().isEmpty())
        {
            wrapper.eq(Menu::getVisible, query.getVisible());
        }
        if (query.getStatus() != null && !query.getStatus().isEmpty())
        {
            wrapper.eq(Menu::getStatus, query.getStatus());
        }
        wrapper.orderByAsc(Menu::getParentId, Menu::getOrderNum);
        // 原 distinct 由 IN 去重等价；原 ifnull(perms,'') 由 applyMenuVoPerms 等价实现
        return convertList(applyMenuVoPerms(menuMapper.selectList(wrapper)));
    }

    /**
     * 查询全部菜单树（目录与菜单，正常状态）
     *
     * <p>复刻原 selectMenuTreeAll 语义：menu_type in ('M','C') 且 status=0（原为数字
     * 字面量，等价转换为 '0'），按 parent_id, order_num 排序；原 distinct 因 menu_id
     * 为主键且无 join 而无效果，省略；ifnull(perms,'') 由查询后补空串等价实现。</p>
     *
     * @return 菜单列表
     */
    public List<MenuBo> selectMenuTreeAll()
    {
        return convertList(applyMenuVoPerms(menuMapper.selectList(menuVoColumnWrapper()
                .in(Menu::getMenuType, "M", "C")
                .eq(Menu::getStatus, "0")
                .orderByAsc(Menu::getParentId, Menu::getOrderNum))));
    }

    /**
     * 根据用户ID查询菜单树
     *
     * @param userId 用户ID
     * @return 菜单列表
     */
    public List<MenuBo> selectMenuTreeByUserId(Long userId)
    {
        if (userId == null || userMapper.selectById(userId) == null)
        {
            // 原 SQL 内连接 sys_user u 且 where u.user_id = #{userId}，用户不存在时无结果
            return new ArrayList<>();
        }
        List<Long> roleIds = userRoleMapper.selectList(Wrappers.lambdaQuery(UserRole.class)
                .select(UserRole::getRoleId)
                .eq(UserRole::getUserId, userId))
                .stream().map(UserRole::getRoleId).collect(Collectors.toList());
        if (roleIds.isEmpty())
        {
            return new ArrayList<>();
        }
        // 原 SQL where ... AND ro.status = 0（左连接 + where 等价内连接），仅保留启用角色
        List<Long> enabledRoleIds = roleMapper.selectList(Wrappers.lambdaQuery(Role.class)
                .select(Role::getRoleId)
                .in(Role::getRoleId, roleIds)
                .eq(Role::getStatus, "0"))
                .stream().map(Role::getRoleId).collect(Collectors.toList());
        if (enabledRoleIds.isEmpty())
        {
            return new ArrayList<>();
        }
        List<Long> menuIds = selectMenuIdsByRoleIds(enabledRoleIds);
        if (menuIds.isEmpty())
        {
            return new ArrayList<>();
        }
        LambdaQueryWrapper<Menu> wrapper = menuVoColumnWrapper()
                .in(Menu::getMenuId, menuIds)
                .in(Menu::getMenuType, "M", "C")
                .eq(Menu::getStatus, "0")
                .orderByAsc(Menu::getParentId, Menu::getOrderNum);
        // 原 distinct 由 IN 去重等价；原 ifnull(perms,'') 由 applyMenuVoPerms 等价实现
        return convertList(applyMenuVoPerms(menuMapper.selectList(wrapper)));
    }

    /**
     * 根据用户ID查询权限
     *
     * <p>复刻原 selectMenuPermsByUserId join 语义：用户关联角色（r.status='0'，
     * 与原 SQL 一致，无 del_flag 过滤）→ 角色关联菜单 → m.status='0' 的菜单 perms
     * 去重（含 null，与原 distinct 一致）。</p>
     *
     * @param userId 用户ID
     * @return 权限列表
     */
    public List<String> selectMenuPermsByUserId(Long userId)
    {
        List<Long> roleIds = userRoleMapper.selectList(Wrappers.lambdaQuery(UserRole.class)
                .select(UserRole::getRoleId)
                .eq(UserRole::getUserId, userId))
                .stream().map(UserRole::getRoleId).collect(Collectors.toList());
        if (roleIds.isEmpty())
        {
            return new ArrayList<>();
        }
        List<Long> enabledRoleIds = roleMapper.selectList(Wrappers.lambdaQuery(Role.class)
                .select(Role::getRoleId)
                .in(Role::getRoleId, roleIds)
                .eq(Role::getStatus, "0"))
                .stream().map(Role::getRoleId).collect(Collectors.toList());
        if (enabledRoleIds.isEmpty())
        {
            return new ArrayList<>();
        }
        List<Long> menuIds = selectMenuIdsByRoleIds(enabledRoleIds);
        if (menuIds.isEmpty())
        {
            return new ArrayList<>();
        }
        return selectPermsByMenuIds(menuIds);
    }

    /**
     * 根据角色ID查询权限
     *
     * <p>复刻原 selectMenuPermsByRoleId join 语义：角色关联的 m.status='0' 菜单
     * perms 去重（含 null，与原 distinct 一致）。</p>
     *
     * @param roleId 角色ID
     * @return 权限列表
     */
    public List<String> selectMenuPermsByRoleId(Long roleId)
    {
        List<Long> menuIds = selectMenuIdsByRoleIds(List.of(roleId));
        if (menuIds.isEmpty())
        {
            return new ArrayList<>();
        }
        return selectPermsByMenuIds(menuIds);
    }

    /**
     * 查询角色组关联且未删除的菜单ID（孤儿关联行随 in 条件丢弃）
     *
     * @param roleIds 角色ID组
     * @return 菜单ID列表
     */
    private List<Long> selectMenuIdsByRoleIds(List<Long> roleIds)
    {
        List<Long> menuIds = roleMenuMapper.selectList(Wrappers.lambdaQuery(RoleMenu.class)
                .select(RoleMenu::getMenuId)
                .in(RoleMenu::getRoleId, roleIds))
                .stream().map(RoleMenu::getMenuId).collect(Collectors.toList());
        if (menuIds.isEmpty())
        {
            return menuIds;
        }
        return menuMapper.selectList(Wrappers.lambdaQuery(Menu.class)
                .select(Menu::getMenuId)
                .in(Menu::getMenuId, menuIds))
                .stream().map(Menu::getMenuId).collect(Collectors.toList());
    }

    /**
     * 查询菜单组中 m.status='0' 菜单的 perms 去重结果（含 null，与原 distinct 一致）
     *
     * @param menuIds 菜单ID组
     * @return 权限列表
     */
    private List<String> selectPermsByMenuIds(List<Long> menuIds)
    {
        return menuMapper.selectList(Wrappers.lambdaQuery(Menu.class)
                .select(Menu::getPerms)
                .in(Menu::getMenuId, menuIds)
                .eq(Menu::getStatus, "0"))
                .stream().map(Menu::getPerms).distinct().collect(Collectors.toList());
    }

    /**
     * 根据角色ID查询菜单树信息
     *
     * @param roleId 角色ID
     * @return 选中菜单列表
     */
    public List<Long> selectMenuIdsByRoleId(Long roleId)
    {
        if (roleId == null)
        {
            return new ArrayList<>();
        }
        Role role = roleMapper.selectOne(Wrappers.lambdaQuery(Role.class)
                .select(Role::getRoleId, Role::isMenuCheckStrictly)
                .eq(Role::getRoleId, roleId));
        boolean menuCheckStrictly = role != null && role.isMenuCheckStrictly();
        // 外层等多价 IN 子查询，按 parent_id, order_num 排序（原 from sys_menu m left join
        // sys_role_menu rm ... where rm.role_id = ? 等价于内连接投影 m.menu_id）。
        // 子查询仅拼接 Long 角色 ID，无请求字符串。
        LambdaQueryWrapper<Menu> wrapper = Wrappers.lambdaQuery(Menu.class)
                .select(Menu::getMenuId)
                .inSql(Menu::getMenuId, "select rm.menu_id from sys_role_menu rm where rm.role_id = " + roleId)
                .orderByAsc(Menu::getParentId, Menu::getOrderNum);
        if (menuCheckStrictly)
        {
            // 与原 SQL 完全相同的 NOT IN 子查询（保留 NULL parent_id 的 NOT IN 语义）
            wrapper.notInSql(Menu::getMenuId, "select m.parent_id from sys_menu m "
                    + "inner join sys_role_menu rm on m.menu_id = rm.menu_id and rm.role_id = " + roleId);
        }
        return menuMapper.selectList(wrapper).stream().map(Menu::getMenuId).collect(Collectors.toList());
    }

    /**
     * 根据菜单ID查询信息
     *
     * <p>复刻原 selectMenuById 语义：selectMenuVo 列子集，ifnull(perms,'') 由
     * 查询后补空串等价实现。</p>
     *
     * @param menuId 菜单ID
     * @return 菜单信息
     */
    public MenuBo selectMenuById(Long menuId)
    {
        Menu menu = menuMapper.selectOne(menuVoColumnWrapper().eq(Menu::getMenuId, menuId));
        if (menu != null)
        {
            applyMenuVoPerms(List.of(menu));
        }
        return converter.convert(menu, MenuBo.class);
    }

    /**
     * 是否存在菜单子节点
     *
     * @param menuId 菜单ID
     * @return 结果
     */
    public int hasChildByMenuId(Long menuId)
    {
        Long count = menuMapper.selectCount(Wrappers.lambdaQuery(Menu.class).eq(Menu::getParentId, menuId));
        return count.intValue();
    }

    /**
     * 查询菜单使用数量
     *
     * @param menuId 菜单ID
     * @return 结果
     */
    public int countMenuUsage(Long menuId)
    {
        Long count = roleMenuMapper.selectCount(Wrappers.lambdaQuery(RoleMenu.class).eq(RoleMenu::getMenuId, menuId));
        return count.intValue();
    }

    /**
     * 新增菜单信息
     *
     * @param menu 菜单信息
     * @return 结果
     */
    public int insertMenu(MenuBo menu)
    {
        Menu po = converter.convert(menu, Menu.class);
        po.setCreateTime(DateUtils.getNowDate());
        return menuMapper.insert(po);
    }

    /**
     * 修改菜单信息
     *
     * @param menu 菜单信息
     * @return 结果
     */
    public int updateMenu(MenuBo menu)
    {
        Menu po = converter.convert(menu, Menu.class);
        // 镜像原 XML updateMenu 的动态 <if> 条件（原 OGNL `x != null and x != ''` / `x != null`）
        return menuMapper.update(null, Wrappers.lambdaUpdate(Menu.class)
                .set(po.getMenuName() != null && !po.getMenuName().isEmpty(), Menu::getMenuName, po.getMenuName())
                .set(po.getParentId() != null, Menu::getParentId, po.getParentId())
                .set(po.getOrderNum() != null, Menu::getOrderNum, po.getOrderNum())
                .set(po.getPath() != null && !po.getPath().isEmpty(), Menu::getPath, po.getPath())
                .set(po.getComponent() != null, Menu::getComponent, po.getComponent())
                .set(po.getQuery() != null, Menu::getQuery, po.getQuery())
                .set(po.getRouteName() != null, Menu::getRouteName, po.getRouteName())
                .set(po.getIsFrame() != null && !po.getIsFrame().isEmpty(), Menu::getIsFrame, po.getIsFrame())
                .set(po.getIsCache() != null && !po.getIsCache().isEmpty(), Menu::getIsCache, po.getIsCache())
                .set(po.getMenuType() != null && !po.getMenuType().isEmpty(), Menu::getMenuType, po.getMenuType())
                .set(po.getVisible() != null, Menu::getVisible, po.getVisible())
                .set(po.getStatus() != null, Menu::getStatus, po.getStatus())
                .set(po.getPerms() != null, Menu::getPerms, po.getPerms())
                .set(po.getIcon() != null && !po.getIcon().isEmpty(), Menu::getIcon, po.getIcon())
                .set(po.getRemark() != null && !po.getRemark().isEmpty(), Menu::getRemark, po.getRemark())
                .set(po.getUpdateBy() != null && !po.getUpdateBy().isEmpty(), Menu::getUpdateBy, po.getUpdateBy())
                .set(Menu::getUpdateTime, DateUtils.getNowDate())
                .eq(Menu::getMenuId, po.getMenuId()));
    }

    /**
     * 保存菜单排序
     *
     * @param menu 菜单信息
     */
    public void updateMenuSort(MenuBo menu)
    {
        Menu po = converter.convert(menu, Menu.class);
        menuMapper.update(null, Wrappers.lambdaUpdate(Menu.class)
                .set(Menu::getOrderNum, po.getOrderNum())
                .eq(Menu::getMenuId, po.getMenuId()));
    }

    /**
     * 删除菜单管理信息
     *
     * @param menuId 菜单ID
     * @return 结果
     */
    public int deleteMenuById(Long menuId)
    {
        return menuMapper.deleteById(menuId);
    }

    /**
     * 校验菜单名称是否唯一
     *
     * <p>复刻原 checkMenuNameUnique 语义：selectMenuVo 列子集 + menu_name/parent_id
     * 过滤 + limit 1，ifnull(perms,'') 由查询后补空串等价实现。</p>
     *
     * @param menuName 菜单名称
     * @param parentId 父菜单ID
     * @return 结果
     */
    public MenuBo selectMenuNameUnique(String menuName, Long parentId)
    {
        Menu menu = menuMapper.selectOne(menuVoColumnWrapper()
                .eq(Menu::getMenuName, menuName)
                .eq(Menu::getParentId, parentId)
                .last("limit 1"));
        if (menu != null)
        {
            applyMenuVoPerms(List.of(menu));
        }
        return converter.convert(menu, MenuBo.class);
    }

    /**
     * 根据路由路径或名称查询菜单信息（用于唯一性校验）
     *
     * <p>复刻原 selectMenusByPathOrRouteName 语义：menu_type in ('M','C') 且
     * path/route_name 四组等值 or 匹配，ifnull(perms,'') 由查询后补空串等价实现。</p>
     *
     * @param path 路由地址
     * @param routeName 路由名称
     * @return 匹配的菜单列表
     */
    public List<MenuBo> selectMenusByPathOrRouteName(String path, String routeName)
    {
        return convertList(applyMenuVoPerms(menuMapper.selectList(menuVoColumnWrapper()
                .in(Menu::getMenuType, "M", "C")
                .and(w -> w.eq(Menu::getPath, path)
                        .or().eq(Menu::getPath, routeName)
                        .or().eq(Menu::getRouteName, path)
                        .or().eq(Menu::getRouteName, routeName)))));
    }

    /**
     * 构建与原 selectMenuVo 相同列清单的查询壳（perms 读原始列，null 由 applyMenuVoPerms 补空串）
     */
    private LambdaQueryWrapper<Menu> menuVoColumnWrapper()
    {
        return Wrappers.lambdaQuery(Menu.class)
                .select(Menu::getMenuId, Menu::getMenuName, Menu::getParentId, Menu::getOrderNum,
                        Menu::getPath, Menu::getComponent, Menu::getQuery, Menu::getRouteName,
                        Menu::getIsFrame, Menu::getIsCache, Menu::getMenuType, Menu::getVisible,
                        Menu::getStatus, Menu::getPerms, Menu::getIcon, Menu::getCreateTime);
    }

    /**
     * 复刻原 selectMenuVo 的 ifnull(perms,'')：perms 为 null 时补为空串
     *
     * @param menus 菜单列表
     * @return 原列表（就地修改）
     */
    private List<Menu> applyMenuVoPerms(List<Menu> menus)
    {
        for (Menu menu : menus)
        {
            if (menu.getPerms() == null)
            {
                menu.setPerms("");
            }
        }
        return menus;
    }

    /**
     * 将 PO 列表转换为 BO 列表
     */
    private List<MenuBo> convertList(List<Menu> records)
    {
        return PageUtils.convert(records, po -> converter.convert(po, MenuBo.class));
    }
}
