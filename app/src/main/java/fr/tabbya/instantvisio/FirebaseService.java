package fr.tabbya.instantvisio;

import com.google.firebase.functions.FirebaseFunctions;
import java.util.HashMap;
import java.util.Map;
import fr.tabbya.instantvisio.jsonconverter.JsonConverter;
import fr.tabbya.instantvisio.jsonconverter.VisioData;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;

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
                    emitter.onError(new Throwable(e));
                }
            });
        }).observeOn(Schedulers.io()).subscribeOn(Schedulers.io());
    }

    public Map<String, Object> getFirebaseFunctionArguments(String name, String phone, String email) {
        Map<String, Object> data = new HashMap<>();
        data.put("name", name);
        data.put("phone", phone);
        data.put("email", email);
        return data;
    }
}
