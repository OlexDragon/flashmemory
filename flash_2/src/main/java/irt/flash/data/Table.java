package irt.flash.data;

import java.math.BigDecimal;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Logger;

public class Table {

	private final Logger logger = (Logger) LogManager.getLogger();

	private String name;
	private TreeMap<BigDecimal, BigDecimal> tableMap = new TreeMap<>();

	public boolean addRow(String[] rowFields){
		logger.entry((Object[])rowFields);
		
		boolean set = false;
		if(rowFields!=null && rowFields.length>=3){
			if(name==null)
				name = rowFields[0];

			if (name.equals(rowFields[0])) {

				BigDecimal value;
				try{
					BigDecimal key = new BigDecimal(rowFields[1]);
					key.setScale(3, BigDecimal.ROUND_HALF_EVEN);

					value = new BigDecimal(rowFields[2]);
					value.setScale(3, BigDecimal.ROUND_HALF_EVEN);
					tableMap.put(key!=null ? key : new BigDecimal(-1), value!=null ? value : new BigDecimal(-1));

					set = true;

				}catch(NumberFormatException e){
					logger.error("{}, key = new BigDecimal({}), value = new BigDecimal({})", name, rowFields[1], rowFields[2]);
					logger.catching(e);
				}

			}
		}
		return logger.exit(set);
	}

	public void addRow(Entry<BigDecimal, BigDecimal> entry) {
		tableMap.put(entry.getKey(), entry.getValue());
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

	public int size(){
		return tableMap.size();
	}

	public Entry<BigDecimal, BigDecimal> getRow(int rowIndex) {
		logger.entry(rowIndex);

		Set<Entry<BigDecimal, BigDecimal>> entrySet = tableMap.entrySet();

		@SuppressWarnings("unchecked")
		Entry<BigDecimal, BigDecimal>[] array = entrySet.toArray(new Entry[entrySet.size()]);

		logger.trace("Entry<BigDecimal, BigDecimal>[] array ={}", (Object[])array);

		return logger.exit(array.length>rowIndex ? array[rowIndex ] : null);
	}

	public void remove(BigDecimal key) {
		tableMap.remove(key);
	}

	public void remove(int selectedRow) {
		Entry<BigDecimal, BigDecimal> row = getRow(selectedRow);
		if(row!=null)
			tableMap.remove(row.getKey());
	}
}
