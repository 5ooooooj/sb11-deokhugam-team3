package com.team3.deokhugam.batch.scheduler;

import com.team3.deokhugam.batch.exception.BatchJobExecutionException;
import com.team3.deokhugam.batch.global.Period;
import com.team3.deokhugam.dto.dashboard.PopularReviewDto;
import com.team3.deokhugam.repository.dashboard.PopularReviewRepository;
import com.team3.deokhugam.service.notification.NotificationService;
import java.time.LocalDate;
import java.time.ZoneId;
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
  private final Job popularReviewJob;
  private final Job powerUserJob;

  public DashboardBatchScheduler(
      PopularReviewRepository popularReviewRepository,
      NotificationService notificationService,
      JobLauncher jobLauncher,
      @Qualifier("popularBookDailyJob") Job popularBookDailyJob,
      @Qualifier("popularBookWeeklyJob") Job popularBookWeeklyJob,
      @Qualifier("popularBookMonthlyJob") Job popularBookMonthlyJob,
      @Qualifier("popularBookAllTimeJob") Job popularBookAllTimeJob,
      @Qualifier("popularReviewJob") Job popularReviewJob,
      @Qualifier("powerUserJob") Job powerUserJob) {
    this.popularReviewRepository = popularReviewRepository;
    this.notificationService = notificationService;
    this.jobLauncher = jobLauncher;
    this.popularBookDailyJob = popularBookDailyJob;
    this.popularBookWeeklyJob = popularBookWeeklyJob;
    this.popularBookMonthlyJob = popularBookMonthlyJob;
    this.popularBookAllTimeJob = popularBookAllTimeJob;
    this.popularReviewJob = popularReviewJob;
    this.powerUserJob = powerUserJob;
  }

  @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul")
  public void runDashboardBatch() {
    JobParameters params = new JobParametersBuilder()
        .addLocalDate("targetDate", LocalDate.now(KST))
        .toJobParameters();

    boolean popularReviewJobSucceeded = false;
    try {
      runJob(popularBookDailyJob, params);
      runJob(popularBookWeeklyJob, params);
      runJob(popularBookMonthlyJob, params);
      runJob(popularBookAllTimeJob, params);
      runJob(popularReviewJob, params);
      popularReviewJobSucceeded = true;
      runJob(powerUserJob, params);
  } catch (BatchJobExecutionException e) {
      log.error("[배치] Job 실패로 인해 일부 배치가 중단되었습니다: {}", e.getMessage());
    } finally {
      if (popularReviewJobSucceeded) {
        sendRankingNotifications();
      }
    }
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
        List<PopularReviewDto> top10 = popularReviewRepository
            .findPopularReviewsByPeriod(period, PageRequest.of(0, 10));

        for (PopularReviewDto review : top10) {
          try {
            notificationService.createRankingNotification(review.reviewId(), period.name());
          } catch (Exception e) {
            log.error("[배치] 랭킹 알림 발송 실패 - period: {}, reviewId: {}, error: {}",
                period, review.reviewId(), e.getMessage());
          }
        }
    }
  }
}
