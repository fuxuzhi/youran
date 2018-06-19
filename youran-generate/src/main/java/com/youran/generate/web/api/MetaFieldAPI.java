package com.youran.generate.web.api;

import com.youran.common.pojo.vo.ReplyVO;
import com.youran.generate.pojo.dto.MetaFieldAddDTO;
import com.youran.generate.pojo.qo.MetaFieldQO;
import com.youran.generate.pojo.dto.MetaFieldUpdateDTO;
import com.youran.generate.pojo.vo.MetaFieldListVO;
import com.youran.generate.pojo.vo.MetaFieldShowVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;

import java.util.List;

/**
 * Title: 【字段】api
 * Description:
 * Author: cbb
 * Create Time:2017/5/12 13:40
 */
@Api(tags = "MetaFieldPO", description = "字段")
public interface MetaFieldAPI {

    /**
     * 新增字段
     */
    @ApiOperation(value = "新增字段")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "metaFieldAddDTO", dataType = "MetaFieldAddDTO", value = "新增字段参数", paramType = "body"),
    })
    ReplyVO<Integer> save(MetaFieldAddDTO metaFieldAddDTO);

    /**
     * 修改字段
     */
    @ApiOperation(value = "修改字段")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "metaFieldUpdateDTO", dataType = "MetaFieldUpdateDTO", value = "修改字段参数", paramType = "body"),
    })
    ReplyVO<Void> update(MetaFieldUpdateDTO metaFieldUpdateDTO);

    /**
     * 字段列表查询
     */
    @ApiOperation(value = "字段列表查询")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "metaFieldQO", dataType = "MetaFieldQO", value = "查询参数", paramType = "body"),
    })
    ReplyVO<List<MetaFieldListVO>> list(MetaFieldQO metaFieldQO);

    /**
     * 查看字段详情
     */
    @ApiOperation(value = "查看字段详情")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "fieldId", dataType = "int", value = "字段id", paramType = "path"),
    })
    ReplyVO<MetaFieldShowVO> show(Integer fieldId);

    /**
     * 删除字段
     */
    @ApiOperation(value = "删除字段")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "fieldId", dataType = "int", value = "字段id", paramType = "path"),
    })
    ReplyVO<Integer> delete(Integer fieldId);

    /**
     * 批量删除字段
     */
    @ApiOperation(value = "批量删除字段")
    @ApiImplicitParams({
        @ApiImplicitParam(name = "fieldId", dataType = "int", value = "字段id数组", paramType = "body"),
    })
    ReplyVO<Integer> deleteBatch(Integer[] fieldId);

}
