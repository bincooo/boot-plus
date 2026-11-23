package com.boot.web.controller.system;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import lombok.RequiredArgsConstructor;
import com.boot.common.core.controller.BaseController;
import com.boot.common.core.domain.AjaxResult;
import com.boot.common.core.domain.model.RegisterBody;
import com.boot.common.utils.StringUtils;
import com.boot.framework.web.service.RegisterService;
import com.boot.system.service.IConfigService;

/**
 * 注册验证
 * 
 * @author boot-plus
 */
@RestController
@RequiredArgsConstructor
public class RegisterController extends BaseController
{
    private final RegisterService registerService;

    private final IConfigService configService;

    @PostMapping("/register")
    public AjaxResult register(@RequestBody RegisterBody user)
    {
        if (!("true".equals(configService.selectConfigByKey("sys.account.registerUser"))))
        {
            return error("当前系统没有开启注册功能！");
        }
        String msg = registerService.register(user);
        return StringUtils.isEmpty(msg) ? success() : error(msg);
    }
}
