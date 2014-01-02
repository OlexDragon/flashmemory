package irt.flash.data;

import java.util.Observable;

	public class Informer extends Observable{
		private final String name;
		private Object value;

		public Informer(String name){
			this.name = name;
		}

		public String getName() {
			return name;
		}

		public void setValue(Object value){
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
