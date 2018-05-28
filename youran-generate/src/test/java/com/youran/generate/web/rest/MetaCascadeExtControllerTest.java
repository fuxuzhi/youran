package com.youran.generate.web.rest;

import com.youran.common.pojo.vo.ReplyVO;
import com.youran.common.util.JsonUtil;
import com.youran.generate.data.MetaCascadeExtData;
import com.youran.generate.help.GenerateHelper;
import com.youran.generate.pojo.dto.MetaCascadeExtAddDTO;
import com.youran.generate.pojo.dto.MetaCascadeExtUpdateDTO;
import com.youran.generate.pojo.po.MetaCascadeExtPO;
import com.youran.generate.pojo.po.MetaEntityPO;
import com.youran.generate.pojo.po.MetaFieldPO;
import com.youran.generate.pojo.po.MetaProjectPO;
import com.youran.generate.web.AbstractWebTest;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

/**
 * Title:
 * Description:
 * Author: cbb
 * Create Time:2017/5/12 14:35
 */
public class MetaCascadeExtControllerTest extends AbstractWebTest {

    @Autowired
    private GenerateHelper generateHelper;

    private MetaProjectPO metaProject;
    private MetaEntityPO metaEntity1;
    private MetaEntityPO metaEntity2;
    private MetaFieldPO metaField1;
    private MetaFieldPO metaField2;

    @Before
    public void init(){
        this.metaProject = generateHelper.saveProjectExample();
        this.metaEntity1 = generateHelper.saveEntityExample(metaProject.getProjectId(),1);
        this.metaEntity2 = generateHelper.saveEntityExample(metaProject.getProjectId(),2);
        this.metaField1 = generateHelper.saveFieldExample(this.metaEntity1.getEntityId());
        this.metaField2 = generateHelper.saveFieldExample(this.metaEntity2.getEntityId());
    }

    @Test
    public void save() throws Exception {
        MetaCascadeExtAddDTO addDTO = MetaCascadeExtData.getAddDTO(metaField1.getFieldId(),metaEntity1.getEntityId(),
            metaField2.getFieldId(),metaEntity2.getEntityId());
        restMockMvc.perform(post(getRootPath()+"/meta_cascade_ext/save")
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .content(JsonUtil.toJSONString(addDTO)))
                .andExpect(jsonPath("$.code").value(is(ReplyVO.SUCCESS_CODE)));

    }

    @Test
    public void update() throws Exception {
        MetaCascadeExtPO metaCascadeExt = generateHelper.saveCascadeExtExample(metaField1.getFieldId(),metaEntity1.getEntityId(),
            metaField2.getFieldId(),metaEntity2.getEntityId());
        MetaCascadeExtUpdateDTO updateDTO = MetaCascadeExtData.getUpdateDTO(metaCascadeExt);
        restMockMvc.perform(put(getRootPath()+"/meta_cascade_ext/update")
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .content(JsonUtil.toJSONString(updateDTO)))
                .andExpect(jsonPath("$.code").value(is(ReplyVO.SUCCESS_CODE)));
    }


    @Test
    public void list() throws Exception {
        generateHelper.saveCascadeExtExample(metaField1.getFieldId(),metaEntity1.getEntityId(),
            metaField2.getFieldId(),metaEntity2.getEntityId());
        restMockMvc.perform(get(getRootPath()+"/meta_cascade_ext/list")
                .param("fieldId",metaField1.getFieldId()+""))
                .andExpect(jsonPath("$.code").value(is(ReplyVO.SUCCESS_CODE)))
                .andExpect(jsonPath("$.data.length()").value(is(1)));
    }

    @Test
    public void show() throws Exception {
        MetaCascadeExtPO metaCascadeExt = generateHelper.saveCascadeExtExample(metaField1.getFieldId(),metaEntity1.getEntityId(),
            metaField2.getFieldId(),metaEntity2.getEntityId());
        restMockMvc.perform(get(getRootPath()+"/meta_cascade_ext/{cascadeExtId}",metaCascadeExt.getCascadeExtId()))
                .andExpect(jsonPath("$.code").value(is(ReplyVO.SUCCESS_CODE)))
                .andExpect(jsonPath("$.data.cascadeExtId").value(is(metaCascadeExt.getCascadeExtId())));
    }

    @Test
    public void del() throws Exception {
        MetaCascadeExtPO metaCascadeExt = generateHelper.saveCascadeExtExample(metaField1.getFieldId(),metaEntity1.getEntityId(),
            metaField2.getFieldId(),metaEntity2.getEntityId());
        restMockMvc.perform(delete(getRootPath()+"/meta_cascade_ext/{cascadeExtId}",metaCascadeExt.getCascadeExtId()))
                .andExpect(jsonPath("$.code").value(is(ReplyVO.SUCCESS_CODE)))
                .andExpect(jsonPath("$.data").value(is(1)));
    }



}