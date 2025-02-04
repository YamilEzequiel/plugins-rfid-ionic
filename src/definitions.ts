export interface RFIDPluginPlugin {
  /**
   * Inicializa el lector RFID.
   * @returns Promise con el resultado de la inicialización
   * @since 1.0.0
   */
  initialize(): Promise<{ success: boolean }>;

  /**
   * Inicia la lectura continua de tags RFID.
   * @returns Promise con el resultado del inicio de lectura
   * @since 1.0.0
   */
  startReading(): Promise<{ success: boolean }>;

  /**
   * Detiene la lectura de tags RFID.
   * @returns Promise con el resultado de la detención
   * @since 1.0.0
   */
  stopReading(): Promise<{ success: boolean }>;

  /**
   * Configura la potencia del lector RFID.
   * @param options Objeto con la potencia a configurar (1-30)
   * @returns Promise con el resultado de la configuración
   * @since 1.0.0
   */
  setPower(options: { power: number }): Promise<{ success: boolean }>;

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
}