package com.team3.deokhugam.domain.comment;

import com.team3.deokhugam.global.entity.SoftDeletableEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "comments")
public class Comment extends SoftDeletableEntity {
}