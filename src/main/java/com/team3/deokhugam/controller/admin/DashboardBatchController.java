package com.team3.deokhugam.controller.admin;

import com.team3.deokhugam.batch.scheduler.DashboardBatchScheduler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
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
  private final Job popularReviewDailyJob;
  private final Job popularReviewWeeklyJob;
  private final Job popularReviewMonthlyJob;
  private final Job popularReviewAllTimeJob;
  private final Job powerUserDailyJob;
  private final Job powerUserWeeklyJob;
  private final Job powerUserMonthlyJob;
  private final Job powerUserAllTimeJob;
  private final DashboardBatchScheduler dashboardBatchScheduler;

  private static final ZoneId KST = ZoneId.of("Asia/Seoul");

  public DashboardBatchController(
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
      @Qualifier("powerUserAllTimeJob") Job powerUserAllTimeJob,
      DashboardBatchScheduler dashboardBatchScheduler) {
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
    this.dashboardBatchScheduler = dashboardBatchScheduler;
  }

  @Operation(summary = "인기 도서 배치 수동 실행", description = "인기 도서 배치들을 수동으로 순차 실행합니다.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "모든 인기 도서 배치 성공"),
      @ApiResponse(responseCode = "500", description = "일부 또는 전체 배치 실패")
  })
  @PostMapping("/popular-books")
  public ResponseEntity<String> runPopularBookJob() {
    JobParameters params = createStandardParameters();
    List<String> failed = new ArrayList<>();

    for (Job job : List.of(popularBookDailyJob, popularBookWeeklyJob, popularBookMonthlyJob, popularBookAllTimeJob)) {
      executeJob(job, params, failed);
    }

    return handleBatchResult("인기 도서 배치 실행 완료", "인기 도서 배치 일부/전체 실패: ", failed);
  }

  @Operation(summary = "인기 리뷰 배치 수동 실행", description = "인기 리뷰 배치들을 수동으로 순차 실행합니다.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "모든 인기 리뷰 배치 성공"),
      @ApiResponse(responseCode = "500", description = "일부 또는 전체 배치 실패")
  })
  @PostMapping("/popular-reviews")
  public ResponseEntity<String> runPopularReviewJob() {
    JobParameters params = createStandardParameters();
    List<String> failed = new ArrayList<>();

    for (Job job : List.of(popularReviewDailyJob, popularReviewWeeklyJob, popularReviewMonthlyJob, popularReviewAllTimeJob)) {
      executeJob(job, params, failed);
    }

    return handleBatchResult("인기 리뷰 배치 실행 완료", "인기 리뷰 배치 일부/전체 실패: ", failed);
  }

  @Operation(summary = "인기 유저 배치 수동 실행", description = "인기 유저 배치들을 수동으로 순차 실행합니다.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "모든 파워 유저 배치 성공"),
      @ApiResponse(responseCode = "500", description = "일부 또는 전체 배치 실패")
  })
  @PostMapping("/power-users")
  public ResponseEntity<String> runPowerUserJob() {
    JobParameters params = createStandardParameters();
    List<String> failed = new ArrayList<>();

    for (Job job : List.of(powerUserDailyJob, powerUserWeeklyJob, powerUserMonthlyJob, powerUserAllTimeJob)) {
      executeJob(job, params, failed);
    }

    return handleBatchResult("파워 유저 배치 실행 완료", "파워 유저 배치 일부/전체 실패: ", failed);
  }

  @Operation(summary = "전체 배치 실행", description = "대시보드와 관련된 모든 배치를 순차 실행합니다.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "전체 대시보드 배치 성공"),
      @ApiResponse(responseCode = "500", description = "일부 또는 전체 배치 실패")
  })
  @PostMapping("/dashboard")
  public ResponseEntity<String> runDashboardBatch() {
    try {
      // 스케줄러가 실패한 Job 이름 리스트를 반환하도록 계약을 변경합니다.
      List<String> failed = dashboardBatchScheduler.runDashboardBatch();

      return handleBatchResult("전체 대시보드 배치 실행 완료", "전체 대시보드 배치 중 일부 실패: ", failed);
    } catch (Exception e) {
      log.error("[배치] 전체 대시보드 스케줄러 실행 중 시스템 예외 발생", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body("전체 배치 실행 중 예외 발생: " + e.getMessage());
    }
  }

  private JobParameters createStandardParameters() {
    return new JobParametersBuilder()
        .addLocalDate("targetDate", LocalDate.now(KST))
        .addLong("timestamp", System.currentTimeMillis())
        .toJobParameters();
  }

  private void executeJob(Job job, JobParameters params, List<String> failedList) {
    try {
      JobExecution execution = jobLauncher.run(job, params);
      if (execution.getStatus().isUnsuccessful()) {
        failedList.add(job.getName());
        log.error("[배치] {} 실패 - Status: {}", job.getName(), execution.getStatus());
      }
    } catch (Exception e) {
      failedList.add(job.getName());
      log.error("[배치] {} 실행 중 예외 발생", job.getName(), e);
    }
  }

  private ResponseEntity<String> handleBatchResult(String successMessage, String failMessagePrefix, List<String> failedList) {
    if (failedList.isEmpty()) {
      return ResponseEntity.ok(successMessage);
    }
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(failMessagePrefix + failedList);
  }
}