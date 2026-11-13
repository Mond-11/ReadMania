package ua.readmania.dto.catalog;

public class CsvRejectedRow {

    private int rowNumber;
    private String isbn;
    private String title;
    private String reason;

    public CsvRejectedRow() {
    }

    public CsvRejectedRow(int rowNumber, String isbn, String title, String reason) {
        this.rowNumber = rowNumber;
        this.isbn = isbn;
        this.title = title;
        this.reason = reason;
    }

    public int getRowNumber() {
        return rowNumber;
    }

    public void setRowNumber(int rowNumber) {
        this.rowNumber = rowNumber;
    }

    public String getIsbn() {
        return isbn;
    }

    public void setIsbn(String isbn) {
        this.isbn = isbn;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
