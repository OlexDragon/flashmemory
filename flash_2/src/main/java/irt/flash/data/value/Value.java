package irt.flash.data.value;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Observable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Value extends Observable implements Comparable<Value>{

	private final Logger logger = (Logger) LogManager.getLogger();

	public enum Status{
				IN_RANGE,
				UNDER_RANGE,
				MORE_THEN_RANGE,
				RANGE_SET
	}
	private int type = 0;

	protected long oldValue;
	protected long value;

	private long minValue;
	private long maxValue;
	protected int factor;
	protected String prefix;

	private boolean error;

	protected Value(){}

	public Value(long value, long minValue, long maxValue, int precision){
		setFactor(precision);
		setMinMax(minValue, maxValue);
		setValue(value);
		setPrefix();
	}

	public Value(long value, double minValue, double maxValue, int precision){
		setFactor(precision);
		setMinMax(Math.round(minValue*factor), Math.round(maxValue*factor));
		setValue(value);
		setPrefix();
	}

	public Value(String value, String minValue, String maxValue, int precision) {
		setFactor(precision);
		setMinMax(parseLong(minValue)*factor, parseLong(maxValue)*factor);
		setValue(value!=null ? parseLong(value) : 0);
		setPrefix();
	}

	public Value(Value value) {
		setMinMax(value.getMinValue(), value.getMaxValue());
		setValue(value.getValue());
		factor = value.getFactor();
		prefix = value.getPrefix();
	}

	private void setFactor(int precision) {
		factor = (int) Math.pow(10, precision);
	}

	public void setMinMax(long minValue, long maxValue) {
		setChanged();
		if (minValue < maxValue) {
			this.minValue = minValue;
			this.maxValue = maxValue;
		} else {
			this.minValue = maxValue;
			this.maxValue = minValue;
		}
		notifyObservers(Status.RANGE_SET);

		setValue(value);
	}

	public void setMinMax(String minValue, String maxValue) {
		setMinMax(parseLong(minValue), parseLong(maxValue));
	}

	public long getMinValue() {
		return minValue;
	}

	public int getRelativeMinValue() {
		return 0;
	}

	protected void setMinValue(int minValue) {
		this.minValue = minValue;
	}

	protected void setMinValue(String minValue) {
		long min = parseLong(minValue);
		setMinValue(min);
	}

	public long getMaxValue() {
		return maxValue;
	}

	public long getRelativeMaxValue() {
		return maxValue-minValue;
	}

	protected void setMaxValue(int maxValue) {
		this.maxValue = maxValue;
	}

	protected void setMaxValue(String maxValue) {
		long max = parseLong(maxValue);
		setMaxValue(max);
	}

	public void setValue(long value) {

		if (this.value != value || value<minValue || value>maxValue) {
			oldValue = this.value;
			setChanged();

			if (value > maxValue) {
				this.value = maxValue;
				error = true;
				notifyObservers(Status.MORE_THEN_RANGE);
			} else if (value < minValue) {
				error = true;
				this.value = minValue;
				notifyObservers(Status.UNDER_RANGE);
			} else {
				error = false;
				this.value = value;
				notifyObservers(Status.IN_RANGE);
			}
		}
	}

	public void setValue(String text) {
		setValue(parseLong(text));
	}

	public void setValue(double value) {
		// TODO Auto-generated method stub
	}

	public long getValue() {
		return value;
	}

	public long getValue(int relativeValue) {
		return relativeValue + minValue;
	}

	/**
	 * @return value - minValue
	 */
	public int getRelativeValue() {
		return (int) (value - minValue);
	}

	public void setRelativeValue(int relValue) {
		setValue(relValue + minValue);
	}

	public long parseLong(String text) {
		long value = 0;
		if(text==null || text.trim().isEmpty()){
			error = true;
		}else{
			text = text.toUpperCase().replaceAll("[^\\d.E-]", "");

			if(!text.isEmpty() && Character.isDigit(text.charAt(text.length()-1)))
				try {
					value = Math.round(Double.parseDouble(text)*factor);
					error = false;
				} catch (NumberFormatException e) {
					error = true;
					logger.catching(e);
				}
			else
				error = true;

		}
		return value;
	}

	public int getFactor() {
		return factor;
	}

	protected NumberFormat getInstance() {
		return NumberFormat.getIntegerInstance();
	}

	public String toString(long value) {
		NumberFormat numberFormat = getInstance();
		double result = (double)value/factor;
		return numberFormat.format(result)+prefix;
	}

	@Override
	public String toString() {
		return toString(value);
	}

	protected void setMinValue(long minValue) {
		this.minValue = minValue;
	}

	protected void setMaxValue(long maxValue) {
		this.maxValue = maxValue;
	}

	public String getPrefix() {
		return prefix;
	}

	public void setPrefix(String prefix) {
		this.prefix = prefix!= null ? prefix : "";
	}

	public void setPrefix() {
		this.prefix = "";
	}

	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}

	public Value getCopy() {
		Value value;
		switch(getClass().getSimpleName()){
		case "ValueFrequency":
			value = new ValueFrequency(this);
			break;
		case "ValueDouble":
			value = new ValueDouble(this);
			break;
		case "Value":
			value = new ValueDouble(this);
			break;
		default:
			value = new Value(this);
		}
		return value;
	}

	@Override
	public boolean equals(Object obj) {
		return obj!=null ? obj.hashCode()==hashCode() : false;
	}

	@Override
	public int hashCode() {
		return new Long(value).hashCode();
	}

	public long getOldValue() {
		return oldValue;
	}

	public void add(long value) {
		setValue(value+this.value);
	}

	public void subtract(long value) {
		setValue(this.value-value);
	}

	public boolean hasChanged() {
		return oldValue!=value;
	}

	public String getExponentialValue() {
		DecimalFormat df = new DecimalFormat("0.00000000000000E000");  
		return df.format((double)value/factor);
	}

	public boolean isError() {
		return error;
	}

	@Override
	public int compareTo(Value o) {
		int result;
		long tmp = value - o.getValue();

		if(tmp>0)
			result = 1;
		else if(tmp<0)
			result = -1;
		else
			result = 0;

		return result;
	}
}
