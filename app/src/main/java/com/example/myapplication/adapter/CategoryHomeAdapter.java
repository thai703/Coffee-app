package com.example.myapplication.adapter;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.example.myapplication.R;
import com.example.myapplication.model.Category;
import java.util.List;

public class CategoryHomeAdapter extends RecyclerView.Adapter<CategoryHomeAdapter.CategoryHomeViewHolder> {

    private Context context;
    private List<Category> categoryList;
    private OnCategoryClickListener listener;
    private int selectedPosition = 0; // Default selection

    public interface OnCategoryClickListener {
        void onCategoryClick(Category category);
    }

    public CategoryHomeAdapter(Context context, List<Category> categoryList, OnCategoryClickListener listener) {
        this.context = context;
        this.categoryList = categoryList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public CategoryHomeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_category_chip, parent, false);
        return new CategoryHomeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CategoryHomeViewHolder holder, int position) {
        Category category = categoryList.get(position);
        boolean isSelected = position == selectedPosition;
        holder.bind(category, isSelected, position);
    }

    @Override
    public int getItemCount() {
        return categoryList != null ? categoryList.size() : 0;
    }

    public void setCategories(List<Category> categories) {
        this.categoryList = categories;
        notifyDataSetChanged();
    }

    class CategoryHomeViewHolder extends RecyclerView.ViewHolder {
        ImageView ivIcon;
        TextView tvName;
        View container;

        public CategoryHomeViewHolder(@NonNull View itemView) {
            super(itemView);
            ivIcon = itemView.findViewById(R.id.iv_category_icon);
            tvName = itemView.findViewById(R.id.tv_category_name);
            container = itemView;
        }

        void bind(Category category, boolean isSelected, int position) {
            tvName.setText(category.getName());

            // Set Selected State
            container.setSelected(isSelected);

            if (isSelected) {
                tvName.setTextColor(ContextCompat.getColor(context, android.R.color.white));
                ivIcon.setColorFilter(ContextCompat.getColor(context, android.R.color.white));
            } else {
                tvName.setTextColor(ContextCompat.getColor(context, R.color.text_primary));
                ivIcon.setColorFilter(ContextCompat.getColor(context, R.color.brown_500));
            }

            // Image Loading Logic (Same as existing)
            String imageUrl = category.getImageUrl();
            if (imageUrl != null && imageUrl.length() > 100) {
                try {
                    byte[] decodedString = Base64.decode(imageUrl, Base64.DEFAULT);
                    Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                    if (decodedByte != null) {
                        ivIcon.setImageBitmap(decodedByte);
                    } else {
                        ivIcon.setImageResource(R.drawable.ic_category_placeholder);
                    }
                } catch (Exception e) {
                    ivIcon.setImageResource(R.drawable.ic_category_placeholder);
                }
            } else {
                int resId = category.getResourceId(context);
                if (resId != 0) {
                    ivIcon.setImageResource(resId);
                } else {
                    ivIcon.setImageResource(R.drawable.ic_category_placeholder);
                }
            }

            // Click Event
            container.setOnClickListener(v -> {
                int previousPosition = selectedPosition;
                selectedPosition = position;
                notifyItemChanged(previousPosition);
                notifyItemChanged(selectedPosition);

                if (listener != null) {
                    listener.onCategoryClick(category);
                }
            });
        }
    }
}
