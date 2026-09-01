package com.nailconnect.api.applications;

import com.nailconnect.api.security.UserPrincipal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController @RequestMapping("/api/v1/applications")
public class ApplicationController {
  private final JdbcTemplate db; public ApplicationController(JdbcTemplate db){this.db=db;}
  @PostMapping @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasRole('TECHNICIAN')") @Transactional
  public ApplicationView apply(@AuthenticationPrincipal UserPrincipal user,@Valid @RequestBody ApplyRequest r){Integer active=db.queryForObject("select count(*) from jobs where id=? and status='ACTIVE' and (expires_at is null or expires_at>now())",Integer.class,r.jobId());if(active==null||active==0)throw new ResponseStatusException(HttpStatus.CONFLICT,"Job is not accepting applications");UUID id=UUID.randomUUID();try{db.update("insert into applications(id,job_id,technician_id,message) values(?,?,?,?)",id,r.jobId(),user.id(),r.message());}catch(Exception e){throw new ResponseStatusException(HttpStatus.CONFLICT,"Already applied to this job");}return get(id,user);}
  @GetMapping("/mine") @PreAuthorize("hasRole('TECHNICIAN')") public List<ApplicationView> mine(@AuthenticationPrincipal UserPrincipal user){return db.query(baseSql()+" where a.technician_id=? order by a.created_at desc",(rs,n)->map(rs),user.id());}
  @GetMapping("/job/{jobId}") @PreAuthorize("hasRole('SALON_OWNER')") public List<ApplicationView> forJob(@AuthenticationPrincipal UserPrincipal user,@PathVariable UUID jobId){Integer allowed=db.queryForObject("select count(*) from jobs j join salons s on s.id=j.salon_id where j.id=? and s.owner_id=?",Integer.class,jobId,user.id());if(allowed==null||allowed==0)throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Not authorized");return db.query(baseSql()+" where a.job_id=? order by a.created_at desc",(rs,n)->map(rs),jobId);}
  @PatchMapping("/{id}/status") @PreAuthorize("hasRole('SALON_OWNER')") @Transactional public ApplicationView status(@AuthenticationPrincipal UserPrincipal user,@PathVariable UUID id,@Valid @RequestBody StatusRequest r){int changed=db.update("update applications a set status=cast(? as application_status),updated_at=now() from jobs j join salons s on s.id=j.salon_id where a.job_id=j.id and a.id=? and s.owner_id=?",r.status(),id,user.id());if(changed==0)throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Application not found or not authorized");return get(id,user);}
  private ApplicationView get(UUID id,UserPrincipal user){var rows=db.query(baseSql()+" where a.id=? and (a.technician_id=? or exists(select 1 from jobs x join salons s on s.id=x.salon_id where x.id=a.job_id and s.owner_id=?))",(rs,n)->map(rs),id,user.id(),user.id());if(rows.isEmpty())throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Application not found");return rows.getFirst();}
  private String baseSql(){return "select a.id,a.job_id,a.technician_id,u.display_name,j.title,a.message,a.status::text,a.created_at,a.updated_at from applications a join users u on u.id=a.technician_id join jobs j on j.id=a.job_id";}
  private ApplicationView map(java.sql.ResultSet r)throws java.sql.SQLException{return new ApplicationView(r.getObject("id",UUID.class),r.getObject("job_id",UUID.class),r.getObject("technician_id",UUID.class),r.getString("display_name"),r.getString("title"),r.getString("message"),r.getString("status"),r.getTimestamp("created_at").toInstant(),r.getTimestamp("updated_at").toInstant());}
  public record ApplyRequest(@NotNull UUID jobId,@Size(max=2000) String message){}
  public record StatusRequest(@Pattern(regexp="VIEWED|CONTACTED|INTERVIEW|OFFER|HIRED|REJECTED") String status){}
  public record ApplicationView(UUID id,UUID jobId,UUID technicianId,String technicianName,String jobTitle,String message,String status,Instant createdAt,Instant updatedAt){}
}
