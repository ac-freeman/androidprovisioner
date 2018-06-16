package provisioner;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import javax.swing.table.AbstractTableModel;

/**
 *
 * @author andrew
 */
/**
 * Custom implementation of AbstractTableModel Is a scrollable list of filenames
 * with checkboxes
 */
public class CheckModel extends AbstractTableModel {

    private final int rows;
    private List<Boolean> rowList;
    Set<Integer> checked = new TreeSet<Integer>();

    public CheckModel(int rows) {
        this.rows = rows;
        rowList = new ArrayList<Boolean>(rows);
        for (int i = 0; i < rows; i++) {
            rowList.add(Boolean.FALSE);
        }
    }

    @Override
    public int getRowCount() {
        return rows;
    }

    @Override
    public int getColumnCount() {
        return 1;
    }

    @Override
    public String getColumnName(int col) {
        return "Available apps";
    }

    @Override
    public Object getValueAt(int row, int col) {
        return rowList.get(row);
    }

    @Override
    public void setValueAt(Object aValue, int row, int col) {
        boolean b = (Boolean) aValue;
        rowList.set(row, b);
        if (b) {
            checked.add(row);
        } else {
            checked.remove(row);
        }
        fireTableRowsUpdated(row, row);
    }

    @Override
    public Class<?> getColumnClass(int col) {
        return getValueAt(0, col).getClass();
    }

    @Override
    public boolean isCellEditable(int row, int col) {
        return true;
    }
}
