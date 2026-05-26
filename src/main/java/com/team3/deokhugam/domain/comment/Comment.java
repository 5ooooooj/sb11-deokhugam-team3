package com.team3.deokhugam.domain.comment;

import com.team3.deokhugam.domain.base.SoftDeletableEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "comments")
public class Comment extends SoftDeletableEntity {
}