package com.boot.system.repo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.toolkit.Db;
import com.boot.common.core.domain.bo.RoleBo;
import com.boot.common.core.domain.bo.UserBo;
import com.boot.common.utils.DateUtils;
import com.boot.common.utils.PageUtils;
import com.boot.system.repo.mapper.DeptMapper;
import com.boot.system.repo.mapper.PostMapper;
import com.boot.system.repo.mapper.RoleMapper;
import com.boot.system.repo.mapper.UserMapper;
import com.boot.system.repo.mapper.UserPostMapper;
import com.boot.system.repo.mapper.UserRoleMapper;
import com.boot.system.repo.po.Dept;
import com.boot.system.repo.po.Post;
import com.boot.system.repo.po.Role;
import com.boot.system.repo.po.User;
import com.boot.system.repo.po.UserPost;
import com.boot.system.repo.po.UserRole;
import com.boot.system.domain.bo.PostBo;
import io.github.linpeilie.Converter;
import org.springframework.stereotype.Repository;
import lombok.RequiredArgsConstructor;

/**
 * 用户信息 聚合仓库
 *
 * @author boot-plus
 */
@Repository
@RequiredArgsConstructor
public class UserRepository
{
    private final UserMapper userMapper;

    private final UserRoleMapper userRoleMapper;

    private final UserPostMapper userPostMapper;

    private final RoleMapper roleMapper;

    private final PostMapper postMapper;

    private final DeptMapper deptMapper;

    private final Converter converter;

    /**
     * 根据条件分页查询用户列表
     *
     * @param page 分页参数
     * @param query 用户查询条件
     * @return 用户信息集合信息
     */
    public IPage<UserBo> selectUserList(IPage<UserBo> page, UserBo query)
    {
        // 原 join 的 dept_name/leader 已不在单表 wrapper 上，仅保留基表排序列
        PageUtils.sanitizeOrders(page, Set.of("user_id", "dept_id", "user_name", "nick_name", "email",
                "phonenumber", "sex", "status", "create_time", "update_time", "login_date"));
        LambdaQueryWrapper<User> wrapper = Wrappers.lambdaQuery(User.class)
                .select(User::getUserId, User::getDeptId, User::getNickName, User::getUserName, User::getEmail,
                        User::getAvatar, User::getPhonenumber, User::getSex, User::getStatus, User::getDelFlag,
                        User::getLoginIp, User::getLoginDate, User::getCreateBy, User::getCreateTime, User::getRemark);
        applyUserFilters(wrapper, query, true);
        if (page == null)
        {
            return convertAll(fillUserDepts(userMapper.selectList(wrapper), true));
        }
        Page<User> poPage = new Page<>(page.getCurrent(), page.getSize());
        poPage.setOrders(page.orders());
        userMapper.selectPage(poPage, wrapper);
        fillUserDepts(poPage.getRecords(), true);
        return PageUtils.convert(poPage, po -> converter.convert(po, UserBo.class));
    }

    /**
     * 根据条件分页查询已配用户角色列表
     *
     * @param page 分页参数
     * @param query 用户查询条件
     * @return 用户信息集合信息
     */
    public IPage<UserBo> selectAllocatedList(IPage<UserBo> page, UserBo query)
    {
        // 原 join 的 dept_name/leader 已不在单表 wrapper 上，仅保留基表排序列
        PageUtils.sanitizeOrders(page, Set.of("user_id", "dept_id", "user_name", "nick_name", "email",
                "phonenumber", "sex", "status", "create_time", "update_time", "login_date"));
        Long roleId = query.getRoleId();
        if (roleId == null)
        {
            // 原 SQL 为 r.role_id = #{q.roleId}，roleId 为 null 时等值比较永不命中
            return emptyUserPage(page);
        }
        LambdaQueryWrapper<User> wrapper = Wrappers.lambdaQuery(User.class)
                .select(User::getUserId, User::getDeptId, User::getUserName, User::getNickName, User::getEmail,
                        User::getPhonenumber, User::getStatus, User::getCreateTime)
                // 复刻原 inner join sys_role r：仅当角色存在时关联行才有效；仅拼接服务端 Long 主键值
                .inSql(User::getUserId, "select ur.user_id from sys_user_role ur "
                        + "inner join sys_role r on r.role_id = ur.role_id where ur.role_id = " + roleId);
        applyUserFilters(wrapper, query, false);
        if (page == null)
        {
            return convertAll(fillUserDepts(userMapper.selectList(wrapper), false));
        }
        Page<User> poPage = new Page<>(page.getCurrent(), page.getSize());
        poPage.setOrders(page.orders());
        userMapper.selectPage(poPage, wrapper);
        fillUserDepts(poPage.getRecords(), false);
        return PageUtils.convert(poPage, po -> converter.convert(po, UserBo.class));
    }

    /**
     * 根据条件分页查询未分配用户角色列表
     *
     * @param page 分页参数
     * @param query 用户查询条件
     * @return 用户信息集合信息
     */
    public IPage<UserBo> selectUnallocatedList(IPage<UserBo> page, UserBo query)
    {
        // 原 join 的 dept_name/leader 已不在单表 wrapper 上，仅保留基表排序列
        PageUtils.sanitizeOrders(page, Set.of("user_id", "dept_id", "user_name", "nick_name", "email",
                "phonenumber", "sex", "status", "create_time", "update_time", "login_date"));
        Long roleId = query.getRoleId();
        LambdaQueryWrapper<User> wrapper = Wrappers.lambdaQuery(User.class)
                .select(User::getUserId, User::getDeptId, User::getUserName, User::getNickName, User::getEmail,
                        User::getPhonenumber, User::getStatus, User::getCreateTime);
        if (roleId != null)
        {
            // 原 not-in 子查询 fixed 条件：仅拼接 Long 主键值，无请求字符串
            wrapper.notInSql(User::getUserId, "select user_id from sys_user_role where role_id = " + roleId);
        }
        else
        {
            // 原 SQL 中 roleId 为 null 时 (r.role_id != null or r.role_id is null) 仅保留无任何角色行的用户
            wrapper.notInSql(User::getUserId, "select user_id from sys_user_role");
        }
        applyUserFilters(wrapper, query, false);
        if (page == null)
        {
            return convertAll(fillUserDepts(userMapper.selectList(wrapper), false));
        }
        Page<User> poPage = new Page<>(page.getCurrent(), page.getSize());
        poPage.setOrders(page.orders());
        userMapper.selectPage(poPage, wrapper);
        fillUserDepts(poPage.getRecords(), false);
        return PageUtils.convert(poPage, po -> converter.convert(po, UserBo.class));
    }

    /**
     * 通过用户名查询用户
     *
     * <p>复刻原 selectUserByUserName join 语义：用户按 user_name + del_flag='0' 过滤，
     * 部门与角色集合由 {@link #fillUserRelations} 装配。</p>
     *
     * @param userName 用户名
     * @return 用户对象信息
     */
    public UserBo selectUserByUserName(String userName)
    {
        User po = userMapper.selectOne(Wrappers.lambdaQuery(User.class)
                .eq(User::getUserName, userName)
                .eq(User::getDelFlag, "0"));
        return converter.convert(fillUserRelations(po), UserBo.class);
    }

    /**
     * 通过用户ID查询用户
     *
     * <p>复刻原 selectUserById join 语义：用户主行不带 del_flag 过滤，
     * 部门与角色集合由 {@link #fillUserRelations} 装配。</p>
     *
     * @param userId 用户ID
     * @return 用户对象信息
     */
    public UserBo selectUserById(Long userId)
    {
        User po = userMapper.selectById(userId);
        return converter.convert(fillUserRelations(po), UserBo.class);
    }

    /**
     * 通过用户ID查询用户详情（含角色组、岗位组）
     *
     * @param userId 用户ID
     * @return 用户对象信息
     */
    public UserBo selectUserDetailById(Long userId)
    {
        UserBo user = selectUserById(userId);
        if (user != null)
        {
            if (user.getRoles() == null)
            {
                user.setRoles(new ArrayList<>());
            }
            user.setRoleIds(selectRoleIdsByUserId(userId).toArray(new Long[0]));
            user.setPostIds(selectPostIdsByUserId(userId).toArray(new Long[0]));
        }
        return user;
    }

    /**
     * 通过用户名查询用户ID
     *
     * @param userName 用户名
     * @return 用户ID
     */
    public Long selectUserIdByUserName(String userName)
    {
        User user = userMapper.selectOne(Wrappers.lambdaQuery(User.class)
                .select(User::getUserId)
                .eq(User::getUserName, userName)
                .eq(User::getDelFlag, "0")
                .last("limit 1"));
        return user != null ? user.getUserId() : null;
    }

    /**
     * 通过手机号码查询用户ID
     *
     * @param phonenumber 手机号码
     * @return 用户ID
     */
    public Long selectUserIdByPhone(String phonenumber)
    {
        User user = userMapper.selectOne(Wrappers.lambdaQuery(User.class)
                .select(User::getUserId)
                .eq(User::getPhonenumber, phonenumber)
                .eq(User::getDelFlag, "0")
                .last("limit 1"));
        return user != null ? user.getUserId() : null;
    }

    /**
     * 通过邮箱查询用户ID
     *
     * @param email 用户邮箱
     * @return 用户ID
     */
    public Long selectUserIdByEmail(String email)
    {
        User user = userMapper.selectOne(Wrappers.lambdaQuery(User.class)
                .select(User::getUserId)
                .eq(User::getEmail, email)
                .eq(User::getDelFlag, "0")
                .last("limit 1"));
        return user != null ? user.getUserId() : null;
    }

    /**
     * 查询用户所属角色
     *
     * <p>复刻原 selectRolesByUserName join 语义：用户按 user_name 匹配（无 del_flag 过滤），
     * 角色取关联行且 r.del_flag='0'，孤儿关联行随 in 条件丢弃。</p>
     *
     * @param userName 用户名
     * @return 角色列表
     */
    public List<RoleBo> selectRolesByUserName(String userName)
    {
        List<Long> userIds = userMapper.selectList(Wrappers.lambdaQuery(User.class)
                .select(User::getUserId)
                .eq(User::getUserName, userName))
                .stream().map(User::getUserId).collect(Collectors.toList());
        if (userIds.isEmpty())
        {
            return new ArrayList<>();
        }
        List<Long> roleIds = userRoleMapper.selectList(Wrappers.lambdaQuery(UserRole.class)
                .select(UserRole::getRoleId)
                .in(UserRole::getUserId, userIds))
                .stream().map(UserRole::getRoleId).collect(Collectors.toList());
        if (roleIds.isEmpty())
        {
            return new ArrayList<>();
        }
        return PageUtils.convert(roleMapper.selectList(RoleRepository.roleVoColumns()
                .in(Role::getRoleId, roleIds)
                .eq(Role::getDelFlag, "0")),
                po -> converter.convert(po, RoleBo.class));
    }

    /**
     * 查询用户所属岗位
     *
     * <p>复刻原 selectPostsByUserName join 语义：用户按 user_name 匹配（无 del_flag 过滤），
     * 岗位取关联行（仅 post_id/post_name/post_code 列），孤儿关联行随 in 条件丢弃。</p>
     *
     * @param userName 用户名
     * @return 岗位列表
     */
    public List<PostBo> selectPostsByUserName(String userName)
    {
        List<Long> userIds = userMapper.selectList(Wrappers.lambdaQuery(User.class)
                .select(User::getUserId)
                .eq(User::getUserName, userName))
                .stream().map(User::getUserId).collect(Collectors.toList());
        if (userIds.isEmpty())
        {
            return new ArrayList<>();
        }
        List<Long> postIds = userPostMapper.selectList(Wrappers.lambdaQuery(UserPost.class)
                .select(UserPost::getPostId)
                .in(UserPost::getUserId, userIds))
                .stream().map(UserPost::getPostId).collect(Collectors.toList());
        if (postIds.isEmpty())
        {
            return new ArrayList<>();
        }
        return PageUtils.convert(postMapper.selectList(Wrappers.lambdaQuery(Post.class)
                .select(Post::getPostId, Post::getPostName, Post::getPostCode)
                .in(Post::getPostId, postIds)),
                po -> converter.convert(po, PostBo.class));
    }

    /**
     * 新增用户信息（含角色/岗位关联）
     *
     * @param user 用户信息
     * @return 结果
     */
    public int insertUser(UserBo user)
    {
        User po = converter.convert(user, User.class);
        po.setCreateTime(DateUtils.getNowDate());
        int rows = userMapper.insert(po);
        user.setUserId(po.getUserId());
        insertUserPosts(po.getUserId(), user.getPostIds());
        insertUserRoles(po.getUserId(), user.getRoleIds());
        return rows;
    }

    /**
     * 新增用户基本信息（仅主表）
     *
     * @param user 用户信息
     * @return 结果
     */
    public int insertUserBasic(UserBo user)
    {
        User po = converter.convert(user, User.class);
        po.setCreateTime(DateUtils.getNowDate());
        int rows = userMapper.insert(po);
        user.setUserId(po.getUserId());
        return rows;
    }

    /**
     * 修改用户信息（仅主表）
     *
     * @param user 用户信息
     * @return 结果
     */
    public int updateUser(UserBo user)
    {
        return updateUserByWrapper(converter.convert(user, User.class));
    }

    /**
     * 修改用户信息（含角色/岗位关联，先删后插）
     *
     * @param user 用户信息
     * @return 结果
     */
    public int updateUserWithRelations(UserBo user)
    {
        Long userId = user.getUserId();
        // 删除用户与角色关联
        userRoleMapper.delete(Wrappers.lambdaQuery(UserRole.class).eq(UserRole::getUserId, userId));
        // 新增用户与角色管理
        insertUserRoles(userId, user.getRoleIds());
        // 删除用户与岗位关联
        userPostMapper.delete(Wrappers.lambdaQuery(UserPost.class).eq(UserPost::getUserId, userId));
        // 新增用户与岗位管理
        insertUserPosts(userId, user.getPostIds());
        return updateUserByWrapper(converter.convert(user, User.class));
    }

    /**
     * 修改用户信息（镜像原 XML updateUser 的动态 <if> 条件）
     *
     * <p>注意：原 XML 中 dept_id 的守卫是 {@code <if test="deptId != 0">}（无 null 判断），
     * OGNL 下 null != 0 为 true（已用 ognl 3.2.10 实测验证），即 deptId 为 null 时会显式置 NULL，此处逐字保留。
     *
     * @param po 用户PO
     * @return 结果
     */
    private int updateUserByWrapper(User po)
    {
        return userMapper.update(null, Wrappers.lambdaUpdate(User.class)
                .set(po.getDeptId() == null || po.getDeptId() != 0, User::getDeptId, po.getDeptId())
                .set(po.getNickName() != null && !po.getNickName().isEmpty(), User::getNickName, po.getNickName())
                .set(po.getEmail() != null, User::getEmail, po.getEmail())
                .set(po.getPhonenumber() != null, User::getPhonenumber, po.getPhonenumber())
                .set(po.getSex() != null && !po.getSex().isEmpty(), User::getSex, po.getSex())
                .set(po.getAvatar() != null && !po.getAvatar().isEmpty(), User::getAvatar, po.getAvatar())
                .set(po.getPassword() != null && !po.getPassword().isEmpty(), User::getPassword, po.getPassword())
                .set(po.getStatus() != null && !po.getStatus().isEmpty(), User::getStatus, po.getStatus())
                .set(po.getLoginIp() != null && !po.getLoginIp().isEmpty(), User::getLoginIp, po.getLoginIp())
                .set(po.getLoginDate() != null, User::getLoginDate, po.getLoginDate())
                .set(po.getUpdateBy() != null && !po.getUpdateBy().isEmpty(), User::getUpdateBy, po.getUpdateBy())
                .set(po.getRemark() != null, User::getRemark, po.getRemark())
                .set(User::getUpdateTime, DateUtils.getNowDate())
                .eq(User::getUserId, po.getUserId()));
    }

    /**
     * 替换用户角色关联（先删后插）
     *
     * @param userId 用户ID
     * @param roleIds 角色组
     */
    public void replaceUserRoles(Long userId, Long[] roleIds)
    {
        userRoleMapper.delete(Wrappers.lambdaQuery(UserRole.class).eq(UserRole::getUserId, userId));
        insertUserRoles(userId, roleIds);
    }

    /**
     * 修改用户状态
     *
     * @param userId 用户ID
     * @param status 状态
     * @return 结果
     */
    public int updateUserStatus(Long userId, String status)
    {
        return userMapper.update(null, Wrappers.lambdaUpdate(User.class)
                .set(User::getStatus, status)
                .set(User::getUpdateTime, DateUtils.getNowDate())
                .eq(User::getUserId, userId));
    }

    /**
     * 修改用户头像
     *
     * @param userId 用户ID
     * @param avatar 头像地址
     * @return 结果
     */
    public int updateUserAvatar(Long userId, String avatar)
    {
        return userMapper.update(null, Wrappers.lambdaUpdate(User.class)
                .set(User::getAvatar, avatar)
                .set(User::getUpdateTime, DateUtils.getNowDate())
                .eq(User::getUserId, userId));
    }

    /**
     * 更新用户登录信息（IP和登录时间）
     *
     * @param userId 用户ID
     * @param loginIp 登录IP地址
     * @param loginDate 登录时间
     * @return 结果
     */
    public int updateLoginInfo(Long userId, String loginIp, Date loginDate)
    {
        return userMapper.update(null, Wrappers.lambdaUpdate(User.class)
                .set(User::getLoginIp, loginIp)
                .set(User::getLoginDate, loginDate)
                .eq(User::getUserId, userId));
    }

    /**
     * 重置用户密码
     *
     * @param userId 用户ID
     * @param password 密码
     * @return 结果
     */
    public int resetUserPwd(Long userId, String password)
    {
        return userMapper.update(null, Wrappers.lambdaUpdate(User.class)
                .set(User::getPwdUpdateDate, DateUtils.getNowDate())
                .set(User::getPassword, password)
                .set(User::getUpdateTime, DateUtils.getNowDate())
                .eq(User::getUserId, userId));
    }

    /**
     * 通过用户ID删除用户（含角色/岗位关联）
     *
     * @param userId 用户ID
     * @return 结果
     */
    public int deleteUserById(Long userId)
    {
        // 删除用户与角色关联
        userRoleMapper.delete(Wrappers.lambdaQuery(UserRole.class).eq(UserRole::getUserId, userId));
        // 删除用户与岗位表
        userPostMapper.delete(Wrappers.lambdaQuery(UserPost.class).eq(UserPost::getUserId, userId));
        // 软删除用户
        return userMapper.update(null, Wrappers.lambdaUpdate(User.class)
                .set(User::getDelFlag, "2")
                .eq(User::getUserId, userId));
    }

    /**
     * 批量删除用户信息（含角色/岗位关联）
     *
     * @param userIds 需要删除的用户ID
     * @return 结果
     */
    public int deleteUserByIds(Long[] userIds)
    {
        // 删除用户与角色关联
        userRoleMapper.delete(Wrappers.lambdaQuery(UserRole.class).in(UserRole::getUserId, Arrays.asList(userIds)));
        // 删除用户与岗位关联
        userPostMapper.delete(Wrappers.lambdaQuery(UserPost.class).in(UserPost::getUserId, Arrays.asList(userIds)));
        // 软删除用户
        return userMapper.update(null, Wrappers.lambdaUpdate(User.class)
                .set(User::getDelFlag, "2")
                .in(User::getUserId, Arrays.asList(userIds)));
    }

    /**
     * 新增用户角色关联
     *
     * @param userId 用户ID
     * @param roleIds 角色组
     */
    private void insertUserRoles(Long userId, Long[] roleIds)
    {
        if (roleIds != null && roleIds.length > 0)
        {
            List<UserRole> list = new ArrayList<>(roleIds.length);
            for (Long roleId : roleIds)
            {
                UserRole ur = new UserRole();
                ur.setUserId(userId);
                ur.setRoleId(roleId);
                list.add(ur);
            }
            Db.saveBatch(list);
        }
    }

    /**
     * 新增用户岗位关联
     *
     * @param userId 用户ID
     * @param postIds 岗位组
     */
    private void insertUserPosts(Long userId, Long[] postIds)
    {
        if (postIds != null && postIds.length > 0)
        {
            List<UserPost> list = new ArrayList<>(postIds.length);
            for (Long postId : postIds)
            {
                UserPost up = new UserPost();
                up.setUserId(userId);
                up.setPostId(postId);
                list.add(up);
            }
            Db.saveBatch(list);
        }
    }

    /**
     * 装配用户关联对象（复刻原 selectUserVo join 的 resultMap 语义）
     *
     * <p>部门：仅当用户有 deptId 时按 dept_id 查询（仅 selectUserVo 关联的 7 列），
     * 部门行不存在时 dept 保持 null；角色：按 sys_user_role 关联行装配（原 left join
     * 不含 r.del_flag 过滤），仅查询关联的 6 列，孤儿关联行随 in 条件丢弃，
     * 无关联行时 roles 保持 null（原 collection 全列 null 不装配）。</p>
     *
     * @param po 用户PO
     * @return 装配后的用户PO
     */
    private User fillUserRelations(User po)
    {
        if (po == null)
        {
            return null;
        }
        if (po.getDeptId() != null)
        {
            po.setDept(deptMapper.selectOne(Wrappers.lambdaQuery(Dept.class)
                    .select(Dept::getDeptId, Dept::getParentId, Dept::getAncestors, Dept::getDeptName,
                            Dept::getOrderNum, Dept::getLeader, Dept::getStatus)
                    .eq(Dept::getDeptId, po.getDeptId())));
        }
        List<Long> roleIds = userRoleMapper.selectList(Wrappers.lambdaQuery(UserRole.class)
                .select(UserRole::getRoleId)
                .eq(UserRole::getUserId, po.getUserId()))
                .stream().map(UserRole::getRoleId).collect(Collectors.toList());
        if (!roleIds.isEmpty())
        {
            po.setRoles(roleMapper.selectList(Wrappers.lambdaQuery(Role.class)
                    .select(Role::getRoleId, Role::getRoleName, Role::getRoleKey, Role::getRoleSort,
                            Role::getDataScope, Role::getStatus)
                    .in(Role::getRoleId, roleIds)));
        }
        return po;
    }

    /**
     * 查询用户角色ID（复刻原 selectRoleListByUserId join 语义：关联行需在 sys_role 中存在，无 del_flag 过滤）
     *
     * @param userId 用户ID
     * @return 角色ID列表
     */
    private List<Long> selectRoleIdsByUserId(Long userId)
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
     * 查询用户岗位ID（复刻原 selectPostListByUserId join 语义：关联行需在 sys_post 中存在）
     *
     * @param userId 用户ID
     * @return 岗位ID列表
     */
    private List<Long> selectPostIdsByUserId(Long userId)
    {
        List<Long> postIds = userPostMapper.selectList(Wrappers.lambdaQuery(UserPost.class)
                .select(UserPost::getPostId)
                .eq(UserPost::getUserId, userId))
                .stream().map(UserPost::getPostId).collect(Collectors.toList());
        if (postIds.isEmpty())
        {
            return postIds;
        }
        return postMapper.selectList(Wrappers.lambdaQuery(Post.class)
                .select(Post::getPostId)
                .in(Post::getPostId, postIds))
                .stream().map(Post::getPostId).collect(Collectors.toList());
    }

    /**
     * 应用用户列表动态条件（复刻原 XML 的 del_flag 与 query 条件）
     *
     * @param wrapper 用户查询壳
     * @param query   用户查询条件
     * @param full    true 为 selectUserList 的全量条件；false 为已分配/未分配列表的 userName/phonenumber 条件
     */
    private void applyUserFilters(LambdaQueryWrapper<User> wrapper, UserBo query, boolean full)
    {
        wrapper.eq(User::getDelFlag, "0");
        if (full && query.getUserId() != null && query.getUserId() != 0)
        {
            wrapper.eq(User::getUserId, query.getUserId());
        }
        if (query.getUserName() != null && !query.getUserName().isEmpty())
        {
            wrapper.like(User::getUserName, query.getUserName());
        }
        if (full && query.getStatus() != null && !query.getStatus().isEmpty())
        {
            wrapper.eq(User::getStatus, query.getStatus());
        }
        if (query.getPhonenumber() != null && !query.getPhonenumber().isEmpty())
        {
            wrapper.like(User::getPhonenumber, query.getPhonenumber());
        }
        if (full)
        {
            // 原 XML 使用 date_format(u.create_time,'%Y%m%d') 与 date_format(#{...},'%Y%m%d') 比较；
            // {0} 由 MP 预编译参数绑定，非字符串拼接
            Object beginTime = query.getParams().get("beginTime");
            if (beginTime != null && !"".equals(beginTime))
            {
                wrapper.apply("cast(create_time as date) >= cast({0} as date)", beginTime);
            }
            Object endTime = query.getParams().get("endTime");
            if (endTime != null && !"".equals(endTime))
            {
                wrapper.apply("cast(create_time as date) <= cast({0} as date)", endTime);
            }
            if (query.getDeptId() != null && query.getDeptId() != 0)
            {
                // 原条件：(u.dept_id = #{q.deptId} OR u.dept_id IN (select t.dept_id from sys_dept t where find_in_set(#{q.deptId}, ancestors)))
                // 子查询仅拼接 Long 主键值，无请求字符串
                Long deptId = query.getDeptId();
                wrapper.and(w -> w.eq(User::getDeptId, deptId)
                        .or().inSql(User::getDeptId,
                                "select t.dept_id from sys_dept t where concat(',', ancestors, ',') like concat('%,', " + deptId + ", ',%')"));
            }
        }
        // 数据范围过滤：由 DataScopeAspect 服务端生成（进入前已把用户可提交的 params.dataScope 清空），
        // 片段内只含服务端拼接的别名/角色 ID/当前用户部门 ID，绝不在本类拼接请求字符串。
        // 片段固定以 " AND (" 开头（Aspect 的生成格式），wrapper 自身会用 AND 连接条件，
        // 因此仅去掉这个由 Aspect 固定添加的前缀，避免生成 "AND AND"。
        String dataScope = normalizeDataScope(query.getParams().get("dataScope"));
        if (!dataScope.isEmpty())
        {
            wrapper.apply(dataScope);
        }
    }

    /**
     * 去除 DataScopeAspect 片段固定携带的前导 " AND "（服务端生成，非请求输入）
     */
    private static String normalizeDataScope(Object dataScope)
    {
        if (dataScope == null)
        {
            return "";
        }
        String fragment = dataScope.toString().trim();
        if (fragment.regionMatches(true, 0, "AND ", 0, 4))
        {
            fragment = fragment.substring(4).trim();
        }
        return fragment;
    }

    /**
     * 批量装配用户部门关系（复刻原 join sys_dept 的 resultMap 语义）
     *
     * <p>原 selectUserList 投影 d.dept_name/d.leader，故按 dept_id 批量补 deptId+deptName+leader；
     * 原 selectAllocatedList/selectUnallocatedList 只投影 u.dept_id，未投影任何 d 列，
     * MyBatis resultMap 仍会因 dept_id 非空创建仅含 deptId 的 Dept 关联，故此处同样只补 deptId，
     * 不额外查询、也不多填 deptName/leader。</p>
     *
     * @param users     用户列表
     * @param withNames 是否补 deptName/leader
     * @return 原列表（就地装配）
     */
    private List<User> fillUserDepts(List<User> users, boolean withNames)
    {
        if (users == null || users.isEmpty())
        {
            return users;
        }
        if (!withNames)
        {
            for (User user : users)
            {
                if (user.getDeptId() != null)
                {
                    Dept dept = new Dept();
                    dept.setDeptId(user.getDeptId());
                    user.setDept(dept);
                }
            }
            return users;
        }
        List<Long> deptIds = users.stream().map(User::getDeptId)
                .filter(Objects::nonNull).distinct().collect(Collectors.toList());
        Map<Long, Dept> deptMap = new HashMap<>();
        if (!deptIds.isEmpty())
        {
            deptMapper.selectList(Wrappers.lambdaQuery(Dept.class)
                    .select(Dept::getDeptId, Dept::getDeptName, Dept::getLeader)
                    .in(Dept::getDeptId, deptIds))
                    .forEach(dept -> deptMap.put(dept.getDeptId(), dept));
        }
        for (User user : users)
        {
            if (user.getDeptId() == null)
            {
                continue;
            }
            Dept dept = deptMap.get(user.getDeptId());
            if (dept == null)
            {
                // 原 left join 未命中时 d 列为空，但 dept_id 仍来自 u.dept_id，关联对象保留 deptId
                dept = new Dept();
                dept.setDeptId(user.getDeptId());
            }
            user.setDept(dept);
        }
        return users;
    }

    /**
     * 构造空的分页结果（保留 current/size/orders）
     */
    private IPage<UserBo> emptyUserPage(IPage<UserBo> page)
    {
        if (page == null)
        {
            return convertAll(new ArrayList<>());
        }
        Page<User> poPage = new Page<>(page.getCurrent(), page.getSize(), 0);
        poPage.setOrders(page.orders());
        return PageUtils.convert(poPage, po -> converter.convert(po, UserBo.class));
    }

    /**
     * 将不分页的 PO 列表包装为分页 BO
     */
    private IPage<UserBo> convertAll(List<User> records)
    {
        Page<User> poPage = new Page<>(1, records.size(), records.size());
        poPage.setRecords(records);
        return PageUtils.convert(poPage, po -> converter.convert(po, UserBo.class));
    }
}
