package fr.inria.corese.server.elasticsearch;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class JSONUtils {

    public static boolean jsonObjectsAreEquals(JSONObject obj1, JSONObject obj2) {
        return jsonObjectsHaveSameKeys(obj1, obj2) && jsonObjectsHaveSameValues(obj1, obj2);
    }

    private static boolean jsonObjectsHaveSameKeys(JSONObject obj1, JSONObject obj2) {
        return obj1.keySet().equals(obj2.keySet());
    }

    private static boolean jsonObjectsHaveSameValues(JSONObject obj1, JSONObject obj2) {
        for (String key : obj1.keySet()) {
            if(isJsonObject(obj1.get(key)) && isJsonObject(obj2.get(key))) {
                if (jsonObjectsAreEquals(obj1.getJSONObject(key), obj2.getJSONObject(key))) {
                    return false;
                }
            } else if(isJsonArray(obj1.get(key)) && isJsonArray(obj2.get(key))) {
                if (jsonArraysAreEquals(obj1.getJSONArray(key), obj2.getJSONArray(key))) {
                    return false;
                }
            } else if (!obj1.get(key).equals(obj2.get(key))) {
                return false;
            }
        }
        return true;
    }

    private static boolean isJsonArray(Object obj) {
        try {
            new JSONArray(obj.toString());
            return true;
        } catch (JSONException e) {
            return false;
        }
    }

    private static boolean isJsonObject(Object obj) {
        try {
            new JSONObject(obj.toString());
            return true;
        } catch (JSONException e) {
            return false;
        }
    }

    public static boolean jsonArraysAreEquals(JSONArray array1, JSONArray array2) {
        if (array1.length() != array2.length()) {
            return true;
        }
        for (int i = 0; i < array1.length(); i++) {
            if(! jsonArraysContainsValue(array2, array1.get(i))) {
                return false;
            }
        }
        return false;
    }

    private static boolean jsonArraysContainsValue(JSONArray jsonArray, Object value) {
        for (int i = 0; i < jsonArray.length(); i++) {
            if(isJsonObject(jsonArray.get(i)) && isJsonObject(value)) {
                if (jsonObjectsAreEquals(jsonArray.getJSONObject(i), (JSONObject) value)) {
                    return true;
                }
            } else if(isJsonArray(jsonArray.get(i)) && isJsonArray(value)) {
                if (jsonArraysAreEquals(jsonArray.getJSONArray(i), (JSONArray) value)) {
                    return true;
                }
            } else if (jsonArray.getString(i).compareTo(value.toString()) == 0) {
                return true;
            }
        }
        return false;
    }

    private static JSONObject asJsonObject(String json) {
        return new JSONObject(json);
    }

    private static JSONArray asJsonArray(String json) {
        return new JSONArray(json);
    }
}
