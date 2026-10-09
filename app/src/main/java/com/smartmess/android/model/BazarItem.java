package com.smartmess.android.model;

import java.io.Serializable;

public class BazarItem implements Serializable {

    public static final String CAT_RAW_MEAL = "raw_meal";
    public static final String CAT_SHARED_FOOD = "shared_food";
    public static final String CAT_UTILITY_ASSET = "utility_asset";

    private String name;
    private String category;
    private String quantity;
    private double price;

    public BazarItem() {
        this.category = CAT_RAW_MEAL;
        this.quantity = "1 kg";
        this.price = 0.0;
    }

    public BazarItem(String name, String category, String quantity, double price) {
        this.name = name;
        this.category = category;
        this.quantity = quantity;
        this.price = price;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getQuantity() {
        return quantity;
    }

    public void setQuantity(String quantity) {
        this.quantity = quantity;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public String getCategoryLabel() {
        if (CAT_SHARED_FOOD.equals(category)) {
            return "Shared Pool";
        } else if (CAT_UTILITY_ASSET.equals(category)) {
            return "Utility";
        }
        return "Raw Meal";
    }
}
