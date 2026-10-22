package knr.shaunmanager.calendar;

/** Missing values from older records and clients retain the day shift. */
public enum WorkShift {
    DAY("day"), NIGHT("night");
    public final String code;
    WorkShift(String code) { this.code = code; }
    public void validate(WorkType type) {
        if (this == NIGHT && (type == WorkType.AM || type == WorkType.PM))
            throw new IllegalArgumentException("NIGHT_HALF_DAY_NOT_SUPPORTED");
    }
    public java.time.LocalTime checkIn(WorkType type) {
        validate(type);
        return this == NIGHT ? java.time.LocalTime.of(13, 50) : type.checkIn;
    }
    public java.time.LocalTime checkOut(WorkType type) {
        validate(type);
        return this == NIGHT ? java.time.LocalTime.of(23, 0) : type.checkOut;
    }
    public int earliestOffset(boolean checkIn) { return this == NIGHT && !checkIn ? 0 : -5; }
    public static WorkShift from(String code) {
        if (code == null || code.isEmpty()) return DAY;
        for (WorkShift shift : values()) if (shift.code.equals(code)) return shift;
        throw new IllegalArgumentException("INVALID_WORK_SHIFT");
    }
}
