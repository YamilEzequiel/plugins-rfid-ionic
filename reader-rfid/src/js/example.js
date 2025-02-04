import { RFID } from 'capacitor-plugin-rfid';

// eslint-disable-next-line no-undef
window.testEcho = () => {
  // eslint-disable-next-line no-undef
  const inputValue = document.getElementById('echoInput').value;
  RFID.echo({ value: inputValue });
};
