package com.team3.deokhugam.controller.admin;

import com.team3.deokhugam.batch.scheduler.DashboardBatchScheduler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/admin/batch")
@Tag(name = "배치 수동 실행 API")
public class DashboardBatchController {

  private final JobLauncher jobLauncher;
  private final Job popularBookDailyJob;
  private final Job popularBookWeeklyJob;
  private final Job popularBookMonthlyJob;
  private final Job popularBookAllTimeJob;
  private final Job popularReviewJob;
  private final Job powerUserJob;
  private final DashboardBatchScheduler dashboardBatchScheduler;

  public DashboardBatchController(
      JobLauncher jobLauncher,
      @Qualifier("popularBookDailyJob") Job popularBookDailyJob,
      @Qualifier("popularBookWeeklyJob") Job popularBookWeeklyJob,
      @Qualifier("popularBookMonthlyJob") Job popularBookMonthlyJob,
      @Qualifier("popularBookAllTimeJob") Job popularBookAllTimeJob,
      @Qualifier("popularReviewJob") Job popularReviewJob,
      @Qualifier("powerUserJob") Job powerUserJob,
      DashboardBatchScheduler dashboardBatchScheduler) {
    this.jobLauncher = jobLauncher;
    this.popularBookDailyJob = popularBookDailyJob;
    this.popularBookWeeklyJob = popularBookWeeklyJob;
    this.popularBookMonthlyJob = popularBookMonthlyJob;
    this.popularBookAllTimeJob = popularBookAllTimeJob;
    this.popularReviewJob = popularReviewJob;
    this.powerUserJob = powerUserJob;
    this.dashboardBatchScheduler = dashboardBatchScheduler;
  }

  @Operation(summary = "인기 도서 배치 수동 실행", description = "인기 도서 배치 수동 실행")
  @ApiResponse(responseCode = "200", description = "배치 성공")
  @PostMapping("/popular-books")
  public ResponseEntity<String> runPopularBookJob() throws Exception {
    JobParameters params = new JobParametersBuilder()
        .addLocalDate("targetDate", LocalDate.now())
        .addLong("timestamp", System.currentTimeMillis())
        .toJobParameters();

    List<String> failed = new ArrayList<>();
    for (Job job : List.of(popularBookDailyJob, popularBookWeeklyJob,
        popularBookMonthlyJob, popularBookAllTimeJob)) {
      try {
        jobLauncher.run(job, params);
      } catch (Exception e) {
        failed.add(job.getName());
        log.error("[배치] 인기 도서 Job 실패 - {}: {}", job.getName(), e.getMessage());
      }
    }

    return failed.isEmpty()
        ? ResponseEntity.ok("인기 도서 배치 실행 완료")
        : ResponseEntity.ok("인기 도서 배치 부분 실패: " + failed);
  }

  @Operation(summary = "인기 리뷰 배치 수동 실행", description = "인기 리뷰 배치 수동 실행")
  @ApiResponse(responseCode = "200", description = "배치 성공")
  @PostMapping("/popular-reviews")
  public ResponseEntity<String> runPopularReviewJob() throws Exception {
    JobParameters params = new JobParametersBuilder()
        .addLocalDate("targetDate", LocalDate.now())
        .addLong("timestamp", System.currentTimeMillis())
        .toJobParameters();

    jobLauncher.run(popularReviewJob, params);
    return ResponseEntity.ok("인기 리뷰 배치 실행 완료");
  }

  @Operation(summary = "인기 유저 배치 수동 실행", description = "인기 유저 배치 수동 실행")
  @ApiResponse(responseCode = "200", description = "배치 성공")
  @PostMapping("/power-users")
  public ResponseEntity<String> runPowerUserJob() throws Exception {
    JobParameters params = new JobParametersBuilder()
        .addLocalDate("targetDate", LocalDate.now())
        .addLong("timestamp", System.currentTimeMillis())
        .toJobParameters();

    jobLauncher.run(powerUserJob, params);
    return ResponseEntity.ok("인기 유저 배치 실행 완료");
  }

  @Operation(summary = "전체 배치 실행", description = "대시보드 배치 전체 수동 실행")
  @PostMapping("/dashboard")
  public ResponseEntity<String> runDashboardBatch() {
    dashboardBatchScheduler.runDashboardBatch();
    return ResponseEntity.ok("배치 실행 완료");
  }

}
