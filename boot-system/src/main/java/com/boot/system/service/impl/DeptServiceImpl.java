package com.boot.system.service.impl;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import com.boot.common.annotation.DataScope;
import com.boot.common.constant.UserConstants;
import com.boot.common.core.domain.TreeSelect;
import com.boot.common.core.domain.bo.DeptBo;
import com.boot.common.core.text.Convert;
import com.boot.common.exception.ServiceException;
import com.boot.common.utils.SecurityUtils;
import com.boot.common.utils.StringUtils;
import com.boot.common.utils.spring.SpringUtils;
import com.boot.system.repo.DeptRepository;
import com.boot.system.service.IDeptService;

/**
 * 部门管理 服务实现
 *
 * @author boot-plus
 */
@Service
@RequiredArgsConstructor
public class DeptServiceImpl implements IDeptService
{
    private final DeptRepository deptRepository;

    /**
     * 查询部门管理数据
     *
     * @param dept 部门信息
     * @return 部门信息集合
     */
    @Override
    @DataScope(deptAlias = "sys_dept", deptField = "dept_id")
    public List<DeptBo> selectDeptList(DeptBo dept)
    {
        return deptRepository.selectDeptList(dept);
    }

    /**
     * 查询部门树结构信息
     *
     * @param dept 部门信息
     * @return 部门树信息集合
     */
    @Override
    public List<TreeSelect> selectDeptTreeList(DeptBo dept)
    {
        List<DeptBo> depts = SpringUtils.getAopProxy(this).selectDeptList(dept);
        return buildDeptTreeSelect(depts);
    }

    /**
     * 构建前端所需要树结构
     *
     * @param depts 部门列表
     * @return 树结构列表
     */
    @Override
    public List<DeptBo> buildDeptTree(List<DeptBo> depts)
    {
        List<DeptBo> returnList = new ArrayList<DeptBo>();
        List<Long> tempList = depts.stream().map(DeptBo::getDeptId).collect(Collectors.toList());
        for (DeptBo dept : depts)
        {
            // 如果是顶级节点, 遍历该父节点的所有子节点
            if (!tempList.contains(dept.getParentId()))
            {
                recursionFn(depts, dept);
                returnList.add(dept);
            }
        }
        if (returnList.isEmpty())
        {
            returnList = depts;
        }
        return returnList;
    }

    /**
     * 构建前端所需要下拉树结构
     *
     * @param depts 部门列表
     * @return 下拉树结构列表
     */
    @Override
    public List<TreeSelect> buildDeptTreeSelect(List<DeptBo> depts)
    {
        List<DeptBo> deptTrees = buildDeptTree(depts);
        return deptTrees.stream().map(TreeSelect::new).collect(Collectors.toList());
    }

    /**
     * 根据角色ID查询部门树信息
     *
     * @param roleId 角色ID
     * @return 选中部门列表
     */
    @Override
    public List<Long> selectDeptListByRoleId(Long roleId)
    {
        return deptRepository.selectDeptIdsByRoleId(roleId);
    }

    /**
     * 根据部门ID查询信息
     *
     * @param deptId 部门ID
     * @return 部门信息
     */
    @Override
    public DeptBo selectDeptById(Long deptId)
    {
        return deptRepository.selectDeptById(deptId);
    }

    /**
     * 根据ID查询所有子部门（正常状态）
     *
     * @param deptId 部门ID
     * @return 子部门数
     */
    @Override
    public int selectNormalChildrenDeptById(Long deptId)
    {
        return deptRepository.selectNormalChildrenDeptById(deptId);
    }

    /**
     * 是否存在子节点
     *
     * @param deptId 部门ID
     * @return 结果
     */
    @Override
    public boolean hasChildByDeptId(Long deptId)
    {
        int result = deptRepository.hasChildByDeptId(deptId);
        return result > 0;
    }

    /**
     * 查询部门是否存在用户
     *
     * @param deptId 部门ID
     * @return 结果 true 存在 false 不存在
     */
    @Override
    public boolean checkDeptExistUser(Long deptId)
    {
        int result = deptRepository.checkDeptExistUser(deptId);
        return result > 0;
    }

    /**
     * 校验部门名称是否唯一
     *
     * @param dept 部门信息
     * @return 结果
     */
    @Override
    public boolean checkDeptNameUnique(DeptBo dept)
    {
        Long deptId = StringUtils.isNull(dept.getDeptId()) ? -1L : dept.getDeptId();
        DeptBo info = deptRepository.checkDeptNameUnique(dept.getDeptName(), dept.getParentId());
        if (StringUtils.isNotNull(info) && info.getDeptId().longValue() != deptId.longValue())
        {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    /**
     * 校验部门是否有数据权限
     *
     * @param deptId 部门id
     */
    @Override
    public void checkDeptDataScope(Long deptId)
    {
        if (!SecurityUtils.isAdmin() && StringUtils.isNotNull(deptId))
        {
            DeptBo dept = new DeptBo();
            dept.setDeptId(deptId);
            List<DeptBo> depts = SpringUtils.getAopProxy(this).selectDeptList(dept);
            if (StringUtils.isEmpty(depts))
            {
                throw new ServiceException("没有权限访问部门数据！");
            }
        }
    }

    /**
     * 新增保存部门信息
     *
     * @param dept 部门信息
     * @return 结果
     */
    @Override
    public int insertDept(DeptBo dept)
    {
        DeptBo info = deptRepository.selectDeptById(dept.getParentId());
        // 如果父节点不为正常状态,则不允许新增子节点
        if (!UserConstants.DEPT_NORMAL.equals(info.getStatus()))
        {
            throw new ServiceException("部门停用，不允许新增");
        }
        dept.setAncestors(info.getAncestors() + "," + dept.getParentId());
        return deptRepository.insertDept(dept);
    }

    /**
     * 修改保存部门信息
     *
     * @param dept 部门信息
     * @return 结果
     */
    @Override
    @Transactional
    public int updateDept(DeptBo dept)
    {
        DeptBo newParentDept = deptRepository.selectDeptById(dept.getParentId());
        DeptBo oldDept = deptRepository.selectDeptById(dept.getDeptId());
        if (StringUtils.isNotNull(newParentDept) && StringUtils.isNotNull(oldDept))
        {
            String newAncestors = newParentDept.getAncestors() + "," + newParentDept.getDeptId();
            String oldAncestors = oldDept.getAncestors();
            dept.setAncestors(newAncestors);
            updateDeptChildren(dept.getDeptId(), newAncestors, oldAncestors);
        }
        int result = deptRepository.updateDept(dept);
        if (UserConstants.DEPT_NORMAL.equals(dept.getStatus()) && StringUtils.isNotEmpty(dept.getAncestors())
                && !StringUtils.equals("0", dept.getAncestors()))
        {
            // 如果该部门是启用状态，则启用该部门的所有上级部门
            updateParentDeptStatusNormal(dept);
        }
        return result;
    }

    /**
     * 修改该部门的父级部门状态
     *
     * @param dept 当前部门
     */
    private void updateParentDeptStatusNormal(DeptBo dept)
    {
        String ancestors = dept.getAncestors();
        Long[] deptIds = Convert.toLongArray(ancestors);
        deptRepository.updateDeptStatusNormal(deptIds);
    }

    /**
     * 修改子元素关系
     *
     * @param deptId 被修改的部门ID
     * @param newAncestors 新的父ID集合
     * @param oldAncestors 旧的父ID集合
     */
    public void updateDeptChildren(Long deptId, String newAncestors, String oldAncestors)
    {
        List<DeptBo> children = deptRepository.selectChildrenDeptById(deptId);
        for (DeptBo child : children)
        {
            child.setAncestors(child.getAncestors().replaceFirst(oldAncestors, newAncestors));
        }
        if (children.size() > 0)
        {
            deptRepository.updateDeptChildren(children);
        }
    }

    /**
     * 保存部门排序
     *
     * @param deptIds 部门ID数组
     * @param orderNums 排序数组
     */
    @Override
    @Transactional
    public void updateDeptSort(String[] deptIds, String[] orderNums)
    {
        try
        {
            for (int i = 0; i < deptIds.length; i++)
            {
                DeptBo dept = new DeptBo();
                dept.setDeptId(Convert.toLong(deptIds[i]));
                dept.setOrderNum(Convert.toInt(orderNums[i]));
                deptRepository.updateDeptSort(dept);
            }
        }
        catch (Exception e)
        {
            throw new ServiceException("保存排序异常，请联系管理员");
        }
    }

    /**
     * 删除部门管理信息
     *
     * @param deptId 部门ID
     * @return 结果
     */
    @Override
    public int deleteDeptById(Long deptId)
    {
        return deptRepository.deleteDeptById(deptId);
    }

    /**
     * 递归列表
     */
    private void recursionFn(List<DeptBo> list, DeptBo t)
    {
        // 得到子节点列表
        List<DeptBo> childList = getChildList(list, t);
        t.setChildren(childList);
        for (DeptBo tChild : childList)
        {
            if (hasChild(list, tChild))
            {
                recursionFn(list, tChild);
            }
        }
    }

    /**
     * 得到子节点列表
     */
    private List<DeptBo> getChildList(List<DeptBo> list, DeptBo t)
    {
        List<DeptBo> tlist = new ArrayList<DeptBo>();
        Iterator<DeptBo> it = list.iterator();
        while (it.hasNext())
        {
            DeptBo n = (DeptBo) it.next();
            if (StringUtils.isNotNull(n.getParentId()) && n.getParentId().longValue() == t.getDeptId().longValue())
            {
                tlist.add(n);
            }
        }
        return tlist;
    }

    /**
     * 判断是否有子节点
     */
    private boolean hasChild(List<DeptBo> list, DeptBo t)
    {
        return getChildList(list, t).size() > 0;
    }
}
