package com.cn.wjw.dao.model;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public abstract class BaseModel {

  /** 版本. */
  private Integer version;

  /** 创建者. */
  private String createdUser;

  /** 创建时间. */
  private LocalDateTime createdDateTime;

  /** 更新者. */
  private String updatedUser;

  /** 更新时间. */
  private LocalDateTime updatedDateTime;
}
