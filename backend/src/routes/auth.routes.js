import { Router } from 'express';
import { 
    login, 
    register, 
    logout, 
    profile, 
    setupFirstAdmin, 
    debugDb,
    getTrabajadores,
    updateTrabajador,
    deleteTrabajador 
} from '../controllers/auth.controller.js';
import { authRequired } from '../middlewares/validateToken.js';
import { isAdmin } from '../middlewares/validateAdmin.js';

const router = Router();

// Rutas de Autenticación y Sesión
router.post('/login', login);
router.post('/logout', logout);
router.get('/profile', authRequired, profile);

// Rutas de Configuración (Temporales)
router.post('/setup-first-admin', setupFirstAdmin);
router.get('/debug-db', debugDb);

// Rutas de Gestión de Trabajadores (Solo Admin)
router.get('/trabajadores', authRequired, isAdmin, getTrabajadores);
router.post('/register', authRequired, isAdmin, register); // 'register' es crear un trabajador
router.put('/trabajadores/:id', authRequired, isAdmin, updateTrabajador);
router.delete('/trabajadores/:id', authRequired, isAdmin, deleteTrabajador);


export default router;