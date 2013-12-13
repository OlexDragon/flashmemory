package irt.flash.data;

import java.math.BigDecimal;
import java.util.TreeMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Logger;

public class Table {

	private final Logger logger = (Logger) LogManager.getLogger();

	private String name;
	private TreeMap<BigDecimal, BigDecimal> tableMap = new TreeMap<>();

	public boolean addRow(String[] fields){
		logger.trace("entry: {}", (Object[])fields);
		
		boolean set = false;
		if(fields!=null && fields.length>=3){
			if(name==null)
				name = fields[0];

			if (name.equals(fields[0])) {
				tableMap.put(new BigDecimal(fields[1]), new BigDecimal(fields[2]));
				set = true;
			}
		}
		return logger.exit(set);
	}

	@Override
	public boolean equals(Object obj) {
		return obj !=null ? obj.hashCode()==hashCode() : false;
	}

	@Override
	public int hashCode() {
		return name!=null ? name.hashCode() : super.hashCode();
	}

	@Override
	public String toString() {
		return "Table [name=" + name + ", tableMap=" + tableMap + "]";
	}

	public String getName() {
		return name;
	}

	public TreeMap<BigDecimal, BigDecimal> getTableMap() {
		return tableMap;
	}
}
