package com.boot.framework.web.service;

import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;
import com.boot.common.constant.CacheConstants;
import com.boot.common.core.cache.CacheProvider;
import com.boot.common.core.domain.bo.UserBo;
import com.boot.common.exception.user.UserPasswordNotMatchException;
import com.boot.common.exception.user.UserPasswordRetryLimitExceedException;
import com.boot.common.utils.SecurityUtils;
import com.boot.framework.security.context.AuthenticationContextHolder;

/**
 * 登录密码方法
 * 
 * @author boot-plus
 */
@Component
@RequiredArgsConstructor
public class PasswordService
{
    private final CacheProvider cacheProvider;

    @Value(value = "${user.password.maxRetryCount}")
    private int maxRetryCount;

    @Value(value = "${user.password.lockTime}")
    private int lockTime;

    public void validate(UserBo user)
    {
        Authentication usernamePasswordAuthenticationToken = AuthenticationContextHolder.getContext();
        String username = usernamePasswordAuthenticationToken.getName();
        String password = usernamePasswordAuthenticationToken.getCredentials().toString();

        Integer retryCount = cacheProvider.get(CacheConstants.PWD_ERR_CNT_CACHE, username, Integer.class);

        if (retryCount == null)
        {
            retryCount = 0;
        }

        if (retryCount >= Integer.valueOf(maxRetryCount).intValue())
        {
            throw new UserPasswordRetryLimitExceedException(maxRetryCount, lockTime);
        }

        if (!matches(user, password))
        {
            retryCount = retryCount + 1;
            cacheProvider.put(CacheConstants.PWD_ERR_CNT_CACHE, username, retryCount, lockTime, TimeUnit.MINUTES);
            throw new UserPasswordNotMatchException();
        }
        else
        {
            clearLoginRecordCache(username);
        }
    }

    public boolean matches(UserBo user, String rawPassword)
    {
        return SecurityUtils.matchesPassword(rawPassword, user.getPassword());
    }

    public void clearLoginRecordCache(String loginName)
    {
        if (cacheProvider.hasKey(CacheConstants.PWD_ERR_CNT_CACHE, loginName))
        {
            cacheProvider.evict(CacheConstants.PWD_ERR_CNT_CACHE, loginName);
        }
    }
}
