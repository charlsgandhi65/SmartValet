-- ============================================
-- SmartValet Database Updates
-- Run these queries in Supabase SQL Editor
-- ============================================

-- 1. Add pricing columns to events table (for Issue #7 from previous session)
ALTER TABLE events 
ADD COLUMN IF NOT EXISTS base_price NUMERIC(10, 2) DEFAULT 100;

ALTER TABLE events 
ADD COLUMN IF NOT EXISTS extra_charge_per_hour NUMERIC(10, 2) DEFAULT 50;

COMMENT ON COLUMN events.base_price IS 'Base price for parking at this event';
COMMENT ON COLUMN events.extra_charge_per_hour IS 'Extra charge per hour after 4 hours of parking';

-- 2. Update customer table - make all fields required except photo (for Issue #9)

-- Add contact_number column if not exists
ALTER TABLE customer 
ADD COLUMN IF NOT EXISTS contact_number VARCHAR(15);

-- Add profile_photo_base64 column for storing Base64 encoded images
ALTER TABLE customer 
ADD COLUMN IF NOT EXISTS profile_photo_base64 TEXT;

-- Add first_name and last_name columns
ALTER TABLE customer 
ADD COLUMN IF NOT EXISTS first_name VARCHAR(100);

ALTER TABLE customer 
ADD COLUMN IF NOT EXISTS last_name VARCHAR(100);

-- Migrate existing full_name data to first_name (only if full_name column exists)
DO $$ 
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'customer' AND column_name = 'full_name'
    ) THEN
        UPDATE customer 
        SET first_name = SPLIT_PART(full_name, ' ', 1),
            last_name = CASE 
                WHEN POSITION(' ' IN full_name) > 0 
                THEN SUBSTRING(full_name FROM POSITION(' ' IN full_name) + 1)
                ELSE ''
            END
        WHERE full_name IS NOT NULL AND (first_name IS NULL OR last_name IS NULL);
    END IF;
END $$;

-- Add default values for existing records before making NOT NULL
UPDATE customer SET first_name = 'User' WHERE first_name IS NULL;
UPDATE customer SET last_name = '' WHERE last_name IS NULL;
UPDATE customer SET contact_number = '+91' WHERE contact_number IS NULL;

-- Make first_name and last_name NOT NULL (required)
ALTER TABLE customer 
ALTER COLUMN first_name SET NOT NULL;

ALTER TABLE customer 
ALTER COLUMN last_name SET NOT NULL;

-- Make contact_number NOT NULL (required)
ALTER TABLE customer 
ALTER COLUMN contact_number SET NOT NULL;

-- Drop full_name column (only if it exists)
DO $$ 
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'customer' AND column_name = 'full_name'
    ) THEN
        ALTER TABLE customer DROP COLUMN full_name;
    END IF;
END $$;

-- Add comments
COMMENT ON COLUMN customer.first_name IS 'Customer first name (required)';
COMMENT ON COLUMN customer.last_name IS 'Customer last name (required)';
COMMENT ON COLUMN customer.contact_number IS 'Contact number with +91 prefix (required, e.g., +919876543210)';
COMMENT ON COLUMN customer.profile_photo_base64 IS 'Base64 encoded profile photo (optional)';

-- 3. Add occupied_spots column to events table (for Issue #3 & #6)
ALTER TABLE events 
ADD COLUMN IF NOT EXISTS occupied_spots INTEGER DEFAULT 0;

COMMENT ON COLUMN events.occupied_spots IS 'Count of booked spots + pending valet requests for this event';

-- 4. Create events_history table (for Issue #6)
CREATE TABLE IF NOT EXISTS events_history (
    event_id VARCHAR(255) PRIMARY KEY,
    event_name VARCHAR(255) NOT NULL,
    event_address TEXT NOT NULL,
    event_date DATE NOT NULL,
    hours_of_event INTEGER NOT NULL,
    total_vehicle_spots INTEGER NOT NULL,
    base_price NUMERIC(10, 2) DEFAULT 100,
    extra_charge_per_hour NUMERIC(10, 2) DEFAULT 50,
    occupied_spots INTEGER DEFAULT 0,
    created_by VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    moved_to_history_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE events_history IS 'Archive of past events that have already occurred';

-- 5. Clean up old storage bucket and policies (if previously created)
-- Drop storage policies if they exist
DROP POLICY IF EXISTS "Users can upload their own profile photos" ON storage.objects;
DROP POLICY IF EXISTS "Users can view their own profile photos" ON storage.objects;
DROP POLICY IF EXISTS "Users can update their own profile photos" ON storage.objects;
DROP POLICY IF EXISTS "Users can delete their own profile photos" ON storage.objects;

-- Delete storage bucket if it exists (optional - only if you want to remove it)
-- DELETE FROM storage.buckets WHERE id = 'Profile_Photos';

-- Note: We're now using Base64 storage in customer.profile_photo_base64 column instead of Supabase Storage

-- 6. Make all staff fields NOT NULL (for Issue #12 & #13)
ALTER TABLE staff 
ALTER COLUMN full_name SET NOT NULL;

ALTER TABLE staff 
ALTER COLUMN email_id SET NOT NULL;

ALTER TABLE staff 
ALTER COLUMN password SET NOT NULL;

ALTER TABLE staff 
ALTER COLUMN contact_number SET NOT NULL;

ALTER TABLE staff 
ALTER COLUMN location SET NOT NULL;

ALTER TABLE staff 
ALTER COLUMN age SET NOT NULL;

ALTER TABLE staff 
ALTER COLUMN experience_level SET NOT NULL;

ALTER TABLE staff 
ALTER COLUMN assigned_event_id SET NOT NULL;

-- 7. Add constraint to ensure contact_number format in customer table
DO $$ 
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint 
        WHERE conname = 'customer_contact_number_format'
    ) THEN
        ALTER TABLE customer
        ADD CONSTRAINT customer_contact_number_format 
        CHECK (contact_number ~ '^\+91[0-9]{10}$');
    END IF;
END $$;

-- 8. Add function to auto-archive past events
CREATE OR REPLACE FUNCTION archive_past_events()
RETURNS void AS $$
BEGIN
    -- Move past events to history
    INSERT INTO events_history 
    SELECT 
        event_id,
        event_name,
        event_address,
        event_date,
        hours_of_event,
        total_vehicle_spots,
        base_price,
        extra_charge_per_hour,
        occupied_spots,
        created_by,
        created_at,
        CURRENT_TIMESTAMP as moved_to_history_at
    FROM events
    WHERE event_date < CURRENT_DATE
    ON CONFLICT (event_id) DO NOTHING;
    
    -- Delete from events table
    DELETE FROM events
    WHERE event_date < CURRENT_DATE;
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION archive_past_events() IS 'Moves past events to events_history table';

-- 9. Create trigger or scheduled job to run archive function daily
-- Option A: Manual - Run this query daily in SQL Editor:
-- SELECT archive_past_events();

-- Option B: Use pg_cron extension (if enabled in Supabase)
-- Run this to schedule daily at midnight:
-- SELECT cron.schedule('archive-past-events', '0 0 * * *', 'SELECT archive_past_events();');

-- 10. Update existing events to set default prices if NULL
UPDATE events 
SET base_price = 100 
WHERE base_price IS NULL;

UPDATE events 
SET extra_charge_per_hour = 50 
WHERE extra_charge_per_hour IS NULL;

-- 11. Initialize occupied_spots for existing events
-- Note: parking_requests links to events via booking_id -> booked_parking -> event_id
UPDATE events e
SET occupied_spots = (
    SELECT COUNT(*) 
    FROM booked_parking b 
    WHERE b.event_id = e.event_id AND b.status = 'active'
) + (
    SELECT COUNT(*) 
    FROM parking_requests pr 
    INNER JOIN booked_parking bp ON pr.booking_id = bp.booking_id
    WHERE bp.event_id = e.event_id AND pr.request_status IN ('pending', 'assigned')
);

-- 12. Create index for better query performance
CREATE INDEX IF NOT EXISTS idx_booked_parking_customer_email 
ON booked_parking(customer_email);

CREATE INDEX IF NOT EXISTS idx_booked_parking_event_id 
ON booked_parking(event_id);

CREATE INDEX IF NOT EXISTS idx_parking_requests_booking_id 
ON parking_requests(booking_id);

CREATE INDEX IF NOT EXISTS idx_staff_assigned_event_id 
ON staff(assigned_event_id);

-- 13. Add request_type constraint to ensure valid values
DO $$ 
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint 
        WHERE conname = 'parking_requests_type_check'
    ) THEN
        ALTER TABLE parking_requests
        ADD CONSTRAINT parking_requests_type_check 
        CHECK (request_type IN ('valet_service', 'car_fetch', 'assistance'));
    END IF;
END $$;

-- ============================================
-- Verification Queries
-- Run these to verify changes were applied
-- ============================================

-- Check events table columns
SELECT column_name, data_type, is_nullable, column_default 
FROM information_schema.columns 
WHERE table_name = 'events' 
ORDER BY ordinal_position;

-- Check customer table columns
SELECT column_name, data_type, is_nullable, column_default 
FROM information_schema.columns 
WHERE table_name = 'customer' 
ORDER BY ordinal_position;

-- Check staff table columns
SELECT column_name, data_type, is_nullable, column_default 
FROM information_schema.columns 
WHERE table_name = 'staff' 
ORDER BY ordinal_position;

-- Check if events_history table exists
SELECT table_name, table_type 
FROM information_schema.tables 
WHERE table_name = 'events_history';

-- ============================================
-- Cleanup Queries (Optional)
-- Run these only if you want to remove old data
-- ============================================

-- Remove Profile_Photos bucket (if it was created earlier)
-- Uncomment the line below to delete the bucket:
-- DELETE FROM storage.buckets WHERE id = 'Profile_Photos';

-- Check if bucket exists
SELECT * FROM storage.buckets WHERE id = 'Profile_Photos';
