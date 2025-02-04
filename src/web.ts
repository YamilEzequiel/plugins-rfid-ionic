import { WebPlugin } from '@capacitor/core';

import type { RFIDPluginPlugin } from './definitions';

export class RFIDPluginWeb extends WebPlugin implements RFIDPluginPlugin {
  async initialize(): Promise<{ success: boolean }> {
    throw this.unimplemented('Not implemented on web.');
  }

  async startReading(): Promise<{ success: boolean }> {
    throw this.unimplemented('Not implemented on web.');
  }

  async stopReading(): Promise<{ success: boolean }> {
    throw this.unimplemented('Not implemented on web.');
  }

  async setPower(options: { power: number }): Promise<{ success: boolean }> {
    throw this.unimplemented('Not implemented on web.');
  }

  async free(): Promise<{ success: boolean }> {
    throw this.unimplemented('Not implemented on web.');
  }

  async getInventoryTag(): Promise<{ epc?: string; rssi?: string; success: boolean; message?: string }> {
    throw this.unimplemented('Not implemented on web.');
  }
}