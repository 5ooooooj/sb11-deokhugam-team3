package com.team3.deokhugam.batch.scheduler;

import com.team3.deokhugam.batch.exception.BatchJobExecutionException;
import com.team3.deokhugam.batch.global.Period;
import com.team3.deokhugam.dto.dashboard.PopularReviewDto;
import com.team3.deokhugam.repository.dashboard.PopularReviewRepository;
import com.team3.deokhugam.service.notification.NotificationService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionException;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class DashboardBatchScheduler {

  private static final ZoneId KST = ZoneId.of("Asia/Seoul");

  private final PopularReviewRepository popularReviewRepository;
  private final NotificationService notificationService;
  private final JobLauncher jobLauncher;
  private final Job popularBookDailyJob;
  private final Job popularBookWeeklyJob;
  private final Job popularBookMonthlyJob;
  private final Job popularBookAllTimeJob;
  private final Job popularReviewDailyJob;
  private final Job popularReviewWeeklyJob;
  private final Job popularReviewMonthlyJob;
  private final Job popularReviewAllTimeJob;
  private final Job powerUserDailyJob;
  private final Job powerUserWeeklyJob;
  private final Job powerUserMonthlyJob;
  private final Job powerUserAllTimeJob;

  public DashboardBatchScheduler(
      PopularReviewRepository popularReviewRepository,
      NotificationService notificationService,
      JobLauncher jobLauncher,
      @Qualifier("popularBookDailyJob") Job popularBookDailyJob,
      @Qualifier("popularBookWeeklyJob") Job popularBookWeeklyJob,
      @Qualifier("popularBookMonthlyJob") Job popularBookMonthlyJob,
      @Qualifier("popularBookAllTimeJob") Job popularBookAllTimeJob,
      @Qualifier("popularReviewDailyJob") Job popularReviewDailyJob,
      @Qualifier("popularReviewWeeklyJob") Job popularReviewWeeklyJob,
      @Qualifier("popularReviewMonthlyJob") Job popularReviewMonthlyJob,
      @Qualifier("popularReviewAllTimeJob") Job popularReviewAllTimeJob,
      @Qualifier("powerUserDailyJob") Job powerUserDailyJob,
      @Qualifier("powerUserWeeklyJob") Job powerUserWeeklyJob,
      @Qualifier("powerUserMonthlyJob") Job powerUserMonthlyJob,
      @Qualifier("powerUserAllTimeJob") Job powerUserAllTimeJob) {
    this.popularReviewRepository = popularReviewRepository;
    this.notificationService = notificationService;
    this.jobLauncher = jobLauncher;
    this.popularBookDailyJob = popularBookDailyJob;
    this.popularBookWeeklyJob = popularBookWeeklyJob;
    this.popularBookMonthlyJob = popularBookMonthlyJob;
    this.popularBookAllTimeJob = popularBookAllTimeJob;
    this.popularReviewDailyJob = popularReviewDailyJob;
    this.popularReviewWeeklyJob = popularReviewWeeklyJob;
    this.popularReviewMonthlyJob = popularReviewMonthlyJob;
    this.popularReviewAllTimeJob = popularReviewAllTimeJob;
    this.powerUserDailyJob = powerUserDailyJob;
    this.powerUserWeeklyJob = powerUserWeeklyJob;
    this.powerUserMonthlyJob = powerUserMonthlyJob;
    this.powerUserAllTimeJob = powerUserAllTimeJob;
  }

  @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul")
  public List<String> runDashboardBatch() {
    List<String> failedJobs = new ArrayList<>();

    JobParameters params = new JobParametersBuilder()
        .addLocalDate("targetDate", LocalDate.now(KST))
        .addLocalDateTime("runAt", LocalDateTime.now(KST))
        .toJobParameters();

    // 인기 도서
    for (Job job : List.of(popularBookDailyJob, popularBookWeeklyJob,
        popularBookMonthlyJob, popularBookAllTimeJob)) {
      try {
        runJob(job, params);
      } catch (BatchJobExecutionException e) {
        log.error("[배치] 인기 도서 Job 실패 - {}: {}", job.getName(), e.getMessage());
        failedJobs.add(job.getName());
      }
    }

    // 인기 리뷰
    boolean popularReviewJobsSucceeded = true;
    for (Job job : List.of(popularReviewDailyJob, popularReviewWeeklyJob, popularReviewMonthlyJob, popularReviewAllTimeJob)) {
      try {
        runJob(job, params);
      } catch (BatchJobExecutionException e) {
        log.error("[배치] 인기 리뷰 Job 실패 - {}: {}", job.getName(), e.getMessage());
        failedJobs.add(job.getName());
        popularReviewJobsSucceeded = false; // 하나라도 실패하면 알림 발송 차단용
      }
    }

    // 파워 유저
    for (Job job : List.of(powerUserDailyJob, powerUserWeeklyJob, powerUserMonthlyJob, powerUserAllTimeJob)) {
      try {
        runJob(job, params);
      } catch (BatchJobExecutionException e) {
        log.error("[배치] 파워 유저 Job 실패 - {}: {}", job.getName(), e.getMessage());
        failedJobs.add(job.getName());
      }
    }

    if (popularReviewJobsSucceeded) {
      try {
        sendRankingNotifications();
      } catch (Exception e) {
        log.error("[배치] 랭킹 알림 발송 단계 전체 오류 발생", e);
        failedJobs.add("NOTIFICATION_STAGE_FAILED");
      }
    } else {
      log.warn("[배치] 인기 리뷰 배치 중 실패 항목이 존재하여 랭킹 알림 발송(sendRankingNotifications)을 건너뜁니다.");
    }

    return failedJobs;
  }

  private void runJob(Job job, JobParameters params) {
    try {
      JobExecution execution = jobLauncher.run(job, params);
      if (!execution.getStatus().isUnsuccessful()) {
        return;
      }
      log.error("[배치] Job 실패 - {}: {}", job.getName(), execution.getStatus());
      throw new BatchJobExecutionException(job.getName());
    } catch (JobInstanceAlreadyCompleteException e) {
      log.warn("[배치] 이미 완료된 Job - {}", job.getName());
    } catch (JobExecutionException e) {
      log.error("[배치] Job 실행 실패 - {}: {}", job.getName(), e.getMessage());
      throw new BatchJobExecutionException(job.getName());
    }
  }

  private void sendRankingNotifications() {
    for (Period period : Period.values()) {
      try {
        List<PopularReviewDto> top10 = popularReviewRepository
            .findPopularReviewsByPeriod(period, PageRequest.of(0, 10));

        for (PopularReviewDto review : top10) {
          try {
            notificationService.createRankingNotification(review.reviewId(), period.name());
          } catch (Exception e) {
            log.error("[배치] 랭킹 개별 알림 발송 실패 - period: {}, reviewId: {}, error: {}",
                period, review.reviewId(), e.getMessage());
          }
        }
      } catch (Exception e) {
        // 특정 Period 조회 중 DB 에러가 나더라도 catch하고 다음 Period로
        log.error("[배치] 랭킹 알림 조회 및 생성 실패 - 특정 기간 데이터 처리 불가 period: {}, error: {}",
            period, e.getMessage());
      }
    }
  }
}
