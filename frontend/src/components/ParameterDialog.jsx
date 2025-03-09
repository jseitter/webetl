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
    
    // Use displayName if available, otherwise fall back to label
    const displayLabel = param.displayName || param.label;
    
    switch (param.parameterType) {
      case 'string':
        return (
          <TextField
            key={param.name}
            fullWidth
            label={displayLabel}
            value={values[param.name] || ''}
            onChange={(e) => handleChange(param.name, e.target.value)}
            margin="normal"
            helperText={param.description}
            required={param.required}
            inputProps={{ maxLength: param.maxLength }}
          />
        );
      case 'number':
        return (
          <TextField
            key={param.name}
            fullWidth
            label={displayLabel}
            type="number"
            value={values[param.name] || ''}
            onChange={(e) => handleChange(param.name, e.target.value)}
            margin="normal"
            helperText={param.description}
            required={param.required}
            inputProps={{ maxLength: param.maxLength && param.maxLength > 0 ? param.maxLength : 255 }}
          />
        );
      case 'boolean':
        return (
          <FormControl key={param.name} fullWidth margin="normal">
            <InputLabel>{displayLabel}</InputLabel>
            <Select
              value={values[param.name] || false}
              onChange={(e) => handleChange(param.name, e.target.value)}
              label={displayLabel}
            >
              <MenuItem value={true}>Yes</MenuItem>
              <MenuItem value={false}>No</MenuItem>
            </Select>
            <FormHelperText>{param.description}</FormHelperText>
          </FormControl>
        );
      case 'select':
        return (
          <FormControl key={param.name} fullWidth margin="normal">
            <InputLabel>{displayLabel}</InputLabel>
            <Select
              value={values[param.name] || ''}
              onChange={(e) => handleChange(param.name, e.target.value)}
              label={displayLabel}
            >
              {param.options.map(option => {
                // Use option display name if available
                const optionDisplayName = param.optionDisplayNames && param.optionDisplayNames[option] 
                  ? param.optionDisplayNames[option] 
                  : option;
                  
                return (
                  <MenuItem key={option} value={option}>{optionDisplayName}</MenuItem>
                );
              })}
            </Select>
            <FormHelperText>{param.description}</FormHelperText>
          </FormControl>
        );
      case 'secret':
        return (
          <TextField
            key={param.name}
            fullWidth
            label={displayLabel}
            type="password"
            value={values[param.name] || ''}
            onChange={(e) => handleChange(param.name, e.target.value)}
            margin="normal"
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
            label={displayLabel}
            value={values[param.name] || ''}
            onChange={(e) => handleChange(param.name, e.target.value)}
            margin="normal"
            helperText={param.description}
            required={param.required}
          />
        );
      default:
        return (
          <TextField
            key={param.name}
            fullWidth
            label={displayLabel}
            value={values[param.name] || ''}
            onChange={(e) => handleChange(param.name, e.target.value)}
            margin="normal"
            helperText={param.description}
            required={param.required}
          />
        );
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