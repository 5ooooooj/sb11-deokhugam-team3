package com.team3.deokhugam.batch.scheduler;

import com.team3.deokhugam.batch.exception.BatchJobExecutionException;
import com.team3.deokhugam.batch.global.Period;
import com.team3.deokhugam.dto.dashboard.PopularReviewDto;
import com.team3.deokhugam.repository.dashboard.PopularReviewRepository;
import com.team3.deokhugam.service.notification.NotificationService;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionException;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DashboardBatchScheduler {

  private final PopularReviewRepository popularReviewRepository;
  private final NotificationService notificationService;
  private final JobLauncher jobLauncher;
  private final Job popularBookJob;
  private final Job popularReviewJob;
  private final Job powerUserJob;

  @Scheduled(cron = "0 0 0 * * *")
  public void runDashboardBatch() {
    JobParameters params = new JobParametersBuilder()
        .addLocalDate("targetDate", LocalDate.now())
        .toJobParameters();

    try {
      runJob(popularBookJob, params); // 실패시 이후 중단
      runJob(popularReviewJob, params); // 실패시 이후 중단
      runJob(powerUserJob, params);
  } catch (BatchJobExecutionException e) {
      log.error("[배치] Job 실패로 인해 일부 배치가 중단되었습니다: {}", e.getMessage());
    } finally {
      sendRankingNotifications();
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
    try {
      for (Period period : Period.values()) {
        List<PopularReviewDto> top10 = popularReviewRepository
            .findPopularReviewsByPeriod(period, PageRequest.of(0, 10));

        top10.forEach(review ->
            notificationService.createRankingNotification(
                review.reviewId(), period.name()
            )
        );
      }
    } catch (Exception e) {
        log.error("[배치] 랭킹 알림 발송 실패: {}", e.getMessage());
    }
  }
}
