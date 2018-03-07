package irt.flash.data;

import java.util.Observable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

	public class Informer extends Observable{

		protected final Logger logger = LogManager.getLogger(getClass().getName());

		private final String name;
		private Object value;

		public Informer(String name){
			this.name = name;
		}

		public String getName() {
			return name;
		}

		public void setValue(Object value){
			logger.trace("name={}, value={}", name, value);
			if(this.value!=value){
				this.value = value;
				setChanged();
				notifyObservers();
			}
		}

		public Object getValue() {
			return value;
		}

		@Override
		public String toString() {
			return "Informer [name=" + name + ", value=" + value + "]";
		}
	}
