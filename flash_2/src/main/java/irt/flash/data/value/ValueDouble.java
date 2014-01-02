package irt.flash.data.value;

import java.text.NumberFormat;


public class ValueDouble extends Value {

	private int precision;

	public ValueDouble(long value, long minValue, long maxValue, int precision){
		super(value, minValue, maxValue, precision);
		this.precision = precision;
	}

	public ValueDouble(long value, double minValue, double maxValue, int precision){
		super(value, minValue, maxValue, precision);
		this.precision = precision;
	}

	public ValueDouble(double value, double minValue, double maxValue, int precision) {
		this(0, minValue, maxValue, precision);
		setValue(value);
	}

	public ValueDouble(ValueDouble valueDouble) {
		super(valueDouble);
		precision = valueDouble.getPrecision();
	}

	public ValueDouble(long value, int precision) {
		this(value, Long.MIN_VALUE, Long.MAX_VALUE, precision);
	}

	public ValueDouble(Value value) {
		super(value);
	}

	@Override
	protected NumberFormat getInstance() {
		NumberFormat nf = NumberFormat.getNumberInstance();
		nf.setMaximumFractionDigits(precision);
		return nf;
	}

	public void setValue(double value) {
		setValue(Math.round(value*factor));
	}

	@Override
	public Value getCopy() {
		return new ValueDouble(this);
	}

	public int getPrecision() {
		return precision;
	}
}