package com.cn.wjw.mybatis.generator.api.dom;

import com.google.googlejavaformat.java.Formatter;
import com.google.googlejavaformat.java.FormatterException;
import org.mybatis.generator.api.dom.DefaultJavaFormatter;
import org.mybatis.generator.api.dom.java.CompilationUnit;

public class StandardFormatter extends DefaultJavaFormatter {

  @Override
  public String getFormattedContent(CompilationUnit compilationUnit) {
    try {
      return new Formatter().formatSource(compilationUnit.accept(this));
    } catch (FormatterException e) {
      throw new RuntimeException(e);
    }
  }
}
