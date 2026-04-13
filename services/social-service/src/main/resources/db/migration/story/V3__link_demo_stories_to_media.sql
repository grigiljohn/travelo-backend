-- Point demo stories at travelo_media rows seeded by media-service (V3/V4 + DemoLocalMediaBlobSeeder).
-- Primary slide URLs align with post demo media 101/102; third story uses dedicated seed 105.

UPDATE stories
SET media_id = 'f1111111-1111-4111-8111-111111111101'::uuid
WHERE id = '4f2f9eb9-2a27-4fcb-8cab-d235a58e905a'::uuid;

UPDATE stories
SET media_id = 'f1111111-1111-4111-8111-111111111102'::uuid
WHERE id = '9e031bc7-91f4-4a7f-9bf5-a31f8938bf67'::uuid;

UPDATE stories
SET media_id = 'f1111111-1111-4111-8111-111111111105'::uuid
WHERE id = 'f0f39270-c24c-41d3-a26a-2f43fd331047'::uuid;
