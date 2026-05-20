package fun.cybercode.uniblox.browser.data;

import java.util.UUID;

public class Tab {
    private final String id;
    private String title;
    private String url;
    private boolean isActive;

    public Tab(String title, String url) {
        this.id = UUID.randomUUID().toString();
        this.title = title;
        this.url = url;
        this.isActive = false;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }
}
