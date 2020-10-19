package com.cn.wjw.mybatis.generator.plugins;

import java.util.List;
import org.mybatis.generator.api.IntrospectedColumn;
import org.mybatis.generator.api.IntrospectedTable;
import org.mybatis.generator.api.Plugin;
import org.mybatis.generator.api.PluginAdapter;
import org.mybatis.generator.api.dom.java.Method;
import org.mybatis.generator.api.dom.java.TopLevelClass;

public class GenerateLombokAnnotationPlugin extends PluginAdapter {

  @Override
  public boolean validate(List<String> warnings) {
    return true;
  }

  @Override
  public boolean modelBaseRecordClassGenerated(
      TopLevelClass topLevelClass, IntrospectedTable introspectedTable) {
    addLombokAnnotation(topLevelClass);
    return true;
  }

  @Override
  public boolean modelSetterMethodGenerated(
      Method method,
      TopLevelClass topLevelClass,
      IntrospectedColumn introspectedColumn,
      IntrospectedTable introspectedTable,
      Plugin.ModelClassType modelClassType) {
    // 因为使用lombok, 不生成Setter
    return false;
  }

  @Override
  public boolean modelGetterMethodGenerated(
      Method method,
      TopLevelClass topLevelClass,
      IntrospectedColumn introspectedColumn,
      IntrospectedTable introspectedTable,
      Plugin.ModelClassType modelClassType) {
    // 因为使用lombok, 不生成Getter
    return false;
  }

  /**
   * 生成lombok注解.
   *
   * <pre>
   * import lombok.Data;
   * import lombok.EqualsAndHashCode;
   *
   * {@code @Data}
   * {@code @EqualsAndHashCode(callSuper = true)}
   * public class XxxModel {
   * </pre>
   *
   * @param topLevelClass Model类
   */
  private void addLombokAnnotation(TopLevelClass topLevelClass) {
    topLevelClass.addImportedType("lombok.Data");
    topLevelClass.addImportedType("lombok.EqualsAndHashCode");
    // 注解 @Data 为class生成Getter, Setter, toString, equals, hashCode方法.
    topLevelClass.addAnnotation("@Data");
    // 注解 @EqualsAndHashCode 的参数callSuper = true指定生成的equals和hashCode方法调用父类的同一方法.
    topLevelClass.addAnnotation("@EqualsAndHashCode(callSuper = true)");
  }
}
