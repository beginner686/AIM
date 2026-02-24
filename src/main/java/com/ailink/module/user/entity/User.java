package com.ailink.module.user.entity;

import com.ailink.common.model.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("user_account")
public class User extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String role;
    private String username;
    private String password;
    private String email;
    private String country;
    private String city;
    private Integer status;
}
