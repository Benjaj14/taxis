import { pool } from '../db.js';
import bcrypt from 'bcryptjs';
import { createAccessToken } from '../libs/jwt.js';

export const login = async (req, res) => {
    const { email, password } = req.body;

    try {
        let userFound = null;
        let userRole = null;
        let userId = null;
        let payload = {};

        // 1. Buscar en la tabla de administradores
        const adminResult = await pool.query('SELECT * FROM usuarioadmin WHERE email = $1', [email]);
        if (adminResult.rows.length > 0) {
            userFound = adminResult.rows[0];
            userRole = 'admin';
            userId = userFound.idadmin;
            payload = { id: userId, role: userRole, email: userFound.email };
        } else {
            // 2. Si no es admin, buscar en la tabla de trabajadores
            const workerResult = await pool.query('SELECT * FROM usuariostrabajadores WHERE email = $1', [email]);
            if (workerResult.rows.length > 0) {
                userFound = workerResult.rows[0];
                userRole = 'trabajador';
                userId = userFound.idtrabajador;
                payload = { id: userId, role: userRole, email: userFound.email, idvehiculo: userFound.idvehiculo };
            }
        }

        if (!userFound) {
            return res.status(400).json({ message: "Usuario o contraseña incorrectos" });
        }

        // 3. Comparar contraseñas
        const isMatch = await bcrypt.compare(password, userFound.password);
        if (!isMatch) {
            return res.status(400).json({ message: "Usuario o contraseña incorrectos" });
        }

        // 4. Crear y firmar el token
        const token = await createAccessToken(payload);

        res.cookie('token', token, {
            httpOnly: true,
            secure: process.env.NODE_ENV === 'production',
            sameSite: 'strict'
        });

        res.json({
            id: userId,
            email: userFound.email,
            role: userRole,
            token: token // Añadir token a la respuesta JSON
        });

    } catch (error) {
        console.error('Error en login:', error);
        res.status(500).json({ message: "Error interno del servidor" });
    }
};

export const register = async (req, res) => {
    const { email, password, nombre, idvehiculo } = req.body;

    try {
        // Encriptar la contraseña
        const passwordHash = await bcrypt.hash(password, 10);

        // Insertar en la base de datos
        const result = await pool.query(
            'INSERT INTO usuariostrabajadores (nombre, email, password, idvehiculo) VALUES ($1, $2, $3, $4) RETURNING *',
            [nombre, email, passwordHash, idvehiculo]
        );

        const newUser = result.rows[0];

        res.status(201).json({
            id: newUser.idtrabajador,
            nombre: newUser.nombre,
            email: newUser.email,
            idvehiculo: newUser.idvehiculo,
        });

    } catch (error) {
        // Manejo de error para email duplicado
        if (error.code === '23505') {
            return res.status(400).json({ message: 'El correo electrónico ya está registrado.' });
        }
        console.error('Error en register:', error);
        res.status(500).json({ message: "Error al registrar el usuario" });
    }
};

export const logout = (req, res) => {
    res.cookie('token', '', {
        expires: new Date(0),
    });
    return res.sendStatus(200);
};

export const profile = async (req, res) => {
    try {
        let userFound = null;

        if (req.user.role === 'admin') {
            const result = await pool.query('SELECT idadmin, email FROM usuarioadmin WHERE idadmin = $1', [req.user.id]);
            userFound = result.rows[0];
            if (userFound) userFound.role = 'admin';

        } else if (req.user.role === 'trabajador') {
            const result = await pool.query('SELECT idtrabajador, nombre, email, idvehiculo FROM usuariostrabajadores WHERE idtrabajador = $1', [req.user.id]);
            userFound = result.rows[0];
            if (userFound) userFound.role = 'trabajador';
        }

        if (!userFound) {
            return res.status(404).json({ message: "Usuario no encontrado" });
        }

        res.json(userFound);

    } catch (error) {
        console.error('Error en profile:', error);
        res.status(500).json({ message: "Error interno del servidor" });
    }
};

// ... (resto de las importaciones y funciones existentes) ...

export const setupFirstAdmin = async (req, res) => {
    // ... (código existente)
};

export const getTrabajadores = async (req, res) => {
    try {
        const result = await pool.query('SELECT idtrabajador, nombre, email, idvehiculo FROM usuariostrabajadores ORDER BY nombre ASC');
        res.json(result.rows);
    } catch (error) {
        console.error('Error en getTrabajadores:', error);
        res.status(500).json({ message: "Error al obtener los trabajadores." });
    }
};

export const updateTrabajador = async (req, res) => {
    const { id } = req.params;
    const { email, password, nombre, idvehiculo } = req.body;

    try {
        // 1. Obtener los datos actuales del trabajador
        const currentDataResult = await pool.query('SELECT * FROM usuariostrabajadores WHERE idtrabajador = $1', [id]);
        if (currentDataResult.rows.length === 0) {
            return res.status(404).json({ message: "Trabajador no encontrado." });
        }
        const currentWorker = currentDataResult.rows[0];

        // 2. Preparar la nueva data, usando valores actuales como base
        const newNombre = nombre || currentWorker.nombre;
        const newEmail = email || currentWorker.email;
        const newIdVehiculo = idvehiculo ? parseInt(idvehiculo, 10) : currentWorker.idvehiculo;
        
        let newPasswordHash;
        if (password) {
            // Si se provee una nueva contraseña, la hasheamos
            newPasswordHash = await bcrypt.hash(password, 10);
        } else {
            // Si no, usamos la contraseña hasheada que ya estaba en la BD
            newPasswordHash = currentWorker.password;
        }

        // 3. Ejecutar una consulta UPDATE estática y robusta
        const updateQuery = `
            UPDATE usuariostrabajadores 
            SET nombre = $1, email = $2, password = $3, idvehiculo = $4 
            WHERE idtrabajador = $5 
            RETURNING idtrabajador, nombre, email, idvehiculo
        `;
        const values = [newNombre, newEmail, newPasswordHash, newIdVehiculo, id];

        const result = await pool.query(updateQuery, values);

        res.json(result.rows[0]);

    } catch (error) {
        console.error('Error en updateTrabajador:', error);
        res.status(500).json({ message: "Error al actualizar el trabajador." });
    }
};

export const deleteTrabajador = async (req, res) => {
    const { id } = req.params;
    try {
        const result = await pool.query("DELETE FROM usuariostrabajadores WHERE idtrabajador = $1 RETURNING *", [id]);
        if (result.rowCount === 0) {
            return res.status(404).json({ message: 'Trabajador no encontrado.' });
        }
        res.sendStatus(204); // No Content
    } catch (error) {
        console.error('Error en deleteTrabajador:', error);
        res.status(500).json({ message: 'Error al eliminar el trabajador.' });
    }
};

export const debugDb = async (req, res) => {
    // ... (código existente)
};

