package knr.shaunmanager.calendar;

import javax.persistence.*;

@Entity
@Table(name = "leaves")
public class Leave {

    @Id
    @Column(name = "work_dt", length = 8, nullable = false)
    private String workDt; // YYYYMMDD

    @Column(name = "leave_type", length = 10, nullable = false)
    private String leaveType; // annual | am | pm

    @Column(name = "location_cd", length = 20, nullable = false)
    private String locationCd; // 외부현장 | 본사

    @Column(name = "work_shift", length = 10)
    private String workShift;
    public String getWorkShift() { return WorkShift.from(workShift).code; }
    public void setWorkShift(String value) { workShift = WorkShift.from(value).code; }

    // Nullable columns preserve automatic execution for existing records.
    @Column(name = "skip_check_in")
    private Boolean skipCheckIn;
    @Column(name = "skip_check_out")
    private Boolean skipCheckOut;
    public boolean isSkipCheckIn() { return Boolean.TRUE.equals(skipCheckIn); }
    public boolean isSkipCheckOut() { return Boolean.TRUE.equals(skipCheckOut); }
    public void setSkipCheckIn(Boolean value) { skipCheckIn = value; }
    public void setSkipCheckOut(Boolean value) { skipCheckOut = value; }

    protected Leave() {}

    public Leave(String workDt, String leaveType, String locationCd) {
        this.workDt = workDt;
        this.leaveType = leaveType;
        this.locationCd = locationCd;
    }

    public String getWorkDt()      { return workDt; }
    public String getLeaveType()   { return leaveType; }
    public String getLocationCd()  { return locationCd; }
    public void setLeaveType(String v)  { this.leaveType = v; }
    public void setLocationCd(String v) { this.locationCd = v; }
}
