package com.boot.system.service.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import com.boot.common.constant.Constants;
import com.boot.common.constant.UserConstants;
import com.boot.common.core.domain.TreeSelect;
import com.boot.common.core.domain.bo.MenuBo;
import com.boot.common.core.text.Convert;
import com.boot.common.exception.ServiceException;
import com.boot.common.utils.SecurityUtils;
import com.boot.common.utils.StringUtils;
import com.boot.system.domain.vo.MetaVo;
import com.boot.system.domain.vo.RouterVo;
import com.boot.system.repo.MenuRepository;
import com.boot.system.service.IMenuService;

/**
 * 菜单 业务层处理
 *
 * @author boot-plus
 */
@Service
@RequiredArgsConstructor
public class MenuServiceImpl implements IMenuService
{
    private static final Logger log = LoggerFactory.getLogger(MenuServiceImpl.class);

    public static final String PREMISSION_STRING = "perms[\"{0}\"]";

    public static final Long MENU_ROOT_ID = 0L;

    private final MenuRepository menuRepository;

    /**
     * 根据用户查询系统菜单列表
     *
     * @param userId 用户ID
     * @return 菜单列表
     */
    @Override
    public List<MenuBo> selectMenuList(Long userId)
    {
        return selectMenuList(new MenuBo(), userId);
    }

    /**
     * 查询系统菜单列表
     *
     * @param menu 菜单信息
     * @return 菜单列表
     */
    @Override
    public List<MenuBo> selectMenuList(MenuBo menu, Long userId)
    {
        List<MenuBo> menuList = null;
        // 管理员显示所有菜单信息
        if (SecurityUtils.isAdmin(userId))
        {
            menuList = menuRepository.selectMenuList(menu);
        }
        else
        {
            menu.getParams().put("userId", userId);
            menuList = menuRepository.selectMenuListByUserId(menu);
        }
        return menuList;
    }

    /**
     * 根据用户ID查询权限
     *
     * @param userId 用户ID
     * @return 权限列表
     */
    @Override
    public Set<String> selectMenuPermsByUserId(Long userId)
    {
        List<String> perms = menuRepository.selectMenuPermsByUserId(userId);
        Set<String> permsSet = new HashSet<>();
        for (String perm : perms)
        {
            if (StringUtils.isNotEmpty(perm))
            {
                permsSet.addAll(Arrays.asList(perm.trim().split(",")));
            }
        }
        return permsSet;
    }

    /**
     * 根据角色ID查询权限
     *
     * @param roleId 角色ID
     * @return 权限列表
     */
    @Override
    public Set<String> selectMenuPermsByRoleId(Long roleId)
    {
        List<String> perms = menuRepository.selectMenuPermsByRoleId(roleId);
        Set<String> permsSet = new HashSet<>();
        for (String perm : perms)
        {
            if (StringUtils.isNotEmpty(perm))
            {
                permsSet.addAll(Arrays.asList(perm.trim().split(",")));
            }
        }
        return permsSet;
    }

    /**
     * 根据用户ID查询菜单
     *
     * @param userId 用户名称
     * @return 菜单列表
     */
    @Override
    public List<MenuBo> selectMenuTreeByUserId(Long userId)
    {
        List<MenuBo> menus = null;
        if (SecurityUtils.isAdmin(userId))
        {
            menus = menuRepository.selectMenuTreeAll();
        }
        else
        {
            menus = menuRepository.selectMenuTreeByUserId(userId);
        }
        return getChildPerms(menus, MENU_ROOT_ID);
    }

    /**
     * 根据角色ID查询菜单树信息
     *
     * @param roleId 角色ID
     * @return 选中菜单列表
     */
    @Override
    public List<Long> selectMenuListByRoleId(Long roleId)
    {
        return menuRepository.selectMenuIdsByRoleId(roleId);
    }

    /**
     * 构建前端路由所需要的菜单
     *
     * @param menus 菜单列表
     * @return 路由列表
     */
    @Override
    public List<RouterVo> buildMenus(List<MenuBo> menus)
    {
        List<RouterVo> routers = new LinkedList<RouterVo>();
        for (MenuBo menu : menus)
        {
            RouterVo router = new RouterVo();
            router.setHidden("1".equals(menu.getVisible()));
            router.setName(getRouteName(menu));
            router.setPath(getRouterPath(menu));
            router.setComponent(getComponent(menu));
            router.setQuery(menu.getQuery());
            router.setMeta(new MetaVo(menu.getMenuName(), menu.getIcon(), StringUtils.equals("1", menu.getIsCache()), menu.getPath()));
            List<MenuBo> cMenus = menu.getChildren();
            if (StringUtils.isNotEmpty(cMenus) && UserConstants.TYPE_DIR.equals(menu.getMenuType()))
            {
                router.setAlwaysShow(true);
                router.setRedirect("noRedirect");
                router.setChildren(buildMenus(cMenus));
            }
            else if (isMenuFrame(menu))
            {
                router.setMeta(null);
                List<RouterVo> childrenList = new ArrayList<RouterVo>();
                RouterVo children = new RouterVo();
                children.setPath(menu.getPath());
                children.setComponent(menu.getComponent());
                children.setName(getRouteName(menu.getRouteName(), menu.getPath()));
                children.setMeta(new MetaVo(menu.getMenuName(), menu.getIcon(), StringUtils.equals("1", menu.getIsCache()), menu.getPath()));
                children.setQuery(menu.getQuery());
                childrenList.add(children);
                router.setChildren(childrenList);
            }
            else if (menu.getParentId().intValue() == MENU_ROOT_ID && isInnerLink(menu))
            {
                router.setMeta(new MetaVo(menu.getMenuName(), menu.getIcon()));
                router.setPath("/");
                List<RouterVo> childrenList = new ArrayList<RouterVo>();
                RouterVo children = new RouterVo();
                String routerPath = innerLinkReplaceEach(menu.getPath());
                children.setPath(routerPath);
                children.setComponent(UserConstants.INNER_LINK);
                children.setName(getRouteName(menu.getRouteName(), routerPath));
                children.setMeta(new MetaVo(menu.getMenuName(), menu.getIcon(), menu.getPath()));
                childrenList.add(children);
                router.setChildren(childrenList);
            }
            routers.add(router);
        }
        return routers;
    }

    /**
     * 构建前端所需要树结构
     *
     * @param menus 菜单列表
     * @return 树结构列表
     */
    @Override
    public List<MenuBo> buildMenuTree(List<MenuBo> menus)
    {
        List<MenuBo> returnList = new ArrayList<MenuBo>();
        List<Long> tempList = menus.stream().map(MenuBo::getMenuId).collect(Collectors.toList());
        for (Iterator<MenuBo> iterator = menus.iterator(); iterator.hasNext();)
        {
            MenuBo menu = (MenuBo) iterator.next();
            // 如果是顶级节点, 遍历该父节点的所有子节点
            if (!tempList.contains(menu.getParentId()))
            {
                recursionFn(menus, menu);
                returnList.add(menu);
            }
        }
        if (returnList.isEmpty())
        {
            returnList = menus;
        }
        return returnList;
    }

    /**
     * 构建前端所需要下拉树结构
     *
     * @param menus 菜单列表
     * @return 下拉树结构列表
     */
    @Override
    public List<TreeSelect> buildMenuTreeSelect(List<MenuBo> menus)
    {
        List<MenuBo> menuTrees = buildMenuTree(menus);
        return menuTrees.stream().map(TreeSelect::new).collect(Collectors.toList());
    }

    /**
     * 根据菜单ID查询信息
     *
     * @param menuId 菜单ID
     * @return 菜单信息
     */
    @Override
    public MenuBo selectMenuById(Long menuId)
    {
        return menuRepository.selectMenuById(menuId);
    }

    /**
     * 是否存在菜单子节点
     *
     * @param menuId 菜单ID
     * @return 结果
     */
    @Override
    public boolean hasChildByMenuId(Long menuId)
    {
        int result = menuRepository.hasChildByMenuId(menuId);
        return result > 0;
    }

    /**
     * 查询菜单使用数量
     *
     * @param menuId 菜单ID
     * @return 结果
     */
    @Override
    public boolean checkMenuExistRole(Long menuId)
    {
        int result = menuRepository.countMenuUsage(menuId);
        return result > 0;
    }

    /**
     * 新增保存菜单信息
     *
     * @param menu 菜单信息
     * @return 结果
     */
    @Override
    public int insertMenu(MenuBo menu)
    {
        return menuRepository.insertMenu(menu);
    }

    /**
     * 修改保存菜单信息
     *
     * @param menu 菜单信息
     * @return 结果
     */
    @Override
    public int updateMenu(MenuBo menu)
    {
        return menuRepository.updateMenu(menu);
    }

    /**
     * 保存菜单排序
     *
     * @param menuIds 菜单ID
     * @param orderNums 排序ID
     */
    @Override
    @Transactional
    public void updateMenuSort(String[] menuIds, String[] orderNums)
    {
        try
        {
            for (int i = 0; i < menuIds.length; i++)
            {
                MenuBo menu = new MenuBo();
                menu.setMenuId(Convert.toLong(menuIds[i]));
                menu.setOrderNum(Convert.toInt(orderNums[i]));
                menuRepository.updateMenuSort(menu);
            }
        }
        catch (Exception e)
        {
            throw new ServiceException("保存排序异常，请联系管理员");
        }
    }

    /**
     * 删除菜单管理信息
     *
     * @param menuId 菜单ID
     * @return 结果
     */
    @Override
    public int deleteMenuById(Long menuId)
    {
        return menuRepository.deleteMenuById(menuId);
    }

    /**
     * 校验菜单名称是否唯一
     *
     * @param menu 菜单信息
     * @return 结果
     */
    @Override
    public boolean checkMenuNameUnique(MenuBo menu)
    {
        Long menuId = StringUtils.isNull(menu.getMenuId()) ? -1L : menu.getMenuId();
        MenuBo info = menuRepository.selectMenuNameUnique(menu.getMenuName(), menu.getParentId());
        if (StringUtils.isNotNull(info) && info.getMenuId().longValue() != menuId.longValue())
        {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    /**
     * 校验路由名称是否唯一
     *
     * @param menu 菜单信息
     * @return 结果
     */
    @Override
    public boolean checkRouteConfigUnique(MenuBo menu)
    {
        Long menuId = StringUtils.isNull(menu.getMenuId()) ? -1L : menu.getMenuId();
        Long parentId = menu.getParentId();
        String path = menu.getPath();
        String routeName = StringUtils.isEmpty(menu.getRouteName()) ? path : menu.getRouteName();
        List<MenuBo> sysMenuList = menuRepository.selectMenusByPathOrRouteName(path, routeName);
        for (MenuBo sysMenu : sysMenuList)
        {
            if (sysMenu.getMenuId().longValue() != menuId.longValue())
            {
                Long dbParentId = sysMenu.getParentId();
                String dbPath = sysMenu.getPath();
                String dbRouteName = StringUtils.isEmpty(sysMenu.getRouteName()) ? dbPath : sysMenu.getRouteName();
                if (StringUtils.equalsAnyIgnoreCase(path, dbPath) && parentId.longValue() == dbParentId.longValue())
                {
                    log.warn("[同级路由冲突] 同级下已存在相同路由路径 '{}'，冲突菜单：{}", dbPath, sysMenu.getMenuName());
                    return UserConstants.NOT_UNIQUE;
                }
                else if (StringUtils.equalsAnyIgnoreCase(path, dbPath) && parentId.longValue() == MENU_ROOT_ID)
                {
                    log.warn("[根目录路由冲突] 根目录下路由 '{}' 必须唯一，已被菜单 '{}' 占用", path, sysMenu.getMenuName());
                    return UserConstants.NOT_UNIQUE;
                }
                else if (StringUtils.equalsAnyIgnoreCase(routeName, dbRouteName))
                {
                    log.warn("[路由名称冲突] 路由名称 '{}' 需全局唯一，已被菜单 '{}' 使用", routeName, sysMenu.getMenuName());
                    return UserConstants.NOT_UNIQUE;
                }
            }
        }
        return UserConstants.UNIQUE;
    }

    /**
     * 校验上级菜单是否为自己或自己的子菜单
     *
     * @param menu 菜单信息
     * @return 结果
     */
    @Override
    public boolean checkParentCycle(MenuBo menu)
    {
        Long menuId = menu.getMenuId();
        Long parentId = menu.getParentId();
        Map<Long, Long> parentMap = menuRepository.selectMenuList(new MenuBo()).stream().collect(Collectors.toMap(MenuBo::getMenuId, MenuBo::getParentId, (v1, v2) -> v1));
        Set<Long> visited = new HashSet<>();
        Long currentId = parentId;
        while (StringUtils.isNotNull(currentId) && visited.add(currentId))
        {
            if (menuId.equals(currentId))
            {
                return true;
            }
            currentId = parentMap.get(currentId);
        }
        return false;
    }

    /**
     * 获取路由名称
     *
     * @param menu 菜单信息
     * @return 路由名称
     */
    public String getRouteName(MenuBo menu)
    {
        // 非外链并且是一级目录（类型为目录）
        if (isMenuFrame(menu))
        {
            return StringUtils.EMPTY;
        }
        return getRouteName(menu.getRouteName(), menu.getPath());
    }

    /**
     * 获取路由名称，如没有配置路由名称则取路由地址
     *
     * @param name 路由名称
     * @param path 路由地址
     * @return 路由名称（驼峰格式）
     */
    public String getRouteName(String name, String path)
    {
        String routerName = StringUtils.isNotEmpty(name) ? name : path;
        return StringUtils.capitalize(routerName);
    }

    /**
     * 获取路由地址
     *
     * @param menu 菜单信息
     * @return 路由地址
     */
    public String getRouterPath(MenuBo menu)
    {
        String routerPath = menu.getPath();
        // 内链打开外网方式
        if (menu.getParentId().intValue() != MENU_ROOT_ID && isInnerLink(menu))
        {
            routerPath = innerLinkReplaceEach(routerPath);
        }
        // 非外链并且是一级目录（类型为目录）
        if (MENU_ROOT_ID == menu.getParentId().intValue() && UserConstants.TYPE_DIR.equals(menu.getMenuType())
                && UserConstants.NO_FRAME.equals(menu.getIsFrame()))
        {
            routerPath = "/" + menu.getPath();
        }
        // 非外链并且是一级目录（类型为菜单）
        else if (isMenuFrame(menu))
        {
            routerPath = "/";
        }
        return routerPath;
    }

    /**
     * 获取组件信息
     *
     * @param menu 菜单信息
     * @return 组件信息
     */
    public String getComponent(MenuBo menu)
    {
        String component = UserConstants.LAYOUT;
        if (StringUtils.isNotEmpty(menu.getComponent()) && !isMenuFrame(menu))
        {
            component = menu.getComponent();
        }
        else if (StringUtils.isEmpty(menu.getComponent()) && menu.getParentId().intValue() != MENU_ROOT_ID && isInnerLink(menu))
        {
            component = UserConstants.INNER_LINK;
        }
        else if (StringUtils.isEmpty(menu.getComponent()) && isParentView(menu))
        {
            component = UserConstants.PARENT_VIEW;
        }
        return component;
    }

    /**
     * 是否为菜单内部跳转
     *
     * @param menu 菜单信息
     * @return 结果
     */
    public boolean isMenuFrame(MenuBo menu)
    {
        return menu.getParentId().intValue() == MENU_ROOT_ID && UserConstants.TYPE_MENU.equals(menu.getMenuType())
                && menu.getIsFrame().equals(UserConstants.NO_FRAME);
    }

    /**
     * 是否为parent_view组件
     *
     * @param menu 菜单信息
     * @return 结果
     */
    public boolean isParentView(MenuBo menu)
    {
        return menu.getParentId().intValue() != MENU_ROOT_ID && UserConstants.TYPE_DIR.equals(menu.getMenuType());
    }

    /**
     * 是否为内链组件
     *
     * @param menu 菜单信息
     * @return 结果
     */
    public boolean isInnerLink(MenuBo menu)
    {
        return menu.getIsFrame().equals(UserConstants.NO_FRAME) && StringUtils.ishttp(menu.getPath());
    }

    /**
     * 根据父节点的ID获取所有子节点
     *
     * @param list 分类表
     * @param parentId 传入的父节点ID
     * @return String
     */
    public List<MenuBo> getChildPerms(List<MenuBo> list, long parentId)
    {
        List<MenuBo> returnList = new ArrayList<MenuBo>();
        for (Iterator<MenuBo> iterator = list.iterator(); iterator.hasNext();)
        {
            MenuBo t = (MenuBo) iterator.next();
            // 一、根据传入的某个父节点ID,遍历该父节点的所有子节点
            if (t.getParentId() == parentId)
            {
                recursionFn(list, t);
                returnList.add(t);
            }
        }
        return returnList;
    }

    /**
     * 递归列表
     *
     * @param list 分类表
     * @param t 子节点
     */
    private void recursionFn(List<MenuBo> list, MenuBo t)
    {
        // 得到子节点列表
        List<MenuBo> childList = getChildList(list, t);
        t.setChildren(childList);
        for (MenuBo tChild : childList)
        {
            if (hasChild(list, tChild))
            {
                recursionFn(list, tChild);
            }
        }
    }

    /**
     * 得到子节点列表
     */
    private List<MenuBo> getChildList(List<MenuBo> list, MenuBo t)
    {
        List<MenuBo> tlist = new ArrayList<MenuBo>();
        Iterator<MenuBo> it = list.iterator();
        while (it.hasNext())
        {
            MenuBo n = (MenuBo) it.next();
            if (n.getParentId().longValue() == t.getMenuId().longValue())
            {
                tlist.add(n);
            }
        }
        return tlist;
    }

    /**
     * 判断是否有子节点
     */
    private boolean hasChild(List<MenuBo> list, MenuBo t)
    {
        return getChildList(list, t).size() > 0;
    }

    /**
     * 内链域名特殊字符替换
     *
     * @return 替换后的内链域名
     */
    public String innerLinkReplaceEach(String path)
    {
        return StringUtils.replaceEach(path, new String[] { Constants.HTTP, Constants.HTTPS, Constants.WWW, ".", ":" },
                new String[] { "", "", "", "/", "/" });
    }
}
