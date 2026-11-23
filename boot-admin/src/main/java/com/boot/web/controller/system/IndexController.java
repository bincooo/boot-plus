package com.boot.web.controller.system;

import java.util.Map;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import lombok.RequiredArgsConstructor;
import com.boot.common.config.BootConfig;
import com.boot.common.core.domain.AjaxResult;
import com.boot.common.core.domain.bo.UserBo;
import com.boot.common.utils.SecurityUtils;
import com.boot.common.utils.StringUtils;
import com.boot.system.service.IUserService;

/**
 * 首页
 *
 * @author boot-plus
 */
@RestController
@RequiredArgsConstructor
public class IndexController
{
    /** 系统基础配置 */
    private final BootConfig bootConfig;

    private final IUserService userService;

    /**
     * 访问首页，提示语
     */
    @RequestMapping("/")
    public String index()
    {
        return StringUtils.format("欢迎使用{}后台管理框架，当前版本：v{}，请通过前端地址访问。", bootConfig.getName(), bootConfig.getVersion());
    }

    /**
     * 解锁屏幕
     */
    @PostMapping("/unlockscreen")
    public AjaxResult unlockScreen(@RequestBody Map<String, String> body)
    {
        String password = body.get("password");
        if (StringUtils.isEmpty(password))
        {
            return AjaxResult.error("密码不能为空");
        }
        String username = SecurityUtils.getUsername();
        UserBo user = userService.selectUserByUserName(username);
        if (user == null)
        {
            return AjaxResult.error("服务器超时，请重新登录");
        }
        if (!SecurityUtils.matchesPassword(password, user.getPassword()))
        {
            return AjaxResult.error("密码错误，请重新输入");
        }

        return AjaxResult.success("解锁成功");
    }
}
