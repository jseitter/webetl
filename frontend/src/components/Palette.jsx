import { useState, useEffect } from 'react';
import { Paper, List, ListItem, ListItemText, Typography, ListItemIcon, Divider, IconButton, Collapse, Tooltip } from '@mui/material';
import StorageIcon from '@mui/icons-material/Storage';
import FilterAltIcon from '@mui/icons-material/FilterAlt';
import TransformIcon from '@mui/icons-material/Transform';
import SaveIcon from '@mui/icons-material/Save';
import InsertDriveFileIcon from '@mui/icons-material/InsertDriveFile';
import PlayArrowIcon from '@mui/icons-material/PlayArrow';
import StopIcon from '@mui/icons-material/Stop';
import ExpandLess from '@mui/icons-material/ExpandLess';
import ExpandMore from '@mui/icons-material/ExpandMore';
import axios from 'axios';

// Map backend icon names to Material-UI icons
const iconMap = {
  'FileIcon': InsertDriveFileIcon,
  'DatabaseIcon': StorageIcon,
  'FilterIcon': FilterAltIcon,
  'MapIcon': TransformIcon
};

function Palette({ isExecuting, onExecute, onStop }) {
  const [componentGroups, setComponentGroups] = useState({});
  const [openSections, setOpenSections] = useState({
    controlFlow: true,
    source: true,
    transform: true,
    destination: true
  });

  const toggleSection = (section) => {
    setOpenSections(prev => ({
      ...prev,
      [section]: !prev[section]
    }));
  };

  useEffect(() => {
    const fetchComponents = async () => {
      try {
        const response = await axios.get('/api/components');
        console.log('API Response:', response);
        
        const groups = response.data.reduce((acc, category) => {
          const type = category.name.toLowerCase();
          acc[type] = category.components.map(comp => ({
            ...comp,
            id: comp.id,
            label: comp.label,
            description: comp.description,
            icon: comp.definition?.icon || 'StorageIcon',
            backgroundColor: comp.definition?.backgroundColor || '#f0f7ff',
            type: type
          }));
          return acc;
        }, {});
        
        console.log('Final component groups:', groups);
        setComponentGroups(groups);
      } catch (error) {
        console.error('Failed to fetch components:', error);
      }
    };
    
    fetchComponents();
  }, []);

  const onDragStart = (event, component) => {
    const dragData = {
      id: component.id,
      type: component.type,
      label: component.label,
      backgroundColor: component.backgroundColor,
      parameters: component.parameters || [],
      supportsControlFlow: component.supportsControlFlow,
      description: component.description
    };
    
    console.log('Dragging component:', dragData);
    
    event.dataTransfer.setData('application/reactflow', dragData.type);
    event.dataTransfer.setData('application/etlcomponent', JSON.stringify(dragData));
    event.dataTransfer.effectAllowed = 'move';
  };

  const controlFlowComponents = [
    {
      id: 'start',
      type: 'source',
      label: 'Start',
      description: 'Starting point of the flow',
      backgroundColor: '#e3f2fd',
      icon: 'PlayArrowIcon',
      supportsControlFlow: true,
      parameters: [],
      definition: {
        id: 'start',
        label: 'Start',
        icon: 'PlayArrowIcon',
        backgroundColor: '#e3f2fd'
      }
    },
    {
      id: 'stop',
      type: 'destination',
      label: 'Stop',
      description: 'Ending point of the flow',
      backgroundColor: '#fbe9e7',
      icon: 'StopIcon',
      supportsControlFlow: true,
      parameters: [],
      definition: {
        id: 'stop',
        label: 'Stop',
        icon: 'StopIcon',
        backgroundColor: '#fbe9e7'
      }
    }
  ];

  // Add PlayArrowIcon and StopIcon to iconMap
  const extendedIconMap = {
    ...iconMap,
    'PlayArrowIcon': PlayArrowIcon,
    'StopIcon': StopIcon
  };

  const getIcon = (iconName) => {
    const IconComponent = extendedIconMap[iconName] || StorageIcon;
    return <IconComponent />;
  };

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
      <ListItem 
        button 
        onClick={() => toggleSection('controlFlow')}
        sx={{ p: 2, pb: 1 }}
      >
        <ListItemText 
          primary="Control Flow" 
          primaryTypographyProps={{ fontWeight: 'bold' }}
        />
        {openSections.controlFlow ? <ExpandLess /> : <ExpandMore />}
      </ListItem>
      <Collapse in={openSections.controlFlow} timeout="auto">
        <List>
          {controlFlowComponents.map((component) => (
            <Tooltip
              key={component.id}
              title={component.description || ''}
              placement="right"
            >
              <ListItem
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
                <ListItemIcon>{getIcon(component.icon)}</ListItemIcon>
                <ListItemText primary={component.label} />
              </ListItem>
            </Tooltip>
          ))}
        </List>
      </Collapse>

      <Divider sx={{ my: 1 }} />

      {/* Dynamic Component Groups */}
      {Object.entries(componentGroups).map(([type, components]) => (
        <div key={type}>
          <ListItem 
            button 
            onClick={() => toggleSection(type)}
            sx={{ p: 2, pb: 1 }}
          >
            <ListItemText 
              primary={`${type.charAt(0).toUpperCase() + type.slice(1)}s`}
              primaryTypographyProps={{ fontWeight: 'bold' }}
            />
            {openSections[type] ? <ExpandLess /> : <ExpandMore />}
          </ListItem>
          <Collapse in={openSections[type]} timeout="auto">
            <List>
              {components.map((component) => (
                <Tooltip 
                  key={component.id}
                  title={component.description || ''}
                  placement="right"
                >
                  <ListItem
                    button
                    draggable
                    onDragStart={(e) => onDragStart(e, component)}
                    sx={{ backgroundColor: component.backgroundColor || '#f0f7ff', my: 0.5, mx: 1, borderRadius: 1 }}
                  >
                    <ListItemIcon>{getIcon(component.icon)}</ListItemIcon>
                    <ListItemText primary={component.label} />
                  </ListItem>
                </Tooltip>
              ))}
            </List>
          </Collapse>
        </div>
      ))}
    </Paper>
  );
}

export default Palette; 