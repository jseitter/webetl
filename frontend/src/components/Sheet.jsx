import { useState, useCallback, useEffect } from 'react';
import ReactFlow, {
  Background,
  Controls,
  addEdge,
  useNodesState,
  useEdgesState,
  BaseEdge,
  getStraightPath,
  getBezierPath,
  ReactFlowProvider,
  useReactFlow
} from 'reactflow';
import 'reactflow/dist/style.css';
import Palette from './Palette';
import { Box, Button, CircularProgress } from '@mui/material';
import CustomNode from './CustomNode';
import PlayArrowIcon from '@mui/icons-material/PlayArrow';
import StopIcon from '@mui/icons-material/Stop';
import axios from 'axios';

const nodeTypes = {
  source: CustomNode,
  transform: CustomNode,
  destination: CustomNode,
};

// Different styles for different connection types
const getEdgeStyle = (type) => {
  return type === 'control-flow' 
    ? { stroke: '#888', strokeWidth: 2, strokeDasharray: '5,5' }
    : { stroke: '#555', strokeWidth: 2 };
};

function SheetWithProvider(props) {
  return (
    <ReactFlowProvider>
      <Sheet {...props} />
    </ReactFlowProvider>
  );
}

function Sheet({ sheet, onUpdate }) {
  const { project } = useReactFlow();
  const [nodes, setNodes, onNodesChange] = useNodesState(sheet.nodes);
  const [edges, setEdges, onEdgesChange] = useEdgesState(sheet.edges);
  const [isExecuting, setIsExecuting] = useState(false);
  const [executionMetrics, setExecutionMetrics] = useState(null);
  const [isSaved, setIsSaved] = useState(false);

  // Notify parent component of changes
  useEffect(() => {
    onUpdate(nodes, edges);
  }, [nodes, edges, onUpdate]);

  // Add this useEffect to clean up old edges
  useEffect(() => {
    setEdges(edges => edges.map(edge => {
      // Update source handles
      if (edge.sourceHandle === 'control-source') {
        edge.sourceHandle = 'control-flow-out';
      }
      // Update target handles
      if (edge.targetHandle === 'control-target') {
        edge.targetHandle = 'control-flow-in';
      }
      return edge;
    }));
  }, []);

  // Update onConnect to prevent creating edges with old IDs
  const onConnect = useCallback((params) => {
    const isControlFlow = params.sourceHandle?.startsWith('control-flow') || 
                         params.targetHandle?.startsWith('control-flow');
    
    // Ensure we only use new handle IDs
    const edge = {
      ...params,
      sourceHandle: params.sourceHandle?.replace('control-source', 'control-flow-out')
                                      .replace('control-target', 'control-flow-in'),
      targetHandle: params.targetHandle?.replace('control-source', 'control-flow-out')
                                      .replace('control-target', 'control-flow-in'),
      type: 'default',
      style: getEdgeStyle(isControlFlow ? 'control-flow' : 'data'),
      animated: isControlFlow,
    };

    setEdges((eds) => addEdge(edge, eds));
  }, [setEdges]);

  const onDrop = (event) => {
    event.preventDefault();
    
    const type = event.dataTransfer.getData('application/reactflow');
    const rawData = event.dataTransfer.getData('application/etlcomponent');
    const componentData = JSON.parse(rawData);
    
    console.log('Drop - type:', type);
    console.log('Drop - componentData:', componentData);

    // Get the ReactFlow viewport position
    const reactFlowBounds = event.currentTarget.getBoundingClientRect();
    const position = project({
      x: event.clientX - reactFlowBounds.left,
      y: event.clientY - reactFlowBounds.top,
    });

    const newNode = {
      id: `${componentData.id}-${Date.now()}`,
      type: type,
      position,
      data: {
        label: componentData.label,
        backgroundColor: componentData.backgroundColor,
        componentData: componentData
      }
    };

    console.log('Created new node:', newNode);
    setNodes((nds) => nds.concat(newNode));
  };

  const onDragOver = (event) => {
    event.preventDefault();
    event.dataTransfer.dropEffect = 'move';
  };

  const isValidConnection = (connection) => {
    const isControlFlow = connection.sourceHandle?.startsWith('control-flow') || 
                         connection.targetHandle?.startsWith('control-flow');
    
    if (isControlFlow) {
      const sourceNode = nodes.find(n => n.id === connection.source);
      const targetNode = nodes.find(n => n.id === connection.target);
      const sourceData = sourceNode?.data?.componentData;
      const targetData = targetNode?.data?.componentData;

      // Start node can connect to any source node
      if (sourceData?.id === 'start') {
        return targetNode?.type === 'source' && 
               connection.sourceHandle === 'control-flow-out' &&
               connection.targetHandle === 'control-flow-in';
      }

      // Stop node can only have incoming control flow
      if (targetData?.id === 'stop') {
        return connection.targetHandle === 'control-flow-in';
      }

      // Other source nodes can have both control connections
      return sourceNode?.type === 'source' && targetNode?.type === 'source';
    }

    // Data flow validation
    const sourceNode = nodes.find(n => n.id === connection.source);
    const targetNode = nodes.find(n => n.id === connection.target);
    
    // Get component data
    const sourceData = sourceNode?.data?.componentData;
    const targetData = targetNode?.data?.componentData;

    // Prevent data connections to/from start/stop nodes
    if (sourceData?.id === 'start' || sourceData?.id === 'stop' ||
        targetData?.id === 'start' || targetData?.id === 'stop') {
      return false;
    }
    
    // Prevent connecting to source nodes with data flow
    if (targetNode?.type === 'source') return false;
    
    // Prevent destination nodes from having outgoing connections
    if (sourceNode?.type === 'destination') return false;

    return true;
  };

  const handleExecute = async () => {
    setIsExecuting(true);
    try {
      const response = await axios.post(`/api/sheets/${sheet.id}/execute`);
      setExecutionMetrics(response.data);
      startMetricsPolling(sheet.id);
    } catch (error) {
      console.error('Execution failed:', error);
      setIsExecuting(false);
    }
  };

  const handleStop = () => {
    setIsExecuting(false);
    setExecutionMetrics(null);
    stopMetricsPolling();
  };

  // Polling for metrics updates
  const [metricsInterval, setMetricsInterval] = useState(null);

  const startMetricsPolling = (sheetId) => {
    const interval = setInterval(async () => {
      try {
        const response = await axios.get(`/api/sheets/${sheetId}/metrics`);
        setExecutionMetrics(response.data);
        
        // Check if execution is complete
        if (isExecutionComplete(response.data)) {
          stopMetricsPolling();
          setIsExecuting(false);
        }
      } catch (error) {
        console.error('Failed to fetch metrics:', error);
      }
    }, 1000);
    setMetricsInterval(interval);
  };

  const stopMetricsPolling = () => {
    if (metricsInterval) {
      clearInterval(metricsInterval);
      setMetricsInterval(null);
    }
  };

  const isExecutionComplete = (metrics) => {
    return Object.values(metrics.nodeMetrics).every(
      node => node.status === 'completed' || node.status === 'error'
    );
  };

  const handleSave = async () => {
    try {
      const sheetData = {
        id: sheet.id,
        nodes: nodes,
        edges: edges
      };
      
      await axios.post('/api/sheets/save', sheetData);
      setIsSaved(true);
      console.log('Sheet saved successfully');
    } catch (error) {
      console.error('Error saving sheet:', error);
    }
  };

  return (
    <Box sx={{ display: 'flex', width: '100%', height: '100%', position: 'relative' }}>
      <Box sx={{ flexGrow: 1, height: '100%' }}>
        <ReactFlow
          nodes={nodes}
          edges={edges}
          onNodesChange={onNodesChange}
          onEdgesChange={onEdgesChange}
          onConnect={onConnect}
          onDrop={isExecuting ? undefined : onDrop}
          onDragOver={isExecuting ? undefined : onDragOver}
          nodeTypes={nodeTypes}
          isValidConnection={isExecuting ? () => false : isValidConnection}
          fitView
          defaultViewport={{ x: 0, y: 0, zoom: 1 }}
        >
          <Background />
          <Controls />
          {isExecuting && <ExecutionOverlay metrics={executionMetrics} />}
        </ReactFlow>
      </Box>
      {!isExecuting && (
        <Palette />
      )}
    </Box>
  );
}

// Add ExecutionOverlay component to show metrics
function ExecutionOverlay({ metrics }) {
  if (!metrics) return null;

  return (
    <div style={{
      position: 'absolute',
      top: 0,
      left: 0,
      right: 0,
      bottom: 0,
      background: 'rgba(0,0,0,0.1)',
      pointerEvents: 'none',
      zIndex: 1000
    }}>
      {Object.entries(metrics.nodeMetrics).map(([nodeId, nodeMetrics]) => (
        <NodeMetrics
          key={nodeId}
          nodeId={nodeId}
          metrics={nodeMetrics}
        />
      ))}
    </div>
  );
}

function NodeMetrics({ nodeId, metrics }) {
  const nodePosition = useNodePosition(nodeId);
  
  if (!nodePosition) return null;

  return (
    <div style={{
      position: 'absolute',
      top: nodePosition.y - 30,
      left: nodePosition.x,
      background: 'white',
      padding: '4px 8px',
      borderRadius: '4px',
      boxShadow: '0 2px 4px rgba(0,0,0,0.1)',
      fontSize: '12px'
    }}>
      <div>Records: {metrics.recordsProcessed}</div>
      <div>Status: {metrics.status}</div>
      {metrics.status === 'running' && <CircularProgress size={16} />}
    </div>
  );
}

// Custom hook to get node position
function useNodePosition(nodeId) {
  const { getNode } = useReactFlow();
  const node = getNode(nodeId);
  return node?.position;
}

export default SheetWithProvider; 