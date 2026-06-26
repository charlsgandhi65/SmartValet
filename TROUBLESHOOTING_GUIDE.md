# Troubleshooting Guide - Profile Issues

## Issues Reported
1. ❌ Profile shows OTHER customer's data (wrong name, email, phone)
2. ❌ Greeting shows wrong customer name
3. ❌ Photo upload shows "Processing image..." then "Failed to upload photo"

## Root Causes & Solutions

### Issue 1 & 2: Wrong Customer Data

**Possible Causes:**
1. Database migration not run yet (first_name/last_name columns don't exist)
2. Multiple customer records with similar emails
3. RLS (Row Level Security) policies not properly configured

**Steps to Fix:**

#### Step 1: Check if columns exist
Run this in Supabase SQL Editor:
```sql
SELECT column_name, data_type 
FROM information_schema.columns 
WHERE table_name = 'customer' 
ORDER BY ordinal_position;
```

**Expected Result:** You should see `first_name` and `last_name` columns.

**If NOT present:** Run Section 2 of `DATABASE_UPDATES.sql` in Supabase.

#### Step 2: Check your customer data
```sql
-- Replace with YOUR actual email
SELECT * FROM customer WHERE email_id = 'your-email@example.com';
```

**What to check:**
- Is there only ONE record for your email?
- Does it have the correct first_name and last_name?
- Is the email_id exactly matching (case-sensitive)?

#### Step 3: Check for duplicate records
```sql
SELECT email_id, COUNT(*) as count
FROM customer
GROUP BY email_id
HAVING COUNT(*) > 1;
```

**If duplicates exist:** Delete the wrong ones:
```sql
-- First, identify which ID to keep
SELECT id, email_id, first_name, last_name, created_at 
FROM customer 
WHERE email_id = 'your-email@example.com'
ORDER BY created_at DESC;

-- Delete the old/wrong records (keep the most recent one)
DELETE FROM customer 
WHERE id = 'id-of-record-to-delete';
```

### Issue 3: Photo Upload Failing

**Possible Causes:**
1. RLS policies blocking UPDATE operations
2. Access token not being passed correctly
3. Image compression failing
4. Network/API errors

**Steps to Fix:**

#### Step 1: Check Logcat for errors
In Android Studio:
1. Open Logcat (View → Tool Windows → Logcat)
2. Filter by "ProfileActivity"
3. Look for these log messages:
   - "Starting image processing for: [your-email]"
   - "Image compressed, size: [number] chars"
   - "Updating database for email: [your-email]"
   - "Update result length: [number]"

**Expected Output:**
```
ProfileActivity: Starting image processing for: john@example.com
ProfileActivity: Image compressed, size: 50000 chars
ProfileActivity: Updating database for email: john@example.com
ProfileActivity: Update result length: 1
ProfileActivity: Photo uploaded successfully
```

**If you see "Update result length: null" or "0":**
→ RLS policy issue (see Step 2 below)

#### Step 2: Check/Fix RLS Policies
Run this in Supabase:
```sql
-- Check existing policies
SELECT policyname, permissive, roles, cmd, qual, with_check
FROM pg_policies
WHERE tablename = 'customer';
```

**Required policies for customer table:**

```sql
-- Allow customers to read their own data
CREATE POLICY "Customers can view own data"
ON customer FOR SELECT
USING (auth.email() = email_id);

-- Allow customers to update their own data
CREATE POLICY "Customers can update own data"
ON customer FOR UPDATE
USING (auth.email() = email_id)
WITH CHECK (auth.email() = email_id);

-- Allow INSERT for signup (using service role or anon key with specific conditions)
CREATE POLICY "Allow signup inserts"
ON customer FOR INSERT
WITH CHECK (true);
```

**If policies don't exist, create them. If they exist but wrong, drop and recreate:**
```sql
-- Drop old policies
DROP POLICY IF EXISTS "old-policy-name" ON customer;

-- Then create the new ones above
```

#### Step 3: Test with a simple update
Try updating your record directly in Supabase:
```sql
UPDATE customer 
SET profile_photo_base64 = 'test-string'
WHERE email_id = 'your-email@example.com';
```

**If this works:** RLS is fine, issue is in the app code.
**If this fails:** RLS policy problem.

## Complete Diagnostic Steps

### 1. Run Database Migration
Open `DATABASE_UPDATES.sql` and run Section 2 in Supabase SQL Editor.

### 2. Verify Schema
```sql
SELECT column_name FROM information_schema.columns 
WHERE table_name = 'customer' AND column_name IN ('first_name', 'last_name', 'profile_photo_base64');
```
Should return 3 rows.

### 3. Check Your Data
```sql
SELECT email_id, first_name, last_name, contact_number 
FROM customer 
WHERE email_id = 'YOUR-EMAIL-HERE';
```

### 4. Rebuild and Test App
1. In Android Studio: Build → Clean Project
2. Build → Rebuild Project
3. Run app
4. Check Logcat while testing

### 5. Test Login Flow
1. Login with your credentials
2. Watch Logcat for "CustomerActivity: Loading data for email: [your-email]"
3. Check if it shows "Query result length: 1"
4. Check if it shows YOUR correct data

### 6. Test Profile Flow
1. Click profile icon
2. Watch Logcat for "ProfileActivity: Loading data for email: [your-email]"
3. Check if name, email, phone shown are YOURS
4. Try uploading photo
5. Watch for "Image compressed" and "Update result" logs

## Quick Fixes

### Fix 1: Delete all customers and re-signup
```sql
-- CAREFUL: This deletes ALL customer data!
DELETE FROM customer;
```
Then signup again in the app with YOUR information.

### Fix 2: Update your record manually
```sql
UPDATE customer 
SET first_name = 'YourFirstName',
    last_name = 'YourLastName',
    contact_number = '+919876543210'
WHERE email_id = 'your-actual-email@example.com';
```

### Fix 3: Enable all RLS policies temporarily for testing
```sql
-- Temporarily allow all operations (NOT for production!)
CREATE POLICY "temp_allow_all_select" ON customer FOR SELECT USING (true);
CREATE POLICY "temp_allow_all_update" ON customer FOR UPDATE USING (true);
CREATE POLICY "temp_allow_all_insert" ON customer FOR INSERT WITH CHECK (true);
```

## What to Share for Further Help

If issues persist, share:
1. **Logcat output** (filter for "ProfileActivity" and "CustomerActivity")
2. **Customer table structure**: Run query #1 from DIAGNOSTIC_QUERIES.sql
3. **Your customer record**: Run query #2 (hide sensitive data)
4. **RLS policies**: Run query #4 from DIAGNOSTIC_QUERIES.sql
5. **Error messages** from Supabase if any

## Expected Behavior After Fix

✅ Login shows: "Good Morning, [YOUR FIRST NAME]"
✅ Profile shows: YOUR full name, YOUR email, YOUR phone
✅ Photo upload shows: "Processing..." → "Profile photo updated!"
✅ Photo displays immediately in profile and on dashboard
