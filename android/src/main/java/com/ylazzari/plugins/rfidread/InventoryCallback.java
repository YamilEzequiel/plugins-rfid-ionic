package com.ylazzari.plugins.rfidread;

import com.rscja.deviceapi.entity.UHFTAGInfo;

/**
 * Interface para callback de inventario RFID
 * @deprecated Use IUHFInventoryCallback from device API instead
 */
@Deprecated
public interface InventoryCallback {
    void callback(UHFTAGInfo tagInfo);
}