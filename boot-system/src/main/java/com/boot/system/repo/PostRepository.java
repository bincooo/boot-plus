package com.boot.system.repo;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.boot.common.utils.DateUtils;
import com.boot.common.utils.PageUtils;
import com.boot.system.repo.mapper.UserPostMapper;
import com.boot.system.repo.mapper.PostMapper;
import com.boot.system.repo.po.Post;
import com.boot.system.repo.po.UserPost;
import com.boot.system.domain.bo.PostBo;
import io.github.linpeilie.Converter;
import org.springframework.stereotype.Repository;
import lombok.RequiredArgsConstructor;

/**
 * 岗位信息 聚合仓库
 *
 * @author boot-plus
 */
@Repository
@RequiredArgsConstructor
public class PostRepository
{
    private final PostMapper postMapper;

    private final UserPostMapper userPostMapper;

    private final Converter converter;

    /**
     * 分页查询岗位信息集合
     *
     * @param page 分页参数
     * @param query 岗位查询条件
     * @return 岗位信息集合
     */
    public IPage<PostBo> selectPostList(IPage<PostBo> page, PostBo query)
    {
        Objects.requireNonNull(page, "page must not be null; use the non-paginated overload");
        Page<Post> poPage = new Page<>(page.getCurrent(), page.getSize());
        poPage.setOrders(page.orders());
        postMapper.selectPage(poPage, buildPostQuery(query));
        return PageUtils.convert(poPage, po -> converter.convert(po, PostBo.class));
    }

    /**
     * 查询岗位信息集合（非分页，用于导出）
     *
     * @param query 岗位查询条件
     * @return 岗位信息集合
     */
    public List<PostBo> selectPostList(PostBo query)
    {
        return PageUtils.convert(postMapper.selectList(buildPostQuery(query)),
                po -> converter.convert(po, PostBo.class));
    }

    /**
     * 查询所有岗位
     *
     * @return 岗位列表
     */
    public List<PostBo> selectPostAll()
    {
        return PageUtils.convert(postMapper.selectList(postColumnWrapper()),
                po -> converter.convert(po, PostBo.class));
    }

    /**
     * 通过岗位ID查询岗位信息
     *
     * @param postId 岗位ID
     * @return 岗位信息
     */
    public PostBo selectPostById(Long postId)
    {
        Post post = postMapper.selectOne(postColumnWrapper().eq(Post::getPostId, postId));
        return converter.convert(post, PostBo.class);
    }

    /**
     * 根据用户ID获取岗位选择框列表
     *
     * <p>复刻原 selectPostListByUserId join 语义：关联行需在 sys_post 中存在
     * （孤儿关联行随 in 条件丢弃），无 del_flag/status 过滤（与原 SQL 一致）。</p>
     *
     * @param userId 用户ID
     * @return 选中岗位ID列表
     */
    public List<Long> selectPostListByUserId(Long userId)
    {
        List<Long> postIds = userPostMapper.selectList(Wrappers.lambdaQuery(UserPost.class)
                .select(UserPost::getPostId)
                .eq(UserPost::getUserId, userId))
                .stream().map(UserPost::getPostId).collect(Collectors.toList());
        if (postIds.isEmpty())
        {
            return postIds;
        }
        return postMapper.selectList(Wrappers.lambdaQuery(Post.class)
                .select(Post::getPostId)
                .in(Post::getPostId, postIds))
                .stream().map(Post::getPostId).collect(Collectors.toList());
    }

    /**
     * 校验岗位名称是否唯一
     *
     * @param postName 岗位名称
     * @return 岗位信息
     */
    public PostBo checkPostNameUnique(String postName)
    {
        Post post = postMapper.selectOne(postColumnWrapper()
                .eq(Post::getPostName, postName)
                .last("limit 1"));
        return converter.convert(post, PostBo.class);
    }

    /**
     * 校验岗位编码是否唯一
     *
     * @param postCode 岗位编码
     * @return 岗位信息
     */
    public PostBo checkPostCodeUnique(String postCode)
    {
        Post post = postMapper.selectOne(postColumnWrapper()
                .eq(Post::getPostCode, postCode)
                .last("limit 1"));
        return converter.convert(post, PostBo.class);
    }

    /**
     * 通过岗位ID查询岗位使用数量
     *
     * @param postId 岗位ID
     * @return 结果
     */
    public int countUserPostById(Long postId)
    {
        Long count = userPostMapper.selectCount(Wrappers.lambdaQuery(UserPost.class).eq(UserPost::getPostId, postId));
        return count.intValue();
    }

    /**
     * 新增岗位信息
     *
     * @param post 岗位信息
     * @return 结果
     */
    public int insertPost(PostBo post)
    {
        Post po = converter.convert(post, Post.class);
        po.setCreateTime(DateUtils.getNowDate());
        return postMapper.insert(po);
    }

    /**
     * 修改岗位信息
     *
     * @param post 岗位信息
     * @return 结果
     */
    public int updatePost(PostBo post)
    {
        Post po = converter.convert(post, Post.class);
        // 镜像原 XML updatePost 的动态 <if> 条件（原 OGNL `x != null and x != ''` / `x != null`）
        return postMapper.update(null, Wrappers.lambdaUpdate(Post.class)
                .set(po.getPostCode() != null && !po.getPostCode().isEmpty(), Post::getPostCode, po.getPostCode())
                .set(po.getPostName() != null && !po.getPostName().isEmpty(), Post::getPostName, po.getPostName())
                .set(po.getPostSort() != null, Post::getPostSort, po.getPostSort())
                .set(po.getStatus() != null && !po.getStatus().isEmpty(), Post::getStatus, po.getStatus())
                .set(po.getRemark() != null, Post::getRemark, po.getRemark())
                .set(po.getUpdateBy() != null && !po.getUpdateBy().isEmpty(), Post::getUpdateBy, po.getUpdateBy())
                .set(Post::getUpdateTime, DateUtils.getNowDate())
                .eq(Post::getPostId, po.getPostId()));
    }

    /**
     * 删除岗位信息
     *
     * @param postId 岗位ID
     * @return 结果
     */
    public int deletePostById(Long postId)
    {
        return postMapper.deleteById(postId);
    }

    /**
     * 批量删除岗位信息
     *
     * @param postIds 需要删除的岗位ID
     * @return 结果
     */
    public int deletePostByIds(Long[] postIds)
    {
        return postMapper.deleteByIds(Arrays.asList(postIds));
    }

    /**
     * 构建与原 XML selectPostVo 相同列清单的查询壳
     */
    private LambdaQueryWrapper<Post> postColumnWrapper()
    {
        return Wrappers.lambdaQuery(Post.class)
                .select(Post::getPostId, Post::getPostCode, Post::getPostName, Post::getPostSort,
                        Post::getStatus, Post::getCreateBy, Post::getCreateTime, Post::getRemark);
    }

    /**
     * 构建岗位列表查询条件（与原 XML selectPostList 的动态条件一致）
     *
     * @param query 岗位查询条件
     * @return 查询条件
     */
    private LambdaQueryWrapper<Post> buildPostQuery(PostBo query)
    {
        LambdaQueryWrapper<Post> wrapper = postColumnWrapper();
        if (query.getPostCode() != null && !query.getPostCode().isEmpty())
        {
            wrapper.like(Post::getPostCode, query.getPostCode());
        }
        if (query.getStatus() != null && !query.getStatus().isEmpty())
        {
            wrapper.eq(Post::getStatus, query.getStatus());
        }
        if (query.getPostName() != null && !query.getPostName().isEmpty())
        {
            wrapper.like(Post::getPostName, query.getPostName());
        }
        return wrapper;
    }
}
