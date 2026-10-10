package com.smartmess.android.ecommerce;

import android.content.Context;
import android.content.SharedPreferences;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.smartmess.android.data.remote.dto.EcommerceProduct;
import com.smartmess.android.data.remote.dto.OrderItemDto;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class CartManager {

    private static final String PREF_NAME = "smartmess_ecommerce_cart";
    private static final String KEY_ITEMS = "cart_items_json";

    private static CartManager instance;
    private final SharedPreferences prefs;
    private final Gson gson;
    private final List<OrderItemDto> cartItems;
    private final List<CartListener> listeners = new ArrayList<>();

    public interface CartListener {
        void onCartChanged();
    }

    private CartManager(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        gson = new Gson();
        cartItems = loadCartItems();
    }

    public static synchronized CartManager getInstance(Context context) {
        if (instance == null) {
            instance = new CartManager(context);
        }
        return instance;
    }

    public void addListener(CartListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void removeListener(CartListener listener) {
        listeners.remove(listener);
    }

    private void notifyListeners() {
        for (CartListener l : listeners) {
            try {
                l.onCartChanged();
            } catch (Throwable ignored) {}
        }
    }

    private List<OrderItemDto> loadCartItems() {
        String json = prefs.getString(KEY_ITEMS, null);
        if (json != null && !json.trim().isEmpty()) {
            try {
                Type type = new TypeToken<ArrayList<OrderItemDto>>() {}.getType();
                List<OrderItemDto> list = gson.fromJson(json, type);
                if (list != null) return list;
            } catch (Exception ignored) {}
        }
        return new ArrayList<>();
    }

    private void saveCartItems() {
        String json = gson.toJson(cartItems);
        prefs.edit().putString(KEY_ITEMS, json).apply();
        notifyListeners();
    }

    public synchronized void addToCart(EcommerceProduct product, int quantity) {
        if (product == null || quantity <= 0) return;

        for (OrderItemDto item : cartItems) {
            if (item.getProductId() != null && item.getProductId() == product.getId()) {
                item.setQuantity(item.getQuantity() + quantity);
                saveCartItems();
                return;
            }
        }

        OrderItemDto newItem = new OrderItemDto(
                product.getId(),
                product.getTitle(),
                product.getPrice(),
                quantity,
                product.getUnit(),
                product.getMainImage()
        );
        cartItems.add(newItem);
        saveCartItems();
    }

    public synchronized void updateQuantity(long productId, int quantity) {
        Iterator<OrderItemDto> iterator = cartItems.iterator();
        while (iterator.hasNext()) {
            OrderItemDto item = iterator.next();
            if (item.getProductId() != null && item.getProductId() == productId) {
                if (quantity <= 0) {
                    iterator.remove();
                } else {
                    item.setQuantity(quantity);
                }
                saveCartItems();
                return;
            }
        }
    }

    public synchronized void removeFromCart(long productId) {
        Iterator<OrderItemDto> iterator = cartItems.iterator();
        while (iterator.hasNext()) {
            OrderItemDto item = iterator.next();
            if (item.getProductId() != null && item.getProductId() == productId) {
                iterator.remove();
                saveCartItems();
                return;
            }
        }
    }

    public synchronized void clearCart() {
        cartItems.clear();
        saveCartItems();
    }

    public synchronized List<OrderItemDto> getCartItems() {
        return new ArrayList<>(cartItems);
    }

    public synchronized int getTotalItemCount() {
        int count = 0;
        for (OrderItemDto item : cartItems) {
            count += item.getQuantity();
        }
        return count;
    }

    public synchronized double getSubtotal() {
        double subtotal = 0;
        for (OrderItemDto item : cartItems) {
            subtotal += item.getSubtotal();
        }
        return subtotal;
    }

    public synchronized int getQuantityForProduct(long productId) {
        for (OrderItemDto item : cartItems) {
            if (item.getProductId() != null && item.getProductId() == productId) {
                return item.getQuantity();
            }
        }
        return 0;
    }
}
