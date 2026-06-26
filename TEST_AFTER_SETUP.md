# Testing Your SmartValet App After Supabase Setup

## ✅ Quick Verification (Optional but Recommended)

Run this in Supabase SQL Editor to verify everything was created:

```sql
-- Check all tables exist
SELECT table_name 
FROM information_schema.tables 
WHERE table_schema = 'public' 
AND table_name IN ('customer', 'administration', 'events', 'staff', 'booked_parking', 'qr_logs', 'parking_requests')
ORDER BY table_name;
```

**Expected Result**: Should show 7 rows (one for each table)

---

## 🔑 Step 1: Verify Your API Keys

Before running the app, make sure your API keys are correct:

1. Go to **Supabase Dashboard** → **Settings** → **API**
2. Copy your **Project URL** and **anon public key**
3. Open `app/src/main/java/com/example/smartvalet/utils/SupabaseClientInstance.java`
4. Verify these lines match:

```java
private static final String SUPABASE_URL = "https://your-project.supabase.co";
private static final String SUPABASE_KEY = "your-anon-key-here";
```

**⚠️ Important**: Use the **anon public** key, NOT the service_role key!

---

## 🧪 Step 2: Create Test Data (Optional)

Create test accounts to test login. Run this in Supabase SQL Editor:

```sql
-- Create test admin
INSERT INTO public.administration (email_id, password, full_name)
VALUES ('admin@test.com', 'admin123', 'Test Admin')
ON CONFLICT (email_id) DO NOTHING;

-- Create test customer
INSERT INTO public.customer (email_id, password, full_name, contact_number)
VALUES ('customer@test.com', 'customer123', 'Test Customer', '1234567890')
ON CONFLICT (email_id) DO NOTHING;

--
SELECT * FROM public.customer; Verify inserts
SELECT * FROM public.administration;
```

---

## 🚀 Step 3: Run Your Android App

### Build and Run:
1. Open your project in Android Studio
2. Build the project (Build → Make Project)
3. Run on emulator or physical device (Run → Run 'app')

---

## ✅ Step 4: Testing Checklist

### Test 1: Customer Signup
- [ ] Open app → Welcome screen
- [ ] Click "Get Started"
- [ ] Select "Customer" role
- [ ] Click "Sign Up" link
- [ ] Enter email and password
- [ ] Click "Create Account"
- [ ] **Expected**: Success message and redirect to login

### Test 2: Customer Login
- [ ] Select "Customer" role
- [ ] Enter email: `customer@test.com` (or your test email)
- [ ] Enter password: `customer123`
- [ ] Click "Sign In"
- [ ] **Expected**: Navigate to CustomerActivity dashboard

### Test 3: Admin Login (if you created test admin)
- [ ] Select "Admin" role
- [ ] Enter email: `admin@test.com`
- [ ] Enter password: `admin123`
- [ ] Click "Sign In"
- [ ] **Expected**: Navigate to AdminActivity dashboard

### Test 4: Create Event (as Admin)
- [ ] Login as admin
- [ ] Click "Manage Events"
- [ ] Click "Create Event"
- [ ] Fill in event details:
  - Event Name: "Test Event"
  - Address: "123 Test Street"
  - Date: "2024-12-25" (YYYY-MM-DD format)
  - Hours: "8"
  - Spots: "500"
- [ ] Click "Create Event"
- [ ] **Expected**: Event created, redirect to events list

### Test 5: View Events (as Customer)
- [ ] Login as customer
- [ ] Click "View Events"
- [ ] **Expected**: See list of events (if any created)

### Test 6: Book Parking (as Customer)
- [ ] From View Events, click "Book Parking" on an event
- [ ] Fill in vehicle details:
  - Vehicle Number: "ABC123"
  - Vehicle Model: "Toyota Camry"
  - Vehicle Color: "Red"
  - Parking Time: "10:00" (HH:mm format)
- [ ] Click "Book Parking"
- [ ] **Expected**: Navigate to Payment screen

---

## 🐛 Common Issues & Solutions

### Issue 1: "Login failed" or "Not authorized"
**Solution**: 
- Check that the user exists in the database
- Verify email matches exactly (case-sensitive)
- Make sure you're checking the correct table (customer/staff/administration)

### Issue 2: "Permission denied" or RLS error
**Solution**: 
- Verify RLS policies were created
- Check you're using the anon public key (not service_role)
- Re-run the RLS policies section from setup script

### Issue 3: "Table does not exist"
**Solution**: 
- Verify tables were created in Supabase Table Editor
- Check table names match exactly (case-sensitive)

### Issue 4: "Cannot connect to Supabase"
**Solution**:
- Check internet connection
- Verify SUPABASE_URL in `SupabaseClientInstance.java`
- Check Supabase project is active (not paused)
- Verify API key is correct

### Issue 5: "Foreign key violation" when booking
**Solution**:
- Make sure customer email exists in `customer` table
- Make sure event_id exists in `events` table
- Check foreign key references match exactly

### Issue 6: Signup works but can't login
**Solution**:
- Check if customer was inserted into `customer` table
- Verify email format matches
- Check Supabase logs for errors

---

## 📊 Check Supabase Dashboard

After testing, verify data in Supabase:

1. Go to **Supabase Dashboard** → **Table Editor**
2. Check these tables have data:
   - ✅ `customer` - Should have your test customer
   - ✅ `administration` - Should have test admin (if created)
   - ✅ `events` - Should have test events (if created as admin)
   - ✅ `booked_parking` - Should have bookings (if booked as customer)

---

## ✅ Success Indicators

Your setup is working correctly if:
- ✅ You can signup a new customer
- ✅ You can login as customer/admin
- ✅ You can create events (as admin)
- ✅ You can view events (as customer)
- ✅ You can book parking (as customer)
- ✅ Data appears in Supabase Table Editor

---

## 🎯 Next Steps

Once basic testing works:
1. Test staff login and functionality
2. Test QR code generation
3. Test payment flow
4. Test valet request flow
5. Test staff QR scanning

---

## 🆘 Need Help?

If something doesn't work:
1. Check Supabase Dashboard → Logs for errors
2. Check Android Studio Logcat for app errors
3. Verify API keys are correct
4. Test SQL queries directly in Supabase SQL Editor
5. Check that all tables and policies exist

---

**You're ready to test! Good luck! 🚀**

