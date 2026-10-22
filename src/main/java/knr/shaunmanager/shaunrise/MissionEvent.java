package knr.shaunmanager.shaunrise;
import javax.persistence.*;
import lombok.Data;
@Entity @Data
public class MissionEvent {
 @Id private String id;
 private String workDate, action, state;
 private int stage, frame;
 private Long messageId;
 @Column(length=4096) private String message;
 public MissionEvent(){}
 public MissionEvent(String id,String date,String action,int stage,String message){this.id=id;this.workDate=date;this.action=action;this.stage=stage;this.message=message;this.state="PENDING";}
}
