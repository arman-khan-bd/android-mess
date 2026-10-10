package com.smartmess.android.data.remote.dto;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class EcommerceProduct implements Serializable {

    @SerializedName("id")
    private long id;

    @SerializedName("uuid")
    private String uuid;

    @SerializedName("title")
    private String title;

    @SerializedName("slug")
    private String slug;

    @SerializedName("description")
    private String description;

    @SerializedName("price")
    private double price;

    @SerializedName("original_price")
    private Double originalPrice;

    @SerializedName("stock_quantity")
    private int stockQuantity;

    @SerializedName("unit")
    private String unit;

    @SerializedName("category")
    private String category;

    @SerializedName("badge")
    private String badge;

    @SerializedName("images")
    private List<String> images;

    @SerializedName("main_image")
    private String mainImage;

    @SerializedName("is_active")
    private boolean isActive;

    @SerializedName("is_featured")
    private boolean isFeatured;

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getUuid() { return uuid; }
    public void setUuid(String uuid) { this.uuid = uuid; }

    public String getTitle() { return title != null ? title : ""; }
    public void setTitle(String title) { this.title = title; }

    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }

    public String getDescription() { return description != null ? description : ""; }
    public void setDescription(String description) { this.description = description; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public Double getOriginalPrice() { return originalPrice; }
    public void setOriginalPrice(Double originalPrice) { this.originalPrice = originalPrice; }

    public int getStockQuantity() { return stockQuantity; }
    public void setStockQuantity(int stockQuantity) { this.stockQuantity = stockQuantity; }

    public String getUnit() { return (unit != null && !unit.isEmpty()) ? unit : "কেজি"; }
    public void setUnit(String unit) { this.unit = unit; }

    public String getCategory() { return (category != null && !category.isEmpty()) ? category : "চাল ও খাদ্যশস্য"; }
    public void setCategory(String category) { this.category = category; }

    public String getBadge() { return badge; }
    public void setBadge(String badge) { this.badge = badge; }

    public List<String> getImages() {
        if (images == null) {
            images = new ArrayList<>();
        }
        if (images.isEmpty() && mainImage != null && !mainImage.isEmpty()) {
            images.add(mainImage);
        }
        return images;
    }
    public void setImages(List<String> images) { this.images = images; }

    public String getMainImage() {
        if (mainImage != null && !mainImage.isEmpty()) return mainImage;
        if (images != null && !images.isEmpty()) return images.get(0);
        return "https://images.unsplash.com/photo-1586201375761-83865001e31c?auto=format&fit=crop&w=600&q=80";
    }
    public void setMainImage(String mainImage) { this.mainImage = mainImage; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    public boolean isFeatured() { return isFeatured; }
    public void setFeatured(boolean featured) { isFeatured = featured; }

    public boolean hasDiscount() {
        return originalPrice != null && originalPrice > price;
    }

    public int getDiscountPercent() {
        if (!hasDiscount() || originalPrice <= 0) return 0;
        return (int) Math.round(((originalPrice - price) / originalPrice) * 100);
    }
}
