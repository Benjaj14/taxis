import { pool } from '../db.js';

const Recorrido = {
  async getAll() {
    const result = await pool.query('SELECT * FROM Recorridos');
    return result.rows;
  },

  async getById(id) {
    const result = await pool.query('SELECT * FROM Recorridos WHERE IdRecorrido = $1', [id]);
    return result.rows[0];
  },

  async getByVehiculoId(vehiculoId) {
    const result = await pool.query('SELECT * FROM Recorridos WHERE IdVehiculo = $1 ORDER BY inicio DESC', [vehiculoId]);
    return result.rows;
  },

  async create({ IdVehiculo, inicio, fin, km, duracion, estado }) {
    const result = await pool.query(
      'INSERT INTO Recorridos (IdVehiculo, inicio, fin, km, duracion, estado) VALUES ($1, $2, $3, $4, $5, $6) RETURNING *',
      [IdVehiculo, inicio, fin, km, duracion, estado]
    );
    return result.rows[0];
  },

  async update(id, { IdVehiculo, inicio, fin, km, duracion, estado }) {
    const result = await pool.query(
      'UPDATE Recorridos SET IdVehiculo = $1, inicio = $2, fin = $3, km = $4, duracion = $5, estado = $6 WHERE IdRecorrido = $7 RETURNING *',
      [IdVehiculo, inicio, fin, km, duracion, estado, id]
    );
    return result.rows[0];
  },

  async delete(id) {
    const result = await pool.query('DELETE FROM Recorridos WHERE IdRecorrido = $1 RETURNING *', [id]);
    return result.rows[0];
  }
};

export default Recorrido;
