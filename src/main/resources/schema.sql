-- extensions --
CREATE EXTENSION IF NOT EXISTS pg_trgm;
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- users 테이블 생성 --
CREATE TABLE IF NOT EXISTS users (
                                     id UUID NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
                                     email VARCHAR(255) UNIQUE NOT NULL,
                                     nickname VARCHAR(20) NOT NULL,
                                     password VARCHAR(255) NOT NULL,
                                     created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                     updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                     deleted_at TIMESTAMPTZ
);

-- books 테이블 생성 --
CREATE TABLE IF NOT EXISTS books (
                                     id UUID NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
                                     title VARCHAR(255) NOT NULL,
                                     author VARCHAR(255) NOT NULL,
                                     description TEXT NOT NULL,
                                     publisher VARCHAR(255) NOT NULL,
                                     published_date DATE NOT NULL,
                                     isbn VARCHAR(20) UNIQUE,
                                     thumbnail_url VARCHAR(500),
                                     review_count INT NOT NULL DEFAULT 0,
                                     rating DECIMAL(3,2) NOT NULL DEFAULT 0.0,
                                     created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                     updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                     deleted_at TIMESTAMPTZ
);

-- reviews 테이블 생성 --
CREATE TABLE IF NOT EXISTS reviews (
                                       id UUID NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
                                       user_id UUID NOT NULL,
                                       book_id UUID NOT NULL,
                                       rating INT NOT NULL,
                                       content TEXT NOT NULL,
                                       like_count INT NOT NULL DEFAULT 0,
                                       comment_count INT NOT NULL DEFAULT 0,
                                       created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                       updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                       deleted_at TIMESTAMPTZ,

                                       CONSTRAINT uk_reviews_user_book UNIQUE (user_id, book_id),
                                       CONSTRAINT fk_reviews_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
                                       CONSTRAINT fk_reviews_book FOREIGN KEY (book_id) REFERENCES books(id) ON DELETE CASCADE,
                                       CONSTRAINT ck_reviews_rating CHECK (rating BETWEEN 1 AND 5)
);

-- comments 테이블 생성 --
CREATE TABLE IF NOT EXISTS comments (
                                        id UUID NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
                                        review_id UUID NOT NULL,
                                        user_id UUID,
                                        content TEXT NOT NULL,
                                        created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                        updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                        deleted_at TIMESTAMPTZ,

                                        CONSTRAINT fk_comments_review FOREIGN KEY (review_id) REFERENCES reviews(id) ON DELETE CASCADE,
                                        CONSTRAINT fk_comments_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL
);

-- review_likes 테이블 생성 --
CREATE TABLE IF NOT EXISTS review_likes (
                                            id UUID NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
                                            review_id UUID NOT NULL,
                                            user_id UUID,
                                            created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

                                            CONSTRAINT uk_review_likes_review_user UNIQUE (review_id, user_id),
                                            CONSTRAINT fk_review_likes_review FOREIGN KEY (review_id) REFERENCES reviews(id) ON DELETE CASCADE,
                                            CONSTRAINT fk_review_likes_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL
);

-- notifications 테이블 생성 --
CREATE TABLE IF NOT EXISTS notifications (
                                             id UUID NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
                                             user_id UUID NOT NULL,
                                             review_id UUID,
                                             type VARCHAR(50) NOT NULL,
                                             message VARCHAR(500) NOT NULL,
                                             confirmed BOOLEAN NOT NULL DEFAULT FALSE,
                                             created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                             updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

                                             CONSTRAINT ck_notifications_type CHECK ( type IN ('COMMENT', 'LIKE', 'POPULAR_REVIEW', 'INTERNAL') ),
                                             CONSTRAINT fk_notifications_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
                                             CONSTRAINT fk_notifications_review FOREIGN KEY (review_id) REFERENCES reviews(id) ON DELETE SET NULL
);

-- popular_books 테이블 생성 --
CREATE TABLE IF NOT EXISTS popular_books (
                                             id UUID NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
                                             book_id UUID NOT NULL,
                                             period VARCHAR(20) NOT NULL,
                                             rank INT NOT NULL,
                                             score DECIMAL(10,4) NOT NULL DEFAULT 0.0,
                                             review_count INT NOT NULL DEFAULT 0,
                                             rating DECIMAL(3,2) NOT NULL DEFAULT 0.0,
                                             calculated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

                                             CONSTRAINT ck_popular_books_period CHECK ( period IN ('DAILY', 'WEEKLY', 'MONTHLY', 'ALL_TIME') ),
                                             CONSTRAINT uk_popular_books_period_rank UNIQUE (period, rank),
                                             CONSTRAINT uk_popular_books_period_book UNIQUE (period, book_id),
                                             CONSTRAINT fk_popular_books_book FOREIGN KEY (book_id) REFERENCES books(id) ON DELETE CASCADE
);

-- popular_reviews 테이블 생성 --
CREATE TABLE IF NOT EXISTS popular_reviews (
                                               id UUID NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
                                               review_id UUID NOT NULL,
                                               period VARCHAR(20) NOT NULL,
                                               rank INT NOT NULL,
                                               score DECIMAL(10,4) NOT NULL DEFAULT 0.0,
                                               like_count INT NOT NULL DEFAULT 0,
                                               comment_count INT NOT NULL DEFAULT 0,
                                               calculated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

                                               CONSTRAINT ck_popular_reviews_period CHECK ( period IN ('DAILY', 'WEEKLY', 'MONTHLY', 'ALL_TIME') ),
                                               CONSTRAINT uk_popular_reviews_period_rank UNIQUE (period, rank),
                                               CONSTRAINT uk_popular_reviews_period_review UNIQUE (period, review_id),
                                               CONSTRAINT fk_popular_reviews_review FOREIGN KEY (review_id) REFERENCES reviews(id) ON DELETE CASCADE
);

-- power_users 테이블 생성 --
CREATE TABLE IF NOT EXISTS power_users (
                                           id UUID NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
                                           user_id UUID NOT NULL,
                                           period VARCHAR(20) NOT NULL,
                                           rank INT NOT NULL,
                                           score DECIMAL(10,4) NOT NULL DEFAULT 0.0,
                                           review_score_sum DECIMAL(10,4) NOT NULL DEFAULT 0.0,
                                           like_count INT NOT NULL DEFAULT 0,
                                           comment_count INT NOT NULL DEFAULT 0,
                                           calculated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

                                           CONSTRAINT ck_power_users_period CHECK (period IN ('DAILY', 'WEEKLY', 'MONTHLY', 'ALL_TIME')),
                                           CONSTRAINT uk_power_users_period_rank UNIQUE (period, rank),
                                           CONSTRAINT uk_power_users_period_user UNIQUE (period, user_id),
                                           CONSTRAINT fk_power_users_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- indexes --

-- users --
-- 논리 삭제 필터링
CREATE INDEX IF NOT EXISTS idx_users_deleted_at ON users(deleted_at);
-- 닉네임 키워드 검색
CREATE INDEX IF NOT EXISTS idx_users_nickname_trgm ON users USING GIN (nickname gin_trgm_ops);

-- books --
-- 논리 삭제 필터링
CREATE INDEX IF NOT EXISTS idx_books_deleted_at ON books(deleted_at);
-- 커서 페이지네이션 정렬 기준
CREATE INDEX IF NOT EXISTS idx_books_title ON books(title, created_at, id);
CREATE INDEX IF NOT EXISTS idx_books_published_date ON books(published_date, created_at, id);
CREATE INDEX IF NOT EXISTS idx_books_rating ON books(rating, created_at, id);
CREATE INDEX IF NOT EXISTS idx_books_review_count ON books(review_count, created_at, id);
-- 키워드 검색
-- LIKE '%keyword%' 검색에 인덱스 적용
CREATE INDEX IF NOT EXISTS idx_books_title_trgm  ON books USING GIN (title  gin_trgm_ops);
CREATE INDEX IF NOT EXISTS idx_books_author_trgm ON books USING GIN (author gin_trgm_ops);
-- 키워드 부분 문자열 검색
CREATE INDEX IF NOT EXISTS idx_books_isbn_trgm   ON books USING GIN (isbn gin_trgm_ops) WHERE isbn IS NOT NULL;

-- reviews --
-- 논리 삭제 필터링
CREATE INDEX IF NOT EXISTS idx_reviews_deleted_at ON reviews(deleted_at);
-- 커서 페이지네이션 정렬
CREATE INDEX IF NOT EXISTS idx_reviews_book_id ON reviews (book_id, created_at, id);
CREATE INDEX IF NOT EXISTS idx_reviews_user_id ON reviews (user_id, created_at, id);
CREATE INDEX IF NOT EXISTS idx_reviews_rating  ON reviews (rating,  created_at, id);
-- 키워드 부분 문자열 검색
CREATE INDEX IF NOT EXISTS idx_reviews_content_trgm ON reviews USING GIN (content gin_trgm_ops);

-- comments --
-- 논리 삭제 필터링
CREATE INDEX IF NOT EXISTS idx_comments_deleted_at ON comments (deleted_at);
-- 리뷰별 댓글 목록 조회
CREATE INDEX IF NOT EXISTS idx_comments_review_id ON comments (review_id, created_at, id);
-- 파워 유저 배치: 사용자별 댓글 수 집계
CREATE INDEX IF NOT EXISTS idx_comments_user_id ON comments (user_id);

-- review_likes --
-- 리뷰별 좋아요 수 집계
CREATE INDEX IF NOT EXISTS idx_review_likes_review_id ON review_likes (review_id);
-- 파워 유저 배치: 사용자별 좋아요 수 집계
CREATE INDEX IF NOT EXISTS idx_review_likes_user_id ON review_likes (user_id);

-- notifications --
-- 사용자별 알림 목록 조회
CREATE INDEX IF NOT EXISTS idx_notifications_user_id ON notifications (user_id, created_at, id);
-- 리뷰 물리 삭제 시 ON DELETE SET NULL 대상 빠른 조회
CREATE INDEX IF NOT EXISTS idx_notifications_review_id ON notifications (review_id);
-- 배치: 확인된 알림 중 1주일 경과된 알림 삭제 (부분 인덱스)
CREATE INDEX IF NOT EXISTS idx_notifications_confirmed_created ON notifications (confirmed, created_at)
    WHERE confirmed = TRUE;

-- popular_books --
-- 기간별 인기 도서 조회
CREATE INDEX IF NOT EXISTS idx_popular_books_period_rank ON popular_books (period, rank, calculated_at, id);
-- 도서 물리 삭제 시 ON DELETE CASCADE 대상 빠른 조회
CREATE INDEX IF NOT EXISTS idx_popular_books_book_id ON popular_books (book_id);

-- popular_reviews --
-- 기간별 인기 리뷰 조회
CREATE INDEX IF NOT EXISTS idx_popular_reviews_period_rank ON popular_reviews (period, rank, calculated_at, id);
-- 리뷰 물리 삭제 시 ON DELETE CASCADE 대상 빠른 조회
CREATE INDEX IF NOT EXISTS idx_popular_reviews_review_id ON popular_reviews (review_id);

-- power_users --
-- 기간별 파워 유저 조회 (커서 페이지네이션, B-Tree)
CREATE INDEX IF NOT EXISTS idx_power_users_period_rank ON power_users (period, rank, calculated_at, id);
-- 사용자 물리 삭제 시 ON DELETE CASCADE 대상 빠른 조회
CREATE INDEX IF NOT EXISTS idx_power_users_user_id ON power_users (user_id);