-- ============================================
-- FIX CUSTOMER TABLE RLS POLICIES
-- Run these in Supabase SQL Editor
-- ============================================

-- 1. First, check if RLS is enabled on customer table
SELECT tablename, rowsecurity 
FROM pg_tables 
WHERE tablename = 'customer';

-- 2. Check existing policies
SELECT schemaname, tablename, policyname, permissive, roles, cmd, qual, with_check
FROM pg_policies
WHERE tablename = 'customer';

-- 3. Drop any existing conflicting policies (if needed)
DROP POLICY IF EXISTS "Allow public select on customer" ON customer;
DROP POLICY IF EXISTS "Allow public insert on customer" ON customer;
DROP POLICY IF EXISTS "Allow public update on customer" ON customer;
DROP POLICY IF EXISTS "Allow public delete on customer" ON customer;
DROP POLICY IF EXISTS "Customers can view own data" ON customer;
DROP POLICY IF EXISTS "Customers can update own data" ON customer;
DROP POLICY IF EXISTS "Allow signup inserts" ON customer;

-- 4. Enable RLS on customer table (if not already enabled)
ALTER TABLE customer ENABLE ROW LEVEL SECURITY;

-- 5. Create new policies

-- Policy 1: Allow anyone to INSERT (for signup)
CREATE POLICY "customer_insert_policy"
ON customer FOR INSERT
WITH CHECK (true);

-- Policy 2: Allow anyone to SELECT (for login and profile viewing)
CREATE POLICY "customer_select_policy"
ON customer FOR SELECT
USING (true);

-- Policy 3: Allow anyone to UPDATE (for profile updates including photo)
CREATE POLICY "customer_update_policy"
ON customer FOR UPDATE
USING (true)
WITH CHECK (true);

-- Policy 4: Optional - Allow DELETE (only if you want customers to delete their accounts)
CREATE POLICY "customer_delete_policy"
ON customer FOR DELETE
USING (true);

-- 6. Verify policies were created
SELECT policyname, cmd, permissive
FROM pg_policies
WHERE tablename = 'customer';

-- ============================================
-- ALTERNATIVE: If you want more restrictive policies
-- (Only uncomment these if you want auth-based restrictions)
-- ============================================

-- First drop the permissive policies above:
-- DROP POLICY IF EXISTS "customer_insert_policy" ON customer;
-- DROP POLICY IF EXISTS "customer_select_policy" ON customer;
-- DROP POLICY IF EXISTS "customer_update_policy" ON customer;
-- DROP POLICY IF EXISTS "customer_delete_policy" ON customer;

-- Then create auth-based policies:

-- Allow authenticated users to view their own data
-- CREATE POLICY "customer_select_own"
-- ON customer FOR SELECT
-- USING (auth.email() = email_id);

-- Allow authenticated users to update their own data
-- CREATE POLICY "customer_update_own"
-- ON customer FOR UPDATE
-- USING (auth.email() = email_id)
-- WITH CHECK (auth.email() = email_id);

-- Allow anyone to insert (for signup)
-- CREATE POLICY "customer_insert_public"
-- ON customer FOR INSERT
-- WITH CHECK (true);

-- ============================================
-- TEST THE POLICIES
-- ============================================

-- Try to select (should work)
SELECT email_id, first_name, last_name FROM customer LIMIT 1;

-- Try to update a record (replace with your actual email)
UPDATE customer 
SET profile_photo_base64 = 'test-update'
WHERE email_id = 'your-email@example.com';

-- Check if update worked
SELECT email_id, 
       CASE WHEN profile_photo_base64 = 'test-update' THEN 'UPDATE WORKS' ELSE 'UPDATE FAILED' END as test_result
FROM customer 
WHERE email_id = 'your-email@example.com';

-- Clean up test
UPDATE customer 
SET profile_photo_base64 = NULL
WHERE email_id = 'your-email@example.com';
