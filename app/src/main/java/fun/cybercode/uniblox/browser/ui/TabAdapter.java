package fun.cybercode.uniblox.browser.ui;

import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import fun.cybercode.uniblox.browser.R;
import fun.cybercode.uniblox.browser.data.Tab;

public class TabAdapter extends ListAdapter<Tab, TabAdapter.TabViewHolder> {

    public interface OnTabInteractionListener {
        void onTabClick(Tab tab);
        void onTabClose(Tab tab);
    }

    private final OnTabInteractionListener listener;

    public TabAdapter(OnTabInteractionListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    private static final DiffUtil.ItemCallback<Tab> DIFF_CALLBACK = new DiffUtil.ItemCallback<Tab>() {
        @Override
        public boolean areItemsTheSame(@NonNull Tab oldItem, @NonNull Tab newItem) {
            return oldItem.getId().equals(newItem.getId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull Tab oldItem, @NonNull Tab newItem) {
            return oldItem.getTitle().equals(newItem.getTitle()) &&
                    oldItem.getUrl().equals(newItem.getUrl()) &&
                    oldItem.isActive() == newItem.isActive();
        }
    };

    @NonNull
    @Override
    public TabViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_tab, parent, false);
        return new TabViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TabViewHolder holder, int position) {
        holder.bind(getItem(position), listener);
    }

    static class TabViewHolder extends RecyclerView.ViewHolder {
        private final TextView titleText;
        private final ImageButton closeBtn;

        TabViewHolder(@NonNull View itemView) {
            super(itemView);
            titleText = itemView.findViewById(R.id.tab_title);
            closeBtn = itemView.findViewById(R.id.btn_close_tab);
        }

        void bind(Tab tab, OnTabInteractionListener listener) {
            titleText.setText(tab.getTitle());
            if (tab.isActive()) {
                titleText.setTypeface(null, Typeface.BOLD);
                itemView.setBackgroundResource(android.R.color.darker_gray);
            } else {
                titleText.setTypeface(null, Typeface.NORMAL);
                itemView.setBackgroundResource(android.R.color.transparent);
            }

            itemView.setOnClickListener(v -> listener.onTabClick(tab));
            closeBtn.setOnClickListener(v -> listener.onTabClose(tab));
        }
    }
}
