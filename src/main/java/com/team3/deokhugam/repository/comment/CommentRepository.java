package com.team3.deokhugam.repository.comment;

import com.team3.deokhugam.domain.comment.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface CommentRepository extends JpaRepository<Comment, UUID> {
}