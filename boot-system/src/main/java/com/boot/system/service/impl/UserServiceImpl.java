package com.boot.system.service.impl;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import jakarta.validation.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import lombok.RequiredArgsConstructor;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.boot.common.annotation.DataScope;
import com.boot.common.constant.UserConstants;
import com.boot.common.core.domain.bo.RoleBo;
import com.boot.common.core.domain.bo.UserBo;
import com.boot.common.exception.ServiceException;
import com.boot.common.utils.SecurityUtils;
import com.boot.common.utils.StringUtils;
import com.boot.common.utils.bean.BeanValidators;
import com.boot.common.utils.spring.SpringUtils;
import com.boot.system.repo.UserRepository;
import com.boot.system.service.IConfigService;
import com.boot.system.service.IDeptService;
import com.boot.system.service.IUserService;
import com.boot.system.domain.bo.PostBo;

/**
 * 用户 业务层处理
 *
 * @author boot-plus
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements IUserService
{
    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);

    private final UserRepository userRepository;

    private final IConfigService configService;

    private final IDeptService deptService;

    private final Validator validator;

    /**
     * 根据条件分页查询用户列表
     *
     * @param page 分页参数
     * @param user 用户信息
     * @return 用户信息集合信息
     */
    @Override
    @DataScope(deptAlias = "sys_user", deptField = "dept_id", userAlias = "sys_user", userField = "user_id")
    public IPage<UserBo> selectUserList(IPage<UserBo> page, UserBo user)
    {
        return userRepository.selectUserList(page, user);
    }

    /**
     * 根据条件分页查询已分配用户角色列表
     *
     * @param page 分页参数
     * @param user 用户信息
     * @return 用户信息集合信息
     */
    @Override
    @DataScope(deptAlias = "sys_user", deptField = "dept_id", userAlias = "sys_user", userField = "user_id")
    public IPage<UserBo> selectAllocatedList(IPage<UserBo> page, UserBo user)
    {
        return userRepository.selectAllocatedList(page, user);
    }

    /**
     * 根据条件分页查询未分配用户角色列表
     *
     * @param page 分页参数
     * @param user 用户信息
     * @return 用户信息集合信息
     */
    @Override
    @DataScope(deptAlias = "sys_user", deptField = "dept_id", userAlias = "sys_user", userField = "user_id")
    public IPage<UserBo> selectUnallocatedList(IPage<UserBo> page, UserBo user)
    {
        return userRepository.selectUnallocatedList(page, user);
    }

    /**
     * 通过用户名查询用户
     *
     * @param userName 用户名
     * @return 用户对象信息
     */
    @Override
    public UserBo selectUserByUserName(String userName)
    {
        return userRepository.selectUserByUserName(userName);
    }

    /**
     * 通过用户ID查询用户详情
     *
     * @param userId 用户ID
     * @return 用户对象信息
     */
    @Override
    public UserBo selectUserDetailById(Long userId)
    {
        return userRepository.selectUserDetailById(userId);
    }

    /**
     * 查询用户所属角色组
     *
     * @param userName 用户名
     * @return 结果
     */
    @Override
    public String selectUserRoleGroup(String userName)
    {
        List<RoleBo> list = userRepository.selectRolesByUserName(userName);
        if (CollectionUtils.isEmpty(list))
        {
            return StringUtils.EMPTY;
        }
        return list.stream().map(RoleBo::getRoleName).collect(Collectors.joining(","));
    }

    /**
     * 查询用户所属岗位组
     *
     * @param userName 用户名
     * @return 结果
     */
    @Override
    public String selectUserPostGroup(String userName)
    {
        List<PostBo> list = userRepository.selectPostsByUserName(userName);
        if (CollectionUtils.isEmpty(list))
        {
            return StringUtils.EMPTY;
        }
        return list.stream().map(PostBo::getPostName).collect(Collectors.joining(","));
    }

    /**
     * 校验用户名称是否唯一
     *
     * @param user 用户信息
     * @return 结果
     */
    @Override
    public boolean checkUserNameUnique(UserBo user)
    {
        Long userId = StringUtils.isNull(user.getUserId()) ? -1L : user.getUserId();
        Long info = userRepository.selectUserIdByUserName(user.getUserName());
        if (StringUtils.isNotNull(info) && info.longValue() != userId.longValue())
        {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    /**
     * 校验手机号码是否唯一
     *
     * @param user 用户信息
     * @return
     */
    @Override
    public boolean checkPhoneUnique(UserBo user)
    {
        Long userId = StringUtils.isNull(user.getUserId()) ? -1L : user.getUserId();
        Long info = userRepository.selectUserIdByPhone(user.getPhonenumber());
        if (StringUtils.isNotNull(info) && info.longValue() != userId.longValue())
        {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    /**
     * 校验email是否唯一
     *
     * @param user 用户信息
     * @return
     */
    @Override
    public boolean checkEmailUnique(UserBo user)
    {
        Long userId = StringUtils.isNull(user.getUserId()) ? -1L : user.getUserId();
        Long info = userRepository.selectUserIdByEmail(user.getEmail());
        if (StringUtils.isNotNull(info) && info.longValue() != userId.longValue())
        {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    /**
     * 校验用户是否允许操作
     *
     * @param user 用户信息
     */
    @Override
    public void checkUserAllowed(UserBo user)
    {
        if (StringUtils.isNotNull(user.getUserId()) && user.isAdmin())
        {
            throw new ServiceException("不允许操作超级管理员用户");
        }
    }

    /**
     * 校验用户是否有数据权限
     *
     * @param userId 用户id
     */
    @Override
    public void checkUserDataScope(Long userId)
    {
        if (!SecurityUtils.isAdmin())
        {
            UserBo user = new UserBo();
            user.setUserId(userId);
            IPage<UserBo> users = SpringUtils.getAopProxy(this).selectUserList(null, user);
            if (StringUtils.isEmpty(users.getRecords()))
            {
                throw new ServiceException("没有权限访问用户数据！");
            }
        }
    }

    /**
     * 新增保存用户信息
     *
     * @param user 用户信息
     * @return 结果
     */
    @Override
    @Transactional
    public int insertUser(UserBo user)
    {
        return userRepository.insertUser(user);
    }

    /**
     * 注册用户信息
     *
     * @param user 用户信息
     * @return 结果
     */
    @Override
    public boolean registerUser(UserBo user)
    {
        return userRepository.insertUserBasic(user) > 0;
    }

    /**
     * 修改保存用户信息
     *
     * @param user 用户信息
     * @return 结果
     */
    @Override
    @Transactional
    public int updateUser(UserBo user)
    {
        return userRepository.updateUserWithRelations(user);
    }

    /**
     * 用户授权角色
     *
     * @param userId 用户ID
     * @param roleIds 角色组
     */
    @Override
    @Transactional
    public void insertUserAuth(Long userId, Long[] roleIds)
    {
        userRepository.replaceUserRoles(userId, roleIds);
    }

    /**
     * 修改用户状态
     *
     * @param user 用户信息
     * @return 结果
     */
    @Override
    public int updateUserStatus(UserBo user)
    {
        return userRepository.updateUserStatus(user.getUserId(), user.getStatus());
    }

    /**
     * 修改用户基本信息
     *
     * @param user 用户信息
     * @return 结果
     */
    @Override
    public int updateUserProfile(UserBo user)
    {
        return userRepository.updateUser(user);
    }

    /**
     * 修改用户头像
     *
     * @param userId 用户ID
     * @param avatar 头像地址
     * @return 结果
     */
    @Override
    public boolean updateUserAvatar(Long userId, String avatar)
    {
        return userRepository.updateUserAvatar(userId, avatar) > 0;
    }

    /**
     * 更新用户登录信息（IP和登录时间）
     *
     * @param userId 用户ID
     * @param loginIp 登录IP地址
     * @param loginDate 登录时间
     * @return 结果
     */
    public void updateLoginInfo(Long userId, String loginIp, Date loginDate)
    {
        userRepository.updateLoginInfo(userId, loginIp, loginDate);
    }

    /**
     * 重置用户密码
     *
     * @param user 用户信息
     * @return 结果
     */
    @Override
    public int resetPwd(UserBo user)
    {
        return userRepository.resetUserPwd(user.getUserId(), user.getPassword());
    }

    /**
     * 重置用户密码
     *
     * @param userId 用户ID
     * @param password 密码
     * @return 结果
     */
    @Override
    public int resetUserPwd(Long userId, String password)
    {
        return userRepository.resetUserPwd(userId, password);
    }

    /**
     * 通过用户ID删除用户
     *
     * @param userId 用户ID
     * @return 结果
     */
    @Override
    @Transactional
    public int deleteUserById(Long userId)
    {
        return userRepository.deleteUserById(userId);
    }

    /**
     * 批量删除用户信息
     *
     * @param userIds 需要删除的用户ID
     * @return 结果
     */
    @Override
    @Transactional
    public int deleteUserByIds(Long[] userIds)
    {
        for (Long userId : userIds)
        {
            UserBo user = new UserBo();
            user.setUserId(userId);
            checkUserAllowed(user);
            checkUserDataScope(userId);
        }
        return userRepository.deleteUserByIds(userIds);
    }

    /**
     * 导入用户数据
     *
     * @param userList 用户数据列表
     * @param isUpdateSupport 是否更新支持，如果已存在，则进行更新数据
     * @param operName 操作用户
     * @return 结果
     */
    @Override
    public String importUser(List<UserBo> userList, Boolean isUpdateSupport, String operName)
    {
        if (StringUtils.isNull(userList) || userList.size() == 0)
        {
            throw new ServiceException("导入用户数据不能为空！");
        }
        int successNum = 0;
        int failureNum = 0;
        StringBuilder successMsg = new StringBuilder();
        StringBuilder failureMsg = new StringBuilder();
        for (UserBo user : userList)
        {
            try
            {
                // 验证是否存在这个用户
                UserBo u = userRepository.selectUserByUserName(user.getUserName());
                if (StringUtils.isNull(u))
                {
                    BeanValidators.validateWithException(validator, user);
                    deptService.checkDeptDataScope(user.getDeptId());
                    String password = configService.selectConfigByKey("sys.user.initPassword");
                    user.setPassword(SecurityUtils.encryptPassword(password));
                    user.setCreateBy(operName);
                    userRepository.insertUserBasic(user);
                    successNum++;
                    successMsg.append("<br/>" + successNum + "、账号 " + user.getUserName() + " 导入成功");
                }
                else if (isUpdateSupport)
                {
                    BeanValidators.validateWithException(validator, user);
                    checkUserAllowed(u);
                    checkUserDataScope(u.getUserId());
                    deptService.checkDeptDataScope(user.getDeptId());
                    user.setUserId(u.getUserId());
                    user.setDeptId(u.getDeptId());
                    user.setUpdateBy(operName);
                    userRepository.updateUser(user);
                    successNum++;
                    successMsg.append("<br/>" + successNum + "、账号 " + user.getUserName() + " 更新成功");
                }
                else
                {
                    failureNum++;
                    failureMsg.append("<br/>" + failureNum + "、账号 " + user.getUserName() + " 已存在");
                }
            }
            catch (Exception e)
            {
                failureNum++;
                String msg = "<br/>" + failureNum + "、账号 " + user.getUserName() + " 导入失败：";
                failureMsg.append(msg + e.getMessage());
                log.error(msg, e);
            }
        }
        if (failureNum > 0)
        {
            failureMsg.insert(0, "很抱歉，导入失败！共 " + failureNum + " 条数据格式不正确，错误如下：");
            throw new ServiceException(failureMsg.toString());
        }
        else
        {
            successMsg.insert(0, "恭喜您，数据已全部导入成功！共 " + successNum + " 条，数据如下：");
        }
        return successMsg.toString();
    }
}
