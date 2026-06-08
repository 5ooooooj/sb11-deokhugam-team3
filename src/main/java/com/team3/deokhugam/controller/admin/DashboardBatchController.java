package com.team3.deokhugam.controller.admin;

import com.team3.deokhugam.batch.scheduler.DashboardBatchScheduler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/batch")
@RequiredArgsConstructor
@Tag(name = "배치 수동 실행 API")
public class DashboardBatchController {

  private final JobLauncher jobLauncher;
  private final Job popularReviewJob;
  private final Job popularBookJob;
  private final Job powerUserJob;
  private final DashboardBatchScheduler dashboardBatchScheduler;

  @Operation(summary = "인기 도서 배치 수동 실행", description = "인기 도서 배치 수동 실행")
  @ApiResponse(responseCode = "200", description = "배치 성공")
  @PostMapping("/popular-books")
  public ResponseEntity<String> runPopularBookJob() throws Exception {
    JobParameters params = new JobParametersBuilder()
        .addLocalDate("targetDate", LocalDate.now())
        .addLong("timestamp", System.currentTimeMillis())
        .toJobParameters();

    jobLauncher.run(popularBookJob, params);
    return ResponseEntity.ok("인기 도서 배치 실행 완료");
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

  @PostMapping("/dashboard")
  public ResponseEntity<String> runDashboardBatch() {
    dashboardBatchScheduler.runDashboardBatch();
    return ResponseEntity.ok("배치 실행 완료");
  }

}
