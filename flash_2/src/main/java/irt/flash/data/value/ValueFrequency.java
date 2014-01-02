package irt.flash.data.value;

import java.text.NumberFormat;

public class ValueFrequency extends Value {

	public ValueFrequency(String value, String minValue, String maxValue) {
		super(value, minValue, maxValue, 0);
	}

	public ValueFrequency(long value, long minValue, long maxValue) {
		super(value, minValue, maxValue, 0);
	}

	public ValueFrequency(Value value) {
		super(value);
	}

	public ValueFrequency(String value, String minValue, String maxValue, int precision) {
		super(value, minValue, maxValue, precision);
	}

	@Override
	public long parseLong(String text) {

		int multiplier = 1;
		text = text.toUpperCase();
		String str = text.replaceAll("[\\d., +]", "");

		if (!str.isEmpty())
			switch(str.charAt(0)){
			case 'K':
				multiplier = 1000;
				break;
			case 'M':
				multiplier = 1000000;
				break;
			case 'G':
				multiplier = 1000000000;
				break;
			case 'E':
				String[] split = text.split("E");
				text = split[0];
				str = split[1].replaceAll("[^\\d.-]", "");
				multiplier = (int) Math.round(Math.pow(10, Integer.parseInt(str)));
			}

		text = text.replaceAll("[^\\d.-]", "");

		return text.isEmpty() ? 0 : Math.round(Double.parseDouble(text)*multiplier);
	}

	@Override
	public String toString(long value) {
		int prefixIndex = 0;
		
		while(true){
			if(value%1000 > 0 || prefixIndex >= 3)
				break;
			value /= 1000;
			prefixIndex++;
		}

		switch(prefixIndex){
		case 0:
			setPrefix(" Hz");
			break;
		case 1:
			setPrefix(" KHz");
			break;
		case 2:
			setPrefix(" MHz");
			break;
		default:
			setPrefix(" GHz");
		
		}

		NumberFormat numberFormat = getInstance();
		return numberFormat.format(value)+getPrefix();
	}
}
