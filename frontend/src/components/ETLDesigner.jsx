import { useState, useEffect, useCallback } from 'react';
import { Box, Tab, Tabs, IconButton, Tooltip, Snackbar, Alert, Paper, AppBar, Toolbar, Typography, TextField, Button } from '@mui/material';
import SaveIcon from '@mui/icons-material/Save';
import BuildIcon from '@mui/icons-material/Build';
import ArrowBackIcon from '@mui/icons-material/ArrowBack';
import AddIcon from '@mui/icons-material/Add';
import Sheet from './Sheet';
import CompileDialog from './CompileDialog';
import { v4 as uuidv4 } from 'uuid';
import axios from 'axios';
import { useParams, useNavigate } from 'react-router-dom';
import PlayArrowIcon from '@mui/icons-material/PlayArrow';
import SmartToyIcon from '@mui/icons-material/SmartToy';
import ChatBot from './ChatBot';
import Dialog from '@mui/material/Dialog';
import DialogActions from '@mui/material/DialogActions';
import DialogContent from '@mui/material/DialogContent';
import DialogContentText from '@mui/material/DialogContentText';
import DialogTitle from '@mui/material/DialogTitle';
import DeleteIcon from '@mui/icons-material/Delete';
import CloseIcon from '@mui/icons-material/Close';

function ETLDesigner() {
  const { projectId } = useParams();
  const navigate = useNavigate();
  const [sheets, setSheets] = useState([]);
  const [activeSheet, setActiveSheet] = useState(0);
  const [unsavedChanges, setUnsavedChanges] = useState(new Set());
  const [snackbar, setSnackbar] = useState({ open: false, message: '', severity: 'success' });
  const [isBackendAvailable, setIsBackendAvailable] = useState(true);
  const [currentSheet, setCurrentSheet] = useState(null);
  const [editingSheetName, setEditingSheetName] = useState(false);
  const [sheetName, setSheetName] = useState('');
  const [compileDialogOpen, setCompileDialogOpen] = useState(false);
  const [chatOpen, setChatOpen] = useState(false);
  const [deleteDialog, setDeleteDialog] = useState({ open: false, sheetId: null });

  // Check backend health periodically
  useEffect(() => {
    const checkHealth = async () => {
      try {
        const controller = new AbortController();
        const timeoutId = setTimeout(() => controller.abort(), 2000); // 2 second timeout

        await axios.get('/api/health', {
          signal: controller.signal,
          validateStatus: (status) => status === 200 // Only accept 200 status
        });

        clearTimeout(timeoutId);

        if (!isBackendAvailable) {
          setIsBackendAvailable(true);
          loadSheets();
        }
      } catch (error) {
        if (!error.status || error.status >= 500 || error.code === 'ERR_CANCELED' || error.code === 'ECONNABORTED') {
          setIsBackendAvailable(false);
        }
      }
    };

    // Initial check
    checkHealth();

    // Set up periodic checking
    const intervalId = setInterval(checkHealth, 5000);

    return () => {
      clearInterval(intervalId);
    };
  }, [isBackendAvailable]);

  // Load sheets for the current project
  useEffect(() => {
    loadSheets();
  }, [projectId]);

  // Keep currentSheet in sync with the activeSheet
  useEffect(() => {
    if (sheets.length > 0 && activeSheet < sheets.length) {
      setCurrentSheet(sheets[activeSheet]);
      setSheetName(sheets[activeSheet].name);
    } else if (sheets.length === 0) {
      setCurrentSheet(null);
      setSheetName('');
    }
  }, [activeSheet, sheets]);

  const loadSheets = async () => {
    try {
      const response = await axios.get(`/api/projects/${projectId}/sheets`, {
        timeout: 2000,
        validateStatus: (status) => status === 200
      });
      
      setSheets(response.data);
      if (response.data.length > 0) {
        setCurrentSheet(response.data[0]);
        setSheetName(response.data[0].name);
      }
    } catch (error) {
      console.error('Error loading sheets for project:', error);
      setIsBackendAvailable(false);
    }
  };

  // Handle Ctrl+S
  useEffect(() => {
    const handleKeyDown = (event) => {
      if ((event.ctrlKey || event.metaKey) && event.key === 's') {
        event.preventDefault();
        if (sheets[activeSheet]) {
          saveSheet(sheets[activeSheet].id);
        }
      }
    };

    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [sheets, activeSheet]);

  const saveSheet = async (sheetId) => {
    const sheet = sheets.find(s => s.id === sheetId);
    if (sheet) {
      try {
        await axios.put(`/api/projects/${projectId}/sheets/${sheetId}`, sheet, {
          timeout: 2000,
          validateStatus: (status) => status === 200
        });
        
        setUnsavedChanges(prev => {
          const next = new Set(prev);
          next.delete(sheetId);
          return next;
        });
        
        setSnackbar({
          open: true,
          message: 'Sheet saved successfully',
          severity: 'success'
        });
      } catch (error) {
        console.error('Error saving sheet:', error);
        setIsBackendAvailable(false);
        setSnackbar({
          open: true,
          message: 'Failed to save sheet: Backend server not available',
          severity: 'error'
        });
      }
    }
  };

  const handleCloseSnackbar = (event, reason) => {
    if (reason === 'clickaway') {
      return;
    }
    setSnackbar(prev => ({ ...prev, open: false }));
  };

  const handleAddSheet = () => {
    const newSheet = {
      id: uuidv4(),
      name: `Sheet ${sheets.length + 1}`,
      nodes: [],
      edges: []
    };
    setSheets([...sheets, newSheet]);
    setUnsavedChanges(prev => new Set(prev).add(newSheet.id));
  };

  const handleSheetChange = (event, newValue) => {
    setActiveSheet(newValue);
  };

  const handleSheetUpdate = useCallback((sheetId, nodes, edges) => {
    setSheets(prev => prev.map(sheet => 
      sheet.id === sheetId 
        ? { ...sheet, nodes, edges }
        : sheet
    ));
    // Only mark as unsaved if the content actually changed
    const currentSheet = sheets.find(s => s.id === sheetId);
    if (currentSheet && (
      JSON.stringify(currentSheet.nodes) !== JSON.stringify(nodes) ||
      JSON.stringify(currentSheet.edges) !== JSON.stringify(edges)
    )) {
      setUnsavedChanges(prev => new Set(prev).add(sheetId));
    }
  }, [sheets]);

  const handleCreateSheet = async () => {
    try {
      const response = await axios.post(`/api/projects/${projectId}/sheets`, {
        name: 'New Sheet'
      });
      setSheets([...sheets, response.data]);
      setCurrentSheet(response.data);
      setSheetName(response.data.name);
    } catch (error) {
      console.error('Error creating sheet:', error);
    }
  };

  const handleSheetNameSave = async () => {
    try {
      await axios.patch(`/api/projects/${projectId}/sheets/${currentSheet.id}`, { 
        name: sheetName 
      });
      
      // Update the sheets array with the new name
      setSheets(sheets.map(s => 
        s.id === currentSheet.id ? { ...s, name: sheetName } : s
      ));
      
      // Update the current sheet with the new name
      setCurrentSheet(prev => ({ ...prev, name: sheetName }));
      
      // Close the editing mode
      setEditingSheetName(false);
    } catch (error) {
      console.error('Error updating sheet name:', error);
      // Show error in snackbar
      setSnackbar({
        open: true,
        message: 'Failed to update sheet name',
        severity: 'error'
      });
    }
  };

  const handleFlowSuggestion = (suggestion) => {
    console.log('Handling flow suggestion:', suggestion);
    // Ensure suggestion has the right structure
    const suggestedNodes = suggestion.nodes || [];
    const suggestedEdges = suggestion.edges || [];
    
    // Add new nodes to the active sheet
    const activeSheetData = sheets[activeSheet];
    if (activeSheetData) {
      // Generate unique IDs for new nodes
      const newNodes = suggestedNodes.map(node => ({
        ...node,
        id: `${node.data.componentData.id}-${Date.now()}`,
        position: { x: 100, y: 100 * (currentSheet.nodes.length + 1) },
        width: 150,
        data: {
          ...node.data,
          handles: []
        }
      }));
      console.log('New nodes:', newNodes);
      
      // Update edge references with new node IDs
      const newEdges = suggestedEdges.map(edge => ({
        ...edge,
        id: `edge-${Date.now()}`,
        type: 'default',
        source: newNodes.find(n => n.id === edge.source)?.id || edge.source,
        target: newNodes.find(n => n.id === edge.target)?.id || edge.target
      }));
      console.log('New edges:', newEdges);
      
      const updatedSheet = {
        ...activeSheetData,
        nodes: [...activeSheetData.nodes, ...newNodes],
        edges: [...activeSheetData.edges, ...newEdges]
      };
      console.log('Updated sheet:', updatedSheet);
      handleSheetUpdate(activeSheetData.id, updatedSheet.nodes, updatedSheet.edges);
    }
  };

  const handleDeleteSheet = async () => {
    try {
      await axios.delete(`/api/projects/${projectId}/sheets/${deleteDialog.sheetId}`);
      
      // Filter out the deleted sheet
      const updatedSheets = sheets.filter(s => s.id !== deleteDialog.sheetId);
      setSheets(updatedSheets);
      
      // Reset to the first sheet or null if no sheets left
      setActiveSheet(0);
      
      setDeleteDialog({ open: false, sheetId: null });
      setSnackbar({
        open: true,
        message: 'Sheet moved to trash',
        severity: 'success'
      });
    } catch (error) {
      console.error('Error deleting sheet:', error);
      setSnackbar({
        open: true,
        message: 'Failed to delete sheet',
        severity: 'error'
      });
    }
  };

  const handleRemoveUnsavedSheet = (sheetId) => {
    const updatedSheets = sheets.filter(s => s.id !== sheetId);
    setSheets(updatedSheets);
    setActiveSheet(Math.max(0, activeSheet - 1));
  };

  return (
    <Box sx={{ height: '100vh', display: 'flex', flexDirection: 'column' }}>
      <AppBar position="static">
        <Toolbar>
          <IconButton 
            edge="start" 
            color="inherit" 
            onClick={() => navigate('/projects')}
          >
            <ArrowBackIcon />
          </IconButton>
          
          {currentSheet ? (
            editingSheetName ? (
              <Box sx={{ display: 'flex', alignItems: 'center', ml: 2 }}>
                <TextField
                  value={sheetName}
                  onChange={(e) => setSheetName(e.target.value)}
                  size="small"
                  sx={{ backgroundColor: 'white' }}
                />
                <Button 
                  color="inherit" 
                  onClick={handleSheetNameSave}
                  sx={{ ml: 1 }}
                >
                  Save
                </Button>
              </Box>
            ) : (
              <Typography 
                variant="h6" 
                sx={{ ml: 2, cursor: 'pointer' }}
                onClick={() => setEditingSheetName(true)}
              >
                {currentSheet.name}
              </Typography>
            )
          ) : null}
          
          <Box sx={{ flexGrow: 1 }} />
          
          <Button
            color="inherit"
            startIcon={<AddIcon />}
            onClick={handleCreateSheet}
          >
            New Sheet
          </Button>
        </Toolbar>
      </AppBar>

      {!isBackendAvailable && (
        <Paper 
          elevation={0}
          sx={{ 
            bgcolor: 'warning.light', 
            color: 'warning.dark',
            p: 1,
            textAlign: 'center',
            borderRadius: 0
          }}
        >
          Backend server is not available. Some features may be limited.
        </Paper>
      )}
      <Box sx={{ 
        borderBottom: 1, 
        borderColor: 'divider',
        display: 'flex',
        alignItems: 'center'
      }}>
        <Tabs 
          value={activeSheet} 
          onChange={handleSheetChange}
          sx={{ flexGrow: 1 }}
        >
          {sheets.map((sheet, index) => (
            <Tab 
              key={sheet.id} 
              label={
                <Box sx={{ display: 'flex', alignItems: 'center' }}>
                  <span>{`${unsavedChanges.has(sheet.id) ? '* ' : ''}${sheet.name}`}</span>
                  <IconButton
                    size="small"
                    onClick={(e) => {
                      e.stopPropagation();
                      if (!sheet.id || unsavedChanges.has(sheet.id)) {
                        handleRemoveUnsavedSheet(sheet.id);
                      } else {
                        setDeleteDialog({ open: true, sheetId: sheet.id });
                      }
                    }}
                  >
                    {!sheet.id || unsavedChanges.has(sheet.id) ? 
                      <CloseIcon fontSize="small" /> : 
                      <DeleteIcon fontSize="small" />
                    }
                  </IconButton>
                </Box>
              }
            />
          ))}
          <Tab label="+" onClick={handleAddSheet} />
        </Tabs>
        <Tooltip title="Compile">
          <span>
            <IconButton
              onClick={() => {
                if (sheets[activeSheet]) {
                  setCompileDialogOpen(true);
                }
              }}
              disabled={!isBackendAvailable || !sheets[activeSheet]}
              sx={{ mr: 1 }}
            >
              <BuildIcon />
            </IconButton>
          </span>
        </Tooltip>
        <Tooltip title="Save (Ctrl+S)">
          <span>
            <IconButton 
              onClick={() => sheets[activeSheet] && saveSheet(sheets[activeSheet].id)}
              disabled={!isBackendAvailable || !sheets[activeSheet] || !unsavedChanges.has(sheets[activeSheet]?.id)}
              sx={{ mr: 2 }}
            >
              <SaveIcon />
            </IconButton>
          </span>
        </Tooltip>
        <Tooltip title="AI Assistant">
          <IconButton onClick={() => setChatOpen(true)}>
            <SmartToyIcon />
          </IconButton>
        </Tooltip>
      </Box>
      {sheets.map((sheet, index) => (
        <Box
          key={sheet.id}
          sx={{
            display: activeSheet === index ? 'flex' : 'none',
            flexGrow: 1,
            position: 'relative'
          }}
        >
          <Sheet 
            sheet={sheet}
            onUpdate={(nodes, edges) => handleSheetUpdate(sheet.id, nodes, edges)}
          />
        </Box>
      ))}
      <Snackbar 
        open={snackbar.open} 
        autoHideDuration={3000} 
        onClose={handleCloseSnackbar}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'right' }}
      >
        <Alert 
          onClose={handleCloseSnackbar} 
          severity={snackbar.severity}
          sx={{ width: '100%' }}
        >
          {snackbar.message}
        </Alert>
      </Snackbar>
      {sheets.length > 0 && activeSheet < sheets.length && (
        <CompileDialog
          open={compileDialogOpen}
          onClose={() => setCompileDialogOpen(false)}
          sheetId={sheets[activeSheet].id}
          projectId={projectId}
        />
      )}
      <ChatBot 
        open={chatOpen}
        onClose={() => setChatOpen(false)}
        onFlowSuggestion={handleFlowSuggestion}
        currentSheet={currentSheet}
      />
      <Dialog
        open={deleteDialog.open}
        onClose={() => setDeleteDialog({ open: false, sheetId: null })}
      >
        <DialogTitle>Delete Sheet</DialogTitle>
        <DialogContent>
          <DialogContentText>
            Are you sure you want to delete this sheet? It will be moved to the trash.
          </DialogContentText>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDeleteDialog({ open: false, sheetId: null })}>Cancel</Button>
          <Button onClick={handleDeleteSheet} color="error">Delete</Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
}

export default ETLDesigner; 