import java.io.FileInputStream;
import java.io.IOException;
import java.util.Calendar;
import java.util.Properties;

public class Run {

	private static final String PATH_TO_RECOURCES = "src/main/resources";

	public static void main(String[] args) throws IOException {
		
		if (License.isExpired()) {
			System.out.println("YOUR LICENSE IS EXPIRED");
			return;
		}

		Properties config = new Properties();
		try {
			config.load(new FileInputStream(PATH_TO_RECOURCES + "/config.txt"));
		} catch (IOException e) {
			System.out.println(e + " /load properties");
		}
		HTMLParser parser = new HTMLParser(config);

		String time = config.getProperty("start.time");
		if (time == null || time.isEmpty()) {
			parser.parse();
			return;
		}

		if (time.equals("0")) {
			do {
				parser.parse();
			} while (true);
		}

		Calendar calendar;
		String separator = ":";
		if (time.contains("."))
			separator = ".";
		int hours = Integer.parseInt(time.substring(0, time.indexOf(separator)));
		int minutes = Integer.parseInt(time.substring(time.indexOf(separator) + 1));
		int currentHours;
		int currentMinutes;
		System.out.println("START TIME IS " + time);
		do {
			calendar = Calendar.getInstance();
			currentHours = calendar.get(Calendar.HOUR_OF_DAY);
			currentMinutes = calendar.get(Calendar.MINUTE);
			System.out.print("\r");
			if (currentMinutes < 10) {
				System.out.print("CURRENT TIME IS " + currentHours + ":0" + currentMinutes);
			} else {
				{
					System.out.print("CURRENT TIME IS " + currentHours + ":" + currentMinutes);
				}
				if (currentHours == hours && currentMinutes == minutes) {
					parser.parse();
				}
				try {
					Thread.sleep(19000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		} while (true);

		// Thread[] threads = new Thread[1];
		// for (int k = 0; k < threads.length; k++) {
		// threads[k] = new Thread(new HTMLParser(), String.format("Thread %d", k));
		// }
		// for (Thread t : threads) t.start();

	}
}
