import { useState, useRef, useEffect } from 'react';
import { 
    Box, Paper, TextField, IconButton, Typography,
    List, ListItem, ListItemText, Fab, Collapse, Button
} from '@mui/material';
import { Send, Chat, Close, Add } from '@mui/icons-material';

function ChatBot({ open, onClose, onFlowSuggestion, currentSheet }) {
    const [messages, setMessages] = useState([]);
    const [input, setInput] = useState('');
    const messagesEndRef = useRef(null);

    const scrollToBottom = () => {
        messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
    };

    useEffect(() => {
        scrollToBottom();
    }, [messages]);

    useEffect(() => {
        console.log("ChatBot received open prop:", open);
        if (open) {
            scrollToBottom();
        }
    }, [open]);

    const handleSend = async () => {
        if (!input.trim()) return;

        const userMessage = { type: 'user', text: input };
        setMessages(prev => [...prev, userMessage]);
        setInput('');

        try {
            const response = await fetch('/api/chat', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ 
                    message: input,
                    currentSheet: currentSheet
                })
            });
            const data = await response.json();
            
            setMessages(prev => [...prev, { 
                type: 'assistant', 
                text: data.content,
                flowSuggestion: data.flowSuggestion
            }]);
            
            if (data.flowSuggestion && onFlowSuggestion) {
                onFlowSuggestion(data.flowSuggestion);
            }
        } catch (error) {
            console.error('Error sending message:', error);
            setMessages(prev => [...prev, { 
                type: 'assistant', 
                text: 'Sorry, there was an error processing your request.' 
            }]);
        }
    };

    return (
        <>
            <Fab
                color="primary"
                aria-label="chat"
                sx={{
                    position: 'fixed',
                    bottom: 16,
                    right: 16,
                    zIndex: 1000
                }}
                onClick={() => {
                    // If chat is open, use onClose to close it
                    if (open) {
                        onClose();
                    } 
                    // If chat is closed, use the global toggle function to open it
                    else if (window.toggleChatFromFloatingButton) {
                        window.toggleChatFromFloatingButton();
                    }
                }}
            >
                {open ? <Close /> : <Chat />}
            </Fab>

            <Collapse in={open}>
                <Paper
                    sx={{
                        position: 'fixed',
                        bottom: 80,
                        right: 16,
                        width: 350,
                        height: 500,
                        display: 'flex',
                        flexDirection: 'column',
                        p: 2,
                        zIndex: 1100,
                        boxShadow: 3
                    }}
                >
                    <Typography variant="h6" gutterBottom>ETL Assistant</Typography>
                    
                    <List sx={{ flex: 1, overflow: 'auto' }}>
                        {messages.map((msg, index) => (
                            <ListItem 
                                key={index}
                                sx={{ 
                                    flexDirection: 'column',
                                    alignItems: msg.type === 'user' ? 'flex-end' : 'flex-start',
                                    mb: 1
                                }}
                            >
                                <Paper 
                                    sx={{ 
                                        p: 1, 
                                        bgcolor: msg.type === 'user' ? '#e3f2fd' : '#f5f5f5',
                                        maxWidth: '80%'
                                    }}
                                >
                                    <Typography variant="body2">{msg.text}</Typography>
                                </Paper>
                                
                                {msg.flowSuggestion && (
                                    <Button 
                                        size="small" 
                                        variant="outlined" 
                                        sx={{ mt: 1 }}
                                        onClick={() => onFlowSuggestion(msg.flowSuggestion)}
                                    >
                                        <Add fontSize="small" sx={{ mr: 0.5 }} />
                                        Apply Suggestion
                                    </Button>
                                )}
                            </ListItem>
                        ))}
                        <div ref={messagesEndRef} />
                    </List>

                    <Box sx={{ display: 'flex', mt: 2 }}>
                        <TextField
                            fullWidth
                            variant="outlined"
                            size="small"
                            value={input}
                            onChange={(e) => setInput(e.target.value)}
                            onKeyPress={(e) => e.key === 'Enter' && handleSend()}
                            placeholder="Ask about ETL flows..."
                        />
                        <IconButton onClick={handleSend} color="primary">
                            <Send />
                        </IconButton>
                    </Box>
                </Paper>
            </Collapse>
        </>
    );
}

export default ChatBot; 