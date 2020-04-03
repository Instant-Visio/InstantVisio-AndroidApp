package fr.tabbya.instantvisio;

import android.util.Log;

import com.google.firebase.functions.FirebaseFunctions;

import java.util.HashMap;
import java.util.Map;

import fr.tabbya.instantvisio.jsonconverter.JsonConverter;
import fr.tabbya.instantvisio.jsonconverter.VisioData;
import io.reactivex.Single;

public class FirebaseService {
    private FirebaseFunctions mFunctions;
    private JsonConverter mJsonConverter;
    private static String FIREBASE_FUNCTION_NAME = "newCall";

    FirebaseService(FirebaseFunctions functions) {
        mFunctions = functions;
        mJsonConverter = new JsonConverter();
    }

    public Single<String> getVisioUrl(String name, String phone, String email) {
        Map<String, Object> arguments = getFirebaseFunctionArguments(name, phone, email);
        return Single.<String>create(emitter -> {
            mFunctions.getHttpsCallable(FIREBASE_FUNCTION_NAME).call(arguments).addOnSuccessListener(httpsCallableResult -> {
                try {
                    VisioData visionData = mJsonConverter.fromJson(httpsCallableResult.getData());
                    String visioUrl = visionData.getUrl();
                    emitter.onSuccess(visioUrl);
                } catch (Exception e) {
                    Log.d("Firebase", "error: " + e);
                    emitter.onError(new Throwable(e));
                }
            }).addOnFailureListener(e -> {
                Log.d("Firebase", "failure listener: " + e);
                emitter.onError(e);
            });
        });
    }

    public Map<String, Object> getFirebaseFunctionArguments(String name, String phone, String email) {
        Map<String, Object> data = new HashMap<>();
        data.put("name", name);
        data.put("phone", phone);
        data.put("email", email);
        data.put("platform", "web");
        return data;
    }
}
