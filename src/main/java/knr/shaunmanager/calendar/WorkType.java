package knr.shaunmanager.calendar;

import java.time.LocalTime;
public enum WorkType {
    WORK("work", "08:20", "18:30"), ANNUAL("annual", null, null),
    AM("am", "13:50", "18:30"), PM("pm", "08:20", "14:20");
    public final String code;
    public final LocalTime checkIn, checkOut;
    WorkType(String code, String in, String out) {
        this.code = code; this.checkIn = in == null ? null : LocalTime.parse(in);
        this.checkOut = out == null ? null : LocalTime.parse(out);
    }
    public static WorkType from(String code) {
        for (WorkType type : values()) if (type.code.equals(code)) return type;
        throw new IllegalArgumentException("INVALID_WORK_TYPE");
    }
}
