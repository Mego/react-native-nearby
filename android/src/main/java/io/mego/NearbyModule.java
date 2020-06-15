package io.mego;

import android.util.Base64;

import androidx.annotation.Nullable;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.AdvertisingOptions;
import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback;
import com.google.android.gms.nearby.connection.ConnectionResolution;
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo;
import com.google.android.gms.nearby.connection.DiscoveryOptions;
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.PayloadCallback;
import com.google.android.gms.nearby.connection.PayloadTransferUpdate;
import com.google.android.gms.nearby.connection.Strategy;

public class NearbyModule extends ReactContextBaseJavaModule {

    class ReceiveBytesPayloadListener extends PayloadCallback {

        @Override
        public void onPayloadReceived(String endpointID, Payload payload) {
            byte[] bytes = payload.asBytes();
            String base64Bytes = Base64.encodeToString(bytes, Base64.DEFAULT);
            WritableMap params = Arguments.createMap();
            params.putString("endpointID", endpointID);
            params.putString("data", base64Bytes);
            sendEvent(reactContext, "PayloadReceived", params);
        }

        @Override
        public void onPayloadTransferUpdate(String endpointID, PayloadTransferUpdate update) {
            WritableMap params = Arguments.createMap();
            params.putString("endpointID", endpointID);
            params.putInt("status", update.getStatus());
            sendEvent(reactContext, "PayloadTransferUpdate", params);
        }
    }

    private final ReceiveBytesPayloadListener payloadCallback = new ReceiveBytesPayloadListener();

    private final ReactApplicationContext reactContext;

    private static Strategy getStrategy(int strategyID) {
        switch(strategyID) {
            case 0:
                return Strategy.P2P_CLUSTER;
            case 1:
                return Strategy.P2P_STAR;
            case 2:
                return Strategy.P2P_POINT_TO_POINT;
            default:
                throw new IllegalArgumentException("strategyID must be between 0 and 2, inclusive");
        }
    }

    public NearbyModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;
    }

    @Override
    public String getName() {
        return "Nearby";
    }

    private void sendEvent(ReactContext reactContext,
                           String eventName,
                           @Nullable WritableMap params) {
        reactContext
                .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                .emit(eventName, params);
    }

    private final EndpointDiscoveryCallback endpointDiscoveryCallback =
            new EndpointDiscoveryCallback() {
                @Override
                public void onEndpointFound(String endpointID, DiscoveredEndpointInfo info) {
                    WritableMap params = Arguments.createMap();
                    params.putString("endpointID", endpointID);
                    params.putString("endpointName", info.getEndpointName());
                    sendEvent(reactContext, "EndpointFound", params);
                }

                @Override
                public void onEndpointLost(String endpointID) {
                    WritableMap params = Arguments.createMap();
                    params.putString("endpointID", endpointID);
                    sendEvent(reactContext, "EndpointLost", params);
                }
            };

    private final ConnectionLifecycleCallback connectionLifecycleCallback =
            new ConnectionLifecycleCallback() {
                @Override
                public void onConnectionInitiated(String endpointID, ConnectionInfo connectionInfo) {
                    WritableMap params = Arguments.createMap();
                    params.putString("endpointID", endpointID);
                    params.putString("endpointName", connectionInfo.getEndpointName());
                    params.putString("authenticationToken", connectionInfo.getAuthenticationToken());
                    sendEvent(reactContext, "NearbyConnectionInitiated", params);
                }

                @Override
                public void onConnectionResult(String endpointID, ConnectionResolution result) {
                    WritableMap params = Arguments.createMap();
                    params.putString("endpointID", endpointID);
                    params.putInt("status", result.getStatus().getStatusCode());
                    sendEvent(reactContext, "NearbyConnectionResult", params);
                }

                @Override
                public void onDisconnected(String endpointID) {
                    // We've been disconnected from this endpoint. No more data can be
                    // sent or received.
                    WritableMap params = Arguments.createMap();
                    params.putString("endpointID", endpointID);
                    sendEvent(reactContext, "NearbyConnectionDisconnected", params);
                }
            };

    @ReactMethod
    public void startAdvertising(Integer strategyID, String nickname, final Promise promise) {
        try {
            Strategy strategy = getStrategy(strategyID);
            AdvertisingOptions advertisingOptions = new AdvertisingOptions.Builder().setStrategy(strategy).build();
            Nearby.getConnectionsClient(getCurrentActivity()).startAdvertising(
                nickname,
                getCurrentActivity().getPackageName(),
                connectionLifecycleCallback,
                advertisingOptions
            ).addOnSuccessListener(promise::resolve)
            .addOnFailureListener(promise::reject);
        } catch (IllegalArgumentException e) {
            promise.reject(e);
        }
    }

    @ReactMethod
    private void startDiscovery(Integer strategyID, String nickname, final Promise promise) {
        try {
            Strategy strategy = getStrategy(strategyID);
            DiscoveryOptions discoveryOptions =
                    new DiscoveryOptions.Builder().setStrategy(strategy).build();
            Nearby.getConnectionsClient(getCurrentActivity())
                    .startDiscovery(getCurrentActivity().getPackageName(),
                            endpointDiscoveryCallback,
                            discoveryOptions)
                    .addOnSuccessListener(promise::resolve)
                    .addOnFailureListener(promise::reject);
        } catch (IllegalArgumentException e) {
            promise.reject(e);
        }
    }

    @ReactMethod
    public void requestConnection(String endpointID, String nickname, final Promise promise) {
        Nearby.getConnectionsClient(getCurrentActivity())
                .requestConnection(nickname, endpointID, connectionLifecycleCallback)
                .addOnSuccessListener(
                        promise::resolve)
                .addOnFailureListener(
                        promise::reject);
    }

    @ReactMethod
    public void acceptConnection(String endpointID, final Promise promise) {
        Nearby.getConnectionsClient(getCurrentActivity()).acceptConnection(endpointID, payloadCallback)
            .addOnSuccessListener(promise::resolve)
            .addOnFailureListener(promise::reject);
    }

    @ReactMethod
    public void rejectConnection(String endpointID, final Promise promise) {
        Nearby.getConnectionsClient(getCurrentActivity()).rejectConnection(endpointID)
            .addOnSuccessListener(promise::resolve)
            .addOnFailureListener(promise::reject);
    }

    @ReactMethod
    public void sendBytesPayload(String endpointID, String base64Bytes, final Promise promise) {
        byte[] bytes = Base64.decode(base64Bytes, Base64.DEFAULT);
        Payload payload = Payload.fromBytes(bytes);
        Nearby.getConnectionsClient(getCurrentActivity()).sendPayload(endpointID, payload)
        .addOnSuccessListener(promise::resolve)
        .addOnFailureListener(promise::reject);
    }
}
