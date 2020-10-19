package com.cn.wjw.mybatis.generator.plugins;

import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.mybatis.generator.api.FullyQualifiedTable;
import org.mybatis.generator.api.IntrospectedColumn;
import org.mybatis.generator.api.IntrospectedTable;
import org.mybatis.generator.api.PluginAdapter;
import org.mybatis.generator.api.dom.java.FullyQualifiedJavaType;
import org.mybatis.generator.api.dom.java.Interface;
import org.mybatis.generator.api.dom.java.Method;
import org.mybatis.generator.api.dom.java.Parameter;
import org.mybatis.generator.internal.util.JavaBeansUtil;
import org.mybatis.generator.runtime.dynamic.sql.elements.AbstractMethodGenerator;

@Slf4j
public class GenerateLogicallyDeleteMethodPlugin extends PluginAdapter {

  private Optional<IntrospectedColumn> deleteFlgColumn;

  private FullyQualifiedTable fullyQualifiedTable;

  private FullyQualifiedJavaType recordType;

  private String tableFieldName;

  @Override
  public void initialized(IntrospectedTable introspectedTable) {

    fullyQualifiedTable = introspectedTable.getFullyQualifiedTable();
    if (!introspectedTable.hasPrimaryKeyColumns()) {
      log.warn(fullyQualifiedTable + "没有主键，无法生成逻辑删除相关方法");
    }
    deleteFlgColumn = introspectedTable.getColumn("delete_flg");
    if (deleteFlgColumn.isEmpty()) {
      log.warn(fullyQualifiedTable + ".delete_flg不存在，无法生成逻辑删除相关方法");
    }
    recordType = new FullyQualifiedJavaType(introspectedTable.getBaseRecordType());
    tableFieldName =
        JavaBeansUtil.getValidPropertyName(
            introspectedTable.getFullyQualifiedTable().getDomainObjectName());
  }

  @Override
  public boolean validate(List<String> warnings) {
    return true;
  }

  @Override
  public boolean clientGenerated(Interface interfaze, IntrospectedTable introspectedTable) {
    if (!introspectedTable.hasPrimaryKeyColumns() || deleteFlgColumn.isEmpty()) {
      return true;
    }
    addSelectByPrimaryKeyNotDeletedMethod(interfaze, introspectedTable);
    addDeleteByPrimaryKeyLogicallyMethod(interfaze, introspectedTable);
    return true;
  }

  /**
   * 生成SelectByPrimaryKeyNotDeleted方法.
   *
   * <pre>
   * default Optional{@code <Department>} selectByPrimaryKeyNotDeleted(Integer id_) {
   *   return selectOne(c -> c.where(id, isEqualTo(id_)).and(deleteFlg, isEqualTo("1")));
   * }
   * </pre>
   *
   * @param interfaze mapper接口
   * @param introspectedTable 表定义
   */
  private void addSelectByPrimaryKeyNotDeletedMethod(
      Interface interfaze, IntrospectedTable introspectedTable) {

    interfaze.addStaticImport("org.mybatis.dynamic.sql.SqlBuilder.*");
    // 返回值
    var returnType = new FullyQualifiedJavaType("java.util.Optional");
    returnType.addTypeArgument(recordType);
    interfaze.addImportedType(returnType);

    // 生成方法
    var method = new Method("selectByPrimaryKeyNotDeleted");
    method.setDefault(true);
    method.setReturnType(returnType);

    method.addBodyLine("return selectOne(c -> c");
    // 生成WHERE条件和参数,包含主键和删除flag
    addPrimaryKeyWhereClauseAndParameters(interfaze, introspectedTable, method);
    var delFlgFieldName =
        AbstractMethodGenerator.calculateFieldName(tableFieldName, deleteFlgColumn.get());
    method.addBodyLine("    .and(" + delFlgFieldName + ", isEqualTo(\"1\"))");
    method.addBodyLine(");");
    interfaze.addMethod(method);
  }

  /**
   * 生成deleteByPrimaryKeyLogically方法.
   *
   * <pre>
   * default int deleteByPrimaryKeyLogically(Integer id_) {
   *   return update(c -> c.set(deleteFlg).equalTo("1").where(id, isEqualTo(id_)).and(deleteFlg,
   *       isEqualTo("0")));
   * }
   * </pre>
   *
   * @param interfaze mapper接口
   * @param introspectedTable 表定义
   */
  private void addDeleteByPrimaryKeyLogicallyMethod(
      Interface interfaze, IntrospectedTable introspectedTable) {

    interfaze.addImportedType(recordType);
    // 生成方法
    Method method = new Method("deleteByPrimaryKeyLogically");
    method.setDefault(true);
    // 返回值
    method.setReturnType(FullyQualifiedJavaType.getIntInstance());
    method.addBodyLine("return update(c -> c");
    // 生成WHERE条件和参数,包含主键和删除flag
    var delFlgFieldName =
        AbstractMethodGenerator.calculateFieldName(tableFieldName, deleteFlgColumn.get());
    method.addBodyLine("    .set(" + delFlgFieldName + ").equalTo(\"1\")");
    addPrimaryKeyWhereClauseAndParameters(interfaze, introspectedTable, method);
    method.addBodyLine("    .and(" + delFlgFieldName + ", isEqualTo(\"0\"))");
    method.addBodyLine(");");
    interfaze.addMethod(method);
  }

  /**
   * 生成只包含主键的相等判断的WHERE条件.
   *
   * <pre>
   * .where(column1, isEqualTo(column1_))
   * .and(column2, isEqualTo(column2_)))
   * .and(column3, isEqualTo(column3_)));
   * </pre>
   *
   * @param interfaze mapper接口
   * @param introspectedTable 表定义
   * @param method 生成的方法
   */
  private void addPrimaryKeyWhereClauseAndParameters(
      Interface interfaze, IntrospectedTable introspectedTable, Method method) {
    var first = true;
    for (IntrospectedColumn column : introspectedTable.getPrimaryKeyColumns()) {
      var fieldName = AbstractMethodGenerator.calculateFieldName(tableFieldName, column);
      interfaze.addImportedType(column.getFullyQualifiedJavaType());
      method.addParameter(
          new Parameter(column.getFullyQualifiedJavaType(), column.getJavaProperty() + "_"));
      if (first) {
        method.addBodyLine(
            "    .where(" + fieldName + ", isEqualTo(" + column.getJavaProperty() + "_))");
        first = false;
      } else {
        method.addBodyLine(
            "    .and(" + fieldName + ", isEqualTo(" + column.getJavaProperty() + "_))");
      }
    }
  }
}
