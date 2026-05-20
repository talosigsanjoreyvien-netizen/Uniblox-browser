package fun.cybercode.uniblox.browser.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface BookmarkDao {
    @Query("SELECT * FROM bookmarks ORDER BY timestamp DESC")
    LiveData<List<Bookmark>> getAllBookmarks();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertBookmark(Bookmark bookmark);

    @Delete
    void deleteBookmark(Bookmark bookmark);

    @Query("DELETE FROM bookmarks WHERE url = :url")
    void deleteBookmarkByUrl(String url);

    @Query("SELECT * FROM bookmarks WHERE url = :url LIMIT 1")
    Bookmark getBookmarkByUrl(String url);
}
