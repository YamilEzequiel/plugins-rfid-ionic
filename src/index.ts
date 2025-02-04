import { registerPlugin } from '@capacitor/core';

import type { RFIDPluginPlugin } from './definitions';

const RFIDPlugin = registerPlugin<RFIDPluginPlugin>('RFIDPlugin');

export * from './definitions';
export { RFIDPlugin };
