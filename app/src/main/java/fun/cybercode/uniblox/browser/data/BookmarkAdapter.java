package fun.cybercode.uniblox.browser.data;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import fun.cybercode.uniblox.browser.R;

public class BookmarkAdapter extends ListAdapter<Bookmark, BookmarkAdapter.BookmarkViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(Bookmark bookmark);
    }

    private final OnItemClickListener listener;

    public BookmarkAdapter(OnItemClickListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    private static final DiffUtil.ItemCallback<Bookmark> DIFF_CALLBACK = new DiffUtil.ItemCallback<Bookmark>() {
        @Override
        public boolean areItemsTheSame(@NonNull Bookmark oldItem, @NonNull Bookmark newItem) {
            return oldItem.getId() == newItem.getId();
        }

        @Override
        public boolean areContentsTheSame(@NonNull Bookmark oldItem, @NonNull Bookmark newItem) {
            return oldItem.getUrl().equals(newItem.getUrl()) && oldItem.getTitle().equals(newItem.getTitle());
        }
    };

    @NonNull
    @Override
    public BookmarkViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_2, parent, false);
        return new BookmarkViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BookmarkViewHolder holder, int position) {
        Bookmark bookmark = getItem(position);
        holder.bind(bookmark, listener);
    }

    static class BookmarkViewHolder extends RecyclerView.ViewHolder {
        private final TextView titleText;
        private final TextView urlText;

        BookmarkViewHolder(@NonNull View itemView) {
            super(itemView);
            titleText = itemView.findViewById(android.R.id.text1);
            urlText = itemView.findViewById(android.R.id.text2);
        }

        void bind(Bookmark bookmark, OnItemClickListener listener) {
            titleText.setText(bookmark.getTitle());
            urlText.setText(bookmark.getUrl());
            itemView.setOnClickListener(v -> listener.onItemClick(bookmark));
        }
    }
}
