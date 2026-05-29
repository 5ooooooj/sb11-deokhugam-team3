package com.team3.deokhugam.repository.review;

import com.team3.deokhugam.domain.review.Review;
import com.team3.deokhugam.dto.review.ReviewSearchRequest;
import java.util.List;

public interface ReviewRepositoryCustom {

  List<Review> search(ReviewSearchRequest request);

  long count(ReviewSearchRequest request);
}