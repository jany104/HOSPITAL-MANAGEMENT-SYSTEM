package hospital.management.system;

import javax.swing.table.DefaultTableModel;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Vector;

public final class ResultSetTableModelBuilder {

    private ResultSetTableModelBuilder() {
    }

    public static DefaultTableModel buildTableModel(ResultSet resultSet) throws SQLException {
        ResultSetMetaData metaData = resultSet.getMetaData();
        int columnCount = metaData.getColumnCount();

        Vector<String> columnNames = new Vector<>();
        for (int column = 1; column <= columnCount; column++) {
            String label = metaData.getColumnLabel(column);
            if (label == null || label.isEmpty()) {
                label = metaData.getColumnName(column);
            }
            columnNames.add(label);
        }

        Vector<Vector<Object>> rows = new Vector<>();
        while (resultSet.next()) {
            Vector<Object> currentRow = new Vector<>();
            for (int columnIndex = 1; columnIndex <= columnCount; columnIndex++) {
                currentRow.add(resultSet.getObject(columnIndex));
            }
            rows.add(currentRow);
        }

        return new DefaultTableModel(rows, columnNames) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }
}
