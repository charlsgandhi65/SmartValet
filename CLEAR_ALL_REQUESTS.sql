-- Clear all parking requests from database
-- Run this in Supabase SQL Editor to start fresh

-- Option 1: Delete ALL parking requests (complete clean slate)
DELETE FROM parking_requests;

-- Option 2: Delete only pending and assigned requests (keep completed for history)
-- DELETE FROM parking_requests WHERE request_status IN ('pending', 'assigned');

-- Verify deletion
SELECT COUNT(*) as remaining_requests FROM parking_requests;

-- Optional: Reset bookings to allow new car fetch requests
-- UPDATE booked_parking SET status = 'active' WHERE status = 'inactive';
