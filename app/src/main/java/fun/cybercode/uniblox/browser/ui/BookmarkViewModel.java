package fun.cybercode.uniblox.browser.ui;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import fun.cybercode.uniblox.browser.data.AppDatabase;
import fun.cybercode.uniblox.browser.data.Bookmark;
import fun.cybercode.uniblox.browser.data.BookmarkDao;

import java.util.List;

public class BookmarkViewModel extends AndroidViewModel {
    private final BookmarkDao bookmarkDao;
    private final LiveData<List<Bookmark>> allBookmarks;

    public BookmarkViewModel(@NonNull Application application) {
        super(application);
        AppDatabase db = AppDatabase.getDatabase(application);
        bookmarkDao = db.bookmarkDao();
        allBookmarks = bookmarkDao.getAllBookmarks();
    }

    public LiveData<List<Bookmark>> getAllBookmarks() {
        return allBookmarks;
    }

    public void insert(Bookmark bookmark) {
        AppDatabase.databaseWriteExecutor.execute(() -> bookmarkDao.insertBookmark(bookmark));
    }

    public void delete(Bookmark bookmark) {
        AppDatabase.databaseWriteExecutor.execute(() -> bookmarkDao.deleteBookmark(bookmark));
    }

    public void deleteByUrl(String url) {
        AppDatabase.databaseWriteExecutor.execute(() -> bookmarkDao.deleteBookmarkByUrl(url));
    }
    
    public void toggleBookmark(String title, String url) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            Bookmark existing = bookmarkDao.getBookmarkByUrl(url);
            if (existing != null) {
                bookmarkDao.deleteBookmark(existing);
            } else {
                bookmarkDao.insertBookmark(new Bookmark(title, url, System.currentTimeMillis()));
            }
        });
    }
}
