package com.nailconnect.api.jobs;

import com.nailconnect.api.security.UserPrincipal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController @RequestMapping("/api/v1/jobs")
public class JobController {
  private final JobService service; public JobController(JobService service){this.service=service;}
  @GetMapping public List<JobView> search(@RequestParam double lat,@RequestParam double lng,@RequestParam(defaultValue="25") @Min(1) @Max(100) int radiusMiles,@RequestParam(required=false) String skill,@RequestParam(defaultValue="50") @Min(1) @Max(100) int limit){return service.search(lat,lng,radiusMiles,skill,limit);}
  @GetMapping("/{id}") public JobView get(@PathVariable UUID id){return service.get(id);}
  @PostMapping @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasRole('SALON_OWNER')") public JobView create(@AuthenticationPrincipal UserPrincipal user,@Valid @RequestBody CreateJobRequest r){return service.create(user.id(),r);}
  @PatchMapping("/{id}/status") @PreAuthorize("hasRole('SALON_OWNER')") public JobView updateStatus(@AuthenticationPrincipal UserPrincipal user,@PathVariable UUID id,@RequestBody StatusRequest r){return service.updateStatus(user.id(),id,r.status());}

  public record CreateJobRequest(@NotNull UUID salonId,@NotBlank @Size(max=180) String title,@NotBlank @Size(max=5000) String description,@NotBlank String employmentType,@Pattern(regexp="W2|CONTRACT_1099|CASH_W2") String paymentType,@PositiveOrZero BigDecimal compensationMin,@PositiveOrZero BigDecimal compensationMax,String compensationUnit,@DecimalMin("0") @DecimalMax("100") BigDecimal commissionPercent,@Size(max=250) String schedule,@Min(0) int minimumExperienceYears,boolean licenseRequired,List<String> skills,Instant expiresAt,boolean publish){}
  public record StatusRequest(@Pattern(regexp="ACTIVE|PAUSED|CLOSED") String status){}
  public record JobView(UUID id,UUID salonId,String salonName,boolean salonVerified,String title,String description,String employmentType,String paymentType,BigDecimal compensationMin,BigDecimal compensationMax,String compensationUnit,BigDecimal commissionPercent,String schedule,int minimumExperienceYears,boolean licenseRequired,String status,double distanceMiles,List<String> skills,Instant createdAt,Instant expiresAt){}
}
