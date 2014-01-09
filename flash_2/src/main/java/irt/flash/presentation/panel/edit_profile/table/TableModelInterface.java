package irt.flash.presentation.panel.edit_profile.table;

import irt.flash.data.Table;

import javax.swing.table.TableModel;

public interface TableModelInterface extends TableModel {

	void setTable(Table value);
	void setEditableRow(int rowIndex);
	void insertRow(int selectedRow);
	void deleteRow(int selectedRow);
	void escape();
}
