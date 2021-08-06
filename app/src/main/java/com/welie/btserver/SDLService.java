package com.welie.btserver;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.os.Handler;
import android.os.Looper;

import com.welie.blessed.BluetoothCentral;
import com.welie.blessed.BluetoothPeripheralManager;

import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.util.UUID;

import timber.log.Timber;

import static android.bluetooth.BluetoothGattCharacteristic.PERMISSION_READ;
import static android.bluetooth.BluetoothGattCharacteristic.PROPERTY_READ;
import static android.bluetooth.BluetoothGattService.SERVICE_TYPE_PRIMARY;

class SDLService extends BaseService {

    private static final UUID SDL_TESTER_SERVICE_UUID = UUID
            .fromString("00001101-0000-1000-8000-00805f9b34fb");
    
    // Characteristic for notifications
    private static final UUID MOBILE_NOTIFICATION_CHARACTERISTIC = UUID
            .fromString("00001102-0000-1000-8000-00805f9b34fb");

    // Characteristic with permissions to read
    private static final UUID MOBILE_REQUEST_CHARACTERISTIC = UUID
            .fromString("00001103-0000-1000-8000-00805f9b34fb");

    // Characteristic with permissions to write
    private static final UUID MOBILE_RESPONSE_CHARACTERISTIC = UUID
            .fromString("00001104-0000-1000-8000-00805f9b34fb");
    // Testing-only characteristic with permissions to write
    
    private static final UUID CHARACTERISTIC_USER_DESCRIPTION_UUID = UUID
            .fromString("00002901-0000-1000-8000-00805f9b34fb");
    private static final UUID CLIENT_CHARACTERISTIC_CONFIGURATION_UUID = UUID
            .fromString("00002902-0000-1000-8000-00805f9b34fb");
    private int messageCounter = 0;
    private ByteBuffer messageCounterBuffer = ByteBuffer.allocate(4);
    private @NotNull final BluetoothGattService SdlService = new BluetoothGattService(SDL_TESTER_SERVICE_UUID, SERVICE_TYPE_PRIMARY);
    private BluetoothGattCharacteristic mMobileNotificationCharacteristic = new BluetoothGattCharacteristic(MOBILE_NOTIFICATION_CHARACTERISTIC,
            BluetoothGattCharacteristic.PROPERTY_NOTIFY,
            /* No permissions */ 0);
    private BluetoothGattCharacteristic mMobileRequestCharacteristic = new BluetoothGattCharacteristic(MOBILE_REQUEST_CHARACTERISTIC,
            PROPERTY_READ, PERMISSION_READ);
    private BluetoothGattCharacteristic mMobileResponseCharacteristic =
            new BluetoothGattCharacteristic(MOBILE_RESPONSE_CHARACTERISTIC,
                                            BluetoothGattCharacteristic.PROPERTY_WRITE,
                                            BluetoothGattCharacteristic.PERMISSION_WRITE);
    private static final String MOBILE_NOTIFICATION_DESCRIPTOR = "Notifications to SDL.";

    private @NotNull final Handler handler = new Handler(Looper.getMainLooper());
    private @NotNull final Runnable notifyRunnable = this::SendRequest;

    public SDLService(@NotNull BluetoothPeripheralManager peripheralManager) {
        super(peripheralManager);

        mMobileNotificationCharacteristic.addDescriptor(
                getClientCharacteristicConfigurationDescriptor());
        mMobileNotificationCharacteristic.addDescriptor(
                getCharacteristicUserDescriptionDescriptor(MOBILE_NOTIFICATION_DESCRIPTOR));

        //some trash value
        mMobileNotificationCharacteristic.setValue(new byte[]{0x00, 0x40});

        /*mMobileRequestCharacteristic.addDescriptor(
                getClientCharacteristicConfigurationDescriptor());
        mMobileRequestCharacteristic.addDescriptor(
                getCharacteristicUserDescriptionDescriptor(MOBILE_REQUEST_DESCRIPTOR));

        mMobileResponseCharacteristic.addDescriptor(
                getClientCharacteristicConfigurationDescriptor());
        mMobileResponseCharacteristic.addDescriptor(
                getCharacteristicUserDescriptionDescriptor(MOBILE_RESPONSE_DESCRIPTOR));*/

        SdlService.addCharacteristic(mMobileNotificationCharacteristic);
        SdlService.addCharacteristic(mMobileRequestCharacteristic);
        SdlService.addCharacteristic(mMobileResponseCharacteristic);

    }

    @Override
    public void onCentralDisconnected(@NotNull BluetoothCentral central) {
        if (noCentralsConnected()) {
            stopNotifying();
        }
    }

    @Override
    public void onNotifyingEnabled(@NotNull BluetoothCentral central, @NotNull BluetoothGattCharacteristic characteristic) {
        Timber.i("onNotifyingEnabled");
        if (characteristic.getUuid().equals(MOBILE_NOTIFICATION_CHARACTERISTIC)) {
            SendRequest();
        }
    }

    @Override
    public void onNotifyingDisabled(@NotNull BluetoothCentral central, @NotNull BluetoothGattCharacteristic characteristic) {
        if (characteristic.getUuid().equals(MOBILE_NOTIFICATION_CHARACTERISTIC)) {
            stopNotifying();
        }
    }

    private void SendRequest() {
        Timber.i("SendRequest");
        String stringified_data = "too long fake string string again";
        byte[] new_message = stringified_data.getBytes();
        mMobileRequestCharacteristic.setValue(new_message);
        messageCounter += 1;
        final byte[] value = new byte[]{(byte) messageCounter};
        notifyCharacteristicChanged(value, mMobileNotificationCharacteristic);
    }
    
    private void stopNotifying() {
        handler.removeCallbacks(notifyRunnable);
    }

    @Override
    public @NotNull BluetoothGattService getService() {
        return SdlService;
    }

    @Override
    public String getServiceName() {
        return "SdlService";
    }

    public BluetoothGattDescriptor getClientCharacteristicConfigurationDescriptor() {
        BluetoothGattDescriptor descriptor = new BluetoothGattDescriptor(
                CLIENT_CHARACTERISTIC_CONFIGURATION_UUID,
                (BluetoothGattDescriptor.PERMISSION_READ | BluetoothGattDescriptor.PERMISSION_WRITE));
        descriptor.setValue(new byte[]{0, 0});
        return descriptor;
    }

    public BluetoothGattDescriptor getCharacteristicUserDescriptionDescriptor(String defaultValue) {
        BluetoothGattDescriptor descriptor = new BluetoothGattDescriptor(
                CHARACTERISTIC_USER_DESCRIPTION_UUID,
                (BluetoothGattDescriptor.PERMISSION_READ | BluetoothGattDescriptor.PERMISSION_WRITE));
        try {
            descriptor.setValue(defaultValue.getBytes("UTF-8"));
        } finally {
            return descriptor;
        }
    }
}