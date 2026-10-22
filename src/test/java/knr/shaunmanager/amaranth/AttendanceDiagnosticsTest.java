package knr.shaunmanager.amaranth;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class AttendanceDiagnosticsTest {
 @Test void logsStageAndPreservesExceptionWithoutSecretMessage() {
  ch.qos.logback.classic.Logger logger=(ch.qos.logback.classic.Logger)org.slf4j.LoggerFactory.getLogger(AttendanceDiagnostics.class);
  ch.qos.logback.core.read.ListAppender<ch.qos.logback.classic.spi.ILoggingEvent> appender=new ch.qos.logback.core.read.ListAppender<>();
  appender.start();logger.addAppender(appender);
  try {
   RuntimeException failure=new IllegalStateException("token=SECRET");
   assertSame(failure,assertThrows(RuntimeException.class,()->AttendanceDiagnostics.step("LOCAL_SAVE",()->{throw failure;})));
   String logs=appender.list.stream().map(e->e.getFormattedMessage()).reduce("",(a,b)->a+b);
   assertTrue(logs.contains("stage=LOCAL_SAVE status=FAILED"));
   assertFalse(logs.contains("SECRET"));
   assertTrue(appender.list.stream().allMatch(e->e.getThrowableProxy()==null));
  } finally {logger.detachAppender(appender);appender.stop();}
 }
 @Test void onlyKnownCodesAreExposed() {
  assertEquals("LOCAL_CONFIG_UNAVAILABLE", AttendanceDiagnostics.code(new AmaranthLoginService.LoginFailure("LOCAL_CONFIG_UNAVAILABLE")));
  assertEquals("UNEXPECTED_FAILURE", AttendanceDiagnostics.code(new IllegalStateException("password=secret")));
  assertEquals("UNEXPECTED_FAILURE", AttendanceDiagnostics.code(new AmaranthLoginService.LoginFailure("token=secret")));
 }
}
