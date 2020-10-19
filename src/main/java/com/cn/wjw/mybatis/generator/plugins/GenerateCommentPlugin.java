package com.cn.wjw.mybatis.generator.plugins;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.mybatis.generator.api.FullyQualifiedTable;
import org.mybatis.generator.api.IntrospectedColumn;
import org.mybatis.generator.api.IntrospectedTable;
import org.mybatis.generator.api.Plugin;
import org.mybatis.generator.api.PluginAdapter;
import org.mybatis.generator.api.dom.java.CompilationUnit;
import org.mybatis.generator.api.dom.java.Field;
import org.mybatis.generator.api.dom.java.Interface;
import org.mybatis.generator.api.dom.java.JavaElement;
import org.mybatis.generator.api.dom.java.TopLevelClass;
import org.mybatis.generator.internal.util.JavaBeansUtil;
import org.mybatis.generator.internal.util.StringUtility;

@Slf4j
public class GenerateCommentPlugin extends PluginAdapter {

  /** 表注释. */
  private String tableRemarks;
  /** 表定义. */
  private FullyQualifiedTable fullyQualifiedTable;
  /** 所有列定义. */
  private List<IntrospectedColumn> allColumns;

  @Override
  public void initialized(IntrospectedTable introspectedTable) {
    tableRemarks = introspectedTable.getRemarks();
    allColumns = introspectedTable.getAllColumns();
    fullyQualifiedTable = introspectedTable.getFullyQualifiedTable();

    // 如果表没有定义注释打印警告
    if (!StringUtility.stringHasValue(tableRemarks)) {
      log.warn(fullyQualifiedTable + "没有定义COMMENTS, 无法生成相关javaDoc.");
    }

    // 如果列没有定义注释打印警告.
    allColumns
        .stream()
        .filter(column -> !StringUtility.stringHasValue(column.getRemarks()))
        .map(IntrospectedColumn::getActualColumnName)
        .map(columnName -> fullyQualifiedTable + "." + columnName + "没有定义COMMENTS, 无法生成相关javaDoc.")
        .forEach(log::warn);
  }

  @Override
  public boolean validate(List<String> warnings) {
    return true;
  }

  @Override
  public boolean modelBaseRecordClassGenerated(
      TopLevelClass modelClass, IntrospectedTable introspectedTable) {
    // 生成Model类javaDoc, 删除 @Generated 注解
    modelClass.addJavaDocLine("/**");
    modelClass.addJavaDocLine(" * " + tableRemarks + "实体类.");
    modelClass.addJavaDocLine(" */");
    removeGeneratedImport(modelClass);
    return true;
  }

  @Override
  public boolean modelFieldGenerated(
      Field field,
      TopLevelClass topLevelClass,
      IntrospectedColumn introspectedColumn,
      IntrospectedTable introspectedTable,
      Plugin.ModelClassType modelClassType) {
    // 生成Model类字段javaDoc, 删除 @Generated 注解
    String remarks = introspectedColumn.getRemarks();
    field.addJavaDocLine("/** " + remarks + ". */");
    removeGeneratedAnnotation(field);
    return true;
  }

  @Override
  public boolean dynamicSqlSupportGenerated(
      TopLevelClass supportClass, IntrospectedTable introspectedTable) {
    // 生成Support类javaDoc, 删除 @Generated 注解
    supportClass.addJavaDocLine("/**");
    supportClass.addJavaDocLine(" * " + tableRemarks + "动态SQL支持类.");
    supportClass.addJavaDocLine(" */");
    removeGeneratedImport(supportClass);

    // 生成Support类表定义字段的javaDoc, 删除 @Generated 注解
    var tableDefinitionFieldName =
        JavaBeansUtil.getValidPropertyName(fullyQualifiedTable.getDomainObjectName());
    var tableDefinitionField =
        supportClass
            .getFields()
            .stream()
            .filter(field -> field.getName().equals(tableDefinitionFieldName))
            .findFirst();
    tableDefinitionField.ifPresent(
        field -> field.addJavaDocLine("/** " + tableRemarks + "表定义. */"));
    tableDefinitionField.ifPresent(this::removeGeneratedAnnotation);

    // 生成Support类列定义字段javaDoc, 删除 @Generated 注解
    supportClass
        .getFields()
        .stream()
        .dropWhile(field -> field.equals(tableDefinitionField.get()))
        .peek(this::removeGeneratedAnnotation)
        .forEach(field -> addSupportClassFieldJavaDoc(field, introspectedTable));

    // 生成内部类的javaDoc, 删除 @Generated 注解
    var innerClass = supportClass.getInnerClasses().get(0);
    innerClass.addJavaDocLine("/**");
    innerClass.addJavaDocLine(" * " + tableRemarks + "表定义类.");
    innerClass.addJavaDocLine(" */");
    removeGeneratedAnnotation(innerClass);

    // 生成内部类字段javaDoc
    innerClass.getFields().forEach(field -> addSupportClassFieldJavaDoc(field, introspectedTable));
    return true;
  }

  @Override
  public boolean clientGenerated(Interface interfaze, IntrospectedTable introspectedTable) {
    interfaze.addJavaDocLine("/**");
    interfaze.addJavaDocLine(" * " + tableRemarks + "Mapper接口.");
    interfaze.addJavaDocLine(" */");
    // 生成Mapper接口方法的javaDoc, 删除 @Generated 注解
    interfaze
        .getMethods()
        .stream()
        .peek(
            method -> {
              method.addJavaDocLine("/**");
              method.addJavaDocLine(" * MBG自动生成的方法.");
              method
                  .getParameters()
                  .forEach(
                      param -> {
                        method.addJavaDocLine(" * @" + param.getName() + " " + param.getName());
                      });
              method.addJavaDocLine(" */");
            })
        .forEach(this::removeGeneratedAnnotation);
    // 生成Mapper接口字段的javaDoc, 删除 @Generated 注解
    interfaze
        .getFields()
        .stream()
        .filter(field -> "BasicColumn[]".equals(field.getType().getShortNameWithoutTypeArguments()))
        .findAny()
        .ifPresent(field -> field.addJavaDocLine("/** " + tableRemarks + "列集合. */"));
    interfaze.getFields().forEach(this::removeGeneratedAnnotation);
    removeGeneratedImport(interfaze);
    return true;
  }

  /**
   * 为Support类和其内部类的列定义字段添加注释.
   *
   * @param field 字段
   * @param introspectedTable 内省表
   */
  private void addSupportClassFieldJavaDoc(Field field, IntrospectedTable introspectedTable) {
    introspectedTable
        .getAllColumns()
        .stream()
        .filter(column -> column.getJavaProperty().equals(field.getName()))
        .findFirst()
        .map(IntrospectedColumn::getRemarks)
        .map(columnRemarks -> "/** " + tableRemarks + "." + columnRemarks + ". */")
        .ifPresent(field::addJavaDocLine);
  }

  /**
   * 删除生成的 @Generated 注解.
   *
   * @param e 可能包含 @Generated 注解的Java元素.
   */
  private void removeGeneratedAnnotation(JavaElement e) {
    e.getAnnotations().removeIf(annotation -> annotation.startsWith("@Generated"));
  }

  /**
   * 删除生成的{@code import javax.annotation.Generated;}代码.
   *
   * @param unit 可能导入了Generated注解的类.
   */
  private void removeGeneratedImport(CompilationUnit unit) {
    unit.getImportedTypes()
        .removeIf(
            importType -> "javax.annotation.Generated".equals(importType.getFullyQualifiedName()));
  }
}
