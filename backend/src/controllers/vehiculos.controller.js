import { pool } from '../db.js';

export const obtenerVehiculos = async (req, res) => {
  try {
    const user = req.user;
    let result;

    if (user.role === 'admin') {
      result = await pool.query('SELECT * FROM vehiculos ORDER BY idvehiculo ASC');
    } else if (user.role === 'trabajador') {
      if (!user.idvehiculo) {
        return res.json([]);
      }
      result = await pool.query('SELECT * FROM vehiculos WHERE idvehiculo = $1', [user.idvehiculo]);
    } else {
      return res.status(403).json({ error: 'Rol de usuario no autorizado' });
    }
    
    res.json(result.rows);
  } catch (error) {
    console.error('Error en obtenerVehiculos:', error);
    res.status(500).json({ error: 'Error al obtener los vehículos' });
  }
};

export const crearVehiculo = async (req, res) => {
    const { patente, marca, modelo } = req.body;
    if (!patente || !marca || !modelo) {
        return res.status(400).json({ message: "La patente, marca y modelo son requeridos." });
    }

    try {
        const result = await pool.query(
            "INSERT INTO vehiculos (patente, marca, modelo) VALUES ($1, $2, $3) RETURNING *",
            [patente, marca, modelo]
        );
        res.status(201).json(result.rows[0]);
    } catch (error) {
        if (error.code === '23505') { // Error de constraint UNIQUE
            return res.status(400).json({ message: 'La patente ya está registrada.' });
        }
        console.error('Error en crearVehiculo:', error);
        res.status(500).json({ error: 'Error al crear el vehículo' });
    }
};

export const actualizarVehiculo = async (req, res) => {
    const { id } = req.params;
    const { marca, modelo } = req.body;

    if (!marca || !modelo) {
        return res.status(400).json({ message: "Marca y modelo son requeridos." });
    }

    try {
        const result = await pool.query(
            "UPDATE vehiculos SET marca = $1, modelo = $2 WHERE idvehiculo = $3 RETURNING *",
            [marca, modelo, id]
        );
        if (result.rows.length === 0) {
            return res.status(404).json({ message: 'Vehículo no encontrado.' });
        }
        res.json(result.rows[0]);
    } catch (error) {
        console.error('Error en actualizarVehiculo:', error);
        res.status(500).json({ error: 'Error al actualizar el vehículo' });
    }
};

export const eliminarVehiculo = async (req, res) => {
    const { id } = req.params;
    try {
        const result = await pool.query("DELETE FROM vehiculos WHERE idvehiculo = $1 RETURNING *", [id]);
        if (result.rowCount === 0) {
            return res.status(404).json({ message: 'Vehículo no encontrado.' });
        }
        res.sendStatus(204); // No Content
    } catch (error) {
        console.error('Error en eliminarVehiculo:', error);
        res.status(500).json({ error: 'Error al eliminar el vehículo' });
    }
};
