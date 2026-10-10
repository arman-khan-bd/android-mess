package com.smartmess.android.data.remote.dto;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;
import java.util.List;

public class EcommerceResponse implements Serializable {

    @SerializedName("success")
    private boolean success;

    @SerializedName("data")
    private List<EcommerceProduct> data;

    @SerializedName("categories")
    private List<String> categories;

    @SerializedName("count")
    private int count;

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public List<EcommerceProduct> getData() { return data; }
    public void setData(List<EcommerceProduct> data) { this.data = data; }

    public List<String> getCategories() { return categories; }
    public void setCategories(List<String> categories) { this.categories = categories; }

    public int getCount() { return count; }
    public void setCount(int count) { this.count = count; }
}
