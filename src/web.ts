import { WebPlugin, PluginListenerHandle } from '@capacitor/core';

import type { RFIDPluginPlugin } from './definitions';

export class RFIDPluginWeb extends WebPlugin implements RFIDPluginPlugin {
  async initReader(): Promise<{ success: boolean; message: string }> {
    throw this.unimplemented('Not implemented on web.');
  }

  async startReading(): Promise<{ success: boolean; message: string }> {
    throw this.unimplemented('Not implemented on web.');
  }

  async stopReading(): Promise<{ success: boolean; message: string }> {
    throw this.unimplemented('Not implemented on web.');
  }

  async getDeviceId(): Promise<{ success: boolean; id: string }> {
    throw this.unimplemented('Not implemented on web.');
  }

  async setPower(options: { power: number }): Promise<{ success: boolean; power: number }> {
    throw this.unimplemented('Not implemented on web.');
  }

  async getPower(): Promise<{ success: boolean; power: number }> {
    throw this.unimplemented('Not implemented on web.');
  }

  async free(): Promise<{ success: boolean }> {
    throw this.unimplemented('Not implemented on web.');
  }

  async getInventoryStatus(): Promise<{ isRunning: boolean; success: boolean }> {
    throw this.unimplemented('Not implemented on web.');
  }

  async clearBuffer(): Promise<{ success: boolean; cleared: number; message?: string }> {
    throw this.unimplemented('Not implemented on web.');
  }

  async resetKeyState(): Promise<{ success: boolean; message: string }> {
    throw this.unimplemented('Not implemented on web.');
  }

  async getInventoryTag(): Promise<{ epc?: string; rssi?: string; success: boolean; message?: string }> {
    throw this.unimplemented('Not implemented on web.');
  }

  async startFilteredReading(): Promise<{ success: boolean; message: string; targetCount: number }> {
    throw this.unimplemented('Not implemented on web.');
  }

  async stopFilteredReading(): Promise<{ success: boolean; message: string; foundCount: number; targetCount: number }> {
    throw this.unimplemented('Not implemented on web.');
  }

  async getFilteredReadingStatus(): Promise<{ isRunning: boolean; foundCount: number; targetCount: number; success: boolean }> {
    throw this.unimplemented('Not implemented on web.');
  }

  async clearFoundTags(): Promise<{ success: boolean; cleared: number; message: string }> {
    throw this.unimplemented('Not implemented on web.');
  }
}