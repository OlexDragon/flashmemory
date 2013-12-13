package irt.flash.data;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Logger;

public class Profile {

	private static final Logger logger = (Logger) LogManager.getLogger();

	public static String profileHeader =	"# IRT Technologies board environment config\n"+
											"# First two lines must start from this text - do not modify";

	private Properties properties = new Properties(); 
	private List<Table> tables = new ArrayList<>();

	private Profile(){}

	private void setProperty(String[] processLine) {
		properties.put(processLine[0], processLine.length>1 ? processLine[1] : "");
	}

	public String getProperty(String key){
		return properties.getProperty(key);
	}

	public Table getTable(String tableName) {
		Table t = new Table();
		t.addRow(new String[]{tableName, "0", "0"});
		return getTable(t);
	}

	private Table getTable(Table table) {
		Table t = null;
		int indexOf = tables.indexOf(table);
		if(indexOf>=0)
			t = tables.get(indexOf);
		return t;
	}

//*** Static Methods *********************************************
	public static Profile parseProfile(String text){
        logger.entry(text);
		Profile profile = null;
		if(text!=null && !text.isEmpty())
			try (Scanner scanner =  new Scanner(text)){
				profile = new Profile();
				while (scanner.hasNextLine()){
			        String[] processLine = processLine(scanner.nextLine().trim());

			        logger.debug("{}", (Object[])processLine);
					if (processLine != null)
						if (processLine.length >= 3 && !processLine[1].replaceAll("\\D", "").isEmpty()) {
							setTableRow(profile, processLine);
						} else
							profile.setProperty(processLine);
				}
			}

		return logger.exit(profile);
	}

	private static void setTableRow(Profile profile, String[] processLine) {
		Table t = new Table();
		t.addRow(processLine);
		logger.debug("entry: {}", t);

		Table table = profile.getTable(t);
		logger.debug("{}", table);

		if(table!=null)
			table.addRow(processLine);
		else
			profile.tables.add(t);
		logger.exit();
	}

	public static String[] processLine(String nextLine) {
		logger.entry(nextLine);
		String[] propertiesStr = null;
		if(nextLine!=null && !nextLine.isEmpty() && nextLine.charAt(0)!='#'){

			if(!(nextLine = removeComments(nextLine.replaceAll("\\s+", " ").trim())).isEmpty())
				propertiesStr = isTable(nextLine) || isThreshold(nextLine) ? nextLine.split(" ") : nextLine.split(" ", 2);
		}
		logger.trace("output:'{}'", (Object[])propertiesStr);
		return propertiesStr;
	}

	private static boolean isThreshold(String nextLine) {
		return nextLine.contains("threshold");
	}

	private static boolean isTable(String nextLine) {
		return nextLine.contains("lut");
	}

	private static String removeComments(String nextLine) {
		logger.entry(nextLine);

		int i = nextLine.indexOf('#');
		if(i>=0)
			nextLine = nextLine.substring(0, i);

		return logger.exit(nextLine);
	}

	public Properties getProperties() {
		return properties;
	}

	public List<Table> getTables() {
		return new ArrayList<Table>(tables);
	}

	@Override
	public String toString() {
		return "Profile [properties=" + properties + ", tables=" + tables + "]";
	}
}
