package com.boot.system.service;

import java.util.List;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.boot.system.domain.bo.PostBo;

/**
 * 岗位信息 服务层
 *
 * @author boot-plus
 */
public interface IPostService
{
    /**
     * 分页查询岗位信息集合
     *
     * @param page 分页参数
     * @param post 岗位查询条件
     * @return 岗位列表
     */
    public IPage<PostBo> selectPostList(IPage<PostBo> page, PostBo post);

    /**
     * 查询岗位信息集合（非分页，用于导出）
     *
     * @param post 岗位查询条件
     * @return 岗位列表
     */
    public List<PostBo> selectPostList(PostBo post);

    /**
     * 查询所有岗位
     *
     * @return 岗位列表
     */
    public List<PostBo> selectPostAll();

    /**
     * 通过岗位ID查询岗位信息
     *
     * @param postId 岗位ID
     * @return 岗位信息
     */
    public PostBo selectPostById(Long postId);

    /**
     * 根据用户ID获取岗位选择框列表
     *
     * @param userId 用户ID
     * @return 选中岗位ID列表
     */
    public List<Long> selectPostListByUserId(Long userId);

    /**
     * 校验岗位名称
     *
     * @param post 岗位信息
     * @return 结果
     */
    public boolean checkPostNameUnique(PostBo post);

    /**
     * 校验岗位编码
     *
     * @param post 岗位信息
     * @return 结果
     */
    public boolean checkPostCodeUnique(PostBo post);

    /**
     * 通过岗位ID查询岗位使用数量
     *
     * @param postId 岗位ID
     * @return 结果
     */
    public int countUserPostById(Long postId);

    /**
     * 删除岗位信息
     *
     * @param postId 岗位ID
     * @return 结果
     */
    public int deletePostById(Long postId);

    /**
     * 批量删除岗位信息
     *
     * @param postIds 需要删除的岗位ID
     * @return 结果
     */
    public int deletePostByIds(Long[] postIds);

    /**
     * 新增保存岗位信息
     *
     * @param post 岗位信息
     * @return 结果
     */
    public int insertPost(PostBo post);

    /**
     * 修改保存岗位信息
     *
     * @param post 岗位信息
     * @return 结果
     */
    public int updatePost(PostBo post);
}
