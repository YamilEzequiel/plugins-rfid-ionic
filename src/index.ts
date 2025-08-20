import { registerPlugin } from '@capacitor/core';

import type { RFIDPluginPlugin } from './definitions';

const RFIDPlugin = registerPlugin<RFIDPluginPlugin>('RFIDUHF');

export * from './definitions';
export { RFIDPlugin };
