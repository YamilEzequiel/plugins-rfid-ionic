# capacitor-plugin-rfid

Capacitor plugin for UHF RFID reading, specifically designed for Chainway C72 devices.

[![Downloads](https://img.shields.io/npm/dt/capacitor-plugin-rfid.svg)](http://npmjs.com/package/capacitor-plugin-rfid)

## Install

```bash
npm install capacitor-plugin-rfid
npx cap sync
```

## Example Ionic - Angular  

https://github.com/YamilEzequiel/rfid-ionic-example

## API

<docgen-index>

* [`initReader()`](#initreader)
* [`startReading()`](#startreading)
* [`stopReading()`](#stopreading)
* [`setPower(...)`](#setpower)
* [`getPower()`](#getpower)
* [`free()`](#free)
* [`getInventoryTag()`](#getinventorytag)
* [`addListener(...)`](#addlistener)
* [`getDeviceId()`](#getdeviceid)

</docgen-index>

<docgen-api>

### initReader()

```typescript
initReader() => Promise<{ success: boolean; message: string }>
```

Initializes the RFID reader.

**Returns:** <code>Promise&lt;{ success: boolean; message: string }&gt;</code>

**Example:**
```typescript
import { RFIDPluginPlugin } from 'capacitor-plugin-rfid';

async initReader() {
  try {
    const result = await RFIDPluginPlugin.initReader();
    console.log('Reader initialized:', result.message);
  } catch (error) {
    console.error('Initialization error:', error);
  }
}
```

--------------------

### startReading()

```typescript
startReading() => Promise<{ success: boolean; message: string }>
```

Starts continuous RFID tag reading.

**Returns:** <code>Promise&lt;{ success: boolean; message: string }&gt;</code>

**Example:**
```typescript
async startRFIDScan() {
  try {
    const result = await RFIDPluginPlugin.startReading();
    console.log('RFID scanning:', result.message);
  } catch (error) {
    console.error('Error starting scan:', error);
  }
}
```

--------------------

### stopReading()

```typescript
stopReading() => Promise<{ success: boolean; message: string }>
```

Stops RFID tag reading.

**Returns:** <code>Promise&lt;{ success: boolean; message: string }&gt;</code>

**Example:**
```typescript
async stopRFIDScan() {
  try {
    const result = await RFIDPluginPlugin.stopReading();
    console.log('RFID scanning stopped:', result.message);
  } catch (error) {
    console.error('Error stopping scan:', error);
  }
}
```

--------------------

### setPower(...)

```typescript
setPower(options: { power: number }) => Promise<{ success: boolean; power: number }>
```

Sets the RFID reader power level (5-30 dBm).

| Param         | Type                            | Description |
| ------------- | ------------------------------- | ----------- |
| **`options`** | <code>{ power: number }</code> | Power value (5-30 dBm) |

**Example:**
```typescript
async setReaderPower(power: number) {
  try {
    const result = await RFIDPluginPlugin.setPower({ power });
    console.log('Power set to:', result.power);
  } catch (error) {
    console.error('Error setting power:', error);
  }
}
```

--------------------

### getPower()

```typescript
getPower() => Promise<{ success: boolean; power: number }>
```

Gets the current power setting of the RFID reader.

**Example:**
```typescript
async getCurrentPower() {
  try {
    const result = await RFIDPluginPlugin.getPower();
    console.log('Current power:', result.power);
  } catch (error) {
    console.error('Error getting power:', error);
  }
}
```

--------------------

### free()

```typescript
free() => Promise<{ success: boolean }>
```

Releases RFID reader resources.

**Example:**
```typescript
async releaseReader() {
  try {
    const result = await RFIDPluginPlugin.free();
    console.log('Reader resources released:', result.success);
  } catch (error) {
    console.error('Error releasing reader:', error);
  }
}
```

--------------------

### getInventoryTag()

```typescript
getInventoryTag() => Promise<{ epc?: string; rssi?: string; success: boolean; message?: string }>
```

Gets the information of the last read tag from the buffer.

**Example:**
```typescript
async getLastTag() {
  try {
    const tag = await RFIDPluginPlugin.getInventoryTag();
    if (tag.success) {
      console.log('Tag EPC:', tag.epc);
      console.log('Tag RSSI:', tag.rssi);
    }
  } catch (error) {
    console.error('Error getting tag:', error);
  }
}
```

--------------------

### addListener(...)

```typescript
addListener(eventName: string, listenerFunc: Function) => Promise<void>
```

Adds a listener for various RFID events.

Available events:
- 'tagFound': Emitted when a new tag is found
- 'keyEvent': Emitted when any key is pressed/released
- 'initSuccess': Emitted when the reader is successfully initialized
- 'initError': Emitted when there's an error during initialization
- 'triggerPressed': Emitted when the trigger button is pressed
- 'triggerReleased': Emitted when the trigger button is released

Event Types:
```typescript
// Tag Found Event
interface TagFoundEvent {
  epc: string;
  rssi: string;
}

// Key Event
interface KeyEvent {
  state: string;
  keyCode: number;
  keyName: string;
}

// Init and Trigger Events
interface MessageEvent {
  message: string;
}
```

**Example using Angular/Ionic Service:**
```typescript
@Injectable({
  providedIn: 'root'
})
export class RFIDService {
  constructor() {
    this.setupListeners();
  }

  private async setupListeners() {
    // Initialize reader
    await RFIDPluginPlugin.initReader();

    // Listen for key events
    RFIDPluginPlugin.addListener('keyEvent', (data: KeyEvent) => {
      console.log(`Key ${data.keyName} (${data.keyCode}) ${data.state}`);
    });

    // Listen for trigger events
    RFIDPluginPlugin.addListener('triggerPressed', (data: MessageEvent) => {
      this.startScanning();
      console.log('Trigger pressed:', data.message);
    });

    RFIDPluginPlugin.addListener('triggerReleased', (data: MessageEvent) => {
      this.stopScanning();
      console.log('Trigger released:', data.message);
    });

    // Listen for tags
    RFIDPluginPlugin.addListener('tagFound', (tag: TagFoundEvent) => {
      console.log('Tag found:', tag.epc, 'RSSI:', tag.rssi);
    });

    // Listen for initialization events
    RFIDPluginPlugin.addListener('initSuccess', (data: MessageEvent) => {
      console.log('Reader initialized:', data.message);
    });

    RFIDPluginPlugin.addListener('initError', (data: MessageEvent) => {
      console.error('Initialization error:', data.message);
    });
  }

  private async startScanning() {
    await RFIDPluginPlugin.startReading();
  }

  private async stopScanning() {
    await RFIDPluginPlugin.stopReading();
  }

  // Example of complete workflow
  async completeWorkflow() {
    try {
      // Initialize
      await RFIDPluginPlugin.initReader();
      
      // Set power to 20 dBm
      await RFIDPluginPlugin.setPower({ power: 20 });
      
      // Start reading
      await RFIDPluginPlugin.startReading();
      
      // Wait for 5 seconds
      await new Promise(resolve => setTimeout(resolve, 5000));
      
      // Stop reading
      await RFIDPluginPlugin.stopReading();
      
      // Get last tag
      const lastTag = await RFIDPluginPlugin.getInventoryTag();
      console.log('Last tag read:', lastTag);
      
      // Free resources
      await RFIDPluginPlugin.free();
    } catch (error) {
      console.error('Workflow error:', error);
    }
  }
}
```

--------------------

### getDeviceId()

```typescript
getDeviceId() => Promise<{ success: boolean, id: string }>
```

Gets the device ID.

--------------------

### Trigger Events
The plugin automatically handles the device's trigger button events. These events are fired when:
- The trigger button (key codes 139, 280, or 293) is pressed or released
- The events are captured through the Android KeyEvent system

**Example of handling trigger events:**
```typescript
// Listen for trigger press
RFIDPluginPlugin.addListener('triggerPressed', (data) => {
  console.log('Trigger pressed:', data.message);
  // Usually you would start reading here
  RFIDPluginPlugin.startReading();
});

// Listen for trigger release
RFIDPluginPlugin.addListener('triggerReleased', (data) => {
  console.log('Trigger released:', data.message);
  // Usually you would stop reading here
  RFIDPluginPlugin.stopReading();
});
```

**Implementation details:**
- The trigger events are handled at the Android native level through MainActivity
- The plugin captures specific key codes (139, 280, 293) that correspond to the device's trigger button
- Events are propagated to the JavaScript layer through Capacitor's event system
- No manual configuration is needed - the events are automatically captured when the plugin is installed

**Best practices:**
- Set up trigger listeners early in your application lifecycle
- Handle both press and release events for complete control
- Consider implementing error handling for failed read attempts
- Clean up listeners when they're no longer needed

## Notes

- This plugin is specifically designed for Chainway C72 devices with UHF RFID capabilities
- Power range is 5-30 dBm
- Key events are captured for all device buttons
- The plugin automatically handles trigger button events (keyCode 139, 280, or 293)
- All async operations return a Promise with a success indicator and relevant data
- Event listeners should be set up early in the application lifecycle
- Remember to free resources when done using the reader
