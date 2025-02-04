# capacitor-plugin-rfid

Capacitor plugin for UHF RFID reading

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
* [`free()`](#free)
* [`getInventoryTag()`](#getinventorytag)

</docgen-index>

<docgen-api>

### initialize()

```typescript
initialize() => Promise<{ success: boolean }>
```

Initializes the RFID reader.

**Returns:** <code>Promise&lt;{ success: boolean }&gt;</code>

--------------------

### startReading()

```typescript
startReading() => Promise<{ success: boolean }>
```

Starts continuous RFID tag reading.

**Returns:** <code>Promise&lt;{ success: boolean }&gt;</code>

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
setPower(options: { power: number }) => Promise<{ success: boolean }>
```

Sets the RFID reader power.

| Param         | Type                            | Description |
| ------------- | ------------------------------- | ----------- |
| **`options`** | <code>{ power: number }</code> | Power value (default: 15) |

**Returns:** <code>Promise&lt;{ success: boolean }&gt;</code>

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

</docgen-api>

## Usage Example

```typescript
import { RFIDUHF } from 'capacitor-plugin-rfid';

// Initialize the reader
await RFIDUHF.initialize();

// Set power
await RFIDUHF.setPower({ power: 20 });

// Start reading
await RFIDUHF.startReading();

// Get read tags
const tagInfo = await RFIDUHF.getInventoryTag();
console.log('EPC:', tagInfo.epc);
console.log('RSSI:', tagInfo.rssi);

// Stop reading
await RFIDUHF.stopReading();

// Free resources
await RFIDUHF.free();
```
