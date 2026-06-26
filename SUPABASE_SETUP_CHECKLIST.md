# ✅ Supabase Setup Checklist for SmartValet

Follow these steps in order to set up your Supabase database.

## 📝 Prerequisites

1. ✅ You have a Supabase account
2. ✅ You have created a Supabase project
3. ✅ You know your Supabase project URL and API keys

---

## 🚀 Step-by-Step Setup

### Step 1: Open Supabase SQL Editor
1. Go to your Supabase Dashboard
2. Navigate to **SQL Editor** (left sidebar)
3. Click **"New Query"**

### Step 2: Run the Complete Setup Script
1. Open the file `supabase_complete_setup.sql` from this project
2. Copy the **entire contents** of the file
3. Paste into Supabase SQL Editor
4. Click **"Run"** or press `Ctrl+Enter` (Windows) / `Cmd+Enter` (Mac)

✅ **Expected Result**: You should see "Success. No rows returned" or similar success message

### Step 3: Verify Setup
Run this query in SQL Editor:

```sql
-- Check all tables were created
SELECT table_name 
FROM information_schema.tables 
WHERE table_schema = 'public' 
AND table_name IN ('customer', 'administration', 'events', 'staff', 'booked_parking', 'qr_logs', 'parking_requests')
ORDER BY table_name;
```

✅ **Expected Result**: Should return 7 tables

---

## 🔑 Step 4: Verify Your API Keys

### In Supabase Dashboard:
1. Go to **Settings** → **API**
2. Note down:
   - **Project URL**: `https://your-project.supabase.co`
   - **anon public key**: `eyJhbGci...` (long token)

### In Your Android Project:
Open `app/src/main/java/com/example/smartvalet/utils/SupabaseClientInstance.java`

Verify these lines match your Supabase project:

```java
private static final String SUPABASE_URL = "https://your-project.supabase.co";
private static final String SUPABASE_KEY = "your-anon-key-here";
```

**⚠️ Important**: Use the **anon public** key, NOT the service_role key!

---

## 🧪 Step 5: Test Your Setup

### Option A: Test in Supabase SQL Editor

```sql
-- Insert a test admin
INSERT INTO public.administration (email_id, password, full_name)
VALUES ('admin@test.com', 'admin123', 'Test Admin')
ON CONFLICT (email_id) DO NOTHING;

-- Insert a test customer
INSERT INTO public.customer (email_id, password, full_name, contact_number)
VALUES ('customer@test.com', 'customer123', 'Test Customer', '1234567890')
ON CONFLICT (email_id) DO NOTHING;

-- Verify inserts
SELECT * FROM public.administration;
SELECT * FROM public.customer;
```

### Option B: Test with Your Android App
1. Build and run your Android app
2. Try signing up as a customer
3. Try logging in with the test credentials above
4. Check if data appears in Supabase Dashboard → Table Editor

---

## 🔍 Step 6: Verify Tables in Supabase Dashboard

1. Go to **Table Editor** in Supabase Dashboard
2. You should see these tables:
   - ✅ `customer`
   - ✅ `administration`
   - ✅ `events`
   - ✅ `staff`
   - ✅ `booked_parking`
   - ✅ `qr_logs`
   - ✅ `parking_requests`

---

## 📋 What Was Created

After running the setup script, you'll have:

### ✅ Tables (7)
- `customer` - Customer accounts
- `administration` - Admin accounts  
- `events` - Event information
- `staff` - Staff members
- `booked_parking` - Parking bookings
- `qr_logs` - QR code scan logs
- `parking_requests` - Valet service requests

### ✅ Indexes (18)
- Optimized for fast queries on email, event_id, booking_id, etc.

### ✅ Row Level Security (RLS)
- Enabled on all tables
- Policies allow public read/write (development mode)

### ✅ Triggers (5)
- Auto-update `updated_at` timestamp on table updates

---

## ⚠️ Common Issues & Solutions

### Issue 1: "relation does not exist"
**Solution**: Make sure you ran the complete setup script. Check that all tables were created.

### Issue 2: "permission denied"
**Solution**: 
- Verify RLS policies were created
- Check that you're using the correct API key (anon public key)
- Run the RLS policies section again

### Issue 3: "foreign key violation"
**Solution**: 
- Make sure parent records exist (e.g., customer before booking)
- Check that foreign keys match existing data

### Issue 4: "duplicate key value violates unique constraint"
**Solution**: 
- Email IDs must be unique
- Use `ON CONFLICT DO NOTHING` when inserting test data

### Issue 5: "Cannot connect to Supabase"
**Solution**:
- Check your internet connection
- Verify SUPABASE_URL in `SupabaseClientInstance.java`
- Check Supabase project is active (not paused)

---

## 📚 Next Steps

After setup is complete:

1. ✅ **Test signup** - Create a customer account via app
2. ✅ **Test login** - Login as customer, admin, or staff
3. ✅ **Create an event** - As admin, create a test event
4. ✅ **Book parking** - As customer, book a parking spot
5. ✅ **Test staff flow** - Create staff and assign to event

---

## 📖 Additional Resources

- **Complete Setup Guide**: See `SUPABASE_SETUP_GUIDE.md`
- **SQL Script**: See `supabase_complete_setup.sql`
- **Quick Reference**: See `SUPABASE_QUICK_REFERENCE.md`
- **Supabase Docs**: https://supabase.com/docs

---

## 🆘 Need Help?

If you encounter issues:

1. ✅ Check Supabase Dashboard → Logs for errors
2. ✅ Verify your API keys are correct
3. ✅ Test queries directly in SQL Editor first
4. ✅ Check that RLS policies match your use case
5. ✅ Verify foreign key relationships

---

## ✅ Setup Complete Checklist

- [ ] Run `supabase_complete_setup.sql` in SQL Editor
- [ ] Verify 7 tables exist in Table Editor
- [ ] Check API keys in `SupabaseClientInstance.java`
- [ ] Insert test admin and customer data
- [ ] Test Android app signup
- [ ] Test Android app login
- [ ] Create a test event as admin
- [ ] Book a parking spot as customer

**Once all checked, your Supabase setup is complete! 🎉**

