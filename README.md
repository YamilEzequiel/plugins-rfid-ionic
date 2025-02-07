# capacitor-plugin-rfid

Capacitor plugin for UHF RFID reading, specifically designed for Chainway C72 devices.

## Install

```bash
npm install capacitor-plugin-rfid
npx cap sync
```

## API

<docgen-index>

* [`initialize()`](#initialize)
* [`startReading()`](#startreading)
* [`stopReading()`](#stopreading)
* [`setPower(...)`](#setpower)
* [`getPower()`](#getpower)
* [`setFrequencyRegion(...)`](#setfrequencyregion)
* [`free()`](#free)
* [`getInventoryTag()`](#getinventorytag)
* [`addListener(...)`](#addlistener)

</docgen-index>

<docgen-api>

### initialize()

```typescript
initialize() => Promise<{ success: boolean; message?: string }>
```

Initializes the RFID reader.

**Returns:** <code>Promise&lt;{ success: boolean; message?: string }&gt;</code>

--------------------

### startReading()

```typescript
startReading() => Promise<{ success: boolean }>
```

Starts continuous RFID tag reading. The plugin will emit 'tagRead' events when tags are detected.

**Returns:** <code>Promise&lt;{ success: boolean }&gt;</code>

**Events:** 
- 'tagRead': `{ epc: string; rssi: string }`

--------------------

### stopReading()

```typescript
stopReading() => Promise<{ success: boolean }>
```

Stops RFID tag reading.

**Returns:** <code>Promise&lt;{ success: boolean }&gt;</code>

--------------------

### setPower(...)

```typescript
setPower(options: { power: number }) => Promise<{ success: boolean; power: number }>
```

Sets the RFID reader power.

| Param         | Type                            | Description |
| ------------- | ------------------------------- | ----------- |
| **`options`** | <code>{ power: number }</code> | Power value (5-33 dBm) |

**Returns:** <code>Promise&lt;{ success: boolean; power: number }&gt;</code>

--------------------

### getPower()

```typescript
getPower() => Promise<{ success: boolean; power: number }>
```

Gets the current power setting of the RFID reader.

**Returns:** <code>Promise&lt;{ success: boolean; power: number }&gt;</code>

--------------------

### setFrequencyRegion(...)

```typescript
setFrequencyRegion(options: { area: number }) => Promise<{ success: boolean; area: number }>
```

Sets the frequency region for the RFID reader.

| Param         | Type                           | Description |
| ------------- | ------------------------------ | ----------- |
| **`options`** | <code>{ area: number }</code> | Region value: 1 (China), 2 (USA), 3 (Europe), 4 (India), 5 (Korea), 6 (Japan) |

**Returns:** <code>Promise&lt;{ success: boolean; area: number }&gt;</code>

--------------------

### free()

```typescript
free() => Promise<{ success: boolean }>
```

Releases RFID reader resources.

**Returns:** <code>Promise&lt;{ success: boolean }&gt;</code>

--------------------

### getInventoryTag()

```typescript
getInventoryTag() => Promise<{ epc?: string; rssi?: string; success?: boolean; message?: string }>
```

Gets the information of the last read tag from the buffer.

**Returns:** <code>Promise&lt;{ epc?: string; rssi?: string; success?: boolean; message?: string }&gt;</code>

--------------------

### addListener(...)

```typescript
addListener(eventName: string, callback: Function) => Promise<void>
```

Adds a listener for various RFID events.

Available events:
- 'tagFound': Emitted when a new tag is found
- 'triggerPressed': Emitted when the physical trigger is pressed
- 'triggerReleased': Emitted when the physical trigger is released
- 'initSuccess': Emitted when the reader is successfully initialized
- 'initError': Emitted when there's an error during initialization

| Event | Callback Data |
| ----- | ------------ |
| 'tagFound' | `{ epc: string; rssi: string }` |
| 'triggerPressed' | `{ message: string }` |
| 'triggerReleased' | `{ message: string }` |
| 'initSuccess' | `{ message: string }` |
| 'initError' | `{ message: string }` |

</docgen-api>

## Usage Example

```typescript
import { RFIDUHF } from 'capacitor-plugin-rfid';

// Initialize the reader
const initResult = await RFIDUHF.initialize();
console.log('Initialization:', initResult.success);

// Listen for trigger events
RFIDUHF.addListener('triggerPressed', (data) => {
  console.log('Trigger pressed:', data.message);
  // Start reading when trigger is pressed
  RFIDUHF.startReading();
});

RFIDUHF.addListener('triggerReleased', (data) => {
  console.log('Trigger released:', data.message);
  // Stop reading when trigger is released
  RFIDUHF.stopReading();
});

// Listen for tag reads
RFIDUHF.addListener('tagFound', (tag) => {
  console.log('Tag EPC:', tag.epc);
  console.log('Tag RSSI:', tag.rssi);
});

// Listen for initialization events
RFIDUHF.addListener('initSuccess', (data) => {
  console.log('Init success:', data.message);
});

RFIDUHF.addListener('initError', (data) => {
  console.log('Init error:', data.message);
});

// Example using Angular/Ionic Service
@Injectable({
  providedIn: 'root'
})
export class RFIDService {
  constructor() {
    this.initializeRFID();
  }

  async initializeRFID() {
    await RFIDUHF.initialize();
    
    RFIDUHF.addListener('triggerPressed', async () => {
      await this.startScanning();
    });

    RFIDUHF.addListener('triggerReleased', async () => {
      await this.stopScanning();
    });

    RFIDUHF.addListener('tagFound', (tag) => {
      this.processTag(tag);
    });
  }

  private async startScanning() {
    await RFIDUHF.startReading();
  }

  private async stopScanning() {
    await RFIDUHF.stopReading();
  }

  private processTag(tag: { epc: string; rssi: string }) {
    // Process the tag data
    console.log('Tag procesado:', tag);
  }
}
```

## Notes

- This plugin is specifically designed for Chainway C72 devices with UHF RFID capabilities
- Power range is 5-33 dBm
- The physical trigger (F4 button) can be used to control reading
- Supported frequency regions:
  - 1: China (920-925 MHz)
  - 2: USA (902-928 MHz)
  - 3: Europe (865-867 MHz)
  - 4: India (865-867 MHz)
  - 5: Korea (917-923.5 MHz)
  - 6: Japan (916.8-920.8 MHz)
