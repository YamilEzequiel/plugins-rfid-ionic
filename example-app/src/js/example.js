import { RFID } from 'capacitor-plugin-rfid';

window.testEcho = () => {
    const inputValue = document.getElementById("echoInput").value;
    RFID.echo({ value: inputValue })
}
