import { useState, useEffect } from 'react';
import { 
  Dialog, DialogTitle, DialogContent, DialogActions, 
  Button, TextField, FormControl, InputLabel, 
  MenuItem, Select, FormHelperText, Box 
} from '@mui/material';

function ParameterDialog({ open, onClose, node, onSave }) {
  const [values, setValues] = useState({});

  useEffect(() => {
    if (node?.data?.componentData?.parameters) {
      const initialValues = {};
      node.data.componentData.parameters.forEach(param => {
        initialValues[param.name] = param.value || '';
      });
      setValues(initialValues);
    }
  }, [node]);

  const handleSave = () => {
    onSave(values);
    onClose();
  };

  const renderParameter = (param) => {
    switch (param.parameterType) {
      case 'string':
        return (
          <TextField
            key={param.name}
            fullWidth
            label={param.label}
            value={values[param.name] || ''}
            onChange={(e) => setValues({...values, [param.name]: e.target.value})}
            helperText={param.description}
            required={param.required}
            inputProps={{ maxLength: param.maxLength }}
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
            onChange={(e) => setValues({...values, [param.name]: e.target.value})}
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
            onChange={(e) => setValues({...values, [param.name]: e.target.value})}
            helperText={param.description}
            required={param.required}
          />
        );
      // Add other parameter types as needed
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