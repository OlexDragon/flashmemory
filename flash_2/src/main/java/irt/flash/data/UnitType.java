package irt.flash.data;

public class UnitType {

	private final int id;
	private final String name;

	public UnitType(int id, String name) {
		this.id = id;
		this.name = name;
	}

	public int getId() {
		return id;
	}

	public String getName() {
		return name;
	}
}
