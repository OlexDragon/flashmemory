package irt.flash.data;

public class ProfileVariable {

	private final long id;
	private final String name;
	private final String description;
	private final int scope;

	public ProfileVariable(long id, String name, int scope, String description) {
		this.id = id;
		this.name = name;
		this.scope = scope;
		this.description = description;
	}

	public long getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public int getScope() {
		return scope;
	}

	public String getDescription() {
		return description;
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
}
