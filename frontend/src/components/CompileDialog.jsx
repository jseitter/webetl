import { useEffect, useState } from 'react';
import { 
    Dialog, DialogTitle, DialogContent, DialogActions,
    Button, Typography, Box, CircularProgress
} from '@mui/material';
import websocketService from '../services/websocket.js';

function CompileDialog({ open, onClose, sheetId, projectId }) {
    const [output, setOutput] = useState([]);
    const [isCompiling, setIsCompiling] = useState(true);
    const [subscription, setSubscription] = useState(null);

    useEffect(() => {
        if (open) {
            setOutput([]);
            setIsCompiling(true);
            
            // Connect to websocket and subscribe to compiler output
            websocketService.connect().then(() => {
                const sub = websocketService.subscribeToCompiler(
                    sheetId,
                    message => {
                        setOutput(prev => [...prev, message]);
                        if (message.includes('completed successfully') || 
                            message.includes('failed') || 
                            message.includes('error')) {
                            setIsCompiling(false);
                        }
                    }
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
                        overflow: 'auto'
                    }}
                >
                    {output.map((line, i) => (
                        <Typography key={i} variant="body2">{line}</Typography>
                    ))}
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