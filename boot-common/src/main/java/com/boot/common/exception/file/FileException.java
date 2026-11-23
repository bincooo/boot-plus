package com.boot.common.exception.file;

import java.io.Serial;
import com.boot.common.exception.base.BaseException;

/**
 * 文件信息异常类
 * 
 * @author boot-plus
 */
public class FileException extends BaseException
{
    @Serial
    private static final long serialVersionUID = 1L;

    public FileException(String code, Object[] args)
    {
        super("file", code, args, null);
    }

}
