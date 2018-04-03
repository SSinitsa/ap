import java.io.FileInputStream;
import java.io.IOException;
import java.util.Calendar;
import java.util.Properties;

public class Run {

	private static final String PATH_TO_RECOURCES = "./resources";

	public static void main(String[] args) throws IOException {

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

		if (time.matches("\\d")) {
			do {
				long timer = Long.parseLong(time)*60*60;
				parser.parse();
				for (;timer>0; timer--) {
					System.out.print("\r");
					System.out.print("Wait " + timer + "seconds..." );
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				try {
					Thread.sleep(Long.parseLong(time)*1000*60*60);
				} catch (NumberFormatException | InterruptedException e) {
					System.out.println("Time error \n" + e);
					return;
				}
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
