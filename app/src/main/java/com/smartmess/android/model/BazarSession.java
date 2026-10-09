package com.smartmess.android.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

public class BazarSession implements Serializable {

    private String uuid;
    private long messId;
    private String date;
    private String title;
    private List<BazarItem> items = new ArrayList<>();
    private List<BazarPayer> payers = new ArrayList<>();
    private List<String> receiptUris = new ArrayList<>();

    public BazarSession() {}

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public long getMessId() {
        return messId;
    }

    public void setMessId(long messId) {
        this.messId = messId;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public List<BazarItem> getItems() {
        return items;
    }

    public void setItems(List<BazarItem> items) {
        this.items = items;
    }

    public List<BazarPayer> getPayers() {
        return payers;
    }

    public void setPayers(List<BazarPayer> payers) {
        this.payers = payers;
    }

    public List<String> getReceiptUris() {
        return receiptUris;
    }

    public void setReceiptUris(List<String> receiptUris) {
        this.receiptUris = receiptUris;
    }

    public double getTotalItemCost() {
        double total = 0.0;
        if (items != null) {
            for (BazarItem item : items) {
                total += item.getPrice();
            }
        }
        return round(total);
    }

    public double getTotalPaid() {
        double total = 0.0;
        if (payers != null) {
            for (BazarPayer payer : payers) {
                total += payer.getAmountPaid();
            }
        }
        return round(total);
    }

    public double getDifference() {
        return round(getTotalItemCost() - getTotalPaid());
    }

    public boolean isBalanced() {
        return Math.abs(getDifference()) < 0.01;
    }

    public double getCategoryTotal(String category) {
        double total = 0.0;
        if (items != null) {
            for (BazarItem item : items) {
                if (category.equalsIgnoreCase(item.getCategory())) {
                    total += item.getPrice();
                }
            }
        }
        return round(total);
    }

    public String buildSummaryTitle() {
        if (title != null && !title.trim().isEmpty()) {
            return title.trim();
        }
        if (items == null || items.isEmpty()) {
            return "Bazar Session";
        }
        StringBuilder sb = new StringBuilder();
        int count = Math.min(items.size(), 3);
        for (int i = 0; i < count; i++) {
            BazarItem item = items.get(i);
            if (item.getName() != null && !item.getName().trim().isEmpty()) {
                if (sb.length() > 0) sb.append(", ");
                sb.append(item.getName().trim());
                if (item.getQuantity() != null && !item.getQuantity().trim().isEmpty()) {
                    sb.append(" (").append(item.getQuantity().trim()).append(")");
                }
            }
        }
        if (items.size() > 3) {
            sb.append(" +").append(items.size() - 3).append(" more");
        }
        return sb.length() > 0 ? sb.toString() : "Bazar Session";
    }

    private static double round(double val) {
        return BigDecimal.valueOf(val).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}
