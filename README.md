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
* [`startFilteredReading(...)`](#startfilteredreading)
* [`stopFilteredReading()`](#stopfilteredreading)
* [`getFilteredReadingStatus()`](#getfilteredreadingstatus)
* [`clearFoundTags()`](#clearfoundtags)
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
import { RFIDPlugin } from 'capacitor-plugin-rfid';

async initReader() {
  try {
    const result = await RFIDPlugin.initReader();
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
    const result = await RFIDPlugin.startReading();
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
    const result = await RFIDPlugin.stopReading();
    console.log('RFID scanning stopped:', result.message);
  } catch (error) {
    console.error('Error stopping scan:', error);
  }
}
```

--------------------

### startFilteredReading(...)

```typescript
startFilteredReading(options: { targetTags: string[] }) => Promise<{ success: boolean; message: string; targetCount: number }>
```

Starts filtered RFID reading that only notifies when new target tags are found. This method is optimized for scenarios where you need to scan for specific tags and want to avoid the performance bottleneck of constant notifications.

**Performance Benefits:**
- Only notifies when a target tag is found for the first time
- Uses efficient HashSet lookups (O(1) complexity)
- Reduces bridge communication overhead significantly
- Ideal for bulk scanning scenarios

| Param         | Type                              | Description |
| ------------- | --------------------------------- | ----------- |
| **`options`** | <code>{ targetTags: string[] }</code> | Array of target EPC strings to monitor |

**Returns:** <code>Promise&lt;{ success: boolean; message: string; targetCount: number }&gt;</code>

**Example:**
```typescript
async startFilteredScan() {
  const targetTags = [
    'E2806894000040002000000001',
    'E2806894000040002000000002',
    'E2806894000040002000000003'
  ];
  
  try {
    const result = await RFIDPlugin.startFilteredReading({ targetTags });
    console.log('Filtered reading started:', result.message);
    console.log('Monitoring', result.targetCount, 'target tags');
  } catch (error) {
    console.error('Error starting filtered scan:', error);
  }
}
```

--------------------

### stopFilteredReading()

```typescript
stopFilteredReading() => Promise<{ success: boolean; message: string; foundCount: number; targetCount: number }>
```

Stops filtered RFID reading and returns statistics about the session.

**Returns:** <code>Promise&lt;{ success: boolean; message: string; foundCount: number; targetCount: number }&gt;</code>

**Example:**
```typescript
async stopFilteredScan() {
  try {
    const result = await RFIDPlugin.stopFilteredReading();
    console.log('Filtered reading stopped:', result.message);
    console.log(`Found ${result.foundCount} of ${result.targetCount} target tags`);
  } catch (error) {
    console.error('Error stopping filtered scan:', error);
  }
}
```

--------------------

### getFilteredReadingStatus()

```typescript
getFilteredReadingStatus() => Promise<{ isRunning: boolean; foundCount: number; targetCount: number; success: boolean }>
```

Gets the current status of the filtered reading session with real-time statistics.

**Returns:** <code>Promise&lt;{ isRunning: boolean; foundCount: number; targetCount: number; success: boolean }&gt;</code>

**Example:**
```typescript
async checkFilteredStatus() {
  try {
    const status = await RFIDPlugin.getFilteredReadingStatus();
    console.log('Filtered reading running:', status.isRunning);
    console.log(`Progress: ${status.foundCount}/${status.targetCount} tags found`);
  } catch (error) {
    console.error('Error getting status:', error);
  }
}
```

--------------------

### clearFoundTags()

```typescript
clearFoundTags() => Promise<{ success: boolean; cleared: number; message: string }>
```

Clears the internal memory of found tags during filtered reading. This allows the same tags to be detected again as "new" discoveries.

**Returns:** <code>Promise&lt;{ success: boolean; cleared: number; message: string }&gt;</code>

**Example:**
```typescript
async resetFoundTags() {
  try {
    const result = await RFIDPlugin.clearFoundTags();
    console.log('Found tags cleared:', result.message);
    console.log('Cleared', result.cleared, 'tags from memory');
  } catch (error) {
    console.error('Error clearing found tags:', error);
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
    const result = await RFIDPlugin.setPower({ power });
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
    const result = await RFIDPlugin.getPower();
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
    const result = await RFIDPlugin.free();
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
    const tag = await RFIDPlugin.getInventoryTag();
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
- 'tagFound': Emitted when a new tag is found during regular reading
- 'filteredTagFound': Emitted when a target tag is found during filtered reading
- 'tagFoundInventory': Emitted when a new tag is found using the inventory callback
- 'keyEvent': Emitted when any key is pressed/released (all device keys)
- 'initSuccess': Emitted when the reader is successfully initialized
- 'initError': Emitted when there's an error during initialization
- 'triggerPressed': Emitted when the trigger button is pressed (key codes 139, 280, 293)
- 'triggerReleased': Emitted when the trigger button is released (key codes 139, 280, 293)
- 'triggerAutoReset': Emitted when the trigger is auto-reset due to timeout
- 'keyStateReset': Emitted when key state is manually reset

Event Types:
```typescript
// Tag Found Event (regular reading)
interface TagFoundEvent {
  epc: string;
  rssi: string;
  timestamp?: number;
}

// Filtered Tag Found Event (filtered reading)
interface FilteredTagFoundEvent {
  epc: string;
  rssi: string;
  timestamp?: number;
}

// Tag Found Inventory Event
interface TagFoundInventoryEvent {
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
    await RFIDPlugin.initReader();

    // Listen for key events
    RFIDPlugin.addListener('keyEvent', (data: KeyEvent) => {
      console.log(`Key ${data.keyName} (${data.keyCode}) ${data.state}`);
    });

    // Listen for trigger events
    RFIDPlugin.addListener('triggerPressed', (data: MessageEvent) => {
      this.startScanning();
      console.log('Trigger pressed:', data.message);
    });

    RFIDPlugin.addListener('triggerReleased', (data: MessageEvent) => {
      this.stopScanning();
      console.log('Trigger released:', data.message);
    });

    // Listen for tags during regular reading
    RFIDPlugin.addListener('tagFound', (tag: TagFoundEvent) => {
      console.log('Tag found:', tag.epc, 'RSSI:', tag.rssi);
    });

    // Listen for target tags during filtered reading
    RFIDPlugin.addListener('filteredTagFound', (tag: FilteredTagFoundEvent) => {
      console.log('Target tag found:', tag.epc, 'RSSI:', tag.rssi);
      // This will only fire once per unique target tag
    });

    // Listen for tags found using the inventory callback
    RFIDPlugin.addListener('tagFoundInventory', (tag: TagFoundInventoryEvent) => {
      console.log('Tag found using inventory callback:', tag.epc, 'RSSI:', tag.rssi);
    });

    // Listen for initialization events
    RFIDPlugin.addListener('initSuccess', (data: MessageEvent) => {
      console.log('Reader initialized:', data.message);
    });

    RFIDPlugin.addListener('initError', (data: MessageEvent) => {
      console.error('Initialization error:', data.message);
    });
  }

  private async startScanning() {
    await RFIDPlugin.startReading();
  }

  private async stopScanning() {
    await RFIDPlugin.stopReading();
  }

  // Example of complete workflow
  async completeWorkflow() {
    try {
      // Initialize
      await RFIDPlugin.initReader();
      
      // Set power to 20 dBm
      await RFIDPlugin.setPower({ power: 20 });
      
      // Start reading
      await RFIDPlugin.startReading();
      
      // Wait for 5 seconds
      await new Promise(resolve => setTimeout(resolve, 5000));
      
      // Stop reading
      await RFIDPlugin.stopReading();
      
      // Get last tag
      const lastTag = await RFIDPlugin.getInventoryTag();
      console.log('Last tag read:', lastTag);
      
      // Free resources
      await RFIDPlugin.free();
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
RFIDPlugin.addListener('triggerPressed', (data) => {
  console.log('Trigger pressed:', data.message);
  // Usually you would start reading here
  RFIDPlugin.startReading();
});

// Listen for trigger release
RFIDPlugin.addListener('triggerReleased', (data) => {
  console.log('Trigger released:', data.message);
  // Usually you would stop reading here
  RFIDPlugin.stopReading();
});
```

**Implementation details:**
- The trigger events are handled at the Android native level through MainActivity
- The plugin captures specific key codes (139, 280, 293) that correspond to the device's trigger button
- Events are propagated to the JavaScript layer through Capacitor's event system
- No manual configuration is needed - the events are automatically captured when the plugin is installed
- Key events include debounce protection (300ms) and auto-reset timeout (5 seconds)

**Best practices:**
- Set up trigger listeners early in your application lifecycle
- Handle both press and release events for complete control
- Consider implementing error handling for failed read attempts
- Clean up listeners when they're no longer needed
- Use `resetKeyState()` if you encounter sticky key states

## Ionic Angular Implementation Guide

### Step 1: Install the Plugin

```bash
npm install capacitor-plugin-rfid
npx cap sync android
```

### Step 2: Create an RFID Service

```typescript
// src/app/services/rfid.service.ts
import { Injectable } from '@angular/core';
import { RFIDPlugin } from 'capacitor-plugin-rfid';
import { BehaviorSubject } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class RfidService {
  private isInitialized = new BehaviorSubject<boolean>(false);
  private foundTags = new BehaviorSubject<any[]>([]);
  private isScanning = new BehaviorSubject<boolean>(false);

  constructor() {
    this.initializePlugin();
  }

  async initializePlugin() {
    try {
      const result = await RFIDPlugin.initReader();
      console.log('RFID Plugin initialized:', result.message);
      this.isInitialized.next(result.success);
      this.setupListeners();
    } catch (error) {
      console.error('Error initializing RFID plugin:', error);
      this.isInitialized.next(false);
    }
  }

  private async setupListeners() {
    // Listen for trigger events
    RFIDPlugin.addListener('triggerPressed', (data) => {
      console.log('Trigger pressed:', data.message);
      this.startScanning();
    });

    RFIDPlugin.addListener('triggerReleased', (data) => {
      console.log('Trigger released:', data.message);
      this.stopScanning();
    });

    // Listen for tags
    RFIDPlugin.addListener('tagFound', (tag) => {
      console.log('Tag found:', tag.epc, 'RSSI:', tag.rssi);
      this.addFoundTag(tag);
    });

    // Listen for filtered tags
    RFIDPlugin.addListener('filteredTagFound', (tag) => {
      console.log('Target tag found:', tag.epc, 'RSSI:', tag.rssi);
      this.addFoundTag(tag);
    });

    // Error handling
    RFIDPlugin.addListener('initError', (data) => {
      console.error('RFID Init Error:', data.message);
    });
  }

  async startScanning() {
    try {
      const result = await RFIDPlugin.startReading();
      console.log('Scanning started:', result.message);
      this.isScanning.next(true);
      return result;
    } catch (error) {
      console.error('Error starting scan:', error);
      this.isScanning.next(false);
      throw error;
    }
  }

  async stopScanning() {
    try {
      const result = await RFIDPlugin.stopReading();
      console.log('Scanning stopped:', result.message);
      this.isScanning.next(false);
      return result;
    } catch (error) {
      console.error('Error stopping scan:', error);
      throw error;
    }
  }

  async startFilteredScanning(targetTags: string[]) {
    try {
      const result = await RFIDPlugin.startFilteredReading({ targetTags });
      console.log('Filtered scanning started:', result.message);
      this.isScanning.next(true);
      return result;
    } catch (error) {
      console.error('Error starting filtered scan:', error);
      this.isScanning.next(false);
      throw error;
    }
  }

  async setPower(power: number) {
    try {
      const result = await RFIDPlugin.setPower({ power });
      console.log('Power set to:', result.power);
      return result;
    } catch (error) {
      console.error('Error setting power:', error);
      throw error;
    }
  }

  async resetKeyState() {
    try {
      const result = await RFIDPlugin.resetKeyState();
      console.log('Key state reset:', result.message);
      return result;
    } catch (error) {
      console.error('Error resetting key state:', error);
      throw error;
    }
  }

  private addFoundTag(tag: any) {
    const currentTags = this.foundTags.value;
    const exists = currentTags.find(t => t.epc === tag.epc);
    if (!exists) {
      this.foundTags.next([...currentTags, tag]);
    }
  }

  // Observables for components
  get isInitialized$() { return this.isInitialized.asObservable(); }
  get foundTags$() { return this.foundTags.asObservable(); }
  get isScanning$() { return this.isScanning.asObservable(); }

  clearFoundTags() {
    this.foundTags.next([]);
  }
}
```

### Step 3: Use in Your Component

```typescript
// src/app/pages/scanner/scanner.page.ts
import { Component, OnInit, OnDestroy } from '@angular/core';
import { RfidService } from '../../services/rfid.service';
import { Observable, Subscription } from 'rxjs';

@Component({
  selector: 'app-scanner',
  templateUrl: './scanner.page.html',
  styleUrls: ['./scanner.page.scss'],
})
export class ScannerPage implements OnInit, OnDestroy {
  isInitialized$: Observable<boolean>;
  foundTags$: Observable<any[]>;
  isScanning$: Observable<boolean>;
  
  private subscriptions: Subscription[] = [];
  power = 20;
  targetTags: string[] = [];

  constructor(private rfidService: RfidService) {
    this.isInitialized$ = this.rfidService.isInitialized$;
    this.foundTags$ = this.rfidService.foundTags$;
    this.isScanning$ = this.rfidService.isScanning$;
  }

  ngOnInit() {
    // Component initialization
  }

  ngOnDestroy() {
    this.subscriptions.forEach(sub => sub.unsubscribe());
  }

  async onStartScan() {
    try {
      await this.rfidService.startScanning();
    } catch (error) {
      console.error('Error starting scan:', error);
    }
  }

  async onStopScan() {
    try {
      await this.rfidService.stopScanning();
    } catch (error) {
      console.error('Error stopping scan:', error);
    }
  }

  async onStartFilteredScan() {
    if (this.targetTags.length === 0) {
      console.warn('No target tags specified');
      return;
    }
    
    try {
      await this.rfidService.startFilteredScanning(this.targetTags);
    } catch (error) {
      console.error('Error starting filtered scan:', error);
    }
  }

  async onSetPower() {
    try {
      await this.rfidService.setPower(this.power);
    } catch (error) {
      console.error('Error setting power:', error);
    }
  }

  onClearTags() {
    this.rfidService.clearFoundTags();
  }

  async onResetKeyState() {
    try {
      await this.rfidService.resetKeyState();
    } catch (error) {
      console.error('Error resetting key state:', error);
    }
  }

  addTargetTag(tag: string) {
    if (tag.trim() && !this.targetTags.includes(tag.trim())) {
      this.targetTags.push(tag.trim());
    }
  }

  removeTargetTag(index: number) {
    this.targetTags.splice(index, 1);
  }
}
```

### Step 4: Template Example

```html
<!-- src/app/pages/scanner/scanner.page.html -->
<ion-header>
  <ion-toolbar>
    <ion-title>RFID Scanner</ion-title>
  </ion-toolbar>
</ion-header>

<ion-content>
  <div *ngIf="isInitialized$ | async; else notInitialized">
    
    <!-- Power Control -->
    <ion-card>
      <ion-card-header>
        <ion-card-title>Power Control</ion-card-title>
      </ion-card-header>
      <ion-card-content>
        <ion-item>
          <ion-label>Power (5-30 dBm)</ion-label>
          <ion-input type="number" [(ngModel)]="power" min="5" max="30"></ion-input>
          <ion-button slot="end" (click)="onSetPower()">Set</ion-button>
        </ion-item>
      </ion-card-content>
    </ion-card>

    <!-- Scanning Controls -->
    <ion-card>
      <ion-card-header>
        <ion-card-title>Scanning</ion-card-title>
      </ion-card-header>
      <ion-card-content>
        <ion-button expand="block" (click)="onStartScan()" [disabled]="isScanning$ | async">
          Start Scanning
        </ion-button>
        <ion-button expand="block" (click)="onStopScan()" [disabled]="!(isScanning$ | async)">
          Stop Scanning
        </ion-button>
        <ion-button expand="block" fill="outline" (click)="onResetKeyState()">
          Reset Key State
        </ion-button>
      </ion-card-content>
    </ion-card>

    <!-- Filtered Scanning -->
    <ion-card>
      <ion-card-header>
        <ion-card-title>Filtered Scanning</ion-card-title>
      </ion-card-header>
      <ion-card-content>
        <ion-item>
          <ion-label position="stacked">Target Tags</ion-label>
          <ion-textarea [(ngModel)]="newTargetTag" placeholder="Enter EPC tags (one per line)"></ion-textarea>
        </ion-item>
        <ion-button expand="block" (click)="onStartFilteredScan()" [disabled]="isScanning$ | async">
          Start Filtered Scan
        </ion-button>
      </ion-card-content>
    </ion-card>

    <!-- Found Tags -->
    <ion-card>
      <ion-card-header>
        <ion-card-title>
          Found Tags
          <ion-button size="small" fill="clear" (click)="onClearTags()">Clear</ion-button>
        </ion-card-title>
      </ion-card-header>
      <ion-card-content>
        <ion-list *ngIf="(foundTags$ | async)?.length > 0; else noTags">
          <ion-item *ngFor="let tag of foundTags$ | async">
            <ion-label>
              <h3>{{ tag.epc }}</h3>
              <p>RSSI: {{ tag.rssi }}</p>
              <p *ngIf="tag.timestamp">{{ tag.timestamp | date:'medium' }}</p>
            </ion-label>
          </ion-item>
        </ion-list>
        <ng-template #noTags>
          <p>No tags found yet...</p>
        </ng-template>
      </ion-card-content>
    </ion-card>

  </div>

  <ng-template #notInitialized>
    <ion-card>
      <ion-card-content>
        <p>RFID Plugin is not initialized. Please check your device and try again.</p>
      </ion-card-content>
    </ion-card>
  </ng-template>
</ion-content>
```

**Important Notes for Ionic Implementation:**

1. **Permissions**: Make sure your Android app has the necessary permissions in `android/app/src/main/AndroidManifest.xml`
2. **Device Support**: This plugin is specifically designed for Chainway C72 devices
3. **Lifecycle**: Initialize the RFID service early in your app lifecycle (preferably in a service)
4. **Error Handling**: Always wrap RFID operations in try-catch blocks
5. **Memory Management**: Clear found tags and stop scanning when leaving the page
6. **Key Events**: The trigger button events are automatically handled - you just need to listen for them

## Performance Optimization: When to Use Each Reading Method

### Regular Reading (`startReading()`)
Use this method when:
- You need to detect all RFID tags in range
- Real-time tag discovery is more important than performance
- You're scanning a small number of tags (< 50)
- You need immediate notification of every tag read

### Filtered Reading (`startFilteredReading()`)
Use this method when:
- You're looking for specific target tags from a known list
- You're scanning large quantities of tags (> 50)
- Performance and reduced bridge communication is critical
- You want to avoid duplicate notifications for the same tag
- You're implementing inventory checking against a database

**Performance Benefits of Filtered Reading:**
- **Reduced Bridge Overhead**: Only sends notifications for target tags, not every tag detected
- **Memory Efficient**: Uses HashSet for O(1) lookup performance
- **Duplicate Prevention**: Each target tag only notifies once until memory is cleared
- **Scalable**: Performance doesn't degrade with large target lists

## Example: Complete Filtered Reading Workflow

```typescript
@Injectable({
  providedIn: 'root'
})
export class OptimizedRFIDService {
  private targetTags: string[] = [];
  private foundTags: Set<string> = new Set();

  constructor() {
    this.setupFilteredListeners();
  }

  private async setupFilteredListeners() {
    await RFIDPlugin.initReader();

    // Listen only for target tags
    RFIDPlugin.addListener('filteredTagFound', (tag: FilteredTagFoundEvent) => {
      console.log('Target tag discovered:', tag.epc);
      this.foundTags.add(tag.epc);
      this.processFoundTag(tag);
    });

    // Setup trigger events for filtered reading
    RFIDPlugin.addListener('triggerPressed', () => {
      this.startFilteredScan();
    });

    RFIDPlugin.addListener('triggerReleased', () => {
      this.stopFilteredScan();
    });
  }

  async startInventoryCheck(expectedTags: string[]) {
    this.targetTags = expectedTags;
    this.foundTags.clear();
    
    try {
      const result = await RFIDPlugin.startFilteredReading({ 
        targetTags: this.targetTags 
      });
      console.log(`Started scanning for ${result.targetCount} target tags`);
    } catch (error) {
      console.error('Error starting inventory check:', error);
    }
  }

  async getInventoryResults() {
    const status = await RFIDPlugin.getFilteredReadingStatus();
    const missing = this.targetTags.filter(tag => !this.foundTags.has(tag));
    
    return {
      total: status.targetCount,
      found: status.foundCount,
      missing: missing.length,
      missingTags: missing,
      foundTags: Array.from(this.foundTags)
    };
  }

  private processFoundTag(tag: FilteredTagFoundEvent) {
    // Custom logic for each found target tag
    // This will only execute once per unique target tag
  }
}
```

## Notes

- This plugin is specifically designed for Chainway C72 devices with UHF RFID capabilities
- Power range is 5-30 dBm
- Key events are captured for all device buttons
- The plugin automatically handles trigger button events (keyCode 139, 280, or 293)
- All async operations return a Promise with a success indicator and relevant data
- Event listeners should be set up early in the application lifecycle
- Remember to free resources when done using the reader
- For bulk scanning scenarios (>50 tags), use `startFilteredReading()` for optimal performance
