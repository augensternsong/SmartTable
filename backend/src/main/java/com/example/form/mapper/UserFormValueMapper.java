package com.example.form.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.form.entity.UserFormValue;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 用户填写当前值 Mapper.
 */
@Mapper
public interface UserFormValueMapper extends BaseMapper<UserFormValue> {

    /**
     * 查询某用户在指定模板下所有栏位的当前填写值, 不含已停用栏位的数据也带回(便于提醒).
     */
    @Select("""
            SELECT v.*
            FROM user_form_value v
            JOIN form_template_field f ON f.id = v.field_id
            WHERE v.user_id = #{userId} AND v.template_id = #{templateId}
            """)
    List<UserFormValue> selectByUserAndTemplate(@Param("userId") String userId,
                                                @Param("templateId") String templateId);
}
