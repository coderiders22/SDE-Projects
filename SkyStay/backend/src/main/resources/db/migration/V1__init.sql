CREATE TABLE users (
                       id            UUID PRIMARY KEY,
                       name          VARCHAR(120) NOT NULL,
                       email         VARCHAR(255) NOT NULL UNIQUE,
                       password_hash VARCHAR(255) NOT NULL,
                       role          VARCHAR(20)  NOT NULL,
                       created_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE hotels (
                        id              UUID PRIMARY KEY,
                        name            VARCHAR(160) NOT NULL,
                        city            VARCHAR(120) NOT NULL,
                        country         VARCHAR(120) NOT NULL,
                        description     TEXT NOT NULL,
                        rating          NUMERIC(2,1) NOT NULL DEFAULT 0,
                        price_per_night NUMERIC(10,2) NOT NULL,
                        image_url       TEXT,
                        amenities       TEXT[] NOT NULL DEFAULT '{}',
                        created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_hotels_city ON hotels(city);

CREATE TABLE rooms (
                       id              UUID PRIMARY KEY,
                       hotel_id        UUID NOT NULL REFERENCES hotels(id) ON DELETE CASCADE,
                       type            VARCHAR(20) NOT NULL,
                       price_per_night NUMERIC(10,2) NOT NULL,
                       capacity        INT NOT NULL,
                       available       BOOLEAN NOT NULL DEFAULT TRUE
);
CREATE INDEX idx_rooms_hotel ON rooms(hotel_id);

CREATE TABLE flights (
                         id               UUID PRIMARY KEY,
                         airline          VARCHAR(120) NOT NULL,
                         flight_number    VARCHAR(20)  NOT NULL,
                         origin           VARCHAR(10)  NOT NULL,
                         destination      VARCHAR(10)  NOT NULL,
                         departure_time   TIMESTAMPTZ  NOT NULL,
                         arrival_time     TIMESTAMPTZ  NOT NULL,
                         price            NUMERIC(10,2) NOT NULL,
                         seats_available  INT NOT NULL
);
CREATE INDEX idx_flights_route ON flights(origin, destination, departure_time);

CREATE TABLE bookings (
                          id          UUID PRIMARY KEY,
                          user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                          kind        VARCHAR(10) NOT NULL,
                          hotel_id    UUID REFERENCES hotels(id),
                          room_id     UUID REFERENCES rooms(id),
                          flight_id   UUID REFERENCES flights(id),
                          start_date  TIMESTAMPTZ NOT NULL,
                          end_date    TIMESTAMPTZ,
                          total       NUMERIC(10,2) NOT NULL,
                          status      VARCHAR(20) NOT NULL,
                          payment     VARCHAR(20) NOT NULL,
                          created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_bookings_user ON bookings(user_id);
CREATE INDEX idx_bookings_room ON bookings(room_id);

CREATE UNIQUE INDEX uq_room_window
    ON bookings (room_id, start_date, end_date)
    WHERE status <> 'CANCELLED' AND room_id IS NOT NULL;

CREATE TABLE reviews (
                         id         UUID PRIMARY KEY,
                         hotel_id   UUID NOT NULL REFERENCES hotels(id) ON DELETE CASCADE,
                         user_id    UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                         rating     INT NOT NULL CHECK (rating BETWEEN 1 AND 5),
                         comment    TEXT,
                         created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                         UNIQUE (hotel_id, user_id)
);

INSERT INTO users (id, name, email, password_hash, role) VALUES
    ('00000000-0000-0000-0000-000000000001',
     'SkyStay Admin',
     'admin@skystay.io',
     '$2a$10$DowFnYf2bJtZmH8d2Z3a3eL5gJk1xX1F7w9c4uYJZBp1g7g2k6yEi',
     'ADMIN');