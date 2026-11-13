package ua.readmania.dto.catalog;

import java.util.ArrayList;
import java.util.List;

public class CsvImportResponse {

    private int totalRows;
    private int addedCount;
    private int rejectedCount;
    private List<CsvRejectedRow> rejectedRows = new ArrayList<>();

    public CsvImportResponse() {
    }

    public CsvImportResponse(int totalRows, int addedCount, int rejectedCount, List<CsvRejectedRow> rejectedRows) {
        this.totalRows = totalRows;
        this.addedCount = addedCount;
        this.rejectedCount = rejectedCount;
        this.rejectedRows = rejectedRows != null ? rejectedRows : new ArrayList<>();
    }

    public int getTotalRows() {
        return totalRows;
    }

    public void setTotalRows(int totalRows) {
        this.totalRows = totalRows;
    }

    public int getAddedCount() {
        return addedCount;
    }

    public void setAddedCount(int addedCount) {
        this.addedCount = addedCount;
    }

    public int getRejectedCount() {
        return rejectedCount;
    }

    public void setRejectedCount(int rejectedCount) {
        this.rejectedCount = rejectedCount;
    }

    public List<CsvRejectedRow> getRejectedRows() {
        return rejectedRows;
    }

    public void setRejectedRows(List<CsvRejectedRow> rejectedRows) {
        this.rejectedRows = rejectedRows != null ? rejectedRows : new ArrayList<>();
    }
}
