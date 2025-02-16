import { useState, useEffect } from 'react';
import { 
  Box, 
  List, 
  ListItem, 
  ListItemText, 
  IconButton, 
  Typography, 
  Button,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField
} from '@mui/material';
import DeleteIcon from '@mui/icons-material/Delete';
import EditIcon from '@mui/icons-material/Edit';
import AddIcon from '@mui/icons-material/Add';
import { useNavigate } from 'react-router-dom';
import axios from 'axios';

function ProjectList() {
  const [projects, setProjects] = useState([]);
  const [newProjectDialog, setNewProjectDialog] = useState(false);
  const [newProjectName, setNewProjectName] = useState('');
  const navigate = useNavigate();

  useEffect(() => {
    fetchProjects();
  }, []);

  const fetchProjects = async () => {
    try {
      const response = await axios.get('/api/projects', {
        withCredentials: true
      });
      setProjects(Array.isArray(response.data) ? response.data : []);
    } catch (error) {
      console.error('Error fetching projects:', error);
    }
  };

  const handleCreateProject = async () => {
    try {
      const response = await axios.post('/api/projects', 
        { name: newProjectName },
        { withCredentials: true }
      );
      setProjects([...projects, response.data]);
      setNewProjectDialog(false);
      setNewProjectName('');
    } catch (error) {
      console.error('Error creating project:', error);
    }
  };

  const handleDeleteProject = async (projectId) => {
    try {
      await axios.delete(`/api/projects/${projectId}`);
      setProjects(projects.filter(p => p.id !== projectId));
    } catch (error) {
      console.error('Error deleting project:', error);
    }
  };

  const handleOpenProject = (projectId) => {
    navigate(`/projects/${projectId}`);
  };

  return (
    <Box sx={{ maxWidth: 800, margin: 'auto', mt: 4, p: 2 }}>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 3 }}>
        <Typography variant="h4">ETL Projects</Typography>
        <Button
          variant="contained"
          startIcon={<AddIcon />}
          onClick={() => setNewProjectDialog(true)}
        >
          New Project
        </Button>
      </Box>

      <List>
        {projects.map((project) => (
          <ListItem
            key={project.id}
            sx={{
              border: '1px solid #ddd',
              borderRadius: 1,
              mb: 1,
              '&:hover': { backgroundColor: '#f5f5f5' }
            }}
            secondaryAction={
              <IconButton 
                edge="end" 
                onClick={() => handleDeleteProject(project.id)}
                color="error"
              >
                <DeleteIcon />
              </IconButton>
            }
          >
            <ListItemText
              primary={project.name}
              onClick={() => handleOpenProject(project.id)}
              sx={{ cursor: 'pointer' }}
            />
          </ListItem>
        ))}
      </List>

      <Dialog open={newProjectDialog} onClose={() => setNewProjectDialog(false)}>
        <DialogTitle>Create New Project</DialogTitle>
        <DialogContent>
          <TextField
            autoFocus
            margin="dense"
            label="Project Name"
            fullWidth
            value={newProjectName}
            onChange={(e) => setNewProjectName(e.target.value)}
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setNewProjectDialog(false)}>Cancel</Button>
          <Button onClick={handleCreateProject} variant="contained">Create</Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
}

export default ProjectList; 