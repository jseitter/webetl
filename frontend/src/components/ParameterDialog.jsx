import { useState, useEffect } from 'react';
import { 
  Dialog, DialogTitle, DialogContent, DialogActions, 
  Button, TextField, FormControl, InputLabel, 
  MenuItem, Select, FormHelperText, Box 
} from '@mui/material';

function ParameterDialog({ open, onClose, node, onSave }) {
  const [values, setValues] = useState({});
  
  // Add debug logging to help diagnose the issue
  console.log('ParameterDialog rendering with values:', values);
  console.log('Node data:', node?.data?.componentData?.parameters);

  useEffect(() => {
    if (node?.data?.componentData?.parameters) {
      const initialValues = {};
      node.data.componentData.parameters.forEach(param => {
        initialValues[param.name] = param.value || '';
      });
      console.log('Setting initial values:', initialValues);
      setValues(initialValues);
    }
  }, [node]);

  const handleSave = () => {
    console.log('Saving values:', values);
    onSave(values);
    onClose();
  };

  const handleChange = (paramName, value) => {
    console.log(`Changing ${paramName} to:`, value);
    setValues(prevValues => ({
      ...prevValues,
      [paramName]: value
    }));
  };

  const renderParameter = (param) => {
    console.log(`Rendering parameter ${param.name}, type: ${param.parameterType}, maxLength: ${param.maxLength}`);
    
    switch (param.parameterType) {
      case 'string':
        return (
          <TextField
            key={param.name}
            fullWidth
            label={param.label}
            value={values[param.name] || ''}
            onChange={(e) => handleChange(param.name, e.target.value)}
            helperText={param.description}
            required={param.required}
            inputProps={{ maxLength: param.maxLength && param.maxLength > 0 ? param.maxLength : 255 }}
          />
        );
      case 'secret':
        return (
          <TextField
            key={param.name}
            fullWidth
            type="password"
            label={param.label}
            value={values[param.name] || ''}
            onChange={(e) => handleChange(param.name, e.target.value)}
            helperText={param.description}
            required={param.required}
          />
        );
      case 'sql':
        return (
          <TextField
            key={param.name}
            fullWidth
            multiline
            rows={4}
            label={param.label}
            value={values[param.name] || ''}
            onChange={(e) => handleChange(param.name, e.target.value)}
            helperText={param.description}
            required={param.required}
          />
        );
      case 'select':
        return (
          <FormControl key={param.name} fullWidth required={param.required}>
            <InputLabel>{param.label}</InputLabel>
            <Select
              value={values[param.name] || ''}
              onChange={(e) => handleChange(param.name, e.target.value)}
              label={param.label}
            >
              {param.options?.map((option) => (
                <MenuItem key={option} value={option}>{option}</MenuItem>
              ))}
            </Select>
            {param.description && <FormHelperText>{param.description}</FormHelperText>}
          </FormControl>
        );
      // Add other parameter types as needed
      default:
        console.warn(`Unknown parameter type: ${param.parameterType}`);
        return null;
    }
  };

  return (
    <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
      <DialogTitle>Configure {node?.data?.label}</DialogTitle>
      <DialogContent>
        {node?.data?.componentData?.parameters?.map(param => (
          <Box key={param.name} sx={{ my: 2 }}>
            {renderParameter(param)}
          </Box>
        ))}
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose}>Cancel</Button>
        <Button onClick={handleSave} variant="contained">Save</Button>
      </DialogActions>
    </Dialog>
  );
}

export default ParameterDialog; 