package fr.tabbya.instantvisio.jsonconverter;

public class VisioData {
    String name;
    String privacy;
    String created_at;
    String roomUrl;
    String id;
    boolean api_created;
    VisioConfig config;
    String url;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPrivacy() {
        return privacy;
    }

    public void setPrivacy(String privacy) {
        this.privacy = privacy;
    }

    public String getCreated_at() {
        return created_at;
    }

    public void setCreated_at(String created_at) {
        this.created_at = created_at;
    }

    public String getRoomUrl() {
        return roomUrl;
    }

    public void setRoomUrl(String roomUrl) {
        this.roomUrl = roomUrl;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public boolean isApi_created() {
        return api_created;
    }

    public void setApi_created(boolean api_created) {
        this.api_created = api_created;
    }

    public VisioConfig getConfig() {
        return config;
    }

    public void setConfig(VisioConfig config) {
        this.config = config;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
