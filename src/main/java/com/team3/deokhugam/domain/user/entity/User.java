package com.team3.deokhugam.domain.user.entity;

import com.team3.deokhugam.global.entity.SoftDeletableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends SoftDeletableEntity {

  @Column(name = "email", nullable = false, unique = true)
  private String email;

  @Column(name = "nickname", nullable = false, length = 20)
  private String nickname;

  @Column(name = "password", nullable = false)
  private String encodedPassword;

  public User(String email, String nickname, String encodedPassword){
    this.email = email;
    this.nickname=nickname;
    this.encodedPassword=encodedPassword;
  }

  public void updateNickname(String nickname){
    this.nickname=nickname;
  }
}
