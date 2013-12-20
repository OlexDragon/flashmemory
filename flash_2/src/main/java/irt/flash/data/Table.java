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

				BigDecimal value;
				try{
					BigDecimal key = new BigDecimal(fields[1]);
					key.setScale(3, BigDecimal.ROUND_HALF_EVEN);

					value = new BigDecimal(fields[2]);
					value.setScale(3, BigDecimal.ROUND_HALF_EVEN);
					tableMap.put(key!=null ? key : new BigDecimal(-1), value!=null ? value : new BigDecimal(-1));

					set = true;

				}catch(NumberFormatException e){
					logger.error("{}, key = new BigDecimal({}), value = new BigDecimal({})", name, fields[1], fields[2]);
					logger.catching(e);
				}

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
