package com.nailconnect.api.jobs;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

@Service
public class JobService {
  private final JdbcTemplate db; public JobService(JdbcTemplate db){this.db=db;}
  public List<JobController.JobView> search(double lat,double lng,int radius,String skill,int limit){String sql="""
    select j.id,j.salon_id,s.name,s.verified,j.title,j.description,j.employment_type,j.payment_type::text,j.compensation_min,j.compensation_max,j.compensation_unit,j.commission_percent,j.schedule,j.minimum_experience_years,j.license_required,j.status::text,
    ST_Distance(s.location,ST_SetSRID(ST_MakePoint(?,?),4326)::geography)/1609.344 distance_miles,
    coalesce(array_agg(sk.name) filter(where sk.name is not null),'{}') skills,j.created_at,j.expires_at
    from jobs j join salons s on s.id=j.salon_id left join job_skills js on js.job_id=j.id left join skills sk on sk.id=js.skill_id
    where j.status='ACTIVE' and (j.expires_at is null or j.expires_at>now()) and ST_DWithin(s.location,ST_SetSRID(ST_MakePoint(?,?),4326)::geography,?*1609.344)
    and (? is null or exists(select 1 from job_skills x join skills y on y.id=x.skill_id where x.job_id=j.id and lower(y.name)=lower(?)))
    group by j.id,s.id order by distance_miles,j.created_at desc limit ?
    """;return db.query(sql,(rs,n)->map(rs),lng,lat,lng,lat,radius,skill,skill,limit);}
  public JobController.JobView get(UUID id){var rows=db.query("select j.id,j.salon_id,s.name,s.verified,j.title,j.description,j.employment_type,j.payment_type::text,j.compensation_min,j.compensation_max,j.compensation_unit,j.commission_percent,j.schedule,j.minimum_experience_years,j.license_required,j.status::text,0.0 distance_miles,coalesce(array_agg(sk.name) filter(where sk.name is not null),'{}') skills,j.created_at,j.expires_at from jobs j join salons s on s.id=j.salon_id left join job_skills js on js.job_id=j.id left join skills sk on sk.id=js.skill_id where j.id=? group by j.id,s.id",(rs,n)->map(rs),id);if(rows.isEmpty())throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Job not found");return rows.getFirst();}
  @Transactional public JobController.JobView create(UUID owner,JobController.CreateJobRequest r){Integer allowed=db.queryForObject("select count(*) from salons where id=? and owner_id=?",Integer.class,r.salonId(),owner);if(allowed==null||allowed==0)throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Not authorized for this salon");UUID id=UUID.randomUUID();String status=r.publish()?"ACTIVE":"DRAFT";db.update("insert into jobs(id,salon_id,created_by,title,description,employment_type,payment_type,compensation_min,compensation_max,compensation_unit,commission_percent,schedule,minimum_experience_years,license_required,status,expires_at) values(?,?,?,?,?,?,cast(? as payment_type),?,?,?,?,?,?,?,cast(? as job_status),?)",id,r.salonId(),owner,r.title(),r.description(),r.employmentType(),r.paymentType(),r.compensationMin(),r.compensationMax(),r.compensationUnit(),r.commissionPercent(),r.schedule(),r.minimumExperienceYears(),r.licenseRequired(),status,r.expiresAt());if(r.skills()!=null)for(String skill:r.skills())db.update("insert into job_skills(job_id,skill_id) select ?,id from skills where lower(name)=lower(?) on conflict do nothing",id,skill);return get(id);}
  @Transactional public JobController.JobView updateStatus(UUID owner,UUID id,String status){int changed=db.update("update jobs j set status=cast(? as job_status),updated_at=now() from salons s where j.salon_id=s.id and j.id=? and s.owner_id=?",status,id,owner);if(changed==0)throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Job not found or not authorized");return get(id);}
  private JobController.JobView map(ResultSet r)throws SQLException{Array a=r.getArray("skills");String[] skills=a==null?new String[0]:(String[])a.getArray();return new JobController.JobView(r.getObject("id",UUID.class),r.getObject("salon_id",UUID.class),r.getString("name"),r.getBoolean("verified"),r.getString("title"),r.getString("description"),r.getString("employment_type"),r.getString("payment_type"),r.getBigDecimal("compensation_min"),r.getBigDecimal("compensation_max"),r.getString("compensation_unit"),r.getBigDecimal("commission_percent"),r.getString("schedule"),r.getInt("minimum_experience_years"),r.getBoolean("license_required"),r.getString("status"),r.getDouble("distance_miles"),List.of(skills),r.getTimestamp("created_at").toInstant(),r.getTimestamp("expires_at")==null?null:r.getTimestamp("expires_at").toInstant());}
}
