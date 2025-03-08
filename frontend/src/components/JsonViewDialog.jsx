import { useEffect, useState } from 'react';
import { 
    Dialog, DialogTitle, DialogContent, DialogActions,
    Button, Box, CircularProgress, IconButton
} from '@mui/material';
import ContentCopyIcon from '@mui/icons-material/ContentCopy';
import axios from 'axios';

function JsonViewDialog({ open, onClose, sheetId, projectId }) {
    const [jsonData, setJsonData] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [copied, setCopied] = useState(false);

    useEffect(() => {
        if (open && sheetId) {
            setLoading(true);
            setError(null);
            
            // Fetch the sheet data
            axios.get(`/api/projects/${projectId}/sheets/${sheetId}`)
                .then(response => {
                    setJsonData(response.data);
                    setLoading(false);
                })
                .catch(err => {
                    console.error('Error fetching sheet JSON:', err);
                    setError('Failed to load sheet data for sheet ' + sheetId);
                    setLoading(false);
                });
        }
    }, [open, sheetId, projectId]);

    const handleCopyToClipboard = () => {
        if (jsonData) {
            navigator.clipboard.writeText(JSON.stringify(jsonData, null, 2))
                .then(() => {
                    setCopied(true);
                    setTimeout(() => setCopied(false), 2000);
                })
                .catch(err => {
                    console.error('Failed to copy to clipboard:', err);
                });
        }
    };

    return (
        <Dialog open={open} onClose={onClose} maxWidth="md" fullWidth>
            <DialogTitle>
                Sheet JSON Source
                <IconButton 
                    onClick={handleCopyToClipboard}
                    disabled={!jsonData}
                    sx={{ ml: 2 }}
                    color={copied ? "success" : "default"}
                >
                    <ContentCopyIcon />
                </IconButton>
            </DialogTitle>
            <DialogContent>
                <Box 
                    sx={{ 
                        bgcolor: '#f5f5f5',
                        color: '#333',
                        p: 2,
                        fontFamily: 'monospace',
                        height: 400,
                        overflow: 'auto',
                        whiteSpace: 'pre-wrap',
                        fontSize: '0.875rem'
                    }}
                >
                    {loading ? (
                        <Box sx={{ display: 'flex', justifyContent: 'center', pt: 10 }}>
                            <CircularProgress />
                        </Box>
                    ) : error ? (
                        <Box sx={{ color: 'error.main' }}>{error}</Box>
                    ) : (
                        JSON.stringify(jsonData, null, 2)
                    )}
                </Box>
            </DialogContent>
            <DialogActions>
                <Button onClick={onClose}>Close</Button>
            </DialogActions>
        </Dialog>
    );
}

export default JsonViewDialog; 