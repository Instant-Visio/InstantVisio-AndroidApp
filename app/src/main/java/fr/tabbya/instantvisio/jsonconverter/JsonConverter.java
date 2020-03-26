package fr.tabbya.instantvisio.jsonconverter;

import com.google.gson.Gson;

public class JsonConverter {
    Gson gson;

    public JsonConverter() {
        gson = new Gson();
    }

    public VisioData fromJson(Object firebaseFunctionResponse) {
        try {
            String json = gson.toJson(firebaseFunctionResponse);
            return gson.fromJson(json, VisioData.class);
        } catch(Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
