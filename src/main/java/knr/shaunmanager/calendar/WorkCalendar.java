package knr.shaunmanager.calendar;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import java.io.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Component
public class WorkCalendar {
    private final Map<String,String> holidays;
    public WorkCalendar(ObjectMapper mapper) throws IOException {
        try (InputStream stream=new ClassPathResource("holidays.json").getInputStream()) {
            holidays=Collections.unmodifiableMap(mapper.readValue(stream,new TypeReference<Map<String,String>>(){}));
        }
    }
    public Map<String,String> holidays(){return holidays;}
    public boolean isWorkday(LocalDate date) {
        return date.getDayOfWeek()!=DayOfWeek.SATURDAY && date.getDayOfWeek()!=DayOfWeek.SUNDAY
            && !holidays.containsKey(date.format(DateTimeFormatter.BASIC_ISO_DATE));
    }
}
