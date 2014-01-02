package irt.flash.data;

public class UnitProfileVariable extends ProfileVariable {

	private long rowId;

	public UnitProfileVariable(long rowId, long profileVariableId, String name, int scope, String description) {
		super(profileVariableId, name, scope, description);
		this.rowId = rowId;
	}

	public long getRowId() {
		return rowId;
	}
}
