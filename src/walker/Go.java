package walker;

import info.BattleDB;

import java.io.ByteArrayOutputStream;
import java.io.Console;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import org.apache.log4j.Logger;

public class Go {
	private static final Logger LOG = Logger.getLogger("");

	public static void main(String[] args) {
		if (args.length != 1) {
			printHelp();
			return;
		}

		// read configuration file
		try {
			GetConfig.parse(Process.ParseXMLBytes(ReadFileAll(args[0]),
					"读取配置文件"));

			// read password, which only runs OK on REAL TERMINAL
			// Eclipse's command line doesn't work
			if ("".equals(Info.LoginPw.trim())) {
				Console console = System.console();

				if (console != null) {
					char[] password = console.readPassword("請輸入密碼> ");
					Info.LoginPw = String.valueOf(password);
				}
			}
		} catch (Exception e) {
			Go.log("配置文件錯誤，請檢查", LogType.ERROR);
			e.printStackTrace();
			return;
		}

		// version
		System.out.println(Version.printVersion());

		// cards for sold
		Go.log(String.format("讀取已被玩壞的妹紙 (%d只).", Info.CanBeSold.size()),
				LogType.INFO);

		// main logic
		while (true) {
			Process proc = new Process();

			long time = System.currentTimeMillis();
			while (true) {
				try {
					proc.auto();

					// make list smaller per hour
					if (System.currentTimeMillis() - time >= 3600000) {
						int size = BattleDB.hadBattleArrayList.size();
						BattleDB.hadBattleArrayList = new ArrayList<>(
								BattleDB.hadBattleArrayList.subList(
										Math.max(size - 30, 0), size - 1));

						time = System.currentTimeMillis();
					}
				} catch (Exception ex) {
					LOG.error("", ex);
					Process.info.events.add(Info.EventType.cookieOutOfDate);
					Go.log("重新啟動", LogType.WARN);

					break;
				}
			}
		}
	}

	public static void printHelp() {
		System.out.println(Version.printVersion());
		System.out.println("Usage: java -jar MWalkerMod.jar config.xml");
	}

	public enum LogType {
		DEBUG, INFO, WARN, ERROR
	}

	public static void log(String message, LogType type) {
		if (message == null || "".equals(message.trim())) {
			return;
		}

		if (message.contains("\n") == false) {
			// single-line info
			switch (type) {
			case DEBUG:
				LOG.debug(message);
				break;
			case INFO:
				LOG.info(message);
				break;
			case WARN:
				LOG.warn(message);
				break;
			case ERROR:
				LOG.error(message);
				break;
			default:
				LOG.info(message);
				break;
			}

			return;
		} else {
			// multi-line info
			for (String msg : message.split("\n")) {
				if ("".equals(msg.trim()) == false) {
					switch (type) {
					case DEBUG:
						LOG.debug(msg);
						break;
					case INFO:
						LOG.info(msg);
						break;
					case WARN:
						LOG.warn(msg);
						break;
					case ERROR:
						LOG.error(msg);
						break;
					default:
						LOG.info(msg);
						break;
					}
				}
			}
		}
	}

	public static byte[] ReadFileAll(String path) throws Exception {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		InputStream is = null;
		try {
			is = new FileInputStream(path);
			byte[] b = new byte[2000];
			int n;
			while ((n = is.read(b)) != -1) {
				baos.write(b, 0, n);
			}
		} catch (Exception ex) {
			throw ex;
		} finally {
			try {
				if (is != null)
					is.close();
			} catch (Exception ex) {
				throw ex;
			}
		}

		return baos.toByteArray();
	}
}
