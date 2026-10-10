package com.smartmess.android.data.remote.dto;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;
import java.util.List;

public class CreateOrderRequest implements Serializable {

    @SerializedName("customer_name")
    private String customerName;

    @SerializedName("customer_phone")
    private String customerPhone;

    @SerializedName("mess_name")
    private String messName;

    @SerializedName("mess_id")
    private Long messId;

    @SerializedName("delivery_address")
    private String deliveryAddress;

    @SerializedName("notes")
    private String notes;

    @SerializedName("payment_method")
    private String paymentMethod;

    @SerializedName("items")
    private List<OrderItemDto> items;

    public CreateOrderRequest() {}

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public String getCustomerPhone() { return customerPhone; }
    public void setCustomerPhone(String customerPhone) { this.customerPhone = customerPhone; }

    public String getMessName() { return messName; }
    public void setMessName(String messName) { this.messName = messName; }

    public Long getMessId() { return messId; }
    public void setMessId(Long messId) { this.messId = messId; }

    public String getDeliveryAddress() { return deliveryAddress; }
    public void setDeliveryAddress(String deliveryAddress) { this.deliveryAddress = deliveryAddress; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getPaymentMethod() { return paymentMethod != null ? paymentMethod : "cash_on_delivery"; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public List<OrderItemDto> getItems() { return items; }
    public void setItems(List<OrderItemDto> items) { this.items = items; }
}
