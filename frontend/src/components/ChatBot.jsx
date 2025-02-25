import { useState, useRef, useEffect } from 'react';
import { 
    Box, Paper, TextField, IconButton, Typography,
    List, ListItem, ListItemText, Fab, Collapse, Button
} from '@mui/material';
import { Send, Chat, Close, Add } from '@mui/icons-material';

function ChatBot({ open, onClose, onFlowSuggestion, currentSheet }) {
    const [isOpen, setIsOpen] = useState(false);
    const [messages, setMessages] = useState([]);
    const [input, setInput] = useState('');
    const messagesEndRef = useRef(null);

    const scrollToBottom = () => {
        messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
    };

    useEffect(() => {
        scrollToBottom();
    }, [messages]);

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
                type: 'bot', 
                text: data.content,
                flowSuggestion: data.flowSuggestion 
            }]);

            if (data.flowSuggestion) {
                setMessages(prev => [...prev, {
                    type: 'bot',
                    text: 'Would you like me to apply these changes to your flow?',
                    action: {
                        label: 'Apply Changes',
                        onClick: () => onFlowSuggestion(data.flowSuggestion)
                    }
                }]);
            }
        } catch (error) {
            console.error('Chat error:', error);
            setMessages(prev => [...prev, { 
                type: 'bot', 
                text: 'Sorry, there was an error processing your request.' 
            }]);
        }
    };

    return (
        <>
            <Fab 
                color="primary" 
                sx={{ position: 'fixed', bottom: 16, right: 16 }}
                onClick={() => setIsOpen(!isOpen)}
            >
                {isOpen ? <Close /> : <Chat />}
            </Fab>

            <Collapse in={isOpen}>
                <Paper
                    sx={{
                        position: 'fixed',
                        bottom: 80,
                        right: 16,
                        width: 350,
                        height: 500,
                        display: 'flex',
                        flexDirection: 'column',
                        p: 2
                    }}
                >
                    <Typography variant="h6" gutterBottom>ETL Assistant</Typography>
                    
                    <List sx={{ flex: 1, overflow: 'auto' }}>
                        {messages.map((msg, idx) => (
                            <ListItem key={idx} sx={{ 
                                justifyContent: msg.type === 'user' ? 'flex-end' : 'flex-start' 
                            }}>
                                <Paper sx={{ 
                                    p: 1, 
                                    bgcolor: msg.type === 'user' ? 'primary.light' : 'grey.100',
                                    maxWidth: '80%' 
                                }}>
                                    <ListItemText primary={msg.text} />
                                    {msg.action ? (
                                        <Button
                                            variant="contained"
                                            size="small"
                                            onClick={msg.action.onClick}
                                            startIcon={<Add />}
                                        >
                                            {msg.action.label}
                                        </Button>
                                    ) : msg.flowSuggestion && (
                                        <IconButton 
                                            size="small" 
                                            onClick={() => onFlowSuggestion(msg.flowSuggestion)}
                                        >
                                            <Add /> Generate Flow
                                        </IconButton>
                                    )}
                                </Paper>
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