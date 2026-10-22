package knr.shaunmanager.calendar;

import javax.persistence.*;

@Entity
@Table(name = "attendance_log")
public class AttendanceLog {

    @Id
    @Column(name = "work_dt", length = 8, nullable = false)
    private String workDt; // YYYYMMDD

    @Column(name = "check_in_tm", length = 6)
    private String checkInTm;  // HHmmss

    @Column(name = "check_out_tm", length = 6)
    private String checkOutTm; // HHmmss

    @Column(name = "location_cd", length = 20)
    private String locationCd;

    @Column(name = "check_in_success")
    private Boolean checkInSuccess;

    @Column(name = "check_out_success")
    private Boolean checkOutSuccess;

    @Column(name = "error_msg", length = 200)
    private String errorMsg;

    protected AttendanceLog() {}

    public AttendanceLog(String workDt, String locationCd) {
        this.workDt = workDt;
        this.locationCd = locationCd;
    }

    public String getWorkDt()          { return workDt; }
    public String getCheckInTm()       { return checkInTm; }
    public String getCheckOutTm()      { return checkOutTm; }
    public String getLocationCd()      { return locationCd; }
    public Boolean getCheckInSuccess() { return checkInSuccess; }
    public Boolean getCheckOutSuccess(){ return checkOutSuccess; }
    public String getErrorMsg()        { return errorMsg; }

    public void setCheckInTm(String v)        { this.checkInTm = v; }
    public void setCheckOutTm(String v)       { this.checkOutTm = v; }
    public void setLocationCd(String v)       { this.locationCd = v; }
    public void setCheckInSuccess(Boolean v)  { this.checkInSuccess = v; }
    public void setCheckOutSuccess(Boolean v) { this.checkOutSuccess = v; }
    public void setErrorMsg(String v)         { this.errorMsg = v; }
}
