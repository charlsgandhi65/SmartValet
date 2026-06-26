# Greeting Shows Same Name For All Customers - Diagnostic Guide

## Issue
All customers see the same name in the greeting, instead of their own name.

## Possible Root Causes

### 1. Database Migration Not Run
The `first_name` and `last_name` columns don't exist in the customer table yet.

**Check:** Run this in Supabase SQL Editor:
```sql
SELECT column_name, data_type 
FROM information_schema.columns 
WHERE table_name = 'customer' 
AND column_name IN ('first_name', 'last_name', 'full_name')
ORDER BY column_name;
```

**Expected Result:** Should show `first_name` and `last_name` columns.

**If columns are missing:**
1. Open `DATABASE_UPDATES.sql`
2. Copy Section 2 (lines 17-75)
3. Run it in Supabase SQL Editor

### 2. Data Not Migrated or Empty
Columns exist but customers don't have first_name/last_name values.

**Check:** Run this in Supabase SQL Editor:
```sql
SELECT email_id, first_name, last_name 
FROM customer 
ORDER BY email_id;
```

**Expected Result:** Should show actual names, not empty or NULL values.

**If data is empty:**
```sql
-- Update each customer manually
UPDATE customer 
SET first_name = 'John', 
    last_name = 'Doe'
WHERE email_id = 'john@example.com';

UPDATE customer 
SET first_name = 'Jane', 
    last_name = 'Smith'
WHERE email_id = 'jane@example.com';

-- Or set default values
UPDATE customer 
SET first_name = SPLIT_PART(email_id, '@', 1),
    last_name = 'User'
WHERE first_name IS NULL OR first_name = '';
```

### 3. Wrong Customer Email Being Passed
CustomerActivity is receiving the wrong email, so it fetches wrong customer data.

**Check Logcat:** Look for these lines after login:
```
CustomerActivity: LOADING GREETING
CustomerActivity: Customer Email: [should be YOUR email]
CustomerActivity: Query result length: 1
CustomerActivity: Full customer data: {...}
CustomerActivity: firstName: '[YOUR first name]'
CustomerActivity: Final display name: '[YOUR first name]'
```

**If email is wrong:** Check LoginActivity - verify it's passing correct customer_email.

### 4. Query Returning Wrong Data
The query `email_id=eq."email"` is returning wrong customer.

**Check Logcat:** Look for:
```
CustomerActivity: Full customer data: {"email_id":"[email]","first_name":"[name]",...}
```

Verify the email_id in the response matches the logged-in customer.

## Step-by-Step Diagnostic Process

### Step 1: Verify Database Schema

Run in Supabase:
```sql
-- Check if columns exist
SELECT column_name FROM information_schema.columns 
WHERE table_name = 'customer' 
AND column_name IN ('first_name', 'last_name');

-- Should return 2 rows: first_name and last_name
```

**If no results:** Run the database migration (DATABASE_UPDATES.sql Section 2).

### Step 2: Check Customer Data

Run in Supabase:
```sql
-- Check all customers
SELECT email_id, first_name, last_name 
FROM customer;
```

**Look for:**
- Are first_name and last_name populated?
- Do you see YOUR customer accounts?
- Are the names different for each customer?

### Step 3: Test with Logcat

1. Sync & Rebuild project
2. Open Logcat (View → Tool Windows → Logcat)
3. Login as Customer A
4. Look for "CustomerActivity: LOADING GREETING"
5. Check what name is extracted
6. Logout
7. Login as Customer B
8. Check if different name appears

### Step 4: Compare Logcat Outputs

**Customer A Login - Expected:**
```
CustomerActivity: Customer Email: customerA@example.com
CustomerActivity: firstName: 'Alice'
CustomerActivity: Final display name: 'Alice'
CustomerActivity: Setting greeting: Good Morning, Alice
```

**Customer B Login - Expected:**
```
CustomerActivity: Customer Email: customerB@example.com
CustomerActivity: firstName: 'Bob'
CustomerActivity: Final display name: 'Bob'
CustomerActivity: Setting greeting: Good Morning, Bob
```

**If BOTH show same name:**
→ Database issue: all customers have same name in database

**If BOTH show same email:**
→ Login issue: not passing correct email to CustomerActivity

## Quick Fixes

### Fix 1: Manually Update Customer Names

```sql
-- Update your test customers
UPDATE customer SET first_name = 'Alice', last_name = 'Smith' WHERE email_id = 'alice@example.com';
UPDATE customer SET first_name = 'Bob', last_name = 'Jones' WHERE email_id = 'bob@example.com';
UPDATE customer SET first_name = 'Charlie', last_name = 'Brown' WHERE email_id = 'charlie@example.com';
```

### Fix 2: Auto-Generate Names from Email

```sql
UPDATE customer 
SET first_name = INITCAP(SPLIT_PART(email_id, '@', 1)),
    last_name = 'User'
WHERE first_name IS NULL OR first_name = '' OR first_name = 'User';
```

Example: `john.doe@example.com` → First Name: "John.doe", Last Name: "User"

### Fix 3: Re-run Signup

1. Delete test customers:
   ```sql
   DELETE FROM customer WHERE email_id IN ('alice@test.com', 'bob@test.com');
   ```

2. In the app, sign up again with first name and last name

3. Login and check greeting

## Testing Checklist

After applying fixes:

1. ✅ Database has first_name and last_name columns
2. ✅ Each customer has DIFFERENT first_name values
3. ✅ Query in Logcat shows correct email for logged-in customer
4. ✅ firstName extracted in Logcat matches customer's actual first name
5. ✅ Greeting displays different names for different customers

## Common Mistakes

❌ **All customers have first_name = 'User'**
→ Database migration set default value, need to update with real names

❌ **Logcat shows empty firstName: ''**
→ Customer record doesn't have first_name populated

❌ **Logcat shows same email for all logins**
→ LoginActivity not passing correct email to CustomerActivity

❌ **Greeting shows email prefix instead of name**
→ fallback logic kicking in because first_name is empty

## What to Share if Still Not Working

1. **Supabase query result:**
   ```sql
   SELECT email_id, first_name, last_name FROM customer;
   ```

2. **Logcat output** for Customer A login (filter "CustomerActivity")

3. **Logcat output** for Customer B login (filter "CustomerActivity")

4. **Screenshot** of greeting shown for each customer

This will help identify if it's a database issue or a code issue.
