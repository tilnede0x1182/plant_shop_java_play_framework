# --- !Ups

CREATE TABLE IF NOT EXISTS users (
	id SERIAL PRIMARY KEY,
	email VARCHAR(255) UNIQUE NOT NULL,
	name VARCHAR(64) NOT NULL,
	password_hash VARCHAR(255) NOT NULL,
	is_admin BOOLEAN NOT NULL DEFAULT FALSE,
	created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS plants (
	id SERIAL PRIMARY KEY,
	name VARCHAR(100) NOT NULL,
	description TEXT,
	price NUMERIC(10,2) NOT NULL,
	stock INTEGER NOT NULL DEFAULT 0,
	created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS orders (
	id SERIAL PRIMARY KEY,
	user_id INTEGER REFERENCES users(id) ON DELETE SET NULL, -- ON DELETE SET NULL pour garder l'historique
	total NUMERIC(10,2) NOT NULL,
	status VARCHAR(50) NOT NULL DEFAULT 'pending', -- Ajout de la colonne status
	created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS order_items (
	id SERIAL PRIMARY KEY,
	order_id INTEGER REFERENCES orders(id) ON DELETE CASCADE,
	plant_id INTEGER REFERENCES plants(id) ON DELETE SET NULL, -- ON DELETE SET NULL pour ne pas perdre l'item si la plante est supprimée
	quantity INTEGER NOT NULL,
	price NUMERIC(10,2) NOT NULL
);

-- Index et contraintes utiles
CREATE INDEX idx_orders_user_id ON orders(user_id);
CREATE INDEX idx_order_items_order_id ON order_items(order_id);
CREATE INDEX idx_order_items_plant_id ON order_items(plant_id);

# --- !Downs

DROP TABLE IF EXISTS order_items CASCADE;
DROP TABLE IF EXISTS orders CASCADE;
DROP TABLE IF EXISTS plants CASCADE;
DROP TABLE IF EXISTS users CASCADE;
