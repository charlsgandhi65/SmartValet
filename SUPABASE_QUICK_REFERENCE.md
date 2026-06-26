# Supabase Quick Reference for SmartValet

## 📚 Quick SQL Queries

### View All Tables
```sql
SELECT table_name 
FROM information_schema.tables 
WHERE table_schema = 'public' 
ORDER BY table_name;
```

### Check RLS Status
```sql
SELECT tablename, rowsecurity 
FROM pg_tables 
WHERE schemaname = 'public' 
ORDER BY tablename;
```

### View All Policies
```sql
SELECT tablename, policyname, cmd, permissive 
FROM pg_policies 
WHERE schemaname = 'public'
ORDER BY tablename, policyname;
```

### View All Indexes
```sql
SELECT tablename, indexname 
FROM pg_indexes 
WHERE schemaname = 'public' 
ORDER BY tablename, indexname;
```

---

## 🔑 Common Operations

### Insert a Test Admin
```sql
INSERT INTO public.administration (email_id, password, full_name)
VALUES ('admin@smartvalet.com', 'admin123', 'Admin User')
ON CONFLICT (email_id) DO NOTHING;
```

### Insert a Test Customer
```sql
INSERT INTO public.customer (email_id, password, full_name, contact_number)
VALUES ('customer@example.com', 'customer123', 'Test Customer', '1234567890')
ON CONFLICT (email_id) DO NOTHING;
```

### Insert a Test Event
```sql
INSERT INTO public.events (event_name, event_address, event_date, hours_of_event, total_vehicle_spots, created_by)
VALUES ('Test Event', '123 Main St, City', '2024-12-25', 8, 500, 'admin@smartvalet.com');
```

### View All Customers
```sql
SELECT * FROM public.customer ORDER BY created_at DESC;
```

### View All Events
```sql
SELECT * FROM public.events ORDER BY event_date DESC;
```

### View All Bookings
```sql
SELECT 
    bp.booking_id,
    bp.customer_email,
    bp.parking_spot_number,
    bp.vehicle_number,
    bp.payment_status,
    bp.status,
    e.event_name
FROM public.booked_parking bp
LEFT JOIN public.events e ON bp.event_id = e.event_id
ORDER BY bp.created_at DESC;
```

### View All Staff
```sql
SELECT 
    s.*,
    e.event_name as assigned_event
FROM public.staff s
LEFT JOIN public.events e ON s.assigned_event_id = e.event_id
ORDER BY s.created_at DESC;
```

### View Parking Requests
```sql
SELECT 
    pr.*,
    bp.vehicle_number,
    s.full_name as assigned_staff_name
FROM public.parking_requests pr
LEFT JOIN public.booked_parking bp ON pr.booking_id = bp.booking_id
LEFT JOIN public.staff s ON pr.assigned_staff_id = s.staff_id
ORDER BY pr.created_at DESC;
```

### View QR Logs
```sql
SELECT 
    ql.*,
    bp.vehicle_number,
    s.full_name as staff_name
FROM public.qr_logs ql
LEFT JOIN public.booked_parking bp ON ql.booking_id = bp.booking_id
LEFT JOIN public.staff s ON ql.scanned_by_staff_id = s.staff_id
ORDER BY ql.scan_timestamp DESC;
```

---

## 🛠️ Maintenance Queries

### Update Customer Password
```sql
UPDATE public.customer 
SET password = 'newpassword123'
WHERE email_id = 'customer@example.com';
```

### Assign Staff to Event
```sql
UPDATE public.staff 
SET assigned_event_id = 'event-uuid-here'
WHERE email_id = 'staff@smartvalet.com';
```

### Mark Booking as Paid
```sql
UPDATE public.booked_parking 
SET payment_status = 'paid',
    payment_amount = 50.00,
    payment_method = 'card'
WHERE booking_id = 'booking-uuid-here';
```

### Complete a Parking Request
```sql
UPDATE public.parking_requests 
SET request_status = 'completed',
    completed_at = NOW()
WHERE request_id = 'request-uuid-here';
```

### Cancel a Booking
```sql
UPDATE public.booked_parking 
SET status = 'cancelled'
WHERE booking_id = 'booking-uuid-here';
```

---

## 🔍 Search Queries

### Find Bookings by Customer Email
```sql
SELECT * FROM public.booked_parking 
WHERE customer_email = 'customer@example.com'
ORDER BY created_at DESC;
```

### Find Bookings by Event
```sql
SELECT * FROM public.booked_parking 
WHERE event_id = 'event-uuid-here'
ORDER BY parking_spot_number;
```

### Find Staff by Event
```sql
SELECT * FROM public.staff 
WHERE assigned_event_id = 'event-uuid-here';
```

### Find Events by Date Range
```sql
SELECT * FROM public.events 
WHERE event_date BETWEEN '2024-01-01' AND '2024-12-31'
ORDER BY event_date;
```

### Find Active Bookings
```sql
SELECT * FROM public.booked_parking 
WHERE status = 'active'
ORDER BY created_at DESC;
```

### Find Pending Parking Requests
```sql
SELECT * FROM public.parking_requests 
WHERE request_status = 'pending'
ORDER BY created_at DESC;
```

---

## 📊 Analytics Queries

### Count Total Customers
```sql
SELECT COUNT(*) as total_customers FROM public.customer;
```

### Count Total Events
```sql
SELECT COUNT(*) as total_events FROM public.events;
```

### Count Active Bookings
```sql
SELECT COUNT(*) as active_bookings 
FROM public.booked_parking 
WHERE status = 'active';
```

### Total Revenue (Paid Bookings)
```sql
SELECT 
    SUM(payment_amount) as total_revenue,
    COUNT(*) as paid_bookings
FROM public.booked_parking 
WHERE payment_status = 'paid';
```

### Bookings by Payment Status
```sql
SELECT 
    payment_status,
    COUNT(*) as count,
    SUM(payment_amount) as total_amount
FROM public.booked_parking 
GROUP BY payment_status;
```

### Events with Booking Counts
```sql
SELECT 
    e.event_name,
    e.event_date,
    COUNT(bp.booking_id) as total_bookings,
    COUNT(CASE WHEN bp.status = 'active' THEN 1 END) as active_bookings
FROM public.events e
LEFT JOIN public.booked_parking bp ON e.event_id = bp.event_id
GROUP BY e.event_id, e.event_name, e.event_date
ORDER BY e.event_date DESC;
```

---

## 🗑️ Cleanup Queries (Use with Caution!)

### Delete All Test Data
```sql
-- Delete in order (respects foreign keys)
DELETE FROM public.parking_requests;
DELETE FROM public.qr_logs;
DELETE FROM public.booked_parking;
DELETE FROM public.staff;
DELETE FROM public.events;
DELETE FROM public.customer;
DELETE FROM public.administration;
```

### Delete Old Completed Bookings
```sql
DELETE FROM public.booked_parking 
WHERE status = 'completed' 
AND created_at < NOW() - INTERVAL '30 days';
```

### Delete Old QR Logs
```sql
DELETE FROM public.qr_logs 
WHERE scan_timestamp < NOW() - INTERVAL '90 days';
```

---

## 🔧 Troubleshooting Queries

### Check for Orphaned Records
```sql
-- Bookings without valid customer
SELECT bp.* 
FROM public.booked_parking bp
LEFT JOIN public.customer c ON bp.customer_email = c.email_id
WHERE c.email_id IS NULL;

-- Bookings without valid event
SELECT bp.* 
FROM public.booked_parking bp
LEFT JOIN public.events e ON bp.event_id = e.event_id
WHERE e.event_id IS NULL;

-- Staff without valid event
SELECT s.* 
FROM public.staff s
LEFT JOIN public.events e ON s.assigned_event_id = e.event_id
WHERE s.assigned_event_id IS NOT NULL AND e.event_id IS NULL;
```

### Check Duplicate Email Issues
```sql
SELECT email_id, COUNT(*) as count
FROM public.customer
GROUP BY email_id
HAVING COUNT(*) > 1;

SELECT email_id, COUNT(*) as count
FROM public.staff
GROUP BY email_id
HAVING COUNT(*) > 1;
```

### Check Missing Required Fields
```sql
-- Customers without email
SELECT * FROM public.customer WHERE email_id IS NULL OR email_id = '';

-- Events without name
SELECT * FROM public.events WHERE event_name IS NULL OR event_name = '';

-- Bookings without QR code
SELECT * FROM public.booked_parking WHERE qr_code IS NULL OR qr_code = '';
```

---

## 📱 Android App Integration Notes

### API URL Format
Your Supabase REST API endpoints follow this pattern:
- **Base URL**: `https://ywvkxqpwnnanscxgkszq.supabase.co`
- **Tables**: `/rest/v1/{table_name}`
- **Full Example**: `https://ywvkxqpwnnanscxgkszq.supabase.co/rest/v1/customer`

### Filtering (PostgREST format)
```
email_id=eq.customer@example.com
event_id=eq.uuid-here
status=eq.active
payment_status=in.(pending,paid)
```

### Ordering
```
order=created_at.desc
order=event_date.asc
order=created_at.desc,status.asc
```

### Combining Filters
```
customer_email=eq.user@example.com&status=eq.active&order=created_at.desc
```

---

## 🔐 Security Notes

**Current Setup:**
- ✅ All tables have RLS enabled
- ✅ Public policies allow read/write (using anon key)
- ⚠️ Suitable for development/testing
- ⚠️ For production, add user-based authentication checks

**For Production:**
1. Implement JWT-based authentication
2. Update policies to check authenticated user email
3. Restrict policies based on user roles
4. Use service role key only on backend servers

---

## 📞 Need Help?

1. Check Supabase Dashboard → Logs for errors
2. Verify API keys in `SupabaseClientInstance.java`
3. Test queries in Supabase SQL Editor first
4. Check foreign key constraints if inserts fail

