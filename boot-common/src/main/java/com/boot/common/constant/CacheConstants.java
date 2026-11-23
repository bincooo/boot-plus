package com.boot.common.constant;

/**
 * 缓存区名称常量（caffeine/redis 由 spring.cache.type 切换）
 *
 * @author boot-plus
 */
public class CacheConstants
{
    /**
     * 参数配置 cache 名称
     */
    public static final String SYS_CONFIG_CACHE = "sys_config";

    /**
     * 数据字典 cache 名称
     */
    public static final String SYS_DICT_CACHE = "sys_dict";

    /**
     * 登录用户 cache 名称
     */
    public static final String LOGIN_TOKEN_CACHE = "login_tokens";

    /**
     * 验证码 cache 名称
     */
    public static final String CAPTCHA_CODE_CACHE = "captcha_codes";

    /**
     * 防重提交 cache 名称
     */
    public static final String REPEAT_SUBMIT_CACHE = "repeat_submit";

    /**
     * 限流 cache 名称
     */
    public static final String RATE_LIMIT_CACHE = "rate_limit";

    /**
     * 登录账户密码错误次数 cache 名称
     */
    public static final String PWD_ERR_CNT_CACHE = "pwd_err_cnt";
}
