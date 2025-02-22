import SockJS from 'sockjs-client';
import { Stomp } from '@stomp/stompjs';

class WebSocketService {
    connect() {
        const socket = new SockJS('http://localhost:8085/websocket');
        this.stompClient = Stomp.over(socket);
        this.stompClient.debug = () => {}; // Disable debug logging
        return new Promise((resolve, reject) => {
            this.stompClient.connect({}, () => resolve(), reject);
        });
    }

    subscribeToCompiler(sheetId, callback) {
        return this.stompClient.subscribe(
            `/topic/compiler/${sheetId}`,
            message => callback(message.body)
        );
    }

    disconnect() {
        if (this.stompClient) {
            this.stompClient.disconnect();
        }
    }
}

export default new WebSocketService(); 