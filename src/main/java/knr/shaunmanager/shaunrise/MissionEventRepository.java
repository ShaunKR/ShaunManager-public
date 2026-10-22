package knr.shaunmanager.shaunrise;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface MissionEventRepository extends JpaRepository<MissionEvent,String> {
 List<MissionEvent> findByStateOrderByIdAsc(String state);
 List<MissionEvent> findByAction(String action);
}
