-- ============================================
-- DIAGNOSTIC QUERIES
-- Run these in Supabase to check current state
-- ============================================

-- 1. Check if first_name and last_name columns exist
SELECT column_name, data_type, is_nullable 
FROM information_schema.columns 
WHERE table_name = 'customer' 
ORDER BY ordinal_position;

-- 2. Check all customers and their data
SELECT email_id, first_name, last_name, contact_number, 
       CASE WHEN profile_photo_base64 IS NOT NULL THEN 'Has Photo' ELSE 'No Photo' END as photo_status
FROM customer 
ORDER BY email_id;

-- 3. Check if there are duplicate emails (case variations)
SELECT email_id, COUNT(*) as count
FROM customer
GROUP BY email_id
HAVING COUNT(*) > 1;

-- 4. Check RLS policies on customer table
SELECT schemaname, tablename, policyname, permissive, roles, cmd, qual, with_check
FROM pg_policies
WHERE tablename = 'customer';

-- 5. If first_name and last_name don't exist, run Section 2 of DATABASE_UPDATES.sql

-- ============================================
-- QUICK FIX: If columns exist but data is wrong
-- ============================================

-- Check what data you have for YOUR email (replace with your actual email)
SELECT * FROM customer WHERE email_id = 'your-email@example.com';

-- If you see wrong data, you might need to:
-- 1. Delete duplicate/wrong records
-- 2. Update your record with correct data

-- Example: Update your record manually
-- UPDATE customer 
-- SET first_name = 'YourFirstName', 
--     last_name = 'YourLastName',
--     contact_number = '+919876543210'
-- WHERE email_id = 'your-email@example.com';
