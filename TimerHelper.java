import java.util.TimerTask;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;

public class TimerHelper extends TimerTask {

	private static final Object fileLock = new Object();

	public static void log(String format, Object... args) {
		synchronized (fileLock) {
			try (PrintWriter out = new PrintWriter(new FileWriter("/logs/log.txt", true))) {
				out.printf("%s [%s] [TimerHelper]", java.time.LocalDateTime.now(), Thread.currentThread().getName());
				out.printf(format + "%n", args);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}

	private MasterServer m;

	public TimerHelper(MasterServer m) {
		this.m = m;
		TimerHelper.log("Instance of TimerHelper was created");
	}

	public void run() {
		m.healthCheck();
		TimerHelper.log("TimerHelper is running health check");
	}

}
