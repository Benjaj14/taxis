-- Este archivo SQL es solo un esquema de referencia para la estructura de la base de datos.
-- NO DEBE ser ejecutado directamente para crear o modificar la base de datos,
-- ya que la base de datos principal se gestiona externamente (ej. en PgAdmin).

-- 🧑‍💼 Tabla: UsuarioAdmin
CREATE TABLE UsuarioAdmin (
    IdAdmin SERIAL PRIMARY KEY,
    email VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL
);

-- 🚗 Tabla: Vehiculos
CREATE TABLE Vehiculos (
    IdVehiculo SERIAL PRIMARY KEY,
    patente VARCHAR(10) NOT NULL UNIQUE,
    marca VARCHAR(20),
    modelo VARCHAR(20)
);

-- 🛣️ Tabla: Recorridos
CREATE TABLE Recorridos (
    IdRecorrido SERIAL PRIMARY KEY,
    IdVehiculo INT REFERENCES Vehiculos(IdVehiculo) ON DELETE CASCADE,
    inicio TIMESTAMP NOT NULL,
    fin TIMESTAMP,
    km DECIMAL(10,2),
    duracion INT,
    estado VARCHAR(20) NOT NULL
);

-- 📍 Tabla: PosicionesGPS
CREATE TABLE PosicionesGPS (
    IdPosicion SERIAL PRIMARY KEY,
    IdRecorrido INT REFERENCES Recorridos(IdRecorrido) ON DELETE CASCADE,
    latitud DECIMAL(10,7) NOT NULL,
    longitud DECIMAL(10,7) NOT NULL,
    FechaHora TIMESTAMP DEFAULT NOW()
);

-- 👷‍♂️ Tabla: UsuariosTrabajadores
CREATE TABLE UsuariosTrabajadores (
    IdTrabajador SERIAL PRIMARY KEY,
    Nombre VARCHAR(100),
    Email VARCHAR(100) NOT NULL UNIQUE,
    Password VARCHAR(255) NOT NULL,
    IdVehiculo INT REFERENCES Vehiculos(IdVehiculo) ON DELETE SET NULL
);