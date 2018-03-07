package irt.flash.data;

import java.math.BigDecimal;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class Table {

	private final Logger logger = LogManager.getLogger();

	protected String name;
	private TreeMap<BigDecimal, BigDecimal> tableMap = new TreeMap<>();
	private boolean compareByName = true;

	public boolean isCompareByName() {
		return compareByName;
	}

	public void setCompareByName(boolean compareByName) {
		this.compareByName = compareByName;
	}

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
		return set;
	}

	public void addRow(Entry<BigDecimal, BigDecimal> entry) {
		tableMap.put(entry.getKey(), entry.getValue());
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

		return array.length>rowIndex ? array[rowIndex ] : null;
	}

	public void remove(BigDecimal key) {
		tableMap.remove(key);
	}

	public void remove(int selectedRow) {
		Entry<BigDecimal, BigDecimal> row = getRow(selectedRow);
		if(row!=null)
			tableMap.remove(row.getKey());
	}

	@Override
	public String toString() {
		return "Table [name=" + name + ", tableMap=" + tableMap + "]";
	}

/**
 * By default compares by name<br>
 * If setCompareByName(false) will compare each row of the Table
 */
	@Override
	public boolean equals(Object obj) {
		if(compareByName)
			return obj !=null ? obj.hashCode()==hashCode() : false;
		else{
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Table other = (Table) obj;
			if (name == null) {
				if (other.name != null)
					return false;
			} else if (!name.equals(other.name))
				return false;
			if (tableMap == null) {
				if (other.tableMap != null)
					return false;
			} else if (!tableMap.equals(other.tableMap))
				return false;
			return true;
		}
	}

	@Override
	public int hashCode() {
		if(compareByName)
			return name!=null ? name.hashCode() : super.hashCode();
		else{
			final int prime = 31;
			int result = 1;
			result = prime * result + ((name == null) ? 0 : name.hashCode());
			result = prime * result + ((tableMap == null) ? 0 : tableMap.hashCode());
			return result;
		}
	}
}
