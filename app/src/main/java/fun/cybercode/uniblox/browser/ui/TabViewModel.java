package fun.cybercode.uniblox.browser.ui;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import fun.cybercode.uniblox.browser.data.Tab;

import java.util.ArrayList;
import java.util.List;

public class TabViewModel extends ViewModel {
    private final MutableLiveData<List<Tab>> tabs = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Tab> activeTab = new MutableLiveData<>();

    public TabViewModel() {
        // Initial tab
        addNewTab("UNIBLOX Browser", "file:///android_asset/home.html");
    }

    public LiveData<List<Tab>> getTabs() {
        return tabs;
    }

    public LiveData<Tab> getActiveTab() {
        return activeTab;
    }

    public void addNewTab(String title, String url) {
        List<Tab> currentTabs = tabs.getValue();
        if (currentTabs == null) currentTabs = new ArrayList<>();

        Tab newTab = new Tab(title, url);
        currentTabs.add(newTab);
        tabs.setValue(currentTabs);
        setActiveTab(newTab);
    }

    public void setActiveTab(Tab tab) {
        List<Tab> currentTabs = tabs.getValue();
        if (currentTabs != null) {
            for (Tab t : currentTabs) {
                t.setActive(t.getId().equals(tab.getId()));
            }
            tabs.setValue(currentTabs);
            activeTab.setValue(tab);
        }
    }

    public void closeTab(Tab tab) {
        List<Tab> currentTabs = tabs.getValue();
        if (currentTabs != null && currentTabs.size() > 1) {
            int index = currentTabs.indexOf(tab);
            currentTabs.remove(tab);
            tabs.setValue(currentTabs);

            if (tab.isActive()) {
                int nextIndex = Math.max(0, index - 1);
                setActiveTab(currentTabs.get(nextIndex));
            }
        }
    }

    public void updateActiveTabInfo(String title, String url) {
        Tab current = activeTab.getValue();
        if (current != null) {
            current.setTitle(title);
            current.setUrl(url);
            tabs.setValue(tabs.getValue()); // Trigger observers
        }
    }
}
