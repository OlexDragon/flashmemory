package irt.flash.presentation.panel.edit_profile.table;

import irt.flash.data.Table;

import java.math.BigDecimal;
import java.util.AbstractMap;
import java.util.Map.Entry;

import javax.swing.JOptionPane;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Logger;

public abstract class AbstractTableModel extends javax.swing.table.AbstractTableModel  implements TableModelInterface{
	private static final long serialVersionUID = 4484953950095128972L;

	private static final Logger logger = (Logger) LogManager.getLogger();

	private String[] columnNames;
	private Table table;

	private int insertedRowIndex = -1;
	private Entry<BigDecimal, BigDecimal> insertedEntry;
	private int editableRow = -1;

	public AbstractTableModel(String[] columnNames) {
		this.columnNames = columnNames;
	}

	@Override
	public int getRowCount() {
		return table!=null ? table.size()+1 : 1;
	}

	@Override
	public int getColumnCount() {
		return columnNames!=null ? columnNames.length : 1;
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		logger.entry(rowIndex, columnIndex);

		String value = null;
		if(table!=null){

			boolean show = true;

			if(insertedRowIndex>=0){
				if(rowIndex==insertedRowIndex){
					show = false;
					switch (columnIndex) {
					case 0:
						BigDecimal entryKey = insertedEntry.getKey();
						if(entryKey!=null)
							value = entryKey.toString();
						break;
					case 1:
						BigDecimal entryValue = insertedEntry.getValue();
						if(entryValue!=null)
							value = entryValue.toString();
					}
				} else if(rowIndex>insertedRowIndex)
					rowIndex--;
			}

			if (show) {
				Entry<BigDecimal, BigDecimal> row = table.getRow(rowIndex);

				if (row != null) {
					switch (columnIndex) {
					case 0:
						value = row.getKey().toString();
						break;
					case 1:
						value = row.getValue().toString();
					}
				}
			}
		}
		return logger.exit(value);
	}

	@Override
	public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
		logger.trace("aValue={}, rowIndex={}, columnIndex={}", aValue, rowIndex, columnIndex);
		if(aValue==null || ((String)aValue).isEmpty())
			JOptionPane.showMessageDialog(null, "All fields must be filled");
		else if(rowIndex==insertedRowIndex)
			setInsertedEntry(aValue, columnIndex);
		else if(insertedRowIndex<0 || rowIndex<insertedRowIndex)
			updateTable(aValue, rowIndex, columnIndex);
		else
			updateTable(aValue, --rowIndex, columnIndex);
	}

	private void updateTable(Object aValue, int rowIndex, int columnIndex) {
		{
			Entry<BigDecimal, BigDecimal> row = table.getRow(rowIndex);
			switch(columnIndex){
			case 0:
				table.remove(row.getKey());
				row = new AbstractMap.SimpleEntry<>(new BigDecimal((String)aValue), row.getValue());
				table.addRow(row);
				break;
			case 1:
				row.setValue(new BigDecimal((String)aValue));
			}
			fireTableRowsUpdated(rowIndex, rowIndex);
		}
	}

	private void setInsertedEntry(Object aValue, int columnIndex) {
		logger.entry(aValue, columnIndex, insertedEntry);
		switch(columnIndex){
		case 0:
			insertedEntry = new AbstractMap.SimpleEntry<>(new BigDecimal((String) aValue), insertedEntry.getValue());
			break;
		case 1:
			insertedEntry.setValue(new BigDecimal((String) aValue));
		}
		if(insertedEntry.getKey()!=null && insertedEntry.getValue()!=null){
			table.addRow(insertedEntry);
			escape();
		}
		logger.exit(insertedEntry);
	}

	@Override
	public String getColumnName(int column) {
		return columnNames!=null && columnNames.length>column ? columnNames[column] : "No Name" ;
	}

	@Override
	public void setTable(Table table) {
		this.table = table;
		fireTableDataChanged();
	}

	@Override
	public void insertRow(int selectedRow) {
		if(insertedRowIndex<0 && table!=null && table.size()>0){
			setInsertedEntry(selectedRow);
			fireTableRowsInserted(selectedRow, selectedRow);
		}else
			JOptionPane.showMessageDialog(null, "Error");
	}

	private void setInsertedEntry(int selectedRow) {
		insertedRowIndex = selectedRow;
		insertedEntry = new AbstractMap.SimpleEntry<>(null, null);
	}

	@Override
	public boolean isCellEditable(int rowIndex, int columnIndex) {
		return editableRow==rowIndex;
	}

	@Override
	public void setEditableRow(int editableRow) {
		if(table!=null){
			this.editableRow = editableRow;
			if(editableRow==table.size())
				setInsertedEntry(editableRow);
		}
	}

	@Override
	public void escape() {
		insertedRowIndex = -1;
		insertedEntry = null;
		fireTableDataChanged();
	}

	@Override
	public void deleteRow(int selectedRow) {
		table.remove(selectedRow);
		fireTableRowsDeleted(selectedRow, selectedRow);
	}
}
