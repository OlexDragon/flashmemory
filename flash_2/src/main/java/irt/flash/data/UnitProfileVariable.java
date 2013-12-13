package irt.flash.data;

public class UnitProfileVariable extends ProfileVariable {

	private long rowId;

	public UnitProfileVariable(long rowId, long profileVariableId, String name, String description) {
		super(profileVariableId, name, description);
		this.rowId = rowId;
	}

	public long getRowId() {
		return rowId;
	}
}
