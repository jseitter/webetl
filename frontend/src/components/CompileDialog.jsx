import { useEffect, useState, useRef } from 'react';
import { 
    Dialog, DialogTitle, DialogContent, DialogActions,
    Button, Typography, Box, CircularProgress
} from '@mui/material';
import websocketService from '../services/websocket.js';

function CompileDialog({ open, onClose, sheetId, projectId }) {
    const [displayedOutput, setDisplayedOutput] = useState([]);
    const [isCompiling, setIsCompiling] = useState(true);
    const [subscription, setSubscription] = useState(null);
    
    // Use a ref to maintain message buffer between renders
    const messageBufferRef = useRef([]);
    const nextSequenceRef = useRef(0);

    // Function to process buffered messages and update display in sequence order
    const processMessageBuffer = () => {
        // Sort buffer by sequence number
        const buffer = messageBufferRef.current;
        buffer.sort((a, b) => a.sequence - b.sequence);
        
        // Extract messages that are ready to display (in sequence)
        const readyMessages = [];
        while (buffer.length > 0 && buffer[0].sequence === nextSequenceRef.current) {
            const message = buffer.shift();
            readyMessages.push(message.content);
            nextSequenceRef.current++;
        }
        
        // If we have messages to display, update the state
        if (readyMessages.length > 0) {
            setDisplayedOutput(prev => [...prev, ...readyMessages]);
        }
    };

    // Function to add a message to the buffer and try to process
    const addMessage = (message) => {
        if (typeof message === 'string') {
            // Legacy format - assume these are out of sequence
            messageBufferRef.current.push({
                sequence: 9999 + Math.random(), // Use high sequence to place at end
                content: message,
                timestamp: Date.now()
            });
        } else {
            // New sequenced format
            messageBufferRef.current.push(message);
        }
        processMessageBuffer();
        
        // Check if compilation is complete
        const messageContent = typeof message === 'string' ? message : message.content;
        if (messageContent.includes('completed successfully') || 
            messageContent.includes('failed') || 
            messageContent.includes('error')) {
            setIsCompiling(false);
        }
    };

    useEffect(() => {
        if (open) {
            // Reset state when dialog opens
            setDisplayedOutput([]);
            setIsCompiling(true);
            messageBufferRef.current = [];
            nextSequenceRef.current = 0;
            
            // Connect to websocket and subscribe to compiler output
            websocketService.connect().then(() => {
                const sub = websocketService.subscribeToCompiler(
                    sheetId,
                    addMessage
                );
                setSubscription(sub);

                // Start compilation
                fetch(`/api/compiler/sheets/${sheetId}/compile?projectId=${projectId}`, {
                    method: 'POST'
                });
            });
        }
        
        return () => {
            if (subscription) {
                subscription.unsubscribe();
            }
        };
    }, [open, sheetId, projectId]);

    return (
        <Dialog open={open} onClose={onClose} maxWidth="md" fullWidth>
            <DialogTitle>Compiling Sheet</DialogTitle>
            <DialogContent>
                <Box 
                    sx={{ 
                        bgcolor: 'black',
                        color: 'lightgreen',
                        p: 2,
                        fontFamily: 'monospace',
                        height: 400,
                        overflow: 'auto',
                        whiteSpace: 'pre-wrap'
                    }}
                >
                    {displayedOutput.join('\n')}
                </Box>
            </DialogContent>
            <DialogActions>
                {isCompiling ? (
                    <CircularProgress size={24} />
                ) : (
                    <Button onClick={onClose}>Close</Button>
                )}
            </DialogActions>
        </Dialog>
    );
}

export default CompileDialog; 