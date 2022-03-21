
/**
 * @module NXPrinter
 */
module.exports = {
     /**
   * @description
   * .
   *
   * @enum {number}
   */
  PrinterType:{
    CMD_ESC: 1, 
    CMD_TSC: 2, 
    CMD_CPCL: 3, 
    CMD_ZPL: 4, 
    CMD_PIN: 5
  },
  ConnectionType:{
    CON_BLUETOOTH: 1, 
    CON_BLUETOOTH_BLE: 2, 
    CON_WIFI: 3, 
    CON_USB: 4, 
    CON_COM: 5
  },
  Align:{
    ALIGN_LEFT : 0,
    ALIGN_CENTER : 1,
    ALIGN_RIGHT : 2,
  },
  Font:{
    FONT_A_12x24 : 1, 
    FONT_B_9x24 : 2, 
    FONT_C_9x17 : 3, 
    FONT_D_8x16 : 4
  },
  FontSize: {
    SMALL: 20,
    NORMAL: 24,
    BIG: 28
  },
  Type: {
    TEXT: 0,
    BARCODE: 1,
    QRCODE: 2
  }
};