export interface RFIDPluginPlugin {
  /**
   * Inicializa el lector RFID.
   * @returns Promise con el resultado de la inicialización
   * @since 1.0.0
   */
  initReader(): Promise<{ success: boolean, message: string }>;

  /**
   * Inicia la lectura continua de tags RFID.
   * @returns Promise con el resultado del inicio de lectura
   * @since 1.0.0
   */
  startReading(): Promise<{ success: boolean, message: string }>;

  /**
   * Detiene la lectura de tags RFID.
   * @returns Promise con el resultado de la detención
   * @since 1.0.0
   */
  stopReading(): Promise<{ success: boolean, message: string }>;

  /**
   * Obtiene el ID del dispositivo.
   * @returns Promise con el ID del dispositivo
   * @since 1.0.0
   */
  getDeviceId(): Promise<{ success: boolean, id: string }>;

  /**
   * Configura la potencia del lector RFID.
   * @param options Objeto con la potencia a configurar (5-30)
   * @returns Promise con el resultado de la configuración
   * @since 1.0.0
   */
  setPower(options: { power: number }): Promise<{ success: boolean, power: number }>;

  /**
   * Obtiene la potencia actual del lector RFID.
   * @returns Promise con el valor de la potencia actual
   * @since 1.0.0
   */
  getPower(): Promise<{ success: boolean, power: number }>;

  /**
   * Libera los recursos del lector RFID.
   * @returns Promise con el resultado de la liberación
   * @since 1.0.0
   */
  free(): Promise<{ success: boolean }>;

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
   * Resetea el estado de las teclas en caso de problemas con eventos repetitivos.
   * @returns Promise con el resultado del reseteo
   * @since 1.0.0
   */
  resetKeyState(): Promise<{ success: boolean, message: string }>;

  /**
   * Evento emitido cuando se encuentra un nuevo tag
   * @since 1.0.0
   */
  addListener(
    eventName: 'tagFound',
    listenerFunc: (tag: { epc: string, rssi: string, timestamp?: number }) => void
  ): Promise<void>;

  /**
   * Evento emitido cuando se presiona o libera una tecla
   * @since 1.0.0
   */
  addListener(
    eventName: 'keyEvent',
    listenerFunc: (data: { state: string, keyCode: number, keyName: string }) => void
  ): Promise<void>;

  /**
   * Eventos emitidos durante la inicialización y triggers
   * @since 1.0.0
   */
  addListener(
    eventName: 'initSuccess' | 'initError' | 'triggerPressed' | 'triggerReleased',
    listenerFunc: (data: { message: string }) => void
  ): Promise<void>;

  /**
   * Event emitted when the trigger button is pressed
   * Trigger button codes: 139, 280, or 293
   * @since 1.0.0
   */
  addListener(
    eventName: 'triggerPressed',
    listenerFunc: (data: { message: string, keyCode?: number, timestamp?: number }) => void
  ): Promise<void>;

  /**
   * Event emitted when the trigger button is released
   * Trigger button codes: 139, 280, or 293
   * @since 1.0.0
   */
  addListener(
    eventName: 'triggerReleased',
    listenerFunc: (data: { message: string, keyCode?: number, timestamp?: number }) => void
  ): Promise<void>;

  /**
   * Event emitted when the trigger is auto-reset due to timeout
   * @since 1.0.0
   */
  addListener(
    eventName: 'triggerAutoReset',
    listenerFunc: (data: { message: string, reason: string }) => void
  ): Promise<void>;

  /**
   * Event emitted when key state is manually reset
   * @since 1.0.0
   */
  addListener(
    eventName: 'keyStateReset',
    listenerFunc: (data: { message: string, success: boolean }) => void
  ): Promise<void>;
}