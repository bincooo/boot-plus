package com.boot.system.repo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.boot.common.core.domain.bo.DeptBo;
import com.boot.common.utils.DateUtils;
import com.boot.common.utils.PageUtils;
import com.boot.system.repo.mapper.DeptMapper;
import com.boot.system.repo.mapper.RoleMapper;
import com.boot.system.repo.mapper.UserMapper;
import com.boot.system.repo.po.Dept;
import com.boot.system.repo.po.Role;
import com.boot.system.repo.po.User;
import io.github.linpeilie.Converter;
import org.springframework.stereotype.Repository;
import lombok.RequiredArgsConstructor;

/**
 * 部门信息 聚合仓库
 *
 * @author boot-plus
 */
@Repository
@RequiredArgsConstructor
public class DeptRepository
{
    private final DeptMapper deptMapper;

    private final RoleMapper roleMapper;

    private final UserMapper userMapper;

    private final Converter converter;

    /**
     * 查询部门管理数据
     *
     * @param query 部门查询条件
     * @return 部门信息集合
     */
    public List<DeptBo> selectDeptList(DeptBo query)
    {
        LambdaQueryWrapper<Dept> wrapper = Wrappers.lambdaQuery(Dept.class)
                .select(Dept::getDeptId, Dept::getParentId, Dept::getAncestors, Dept::getDeptName,
                        Dept::getOrderNum, Dept::getLeader, Dept::getPhone, Dept::getEmail, Dept::getStatus,
                        Dept::getDelFlag, Dept::getCreateBy, Dept::getCreateTime)
                .eq(Dept::getDelFlag, "0");
        if (query.getDeptId() != null && query.getDeptId() != 0)
        {
            wrapper.eq(Dept::getDeptId, query.getDeptId());
        }
        if (query.getParentId() != null && query.getParentId() != 0)
        {
            wrapper.eq(Dept::getParentId, query.getParentId());
        }
        if (query.getDeptName() != null && !query.getDeptName().isEmpty())
        {
            wrapper.like(Dept::getDeptName, query.getDeptName());
        }
        if (query.getStatus() != null && !query.getStatus().isEmpty())
        {
            wrapper.eq(Dept::getStatus, query.getStatus());
        }
        // 数据范围过滤：由 DataScopeAspect 服务端生成（进入前已清空用户可提交值），
        // 别名 sys_dept.dept_id 与本仓库单表查询一致，无请求字符串拼接。
        // 片段固定以 " AND (" 开头（Aspect 的生成格式），wrapper 自身会用 AND 连接条件，
        // 因此仅去掉这个由 Aspect 固定添加的前缀，避免生成 "AND AND"。
        String dataScope = normalizeDataScope(query.getParams().get("dataScope"));
        if (!dataScope.isEmpty())
        {
            wrapper.apply(dataScope);
        }
        wrapper.orderByAsc(Dept::getParentId, Dept::getOrderNum);
        // 注意：原 selectDeptList 的 <sql id="selectDeptVo"> 并未投影 parent_name（也无标量子查询），
        // 因此原查询的 parentName 恒为 null，这里同样不填充，保持行为等价
        return convertList(deptMapper.selectList(wrapper));
    }

    /**
     * 根据角色ID查询部门树信息
     *
     * @param roleId 角色ID
     * @return 选中部门列表
     */
    public List<Long> selectDeptIdsByRoleId(Long roleId)
    {
        if (roleId == null)
        {
            return new ArrayList<>();
        }
        Role role = roleMapper.selectOne(Wrappers.lambdaQuery(Role.class)
                .select(Role::getRoleId, Role::isDeptCheckStrictly)
                .eq(Role::getRoleId, roleId));
        boolean deptCheckStrictly = role != null && role.isDeptCheckStrictly();
        // 外层等多价 IN 子查询，按 parent_id, order_num 排序（原 from sys_dept d left join
        // sys_role_dept rd ... where rd.role_id = ? 等价于内连接投影 d.dept_id）。
        // 子查询仅拼接 Long 角色 ID，无请求字符串。
        LambdaQueryWrapper<Dept> wrapper = Wrappers.lambdaQuery(Dept.class)
                .select(Dept::getDeptId)
                .inSql(Dept::getDeptId, "select rd.dept_id from sys_role_dept rd where rd.role_id = " + roleId)
                .orderByAsc(Dept::getParentId, Dept::getOrderNum);
        if (deptCheckStrictly)
        {
            // 与原 SQL 完全相同的 NOT IN 子查询（保留 NULL parent_id 的 NOT IN 语义）
            wrapper.notInSql(Dept::getDeptId, "select d.parent_id from sys_dept d "
                    + "inner join sys_role_dept rd on d.dept_id = rd.dept_id and rd.role_id = " + roleId);
        }
        return deptMapper.selectList(wrapper).stream()
                .map(Dept::getDeptId).collect(Collectors.toList());
    }

    /**
     * 根据部门ID查询信息
     *
     * <p>复刻原 selectDeptById 语义：原 SQL 为 9 列子集 + 标量子查询计算 parent_name
     * （无 del_flag 过滤），拆为主表查询 + 按 parent_id 查父部门名的两次等价查询；
     * parent_id 为 null 或父部门不存在时 parent_name 保持 null，与标量子查询一致。</p>
     *
     * @param deptId 部门ID
     * @return 部门信息
     */
    public DeptBo selectDeptById(Long deptId)
    {
        Dept po = deptMapper.selectOne(Wrappers.lambdaQuery(Dept.class)
                .select(Dept::getDeptId, Dept::getParentId, Dept::getAncestors, Dept::getDeptName,
                        Dept::getOrderNum, Dept::getLeader, Dept::getPhone, Dept::getEmail, Dept::getStatus)
                .eq(Dept::getDeptId, deptId));
        if (po != null && po.getParentId() != null)
        {
            // 复刻原标量子查询 (select dept_name from sys_dept where dept_id = d.parent_id)
            Dept parent = deptMapper.selectOne(Wrappers.lambdaQuery(Dept.class)
                    .select(Dept::getDeptName)
                    .eq(Dept::getDeptId, po.getParentId()));
            if (parent != null)
            {
                po.setParentName(parent.getDeptName());
            }
        }
        return converter.convert(po, DeptBo.class);
    }

    /**
     * 根据ID查询所有子部门
     *
     * @param deptId 部门ID
     * @return 部门列表
     */
    public List<DeptBo> selectChildrenDeptById(Long deptId)
    {
        return convertList(deptMapper.selectList(Wrappers.lambdaQuery(Dept.class)
                .apply("concat(',', ancestors, ',') like concat('%,', {0}, ',%')", deptId)));
    }

    /**
     * 根据ID查询所有子部门（正常状态）
     *
     * @param deptId 部门ID
     * @return 子部门数
     */
    public int selectNormalChildrenDeptById(Long deptId)
    {
        Long count = deptMapper.selectCount(Wrappers.lambdaQuery(Dept.class)
                .eq(Dept::getStatus, "0")
                .eq(Dept::getDelFlag, "0")
                .apply("concat(',', ancestors, ',') like concat('%,', {0}, ',%')", deptId));
        return count.intValue();
    }

    /**
     * 是否存在子节点
     *
     * @param deptId 部门ID
     * @return 结果
     */
    public int hasChildByDeptId(Long deptId)
    {
        Long count = deptMapper.selectCount(Wrappers.lambdaQuery(Dept.class)
                .eq(Dept::getDelFlag, "0")
                .eq(Dept::getParentId, deptId)
                .last("limit 1"));
        return count.intValue();
    }

    /**
     * 查询部门是否存在用户
     *
     * <p>复刻原 checkDeptExistUser 语义（跨表统计 sys_user）：dept_id 相等且 del_flag='0' 的用户数。</p>
     *
     * @param deptId 部门ID
     * @return 结果
     */
    public int checkDeptExistUser(Long deptId)
    {
        Long count = userMapper.selectCount(Wrappers.lambdaQuery(User.class)
                .eq(User::getDeptId, deptId)
                .eq(User::getDelFlag, "0"));
        return count.intValue();
    }

    /**
     * 校验部门名称是否唯一
     *
     * @param deptName 部门名称
     * @param parentId 父部门ID
     * @return 结果
     */
    public DeptBo checkDeptNameUnique(String deptName, Long parentId)
    {
        Dept dept = deptMapper.selectOne(Wrappers.lambdaQuery(Dept.class)
                .select(Dept::getDeptId, Dept::getParentId, Dept::getAncestors, Dept::getDeptName,
                        Dept::getOrderNum, Dept::getLeader, Dept::getPhone, Dept::getEmail, Dept::getStatus,
                        Dept::getDelFlag, Dept::getCreateBy, Dept::getCreateTime)
                .eq(Dept::getDeptName, deptName)
                .eq(Dept::getParentId, parentId)
                .eq(Dept::getDelFlag, "0")
                .last("limit 1"));
        return converter.convert(dept, DeptBo.class);
    }

    /**
     * 新增部门信息
     *
     * @param dept 部门信息
     * @return 结果
     */
    public int insertDept(DeptBo dept)
    {
        Dept po = converter.convert(dept, Dept.class);
        po.setCreateTime(DateUtils.getNowDate());
        return deptMapper.insert(po);
    }

    /**
     * 修改部门信息
     *
     * @param dept 部门信息
     * @return 结果
     */
    public int updateDept(DeptBo dept)
    {
        Dept po = converter.convert(dept, Dept.class);
        // 镜像原 XML updateDept 的动态 <if> 条件（原 OGNL `x != null and x != ''` / `x != null`）
        return deptMapper.update(null, Wrappers.lambdaUpdate(Dept.class)
                .set(po.getParentId() != null && po.getParentId() != 0, Dept::getParentId, po.getParentId())
                .set(po.getDeptName() != null && !po.getDeptName().isEmpty(), Dept::getDeptName, po.getDeptName())
                .set(po.getAncestors() != null && !po.getAncestors().isEmpty(), Dept::getAncestors, po.getAncestors())
                .set(po.getOrderNum() != null, Dept::getOrderNum, po.getOrderNum())
                .set(po.getLeader() != null, Dept::getLeader, po.getLeader())
                .set(po.getPhone() != null, Dept::getPhone, po.getPhone())
                .set(po.getEmail() != null, Dept::getEmail, po.getEmail())
                .set(po.getStatus() != null && !po.getStatus().isEmpty(), Dept::getStatus, po.getStatus())
                .set(po.getUpdateBy() != null && !po.getUpdateBy().isEmpty(), Dept::getUpdateBy, po.getUpdateBy())
                .set(Dept::getUpdateTime, DateUtils.getNowDate())
                .eq(Dept::getDeptId, po.getDeptId()));
    }

    /**
     * 修改所在部门正常状态
     *
     * @param deptIds 部门ID组
     */
    public void updateDeptStatusNormal(Long[] deptIds)
    {
        deptMapper.update(null, Wrappers.lambdaUpdate(Dept.class)
                .set(Dept::getStatus, "0")
                .in(Dept::getDeptId, Arrays.asList(deptIds)));
    }

    /**
     * 修改子元素关系
     *
     * <p>原 XML 为 foreach case-when 单语句批量更新；wrapper 无法表达 case-when，
     * 改为逐条等价 update（结果等价，N 条语句，见转换报告）。</p>
     *
     * @param depts 子元素
     * @return 结果
     */
    public int updateDeptChildren(List<DeptBo> depts)
    {
        int rows = 0;
        for (DeptBo dept : depts)
        {
            rows += deptMapper.update(null, Wrappers.lambdaUpdate(Dept.class)
                    .set(Dept::getAncestors, dept.getAncestors())
                    .eq(Dept::getDeptId, dept.getDeptId()));
        }
        return rows;
    }

    /**
     * 保存部门排序
     *
     * @param dept 部门信息
     */
    public void updateDeptSort(DeptBo dept)
    {
        Dept po = converter.convert(dept, Dept.class);
        deptMapper.update(null, Wrappers.lambdaUpdate(Dept.class)
                .set(Dept::getOrderNum, po.getOrderNum())
                .eq(Dept::getDeptId, po.getDeptId()));
    }

    /**
     * 删除部门管理信息
     *
     * @param deptId 部门ID
     * @return 结果
     */
    public int deleteDeptById(Long deptId)
    {
        // 软删除部门
        return deptMapper.update(null, Wrappers.lambdaUpdate(Dept.class)
                .set(Dept::getDelFlag, "2")
                .eq(Dept::getDeptId, deptId));
    }

    /**
     * 去除 DataScopeAspect 片段固定携带的前导 " AND "（服务端生成，非请求输入）
     */
    private static String normalizeDataScope(Object dataScope)
    {
        if (dataScope == null)
        {
            return "";
        }
        String fragment = dataScope.toString().trim();
        if (fragment.regionMatches(true, 0, "AND ", 0, 4))
        {
            fragment = fragment.substring(4).trim();
        }
        return fragment;
    }

    /**
     * 将 PO 列表转换为 BO 列表
     */
    private List<DeptBo> convertList(List<Dept> records)
    {
        return PageUtils.convert(records, po -> converter.convert(po, DeptBo.class));
    }
}
