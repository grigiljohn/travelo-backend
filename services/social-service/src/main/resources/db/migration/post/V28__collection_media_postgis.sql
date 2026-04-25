CREATE EXTENSION IF NOT EXISTS postgis;

ALTER TABLE collection_media
    ADD COLUMN IF NOT EXISTS geom geography(Point, 4326);

UPDATE collection_media
SET geom = ST_SetSRID(ST_MakePoint(longitude, latitude), 4326)::geography
WHERE latitude IS NOT NULL
  AND longitude IS NOT NULL
  AND geom IS NULL;

CREATE OR REPLACE FUNCTION sync_collection_media_geom()
RETURNS trigger AS $$
BEGIN
    IF NEW.latitude IS NOT NULL AND NEW.longitude IS NOT NULL THEN
        NEW.geom := ST_SetSRID(ST_MakePoint(NEW.longitude, NEW.latitude), 4326)::geography;
    ELSE
        NEW.geom := NULL;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_collection_media_geom_sync ON collection_media;
CREATE TRIGGER trg_collection_media_geom_sync
BEFORE INSERT OR UPDATE OF latitude, longitude
ON collection_media
FOR EACH ROW
EXECUTE FUNCTION sync_collection_media_geom();

CREATE INDEX IF NOT EXISTS idx_collection_media_geom_gist
    ON collection_media
    USING GIST (geom);

CREATE INDEX IF NOT EXISTS idx_collection_media_created_at
    ON collection_media (created_at DESC);
