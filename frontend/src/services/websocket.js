import SockJS from 'sockjs-client';
import { Client } from '@stomp/stompjs';

class WebSocketService {
    connect() {
        return new Promise((resolve, reject) => {
            this.stompClient = new Client({
                webSocketFactory: () => new SockJS('http://localhost:8085/websocket'),
                debug: () => {},
                reconnectDelay: 5000,
                heartbeatIncoming: 4000,
                heartbeatOutgoing: 4000
            });

            this.stompClient.onConnect = () => resolve();
            this.stompClient.onStompError = (frame) => reject(frame);
            
            this.stompClient.activate();
        });
    }

    subscribeToCompiler(sheetId, callback) {
        return this.stompClient.subscribe(
            `/topic/compiler/${sheetId}`,
            message => {
                try {
                    // Try to parse as JSON for the new sequenced format
                    const parsed = JSON.parse(message.body);
                    callback(parsed);
                } catch (e) {
                    // Fall back to string format for backward compatibility
                    callback(message.body);
                }
            }
        );
    }

    subscribeToRunner(sheetId, callback) {
        return this.stompClient.subscribe(
            `/topic/runner/${sheetId}`,
            message => {
                try {
                    // Try to parse as JSON for the new sequenced format
                    const parsed = JSON.parse(message.body);
                    callback(parsed);
                } catch (e) {
                    // Fall back to string format for backward compatibility
                    callback(message.body);
                }
            }
        );
    }

    disconnect() {
        if (this.stompClient) {
            this.stompClient.deactivate();
        }
    }
}

export default new WebSocketService(); 