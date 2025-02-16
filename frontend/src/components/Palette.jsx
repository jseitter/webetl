import { useState, useEffect } from 'react';
import { Paper, List, ListItem, ListItemText, Typography, ListItemIcon, Divider, IconButton } from '@mui/material';
import StorageIcon from '@mui/icons-material/Storage';
import FilterAltIcon from '@mui/icons-material/FilterAlt';
import TransformIcon from '@mui/icons-material/Transform';
import SaveIcon from '@mui/icons-material/Save';
import InsertDriveFileIcon from '@mui/icons-material/InsertDriveFile';
import PlayArrowIcon from '@mui/icons-material/PlayArrow';
import StopIcon from '@mui/icons-material/Stop';
import axios from 'axios';

// Map backend icon names to Material-UI icons
const iconMap = {
  'FileIcon': InsertDriveFileIcon,
  'DatabaseIcon': StorageIcon,
  'FilterIcon': FilterAltIcon,
  'MapIcon': TransformIcon
};

function Palette({ isExecuting, onExecute, onStop }) {
  const onDragStart = (event, component) => {
    // Create a clean version of the component without DOM elements
    const dragData = {
      id: component.id,
      type: component.type,
      label: component.label,
      backgroundColor: component.backgroundColor,
      parameters: component.parameters || [],
      supportsControlFlow: component.supportsControlFlow
    };
    
    console.log('Dragging component:', dragData);
    
    event.dataTransfer.setData('application/reactflow', dragData.type);
    event.dataTransfer.setData('application/etlcomponent', JSON.stringify(dragData));
    event.dataTransfer.effectAllowed = 'move';
  };

  const getIcon = (iconName) => {
    const IconComponent = iconMap[iconName] || StorageIcon;
    return <IconComponent />;
  };

  const controlFlowComponents = [
    {
      id: 'start',
      type: 'source',
      label: 'Start',
      backgroundColor: '#e3f2fd',
      icon: <PlayArrowIcon />,
      supportsControlFlow: true,
      parameters: []
    },
    {
      id: 'stop',
      type: 'source',
      label: 'Stop',
      backgroundColor: '#fbe9e7',
      icon: <StopIcon />,
      supportsControlFlow: true
    }
  ];

  const dataFlowComponents = [
    {
      id: 'db-source',
      type: 'source',
      label: 'Database Source',
      backgroundColor: '#f0f7ff',
      icon: <StorageIcon />,
      parameters: [],
      supportsControlFlow: true
    },
    {
      id: 'file-source',
      type: 'source',
      label: 'File Source',
      backgroundColor: '#f0f7ff',
      icon: <InsertDriveFileIcon />,
      parameters: [],
      supportsControlFlow: true
    },
    {
      id: 'filter',
      type: 'transform',
      label: 'Filter',
      backgroundColor: '#fff7f0',
      icon: <FilterAltIcon />,
      parameters: []
    },
    {
      id: 'map',
      type: 'transform',
      label: 'Map',
      backgroundColor: '#fff7f0',
      icon: <TransformIcon />,
      parameters: []
    },
    {
      id: 'db-dest',
      type: 'destination',
      label: 'Database Destination',
      backgroundColor: '#f0fff4',
      icon: <SaveIcon />,
      parameters: []
    },
    {
      id: 'file-dest',
      type: 'destination',
      label: 'File Destination',
      backgroundColor: '#f0fff4',
      icon: <InsertDriveFileIcon />,
      parameters: []
    }
  ];

  return (
    <Paper sx={{ width: 240, overflow: 'auto' }}>
      {/* Execution Controls */}
      <div style={{
        padding: '8px',
        display: 'flex',
        justifyContent: 'center',
        gap: '8px',
        borderBottom: '1px solid rgba(0, 0, 0, 0.12)'
      }}>
        <IconButton 
          color="primary"
          disabled={isExecuting}
          onClick={onExecute}
          size="small"
          sx={{ 
            backgroundColor: isExecuting ? 'transparent' : '#e3f2fd',
            '&:hover': { backgroundColor: '#bbdefb' }
          }}
        >
          <PlayArrowIcon />
        </IconButton>
        <IconButton 
          color="error"
          disabled={!isExecuting}
          onClick={onStop}
          size="small"
          sx={{ 
            backgroundColor: !isExecuting ? 'transparent' : '#fbe9e7',
            '&:hover': { backgroundColor: '#ffccbc' }
          }}
        >
          <StopIcon />
        </IconButton>
      </div>

      {/* Control Flow Section */}
      <Typography variant="subtitle1" sx={{ p: 2, pb: 1, fontWeight: 'bold' }}>
        Control Flow
      </Typography>
      <List>
        {controlFlowComponents.map((component) => (
          <ListItem
            key={component.id}
            button
            draggable
            onDragStart={(e) => onDragStart(e, component)}
            sx={{ 
              backgroundColor: component.backgroundColor,
              my: 0.5,
              mx: 1,
              borderRadius: 1
            }}
          >
            <ListItemIcon>{component.icon}</ListItemIcon>
            <ListItemText primary={component.label} />
          </ListItem>
        ))}
      </List>

      <Divider sx={{ my: 1 }} />

      {/* Data Flow Section */}
      <Typography variant="subtitle1" sx={{ p: 2, pb: 1, fontWeight: 'bold' }}>
        Data Flow
      </Typography>
      <List>
        {dataFlowComponents.map((component) => (
          <ListItem
            key={component.id}
            button
            draggable
            onDragStart={(e) => onDragStart(e, component)}
            sx={{ 
              backgroundColor: component.backgroundColor,
              my: 0.5,
              mx: 1,
              borderRadius: 1
            }}
          >
            <ListItemIcon>{component.icon}</ListItemIcon>
            <ListItemText primary={component.label} />
          </ListItem>
        ))}
      </List>
    </Paper>
  );
}

export default Palette; 