package knr.shaunmanager.calendar;
import javax.persistence.*;

@Entity
@Table(name="attendance_plan")
public class AttendancePlan {
    @Id @Column(length=8) private String workDt;
    private String workType, locationCd;
    private String inTarget, outTarget;
    private String inState="PENDING", outState="PENDING";
    @Column(name = "work_shift", length = 10)
    private String workShift;
    public String getWorkShift() { return WorkShift.from(workShift).code; }
    public void setWorkShift(String value) { workShift = WorkShift.from(value).code; }

    protected AttendancePlan(){}
    public AttendancePlan(String date){workDt=date;}
    public String getWorkDt(){return workDt;}
    public String getWorkType(){return workType;} public void setWorkType(String v){workType=v;}
    public String getLocationCd(){return locationCd;} public void setLocationCd(String v){locationCd=v;}
    public String getInTarget(){return inTarget;} public void setInTarget(String v){inTarget=v;}
    public String getOutTarget(){return outTarget;} public void setOutTarget(String v){outTarget=v;}
    public String getInState(){return inState;} public void setInState(String v){inState=v;}
    public String getOutState(){return outState;} public void setOutState(String v){outState=v;}
}
