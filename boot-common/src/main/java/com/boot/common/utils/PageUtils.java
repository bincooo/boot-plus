package com.boot.common.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.boot.common.core.page.PageDomain;
import com.boot.common.core.page.TableSupport;
import com.boot.common.utils.sql.SqlUtil;

/**
 * 分页工具类
 *
 * @author boot-plus
 */
public class PageUtils
{
    /**
     * 设置请求分页数据
     */
    public static <T> Page<T> startPage()
    {
        PageDomain pageDomain = TableSupport.buildPageRequest();
        Integer pageNum = pageDomain.getPageNum();
        Integer pageSize = pageDomain.getPageSize();
        Page<T> page = new Page<>(pageNum, pageSize);
        String orderByColumn = pageDomain.getOrderByColumn();
        if (StringUtils.isNotEmpty(orderByColumn))
        {
            SqlUtil.escapeOrderBySql(pageDomain.getOrderBy());
            String column = StringUtils.toUnderScoreCase(orderByColumn);
            boolean asc = !"desc".equalsIgnoreCase(pageDomain.getIsAsc());
            page.addOrder(asc ? OrderItem.asc(column) : OrderItem.desc(column));
        }
        return page;
    }

    /**
     * 过滤分页排序字段，只保留白名单内的列
     *
     * <p>单表 wrapper 无法再解析原 join SQL 中的关联表列（如 deptName/leader），
     * 客户端传入这些列会导致 unknown column 错误。这里按 snake_case、大小写不敏感
     * 比较，仅丢弃未知列；若全部被丢弃，查询将不带排序（等价于未请求排序）。</p>
     *
     * @param page           分页对象，为 null 时不处理
     * @param allowedColumns 允许排序的基表列（snake_case）
     */
    public static void sanitizeOrders(IPage<?> page, Collection<String> allowedColumns)
    {
        if (page == null || allowedColumns == null || allowedColumns.isEmpty())
        {
            return;
        }
        Set<String> allowed = allowedColumns.stream()
                .filter(Objects::nonNull)
                .map(PageUtils::normalizeOrderColumn)
                .collect(Collectors.toSet());
        List<OrderItem> orders = page.orders();
        if (orders == null || orders.isEmpty())
        {
            return;
        }
        orders.removeIf(item -> item == null || item.getColumn() == null
                || !allowed.contains(normalizeOrderColumn(item.getColumn())));
    }

    /**
     * 排序字段统一为 snake_case 小写（兼容 camelCase 与大小写差异）
     */
    private static String normalizeOrderColumn(String column)
    {
        String snake = StringUtils.toUnderScoreCase(column.trim());
        return snake == null ? "" : snake.toLowerCase(Locale.ROOT);
    }

    /**
     * 转换分页对象，保留分页元数据
     *
     * @param source 源分页对象
     * @param mapper 记录转换函数
     * @return 转换后的分页对象
     */
    public static <S, T> IPage<T> convert(IPage<S> source, Function<S, T> mapper)
    {
        Page<T> target = new Page<>(source.getCurrent(), source.getSize(), source.getTotal());
        // 防御性拷贝：排序与记录列表独立于源对象，避免调用方修改目标页时影响源分页
        target.setOrders(new ArrayList<>(source.orders()));
        target.setRecords(source.getRecords().stream().map(mapper).collect(Collectors.toCollection(ArrayList::new)));
        return target;
    }

    /**
     * 转换列表
     *
     * @param source 源列表
     * @param mapper 记录转换函数
     * @return 转换后的列表
     */
    public static <S, T> List<T> convert(List<S> source, Function<S, T> mapper)
    {
        return source.stream().map(mapper).toList();
    }
}
