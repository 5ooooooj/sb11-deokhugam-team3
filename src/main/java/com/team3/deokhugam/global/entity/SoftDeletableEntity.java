package com.team3.deokhugam.global.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import java.time.Instant;
import lombok.Getter;

@Getter
@MappedSuperclass
public abstract class SoftDeletableEntity extends BaseEntity {

  @Column(name = "deleted_at")
  private Instant deletedAt;

  public void softDelete() {
    if (this.deletedAt == null) {
      this.deletedAt = Instant.now();
    }
  }

  public boolean isDeleted() {
    return deletedAt != null;
  }
}
