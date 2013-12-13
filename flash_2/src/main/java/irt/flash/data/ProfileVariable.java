package irt.flash.data;

public class ProfileVariable {

	private final long id;
	private final String name;
	private final String description;

	public ProfileVariable(long id, String name, String description) {
		this.id = id;
		this.name = name;
		this.description = description;
	}

	public long getId() {
		return id;
	}
	public String getName() {
		return name;
	}

	@Override
	public String toString() {
		return "ProfileVariable [id=" + id + ", name=" + name + "]";
	}

	@Override
	public boolean equals(Object obj) {
		return obj!=null ? obj.hashCode()==hashCode() : false;
	}

	@Override
	public int hashCode() {
		return id>0 ? new Long(id).hashCode() : super.hashCode();
	}

	public String getDescription() {
		return description;
	}
}
