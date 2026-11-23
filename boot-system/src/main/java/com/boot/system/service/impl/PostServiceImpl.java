package com.boot.system.service.impl;

import java.util.List;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import com.boot.common.constant.UserConstants;
import com.boot.common.exception.ServiceException;
import com.boot.common.utils.StringUtils;
import com.boot.system.repo.PostRepository;
import com.boot.system.service.IPostService;
import com.boot.system.domain.bo.PostBo;

/**
 * 岗位信息 服务层处理
 *
 * @author boot-plus
 */
@Service
@RequiredArgsConstructor
public class PostServiceImpl implements IPostService
{
    private final PostRepository postRepository;

    /**
     * 分页查询岗位信息集合
     *
     * @param page 分页参数
     * @param post 岗位查询条件
     * @return 岗位信息集合
     */
    @Override
    public IPage<PostBo> selectPostList(IPage<PostBo> page, PostBo post)
    {
        return postRepository.selectPostList(page, post);
    }

    /**
     * 查询岗位信息集合（非分页，用于导出）
     *
     * @param post 岗位查询条件
     * @return 岗位信息集合
     */
    @Override
    public List<PostBo> selectPostList(PostBo post)
    {
        return postRepository.selectPostList(post);
    }

    /**
     * 查询所有岗位
     *
     * @return 岗位列表
     */
    @Override
    public List<PostBo> selectPostAll()
    {
        return postRepository.selectPostAll();
    }

    /**
     * 通过岗位ID查询岗位信息
     *
     * @param postId 岗位ID
     * @return 岗位信息
     */
    @Override
    public PostBo selectPostById(Long postId)
    {
        return postRepository.selectPostById(postId);
    }

    /**
     * 根据用户ID获取岗位选择框列表
     *
     * @param userId 用户ID
     * @return 选中岗位ID列表
     */
    @Override
    public List<Long> selectPostListByUserId(Long userId)
    {
        return postRepository.selectPostListByUserId(userId);
    }

    /**
     * 校验岗位名称是否唯一
     *
     * @param post 岗位信息
     * @return 结果
     */
    @Override
    public boolean checkPostNameUnique(PostBo post)
    {
        Long postId = StringUtils.isNull(post.getPostId()) ? -1L : post.getPostId();
        PostBo info = postRepository.checkPostNameUnique(post.getPostName());
        if (StringUtils.isNotNull(info) && info.getPostId().longValue() != postId.longValue())
        {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    /**
     * 校验岗位编码是否唯一
     *
     * @param post 岗位信息
     * @return 结果
     */
    @Override
    public boolean checkPostCodeUnique(PostBo post)
    {
        Long postId = StringUtils.isNull(post.getPostId()) ? -1L : post.getPostId();
        PostBo info = postRepository.checkPostCodeUnique(post.getPostCode());
        if (StringUtils.isNotNull(info) && info.getPostId().longValue() != postId.longValue())
        {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    /**
     * 通过岗位ID查询岗位使用数量
     *
     * @param postId 岗位ID
     * @return 结果
     */
    @Override
    public int countUserPostById(Long postId)
    {
        return postRepository.countUserPostById(postId);
    }

    /**
     * 删除岗位信息
     *
     * @param postId 岗位ID
     * @return 结果
     */
    @Override
    public int deletePostById(Long postId)
    {
        return postRepository.deletePostById(postId);
    }

    /**
     * 批量删除岗位信息
     *
     * @param postIds 需要删除的岗位ID
     * @return 结果
     */
    @Override
    public int deletePostByIds(Long[] postIds)
    {
        for (Long postId : postIds)
        {
            PostBo post = selectPostById(postId);
            if (countUserPostById(postId) > 0)
            {
                throw new ServiceException(String.format("%1$s已分配,不能删除", post.getPostName()));
            }
        }
        return postRepository.deletePostByIds(postIds);
    }

    /**
     * 新增保存岗位信息
     *
     * @param post 岗位信息
     * @return 结果
     */
    @Override
    public int insertPost(PostBo post)
    {
        return postRepository.insertPost(post);
    }

    /**
     * 修改保存岗位信息
     *
     * @param post 岗位信息
     * @return 结果
     */
    @Override
    public int updatePost(PostBo post)
    {
        return postRepository.updatePost(post);
    }
}
