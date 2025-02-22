import { Handle, Position } from 'reactflow';
import { Paper, Typography, IconButton } from '@mui/material';
import SettingsIcon from '@mui/icons-material/Settings';
import { useState } from 'react';
import ParameterDialog from './ParameterDialog';
import DeleteIcon from '@mui/icons-material/Delete';
import { useReactFlow } from 'reactflow';

const handleStyle = {
  width: '12px',
  height: '12px',
  border: '2px solid #555',
  borderRadius: '50%',
  background: '#fff'
};

const controlHandleStyle = {
  ...handleStyle,
  border: '2px solid #888',
};

const dataHandleStyleLeft = {
  ...handleStyle,
  left: '-6px',  // For left handle
};

const dataHandleStyleRight = {
  ...handleStyle,
  right: '-6px', // For right handle
};

const nodeContentStyle = {
  marginTop: '24px',  // Add space for icons at top
  position: 'relative',
  minHeight: '40px'   // Ensure minimum height for handles
};

function CustomNode({ data, type, id }) {
  const { deleteElements } = useReactFlow();
  const [dialogOpen, setDialogOpen] = useState(false);
  const componentData = data?.componentData || {};

  // Add this for debugging
  console.log('Node data:', data);
  console.log('Component parameters:', componentData?.parameters);

  const isSource = type === 'source';
  const isDestination = type === 'destination';
  const isStart = componentData?.id === 'start';
  const isStop = componentData?.id === 'stop';
  
  // Early return if no data
  if (!data) {
    console.error('No data provided to CustomNode');
    return null;
  }

  const handleParameterSave = (values) => {
    if (data.componentData?.parameters) {
      data.componentData.parameters.forEach(param => {
        param.value = values[param.name];
      });
    }
  };

  const handleDelete = () => {
    deleteElements({ nodes: [{ id }] });
  };

  return (
    <Paper 
      elevation={2} 
      sx={{ 
        padding: '10px',
        minWidth: '150px',
        textAlign: 'center',
        backgroundColor: data.backgroundColor || '#fff',
        position: 'relative'
      }}
    >
      {/* Control flow handles */}
      {(isSource || isDestination) && componentData?.supportsControlFlow && (
        <>
          {isStart ? (
            // Start node: only output
            <Handle
              type="source"
              position={Position.Bottom}
              style={controlHandleStyle}
              id="control-flow-out"
              title="Control Flow Out"
            />
          ) : isStop ? (
            // Stop node: only input
            <Handle
              type="target"
              position={Position.Top}
              style={controlHandleStyle}
              id="control-flow-in"
              title="Control Flow In"
            />
          ) : (
            // Other source nodes: both input and output - REVERSED ORDER
            <>
              <Handle
                type="target"
                position={Position.Top}
                style={controlHandleStyle}
                id="control-flow-in"
                title="Control Flow In"
              />
              <Handle
                type="source"
                position={Position.Bottom}
                style={controlHandleStyle}
                id="control-flow-out"
                title="Control Flow Out"
              />
            </>
          )}
        </>
      )}

      {/* Icons */}
      <div style={{ 
        position: 'absolute',
        top: 2,
        right: 2,
        display: 'flex',
        gap: '4px',
        zIndex: 1,
        background: data.backgroundColor, // Match node background
        padding: '2px',
        borderRadius: '4px'
      }}>
        {componentData?.parameters?.length > 0 && (
          <IconButton size="small" onClick={() => setDialogOpen(true)}>
            <SettingsIcon fontSize="small" />
          </IconButton>
        )}
        <IconButton size="small" onClick={handleDelete}>
          <DeleteIcon fontSize="small" color="error" />
        </IconButton>
      </div>

      {/* Node content with margin for icons */}
      <div style={nodeContentStyle}>
        {/* Input data flow handle - not for start/stop nodes */}
        {!isSource && !isStart && !isStop && (
          <Handle
            type="target"
            position={Position.Left}
            style={dataHandleStyleLeft}
            id="data-target"
            title="Data Flow In"
          />
        )}

        <Typography variant="body1">{data.label}</Typography>

        {/* Output data flow handle - not for start/stop nodes */}
        {!isDestination && !isStart && !isStop && (
          <Handle
            type="source"
            position={Position.Right}
            style={dataHandleStyleRight}
            id="data-source"
            title="Data Flow Out"
          />
        )}
      </div>

      <ParameterDialog
        open={dialogOpen}
        onClose={() => setDialogOpen(false)}
        node={{ data, type }}
        onSave={handleParameterSave}
      />
    </Paper>
  );
}

export default CustomNode; 