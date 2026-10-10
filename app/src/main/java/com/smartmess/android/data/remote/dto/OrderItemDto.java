package com.smartmess.android.data.remote.dto;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

public class OrderItemDto implements Serializable {

    @SerializedName("product_id")
    private Long productId;

    @SerializedName("title")
    private String title;

    @SerializedName("price")
    private double price;

    @SerializedName("quantity")
    private int quantity;

    @SerializedName("unit")
    private String unit;

    @SerializedName("image")
    private String image;

    @SerializedName("subtotal")
    private double subtotal;

    public OrderItemDto() {}

    public OrderItemDto(Long productId, String title, double price, int quantity, String unit, String image) {
        this.productId = productId;
        this.title = title;
        this.price = price;
        this.quantity = quantity;
        this.unit = unit;
        this.image = image;
        this.subtotal = price * quantity;
    }

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }

    public String getTitle() { return title != null ? title : ""; }
    public void setTitle(String title) { this.title = title; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) {
        this.quantity = quantity;
        this.subtotal = this.price * quantity;
    }

    public String getUnit() { return (unit != null && !unit.isEmpty()) ? unit : "কেজি"; }
    public void setUnit(String unit) { this.unit = unit; }

    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }

    public double getSubtotal() { return subtotal > 0 ? subtotal : price * quantity; }
    public void setSubtotal(double subtotal) { this.subtotal = subtotal; }
}
