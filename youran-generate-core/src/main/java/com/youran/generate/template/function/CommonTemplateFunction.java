package com.youran.generate.template.function;

import com.youran.generate.util.SwitchCaseUtil;
import org.apache.commons.lang3.StringUtils;

/**
 * 通用模板函数
 * <p>作为内置函数用来辅助freemarker模板
 *
 * @author cbb
 * @date 2019/11/11
 */
public class CommonTemplateFunction {

    /**
     * 下划线转驼峰
     *
     * @param name
     * @param capFirst
     * @return
     */
    public static String underlineToCamelCase(String name, boolean capFirst) {
        return SwitchCaseUtil.underlineToCamelCase(name, capFirst);
    }

    /**
     * 驼峰转下划线
     *
     * @param name
     * @param upCase
     * @return
     */
    public static String camelCaseToSnakeCase(String name, boolean upCase) {
        return SwitchCaseUtil.camelCaseToSnakeCase(name, upCase);
    }

    /**
     * 转换【备注】展示
     *
     * @param comment
     * @return
     */
    public static String convertCommentDisplay(String comment) {
        if (StringUtils.isBlank(comment)) {
            return "";
        }
        return comment.replaceAll("\'", "\"")
            .replaceAll("\n", "");
    }

}
