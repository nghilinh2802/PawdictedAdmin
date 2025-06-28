package com.group7.pawdictedadmin.models;

public class FinancialSummaryItem {
    private String title;
    private String subtitle;
    private String value;
    private int colorResource;

    public FinancialSummaryItem(String title, String subtitle, String value, int colorResource) {
        this.title = title;
        this.subtitle = subtitle;
        this.value = value;
        this.colorResource = colorResource;
    }

    // Getters
    public String getTitle() { return title; }
    public String getSubtitle() { return subtitle; }
    public String getValue() { return value; }
    public int getColorResource() { return colorResource; }
}

