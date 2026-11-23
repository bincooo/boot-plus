package com.boot.web.controller.monitor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import lombok.RequiredArgsConstructor;
import com.boot.common.annotation.Log;
import com.boot.common.constant.CacheConstants;
import com.boot.common.core.controller.BaseController;
import com.boot.common.core.domain.AjaxResult;
import com.boot.common.core.cache.CacheProvider;
import com.boot.common.core.domain.model.LoginUser;
import com.boot.common.core.page.TableDataInfo;
import com.boot.common.enums.BusinessType;
import com.boot.common.utils.StringUtils;
import com.boot.system.domain.vo.UserOnlineVo;
import com.boot.system.service.IUserOnlineService;

/**
 * 在线用户监控
 * 
 * @author boot-plus
 */
@RestController
@RequestMapping("/monitor/online")
@RequiredArgsConstructor
public class UserOnlineController extends BaseController
{
    private final IUserOnlineService userOnlineService;

    private final CacheProvider cacheProvider;

    @PreAuthorize("@ss.hasPermi('monitor:online:list')")
    @GetMapping("/list")
    public TableDataInfo list(String ipaddr, String userName)
    {
        Collection<String> keys = cacheProvider.keys(CacheConstants.LOGIN_TOKEN_CACHE);
        List<UserOnlineVo> userOnlineList = new ArrayList<UserOnlineVo>();
        for (String key : keys)
        {
            LoginUser user = cacheProvider.get(CacheConstants.LOGIN_TOKEN_CACHE, key, LoginUser.class);
            if (StringUtils.isNotEmpty(ipaddr) && StringUtils.isNotEmpty(userName))
            {
                userOnlineList.add(userOnlineService.selectOnlineByInfo(ipaddr, userName, user));
            }
            else if (StringUtils.isNotEmpty(ipaddr))
            {
                userOnlineList.add(userOnlineService.selectOnlineByIpaddr(ipaddr, user));
            }
            else if (StringUtils.isNotEmpty(userName) && StringUtils.isNotNull(user.getUser()))
            {
                userOnlineList.add(userOnlineService.selectOnlineByUserName(userName, user));
            }
            else
            {
                userOnlineList.add(userOnlineService.loginUserToUserOnline(user));
            }
        }
        Collections.reverse(userOnlineList);
        userOnlineList.removeAll(Collections.singleton(null));
        return getDataTable(userOnlineList);
    }

    /**
     * 强退用户
     */
    @PreAuthorize("@ss.hasPermi('monitor:online:forceLogout')")
    @Log(title = "在线用户", businessType = BusinessType.FORCE)
    @DeleteMapping("/{tokenId}")
    public AjaxResult forceLogout(@PathVariable String tokenId)
    {
        cacheProvider.evict(CacheConstants.LOGIN_TOKEN_CACHE, tokenId);
        return success();
    }
}
