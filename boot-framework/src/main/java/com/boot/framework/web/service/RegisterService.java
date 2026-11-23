package com.boot.framework.web.service;

import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;
import com.boot.common.constant.CacheConstants;
import com.boot.common.constant.Constants;
import com.boot.common.constant.UserConstants;
import com.boot.common.core.cache.CacheProvider;
import com.boot.common.core.domain.bo.UserBo;
import com.boot.common.core.domain.model.RegisterBody;
import com.boot.common.exception.user.CaptchaException;
import com.boot.common.exception.user.CaptchaExpireException;
import com.boot.common.utils.DateUtils;
import com.boot.common.utils.MessageUtils;
import com.boot.common.utils.SecurityUtils;
import com.boot.common.utils.StringUtils;
import com.boot.framework.manager.AsyncManager;
import com.boot.framework.manager.factory.AsyncFactory;
import com.boot.system.service.IConfigService;
import com.boot.system.service.IUserService;

/**
 * 注册校验方法
 * 
 * @author boot-plus
 */
@Component
@RequiredArgsConstructor
public class RegisterService
{
    private final IUserService userService;

    private final IConfigService configService;

    private final CacheProvider cacheProvider;

    /**
     * 注册
     */
    public String register(RegisterBody registerBody)
    {
        String msg = "", username = registerBody.getUsername(), password = registerBody.getPassword();
        UserBo sysUser = new UserBo();
        sysUser.setUserName(username);

        // 验证码开关
        boolean captchaEnabled = configService.selectCaptchaEnabled();
        if (captchaEnabled)
        {
            validateCaptcha(username, registerBody.getCode(), registerBody.getUuid());
        }

        if (StringUtils.isEmpty(username))
        {
            msg = "用户名不能为空";
        }
        else if (StringUtils.isEmpty(password))
        {
            msg = "用户密码不能为空";
        }
        else if (username.length() < UserConstants.USERNAME_MIN_LENGTH
                || username.length() > UserConstants.USERNAME_MAX_LENGTH)
        {
            msg = "账户长度必须在2到20个字符之间";
        }
        else if (password.length() < UserConstants.PASSWORD_MIN_LENGTH
                || password.length() > UserConstants.PASSWORD_MAX_LENGTH)
        {
            msg = "密码长度必须在5到20个字符之间";
        }
        else if (!userService.checkUserNameUnique(sysUser))
        {
            msg = "保存用户'" + username + "'失败，注册账号已存在";
        }
        else
        {
            sysUser.setNickName(username);
            sysUser.setPwdUpdateDate(DateUtils.getNowDate());
            sysUser.setPassword(SecurityUtils.encryptPassword(password));
            boolean regFlag = userService.registerUser(sysUser);
            if (!regFlag)
            {
                msg = "注册失败,请联系系统管理人员";
            }
            else
            {
                AsyncManager.me().execute(AsyncFactory.recordLogininfor(username, Constants.REGISTER, MessageUtils.message("user.register.success")));
            }
        }
        return msg;
    }

    /**
     * 校验验证码
     * 
     * @param username 用户名
     * @param code 验证码
     * @param uuid 唯一标识
     * @return 结果
     */
    public void validateCaptcha(String username, String code, String uuid)
    {
        String verifyKey = StringUtils.nvl(uuid, "");
        String captcha = cacheProvider.get(CacheConstants.CAPTCHA_CODE_CACHE, verifyKey, String.class);
        cacheProvider.evict(CacheConstants.CAPTCHA_CODE_CACHE, verifyKey);
        if (captcha == null)
        {
            throw new CaptchaExpireException();
        }
        if (!code.equalsIgnoreCase(captcha))
        {
            throw new CaptchaException();
        }
    }
}
