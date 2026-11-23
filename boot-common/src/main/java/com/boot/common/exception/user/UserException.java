package com.boot.common.exception.user;

import java.io.Serial;
import com.boot.common.exception.base.BaseException;

/**
 * 用户信息异常类
 * 
 * @author boot-plus
 */
public class UserException extends BaseException
{
    @Serial
    private static final long serialVersionUID = 1L;

    public UserException(String code, Object[] args)
    {
        super("user", code, args, null);
    }
}
