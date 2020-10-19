package com.cn.wjw.mybatis.generator.plugins;

import java.util.Comparator;
import java.util.List;
import org.mybatis.generator.api.IntrospectedTable;
import org.mybatis.generator.api.PluginAdapter;
import org.mybatis.generator.api.dom.java.Interface;
import org.mybatis.generator.api.dom.java.Method;
import org.mybatis.generator.api.dom.java.Parameter;

public class FixGeneratedCodeStylePlugin extends PluginAdapter {

  @Override
  public boolean validate(List<String> warnings) {
    return true;
  }

  @Override
  public boolean clientGenerated(Interface interfaze, IntrospectedTable introspectedTable) {
    // 按方法名排序，将重载方法放在一起
    interfaze.getMethods().sort(Comparator.comparing(Method::getName));
    interfaze
        .getMethods()
        .stream()
        // 将参数后缀"_"替换为"Param"
        .forEach(
            method -> {
              method
                  .getParameters()
                  .replaceAll(
                      param -> {
                        var newParam =
                            new Parameter(param.getType(), param.getName().replace("_", "Param"));
                        newParam.getAnnotations().addAll(param.getAnnotations());
                        return newParam;
                      });

              method.getBodyLines().replaceAll(bodyLine -> bodyLine.replace("_", "Param"));
            });
    return true;
  }
}
