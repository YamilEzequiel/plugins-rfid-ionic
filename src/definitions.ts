export interface RFIDPluginPlugin {
  /**
   * Inicializa el lector RFID.
   * @returns Promise con el resultado de la inicialización
   * @since 1.0.0
   */
  initReader(): Promise<{
    success: boolean;
    message: string;
  }>;
  /**
   * Inicia la lectura continua de tags RFID.
   * @returns Promise con el resultado del inicio de lectura
   * @since 1.0.0
   */
  startReading(): Promise<{
    success: boolean;
    message: string;
  }>;
  /**
   * Detiene la lectura de tags RFID.
   * @returns Promise con el resultado de la detención
   * @since 1.0.0
   */
  stopReading(): Promise<{
    success: boolean;
    message: string;
  }>;
  /**
   * Obtiene el ID del dispositivo.
   * @returns Promise con el ID del dispositivo
   * @since 1.0.0
   */
  getDeviceId(): Promise<{
    success: boolean;
    id: string;
  }>;
  /**
   * Configura la potencia del lector RFID.
   * @param options Objeto con la potencia a configurar (5-30)
   * @returns Promise con el resultado de la configuración
   * @since 1.0.0
   */
  setPower(options: { power: number }): Promise<{
    success: boolean;
    power: number;
  }>;
  /**
   * Obtiene la potencia actual del lector RFID.
   * @returns Promise con el valor de la potencia actual
   * @since 1.0.0
   */
  getPower(): Promise<{
    success: boolean;
    power: number;
  }>;
  /**
   * Libera los recursos del lector RFID.
   * @returns Promise con el resultado de la liberación
   * @since 1.0.0
   */
  free(): Promise<{
    success: boolean;
  }>;
  /**
   * Obtiene la información del último tag leído del buffer.
   * @returns Promise con la información del tag (EPC y RSSI)
   * @since 1.0.0
   */
  getInventoryTag(): Promise<{
    epc?: string;
    rssi?: string;
    success: boolean;
    message?: string;
  }>;
  /**
   * Obtiene el estado actual del inventario.
   * @returns Promise con el estado del inventario
   * @since 1.0.0
   */
  getInventoryStatus(): Promise<{
    isRunning: boolean;
    success: boolean;
  }>;
  /**
   * Limpia el buffer del lector RFID.
   * @returns Promise con el resultado de la limpieza
   * @since 1.0.0
   */
  clearBuffer(): Promise<{
    success: boolean;
    cleared: number;
    message?: string;
  }>;

  /**
   * Resetea el estado de las teclas en caso de problemas con eventos repetitivos.
   * @returns Promise con el resultado del reseteo
   * @since 1.0.0
   */
  resetKeyState(): Promise<{
    success: boolean;
    message: string;
  }>;

  // ========== EVENTOS ==========
  /**
   * Evento emitido cuando se encuentra un nuevo tag durante la lectura
   * @since 1.0.0
   */
  addListener(
    eventName: 'tagFound',
    listenerFunc: (tag: { epc: string; rssi: string; timestamp?: number }) => void,
  ): Promise<any>;

  /**
   * Eventos emitidos durante la inicialización del lector
   * @since 1.0.0
   */
  addListener(eventName: 'initSuccess' | 'initError', listenerFunc: (data: { message: string }) => void): Promise<any>;
  /**
   * Event emitted when the trigger button is pressed
   * Trigger button codes: 139, 280, or 293
   * @since 1.0.0
   */
  addListener(
    eventName: 'triggerPressed',
    listenerFunc: (data: { message: string; keyCode?: number; timestamp?: number }) => void,
  ): Promise<any>;
  /**
   * Event emitted when the trigger button is released
   * Trigger button codes: 139, 280, or 293
   * @since 1.0.0
   */
  addListener(
    eventName: 'triggerReleased',
    listenerFunc: (data: { message: string; keyCode?: number; timestamp?: number }) => void,
  ): Promise<any>;
  /**
   * Event emitted when the trigger is auto-reset due to timeout
   * @since 1.0.0
   */
  addListener(
    eventName: 'triggerAutoReset',
    listenerFunc: (data: { message: string; reason: string }) => void,
  ): Promise<any>;
  /**
   * Event emitted when key state is manually reset
   * @since 1.0.0
   */
  addListener(
    eventName: 'keyStateReset',
    listenerFunc: (data: { message: string; success: boolean }) => void,
  ): Promise<any>;

  /**
   * Event emitted when a target tag is found during filtered reading
   * @since 1.0.0
   */
  addListener(
    eventName: 'filteredTagFound',
    listenerFunc: (tag: { epc: string; rssi: string; timestamp?: number }) => void,
  ): Promise<any>;

  /**
   * Starts filtered RFID reading that only notifies when new target tags are found.
   * This method is optimized for scenarios where you need to scan for specific tags
   * and want to avoid the performance bottleneck of constant notifications.
   * @returns Promise with the result of starting filtered reading
   * @since 1.0.0
   */
  startFilteredReading(options: { targetTags: string[] }): Promise<{
    success: boolean;
    message: string;
    targetCount: number;
  }>;

  /**
   * Stops filtered RFID reading.
   * @returns Promise with the result and statistics of the filtered reading session
   * @since 1.0.0
   */
  stopFilteredReading(): Promise<{
    success: boolean;
    message: string;
    foundCount: number;
    targetCount: number;
  }>;

  /**
   * Gets the current status of filtered reading session.
   * @returns Promise with current status and statistics
   * @since 1.0.0
   */
  getFilteredReadingStatus(): Promise<{
    isRunning: boolean;
    foundCount: number;
    targetCount: number;
    success: boolean;
  }>;

  /**
   * Clears the internal memory of found tags during filtered reading.
   * This allows the same tags to be detected again as "new" discoveries.
   * @returns Promise with the result of clearing found tags
   * @since 1.0.0
   */
  clearFoundTags(): Promise<{
    success: boolean;
    cleared: number;
    message: string;
  }>;

  /**
   * Event emitted when any key is pressed or released
   * @since 1.0.0
   */
  addListener(
    eventName: 'keyEvent',
    listenerFunc: (data: { state: string; keyCode: number; keyName: string }) => void,
  ): Promise<any>;

  /**
   * Event emitted when a tag is found using the inventory callback
   * @since 1.0.0
   */
  addListener(
    eventName: 'tagFoundInventory',
    listenerFunc: (tag: { epc: string; rssi: string }) => void,
  ): Promise<any>;

  /**
   * Simulates a key press for testing purposes
   * @param options Object with keyCode to simulate (default: 293)
   * @returns Promise with simulation result
   * @since 1.0.0
   */
  simulateKeyPress(options?: { keyCode?: number }): Promise<{
    success: boolean;
    message: string;
  }>;

  /**
   * Stops the InfoWedge monitoring system
   * @returns Promise with stop result
   * @since 1.0.0
   */
  stopInfoWedgeMonitoring(): Promise<{
    success: boolean;
    message: string;
  }>;

  /**
   * Checks if the accessibility service is enabled
   * @returns Promise with accessibility status
   * @since 1.0.0
   */
  checkAccessibilityPermission(): Promise<{
    enabled: boolean;
    success: boolean;
    message: string;
  }>;

  /**
   * Requests accessibility permission by opening settings
   * @returns Promise with request result
   * @since 1.0.0
   */
  requestAccessibilityPermission(): Promise<{
    success: boolean;
    message: string;
  }>;

  /**
   * Tests the complete key event flow for debugging
   * @returns Promise with test result
   * @since 1.0.0
   */
  testKeyEventFlow(): Promise<{
    success: boolean;
    message: string;
  }>;
}
