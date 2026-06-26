-- Mark all bookings as inactive (for testing/cleanup)
-- Run this in Supabase SQL Editor

-- Option 1: Mark ALL bookings as inactive
UPDATE booked_parking SET status = 'inactive';

-- Option 2: Mark only bookings where payment is completed (more selective)
-- UPDATE booked_parking SET status = 'inactive' WHERE payment_status = 'completed';

-- Verify the update
SELECT 
    booking_id,
    customer_email,
    vehicle_number,
    status,
    payment_status,
    booking_date
FROM booked_parking
ORDER BY booking_date DESC
LIMIT 10;

-- Check how many active bookings remain
SELECT 
    status,
    COUNT(*) as count
FROM booked_parking
GROUP BY status;
