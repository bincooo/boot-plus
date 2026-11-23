package com.boot.system.repo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.boot.common.utils.DateUtils;
import com.boot.common.utils.PageUtils;
import com.boot.system.domain.vo.NoticeReadUserVo;
import com.boot.system.repo.mapper.DeptMapper;
import com.boot.system.repo.mapper.NoticeMapper;
import com.boot.system.repo.mapper.NoticeReadMapper;
import com.boot.system.repo.mapper.UserMapper;
import com.boot.system.repo.po.Dept;
import com.boot.system.repo.po.NoticeReadUserRow;
import com.boot.system.repo.po.Notice;
import com.boot.system.repo.po.NoticeRead;
import com.boot.system.repo.po.User;
import com.boot.system.domain.bo.NoticeBo;
import com.boot.system.domain.bo.NoticeReadBo;
import io.github.linpeilie.Converter;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;
import lombok.RequiredArgsConstructor;

/**
 * 公告已读记录 聚合仓库
 *
 * @author boot-plus
 */
@Repository
@RequiredArgsConstructor
public class NoticeReadRepository
{
    private final NoticeReadMapper noticeReadMapper;

    private final NoticeMapper noticeMapper;

    private final UserMapper userMapper;

    private final DeptMapper deptMapper;

    private final Converter converter;

    /**
     * 新增已读记录（忽略重复）
     *
     * <p>原 XML 为 insert ignore；MP 无法表达 INSERT IGNORE，改为普通 insert 并捕获
     * 唯一键 uk_user_notice 冲突（DuplicateKeyException）等价忽略。比原 insert ignore
     * 略严格：其他错误不再被 IGNORE 吞掉而向上传播（见转换报告）。</p>
     *
     * @param noticeRead 已读记录
     * @return 结果
     */
    public int insertNoticeRead(NoticeReadBo noticeRead)
    {
        NoticeRead po = converter.convert(noticeRead, NoticeRead.class);
        // 原 XML read_time 取 sysdate()，统一改为应用时间（与第二波 insert 转换一致）
        po.setReadTime(DateUtils.getNowDate());
        return insertIgnoreDuplicate(po);
    }

    /**
     * 查询某用户未读公告数量
     *
     * <p>复刻原 selectUnreadCount 的 not exists 子查询：取该用户已读的 notice_id 组，
     * 统计 status='0' 且不在已读组内的公告数。read_id 组为空时不追加 notIn 条件；
     * null notice_id（DDL 中 not null，防御性过滤）在原 not exists 中永不匹配，过滤后语义一致。</p>
     *
     * @param userId 用户ID
     * @return 未读数量
     */
    public int selectUnreadCount(Long userId)
    {
        List<Long> readIds = noticeReadMapper.selectList(Wrappers.lambdaQuery(NoticeRead.class)
                .select(NoticeRead::getNoticeId)
                .eq(NoticeRead::getUserId, userId))
                .stream().map(NoticeRead::getNoticeId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        LambdaQueryWrapper<Notice> wrapper = Wrappers.lambdaQuery(Notice.class)
                .eq(Notice::getStatus, "0");
        if (!readIds.isEmpty())
        {
            wrapper.notIn(Notice::getNoticeId, readIds);
        }
        return noticeMapper.selectCount(wrapper).intValue();
    }

    /**
     * 查询某用户是否已读某公告
     *
     * @param noticeId 公告ID
     * @param userId   用户ID
     * @return 已读记录数（0未读 1已读）
     */
    public int selectIsRead(Long noticeId, Long userId)
    {
        Long count = noticeReadMapper.selectCount(Wrappers.lambdaQuery(NoticeRead.class)
                .eq(NoticeRead::getNoticeId, noticeId)
                .eq(NoticeRead::getUserId, userId));
        return count.intValue();
    }

    /**
     * 查询公告列表并标记当前用户已读状态
     *
     * @param userId 用户ID
     * @param limit  最多返回条数
     * @return 带 isRead 标记的公告列表
     */
    public List<NoticeBo> selectNoticeListWithReadStatus(Long userId, int limit)
    {
        // 原 SQL 只投影这些列（不含 notice_content），并按 notice_id desc 在 SQL 层 limit
        List<Notice> notices = noticeMapper.selectList(Wrappers.lambdaQuery(Notice.class)
                .select(Notice::getNoticeId, Notice::getNoticeTitle, Notice::getNoticeType, Notice::getStatus,
                        Notice::getCreateBy, Notice::getCreateTime)
                .eq(Notice::getStatus, "0")
                .orderByDesc(Notice::getNoticeId)
                // limit 为 int 参数，仅拼接数值常量
                .last("limit " + limit));
        if (!notices.isEmpty())
        {
            List<Long> noticeIds = notices.stream().map(Notice::getNoticeId).collect(Collectors.toList());
            // 原 left join sys_notice_read r on r.notice_id = n.notice_id and r.user_id = #{userId}：
            // 批量取该用户在这些公告上的已读记录
            Set<Long> readNoticeIds = noticeReadMapper.selectList(Wrappers.lambdaQuery(NoticeRead.class)
                    .select(NoticeRead::getNoticeId)
                    .eq(NoticeRead::getUserId, userId)
                    .in(NoticeRead::getNoticeId, noticeIds))
                    .stream().map(NoticeRead::getNoticeId).collect(Collectors.toSet());
            for (Notice notice : notices)
            {
                notice.setIsRead(readNoticeIds.contains(notice.getNoticeId()));
            }
        }
        return PageUtils.convert(notices, po -> converter.convert(po, NoticeBo.class));
    }

    /**
     * 批量标记已读
     *
     * <p>原 XML 为 foreach 多行 insert ignore；MP 无法表达 INSERT IGNORE，改为逐行
     * insert 并捕获唯一键冲突等价忽略（每行独立插入或忽略，与多行 insert ignore 的
     * 逐行语义一致；N 条语句，见转换报告）。调用方保证 noticeIds 非空。</p>
     *
     * @param userId    用户ID
     * @param noticeIds 公告ID数组
     * @return 结果
     */
    public int insertNoticeReadBatch(Long userId, Long[] noticeIds)
    {
        int rows = 0;
        for (Long noticeId : noticeIds)
        {
            NoticeRead po = new NoticeRead();
            po.setNoticeId(noticeId);
            po.setUserId(userId);
            // 原 XML read_time 取 sysdate()，统一改为应用时间（与第二波 insert 转换一致）
            po.setReadTime(DateUtils.getNowDate());
            rows += insertIgnoreDuplicate(po);
        }
        return rows;
    }

    /**
     * 等价于原 XML 的 insert ignore：唯一键 uk_user_notice 冲突时按未插入返回 0
     *
     * @param po 已读记录PO
     * @return 结果
     */
    private int insertIgnoreDuplicate(NoticeRead po)
    {
        try
        {
            return noticeReadMapper.insert(po);
        }
        catch (DuplicateKeyException e)
        {
            return 0;
        }
    }

    /**
     * 分页查询已阅读某公告的用户列表
     *
     * @param page 分页参数
     * @param noticeId 公告ID
     * @param searchValue 搜索值
     * @return 已读用户列表
     */
    public IPage<NoticeReadUserVo> selectReadUsersByNoticeId(IPage<NoticeReadUserVo> page, Long noticeId,
            String searchValue)
    {
        Objects.requireNonNull(page, "page must not be null; use the non-paginated overload");
        // 原 join 的排序字段（user_name/nick_name/dept_name）已不在单表 wrapper 上，静默丢弃非基表列
        PageUtils.sanitizeOrders(page, Set.of("read_time"));
        // 1. 复刻 inner join sys_user u ... and u.del_flag='0' + 可选 user_name/nick_name 模糊筛选。
        // 用户过滤下推为 SQL 子查询，在数据库内完成后再分页，避免把整张用户表物化成 IN 列表。
        LambdaQueryWrapper<NoticeRead> wrapper = Wrappers.lambdaQuery(NoticeRead.class)
                .eq(NoticeRead::getNoticeId, noticeId);
        if (searchValue != null && !searchValue.isEmpty())
        {
            // 服务端构造的子查询；{0} 由 MP 预编译参数绑定，非字符串拼接
            wrapper.apply("user_id in (select user_id from sys_user where del_flag='0' "
                    + "and (user_name like concat('%',{0},'%') or nick_name like concat('%',{0},'%')))",
                    searchValue);
        }
        else
        {
            wrapper.inSql(NoticeRead::getUserId, "select user_id from sys_user where del_flag='0'");
        }
        // 2. 对 sys_notice_read 分页，保留 IPage 的 current/size/orders 与总条数
        Page<NoticeRead> readPage = new Page<>(page.getCurrent(), page.getSize());
        readPage.setOrders(page.orders());
        noticeReadMapper.selectPage(readPage, wrapper.orderByDesc(NoticeRead::getReadTime));
        List<NoticeRead> reads = readPage.getRecords();
        // 3. 批量装配用户与部门信息，字段名与原 SQL 别名一致
        List<Long> readUserIds = reads.stream().map(NoticeRead::getUserId)
                .filter(Objects::nonNull).distinct().collect(Collectors.toList());
        Map<Long, User> userMap = new HashMap<>();
        if (!readUserIds.isEmpty())
        {
            userMapper.selectList(Wrappers.lambdaQuery(User.class)
                    .select(User::getUserId, User::getUserName, User::getNickName, User::getPhonenumber,
                            User::getDeptId)
                    .in(User::getUserId, readUserIds))
                    .forEach(user -> userMap.put(user.getUserId(), user));
        }
        List<Long> deptIds = userMap.values().stream().map(User::getDeptId)
                .filter(Objects::nonNull).distinct().collect(Collectors.toList());
        Map<Long, Dept> deptMap = new HashMap<>();
        if (!deptIds.isEmpty())
        {
            deptMapper.selectList(Wrappers.lambdaQuery(Dept.class)
                    .select(Dept::getDeptId, Dept::getDeptName)
                    .in(Dept::getDeptId, deptIds))
                    .forEach(dept -> deptMap.put(dept.getDeptId(), dept));
        }
        List<NoticeReadUserRow> rows = new ArrayList<>(reads.size());
        for (NoticeRead read : reads)
        {
            NoticeReadUserRow row = new NoticeReadUserRow();
            row.setUserId(read.getUserId());
            row.setReadTime(read.getReadTime());
            User user = userMap.get(read.getUserId());
            if (user != null)
            {
                row.setUserName(user.getUserName());
                row.setNickName(user.getNickName());
                row.setPhonenumber(user.getPhonenumber());
                if (user.getDeptId() != null)
                {
                    Dept dept = deptMap.get(user.getDeptId());
                    if (dept != null)
                    {
                        row.setDeptName(dept.getDeptName());
                    }
                }
            }
            rows.add(row);
        }
        Page<NoticeReadUserRow> rowPage = new Page<>(readPage.getCurrent(), readPage.getSize(), readPage.getTotal());
        rowPage.setOrders(new ArrayList<>(readPage.orders()));
        rowPage.setRecords(rows);
        return PageUtils.convert(rowPage, row -> converter.convert(row, NoticeReadUserVo.class));
    }

    /**
     * 公告删除时清理对应已读记录
     *
     * @param noticeIds 公告ID数组
     * @return 结果
     */
    public int deleteByNoticeIds(Long[] noticeIds)
    {
        return noticeReadMapper.delete(Wrappers.lambdaQuery(NoticeRead.class)
                .in(NoticeRead::getNoticeId, Arrays.asList(noticeIds)));
    }
}
