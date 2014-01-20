package walker;

public class Version {
	private static String major = "3";
	private static String minor = "2";
	private static String build = "0";

	public static String printVersion() {
		return String.format("MAWalkerMOD v%s.%s.%s", major, minor, build);
	}
}