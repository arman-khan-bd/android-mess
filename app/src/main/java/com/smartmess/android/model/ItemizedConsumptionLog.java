package com.smartmess.android.model;

import java.io.Serializable;

public class ItemizedConsumptionLog implements Serializable {
    private long id;
    private String uuid;
    private long messId;
    private String bazarSessionUuid;
    private long bazarId;
    private long mealId;
    private long userId;
    private String userName;
    private String itemName;
    private String itemCategory;
    private double itemPrice;
    private String mealDate;
    private String mealType;
    private int activeDinersCount;
    private double costShare;
    private int isConsumed;
    private String createdAt;

    public ItemizedConsumptionLog() {
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

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

    public String getBazarSessionUuid() {
        return bazarSessionUuid;
    }

    public void setBazarSessionUuid(String bazarSessionUuid) {
        this.bazarSessionUuid = bazarSessionUuid;
    }

    public long getBazarId() {
        return bazarId;
    }

    public void setBazarId(long bazarId) {
        this.bazarId = bazarId;
    }

    public long getMealId() {
        return mealId;
    }

    public void setMealId(long mealId) {
        this.mealId = mealId;
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public String getItemCategory() {
        return itemCategory;
    }

    public void setItemCategory(String itemCategory) {
        this.itemCategory = itemCategory;
    }

    public double getItemPrice() {
        return itemPrice;
    }

    public void setItemPrice(double itemPrice) {
        this.itemPrice = itemPrice;
    }

    public String getMealDate() {
        return mealDate;
    }

    public void setMealDate(String mealDate) {
        this.mealDate = mealDate;
    }

    public String getMealType() {
        return mealType;
    }

    public void setMealType(String mealType) {
        this.mealType = mealType;
    }

    public int getActiveDinersCount() {
        return activeDinersCount;
    }

    public void setActiveDinersCount(int activeDinersCount) {
        this.activeDinersCount = activeDinersCount;
    }

    public double getCostShare() {
        return costShare;
    }

    public void setCostShare(double costShare) {
        this.costShare = costShare;
    }

    public int getIsConsumed() {
        return isConsumed;
    }

    public void setIsConsumed(int isConsumed) {
        this.isConsumed = isConsumed;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
}
