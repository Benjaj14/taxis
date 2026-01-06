import { pool } from '../db.js';

const Vehiculo = {
  async getAll() {
    const result = await pool.query('SELECT * FROM Vehiculos');
    return result.rows;
  },

  async getById(id) {
    const result = await pool.query('SELECT * FROM Vehiculos WHERE IdVehiculo = $1', [id]);
    return result.rows[0];
  },

  async getByPatente(patente) {
    const result = await pool.query('SELECT * FROM Vehiculos WHERE patente = $1', [patente]);
    return result.rows[0];
  },

  async create({ patente, marca, modelo }) {
    const result = await pool.query(
      'INSERT INTO Vehiculos (patente, marca, modelo) VALUES ($1, $2, $3) RETURNING *',
      [patente, marca, modelo]
    );
    return result.rows[0];
  },

  async update(id, { patente, marca, modelo }) {
    const result = await pool.query(
      'UPDATE Vehiculos SET patente = $1, marca = $2, modelo = $3 WHERE IdVehiculo = $4 RETURNING *',
      [patente, marca, modelo, id]
    );
    return result.rows[0];
  },

  async delete(id) {
    const result = await pool.query('DELETE FROM Vehiculos WHERE IdVehiculo = $1 RETURNING *', [id]);
    return result.rows[0];
  }
};

export default Vehiculo;
