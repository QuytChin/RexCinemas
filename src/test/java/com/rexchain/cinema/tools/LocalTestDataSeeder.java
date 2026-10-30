package com.rexchain.cinema.tools;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.*;
import java.time.*;
import java.util.*;

/** Explicitly invoked, local-only seed utility. Never runs as part of application startup. */
public final class LocalTestDataSeeder {
    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final String MARK = "REX_TEST_";
    private final Connection c;
    private final String passwordHash;
    private final Map<String, Long> ids = new HashMap<>();

    private LocalTestDataSeeder(Connection c, String password) {
        this.c = c;
        this.passwordHash = new BCryptPasswordEncoder().encode(password);
    }

    public static void main(String[] args) throws Exception {
        String url = required("DB_URL");
        String user = required("DB_USERNAME");
        String pass = required("DB_PASSWORD");
        String demoPass = required("DEMO_CUSTOMER_PASSWORD");
        if (!url.matches("jdbc:mysql://(localhost|127\\.0\\.0\\.1)(:3306)?/rex_chain_cinema(_restore_[0-9]{8}_[0-9]{6})?([?].*)?"))
            throw new IllegalArgumentException("Refusing non-local or non-audit database URL");
        try (Connection c = DriverManager.getConnection(url, user, pass)) {
            c.setAutoCommit(false);
            try {
                LocalTestDataSeeder seed = new LocalTestDataSeeder(c, demoPass);
                seed.assertTarget();
                seed.cleanupOwnedRows();
                seed.seedUsers();
                seed.seedMovies();
                seed.seedRoomsAndSeats();
                seed.seedShowtimesAndSeatMaps();
                seed.seedCombos();
                seed.seedBookingsAndTickets();
                seed.seedContentAndNotifications();
                seed.validate();
                c.commit();
                System.out.println("REX_TEST seed committed; integrity checks passed.");
            } catch (Exception e) {
                c.rollback();
                throw e;
            }
        }
    }

    private static String required(String key) {
        String value = System.getenv(key);
        if (value == null || value.isBlank()) throw new IllegalStateException("Required environment variable is missing: " + key);
        return value;
    }

    private void assertTarget() throws SQLException {
        try (Statement s = c.createStatement(); ResultSet r = s.executeQuery("SELECT DATABASE(), @@hostname, @@port")) {
            if (!r.next() || !r.getString(1).startsWith("rex_chain_cinema") || r.getInt(3) != 3306)
                throw new IllegalStateException("Connected database is outside the approved local seed target");
        }
        try (Statement s = c.createStatement(); ResultSet r = s.executeQuery("SELECT id FROM cinemas WHERE id IN (1,2) ORDER BY id")) {
            int found = 0; while (r.next()) found++;
            if (found != 2) throw new IllegalStateException("Expected local Q1 and Go Vap cinemas are missing");
        }
    }

    private void cleanupOwnedRows() throws SQLException {
        // Every predicate below is anchored to a REX_TEST_ identifier; no broad table cleanup.
        exec("DELETE n FROM account_notifications n JOIN app_users u ON u.id=n.user_id WHERE u.email LIKE 'rex-test-%@rexchain.test'");
        exec("DELETE r FROM reviews r JOIN movies m ON m.id=r.movie_id WHERE m.title LIKE 'REX_TEST_%' OR m.poster_url LIKE '/images/movies/collection/%'");
        exec("DELETE bs FROM booking_seats bs JOIN bookings b ON b.id=bs.booking_id WHERE b.booking_code LIKE 'REX_TEST_%'");
        exec("DELETE bc FROM booking_combos bc JOIN bookings b ON b.id=bc.booking_id WHERE b.booking_code LIKE 'REX_TEST_%'");
        exec("DELETE pt FROM payment_transactions pt JOIN bookings b ON b.booking_code=pt.booking_code WHERE b.booking_code LIKE 'REX_TEST_%'");
        exec("DELETE la FROM late_payment_case_audits la JOIN late_payment_cases lc ON lc.id=la.late_payment_case_id JOIN bookings b ON b.id=lc.booking_id WHERE b.booking_code LIKE 'REX_TEST_%'");
        exec("DELETE lc FROM late_payment_cases lc JOIN bookings b ON b.id=lc.booking_id WHERE b.booking_code LIKE 'REX_TEST_%'");
        exec("DELETE l FROM loyalty_ledger l JOIN app_users u ON u.id=l.user_id WHERE u.email LIKE 'rex-test-%@rexchain.test'");
        exec("DELETE FROM reward_redemptions WHERE idempotency_key LIKE 'REX_TEST_%'");
        exec("DELETE FROM bookings WHERE booking_code LIKE 'REX_TEST_%'");
        exec("DELETE ss FROM showtime_seats ss JOIN showtimes st ON st.id=ss.showtime_id JOIN movies m ON m.id=st.movie_id WHERE m.title LIKE 'REX_TEST_%' OR m.poster_url LIKE '/images/movies/collection/%'");
        exec("DELETE st FROM showtimes st JOIN movies m ON m.id=st.movie_id WHERE m.title LIKE 'REX_TEST_%' OR m.poster_url LIKE '/images/movies/collection/%'");
        exec("DELETE FROM support_messages WHERE session_id LIKE 'REX_TEST_%'");
        exec("DELETE FROM articles WHERE title LIKE 'REX_TEST_%'");
        exec("DELETE FROM cinema_events WHERE title LIKE 'REX_TEST_%'");
        exec("DELETE FROM media_items WHERE title LIKE 'REX_TEST_%'");
        exec("DELETE FROM vouchers WHERE code LIKE 'REX_TEST_%'");
        exec("DELETE FROM loyalty_rewards WHERE name LIKE 'REX_TEST_%'");
        exec("DELETE FROM combo_products WHERE name LIKE 'REX_TEST_%'");
        exec("DELETE FROM seats WHERE auditorium_id IN (SELECT id FROM auditoriums WHERE name LIKE 'REX_TEST_%')");
        exec("DELETE FROM auditoriums WHERE name LIKE 'REX_TEST_%'");
        exec("DELETE FROM movies WHERE title LIKE 'REX_TEST_%' OR poster_url LIKE '/images/movies/collection/%'");
        exec("DELETE FROM app_users WHERE email LIKE 'rex-test-%@rexchain.test'");
    }

    private void seedUsers() throws SQLException {
        String[] names = {"An Nhiên", "Bảo Minh", "Cẩm Tú", "Đức Anh", "Gia Hân", "Hải Đăng", "Khánh Linh", "Minh Khoa",
                "Ngọc Hà", "Phúc An", "Quốc Bảo", "Thảo Vy", "Tuấn Kiệt", "Uyên Nhi", "Việt Anh", "Xuân Mai",
                "Yến Nhi", "Ánh Dương", "Bình An", "Châu Giang", "Đăng Khôi", "Hoài Nam", "Kim Ngân", "Lâm Phong"};
        for (int i=0;i<names.length;i++) {
            String email = String.format(Locale.ROOT,"rex-test-customer-%02d@rexchain.test",i+1);
            long id = insertId("INSERT INTO app_users(created_at,email,enabled,full_name,lifetime_points,loyalty_points,password,role,version,tier_qualifying_points,redeemed_points,reversed_points,credential_version,favorite_cinema_id) VALUES(?,?,?,?,0,0,?,'CUSTOMER',0,0,0,0,0,?)",
                    ts(LocalDateTime.now(ZONE).minusDays(24-i)), email, true, names[i], passwordHash, (i%2)+1);
            ids.put("user"+(i+1), id);
        }
    }

    private void seedMovies() throws SQLException {
        String[] titles = {"Đường Chân Trời Mùa Hạ","Bí Mật Ga Cuối","Bếp Nhà Có Nắng","Thành Phố Trong Mưa","Hành Tinh Thứ Chín",
                "Chuyến Tàu Đêm","Vườn Sao Nhỏ","Ngày Gió Trở Về","Mật Mã Ánh Trăng","Tiệm Sách Cuối Phố","Rừng Thì Thầm","Một Ngày Rực Rỡ"};
        String[] slugs = {"duong-chan-troi-mua-ha","bi-mat-ga-cuoi","bep-nha-co-nang","thanh-pho-trong-mua","hanh-tinh-thu-chin",
                "chuyen-tau-dem","vuon-sao-nho","ngay-gio-tro-ve","mat-ma-anh-trang","tien-sach-cuoi-pho","rung-thi-tham","mot-ngay-ruc-ro"};
        String[] descriptions = {
                "An trở về miền biển quê nhà và tìm lại ước mơ trong một mùa hè đáng nhớ.",
                "Chiếc va-li bỏ quên ở sân ga mở ra bí mật gắn với chuyến tàu đêm.",
                "Ba thế hệ trong gia đình tìm thấy niềm vui bên những bữa cơm nhà.",
                "Một kiến trúc sư trẻ đi tìm ký ức và lựa chọn mới giữa thành phố ngày mưa.",
                "Đoàn thám hiểm phát hiện dấu vết lạ trên hành tinh xa ngoài rìa hệ Mặt Trời.",
                "Những hành khách xa lạ lần theo manh mối về vụ mất tích trên chuyến tàu đêm.",
                "Cô bé Linh khám phá khu vườn kỳ diệu chỉ xuất hiện khi màn đêm buông xuống.",
                "Trở về quê biển, Huy tìm cách hàn gắn tình thân và bắt đầu lại.",
                "Một mật mã cổ dưới đài thiên văn dẫn nữ khảo cổ đến bí ẩn gia đình.",
                "Hiệu sách nhỏ trở thành nơi những người xa lạ tìm thấy niềm vui và khởi đầu mới.",
                "Hai người đi rừng lạc vào khu rừng cổ và phát hiện bí mật bị chôn giấu.",
                "Một nghệ sĩ đường phố nhận ra niềm vui trong những điều giản dị quanh mình."
        };
        String[] genres = {"Tâm lý, Tình cảm","Bí ẩn, Giật gân","Gia đình, Hài","Tâm lý, Tình cảm","Khoa học viễn tưởng, Phiêu lưu","Bí ẩn, Phiêu lưu",
                "Gia đình, Giả tưởng","Gia đình, Tâm lý","Bí ẩn, Phiêu lưu","Tình cảm, Đời thường","Phiêu lưu, Giả tưởng","Hài, Đời thường"};
        int[] durations = {92,118,105,130,145,88,112,125,101,137,96,120};
        LocalDate today = LocalDate.now(ZONE);
        for(int i=0;i<titles.length;i++) {
            String title=titles[i];
            String status = i<8 ? "NOW_SHOWING" : i<10 ? "COMING_SOON" : "ENDED";
            LocalDate release = i<8 ? today.minusDays(i+1) : i<10 ? today.plusDays(i+3) : today.minusDays(45+i);
            long id=insertId("INSERT INTO movies(active,age_rating,created_at,description,duration_minutes,genre,language,poster_url,trailer_url,release_date,status,title) VALUES(1,?,?,?,?,?,?,?,?,?,?,?)",
                    new Object[]{new String[]{"P","T13","T16","C13","C16","K"}[i%6],ts(LocalDateTime.now(ZONE)),descriptions[i],durations[i],genres[i],"Tiếng Việt","/images/movies/collection/"+slugs[i]+".webp","https://www.youtube.com/watch?v=ttDbSrXnCH4",java.sql.Date.valueOf(release),status,title});
            ids.put("movie"+i,id);
        }
    }

    private void seedRoomsAndSeats() throws SQLException {
        long q1=cinemaId(1), gv=cinemaId(2);
        createRoom("roomQ1",q1,"REX_TEST_Q1_ROOM",8,6);
        createRoom("roomGV",gv,"REX_TEST_GV_ROOM",10,8);
        for(String key:List.of("roomQ1","roomGV")) {
            long room=ids.get(key);
            try(PreparedStatement p=c.prepareStatement("INSERT INTO seats(seat_number,seat_row,type,auditorium_id) VALUES(?,?,?,?)")) {
                int rows=key.equals("roomQ1")?6:8, cols=key.equals("roomQ1")?8:10;
                for(int row=0;row<rows;row++) for(int col=1;col<=cols;col++) {
                    p.setInt(1,col); p.setString(2,String.valueOf((char)('A'+row)));
                    p.setString(3,row==rows-1&&col==1?"COUPLE":row>=rows-2?"VIP":"STANDARD"); p.setLong(4,room); p.addBatch();
                }
                p.executeBatch();
            }
        }
    }

    private void seedShowtimesAndSeatMaps() throws SQLException {
        LocalDateTime now=LocalDateTime.now(ZONE).withSecond(0).withNano(0);
        for(int i=0;i<8;i++) {
            long room=ids.get(i%2==0?"roomQ1":"roomGV"), movie=ids.get("movie"+i);
            LocalDateTime start=now.plusDays(i%5+1).withHour(new int[]{10,13,16,19,21}[i%5]).withMinute(0);
            long st=insertId("INSERT INTO showtimes(active,base_price,start_time,end_time,auditorium_id,movie_id) VALUES(1,?,?,?,?,?)",
                    new Object[]{new BigDecimal(i%3==0?"95000":"85000"),ts(start),ts(start.plusMinutes(movieDuration(movie)+15L)),room,movie});
            ids.put("showtime"+i,st);
            try(PreparedStatement p=c.prepareStatement("INSERT INTO showtime_seats(held_by_user_id,hold_until,price,status,version,seat_id,showtime_id) SELECT NULL,NULL,CASE s.type WHEN 'VIP' THEN ? WHEN 'COUPLE' THEN ? ELSE ? END,'AVAILABLE',0,s.id,? FROM seats s WHERE s.auditorium_id=?")) {
                p.setBigDecimal(1,new BigDecimal("105000"));p.setBigDecimal(2,new BigDecimal("170000"));p.setBigDecimal(3,new BigDecimal("85000"));p.setLong(4,st);p.setLong(5,room);p.executeUpdate();
            }
        }
    }

    private void seedBookingsAndTickets() throws SQLException {
        // One historical showtime per room/day, with unique seat allocation and a mix of legitimate outcomes.
        LocalDateTime now=LocalDateTime.now(ZONE).withSecond(0).withNano(0);
        for(int day=1;day<=7;day++) {
            for(int roomIndex=0;roomIndex<2;roomIndex++) {
                long room=ids.get(roomIndex==0?"roomQ1":"roomGV"), movie=ids.get("movie"+((day+roomIndex)%8));
                LocalDateTime start=now.minusDays(day).withHour(19).withMinute(0);
                long st=insertId("INSERT INTO showtimes(active,base_price,start_time,end_time,auditorium_id,movie_id) VALUES(1,85000,?,?,?,?)",
                        new Object[]{ts(start),ts(start.plusMinutes(movieDuration(movie)+15L)),room,movie});
                ids.put("past"+day+roomIndex,st);
                try(PreparedStatement p=c.prepareStatement("INSERT INTO showtime_seats(held_by_user_id,hold_until,price,status,version,seat_id,showtime_id) SELECT NULL,NULL,CASE s.type WHEN 'VIP' THEN 105000 WHEN 'COUPLE' THEN 170000 ELSE 85000 END,'AVAILABLE',0,s.id,? FROM seats s WHERE s.auditorium_id=?")) {p.setLong(1,st);p.setLong(2,room);p.executeUpdate();}
                // Five paid seats per show; payments are labeled as local mock data and never sent to a provider.
                try(PreparedStatement q=c.prepareStatement("SELECT ss.id,ss.price,s.seat_row,s.seat_number FROM showtime_seats ss JOIN seats s ON s.id=ss.seat_id WHERE ss.showtime_id=? ORDER BY CASE s.type WHEN 'COUPLE' THEN 0 WHEN 'VIP' THEN 1 ELSE 2 END,s.seat_row,s.seat_number LIMIT 5")) {
                    q.setLong(1,st); try(ResultSet r=q.executeQuery()) {int n=0;while(r.next()) {int customer=(day*2+roomIndex+n)%24; long user=ids.get("user"+(customer+1)); String code=String.format(Locale.ROOT,"REX_TEST_%02d_%d_%d",day,roomIndex,n+1); BigDecimal amount=r.getBigDecimal(2); LocalDateTime created=start.minusHours(3);
                        int points=earnedPoints(amount);
                        long booking=insertId("INSERT INTO bookings(booking_code,created_at,payment_method,payment_reference,points_awarded,paid_at,seat_amount,combo_amount,discount_amount,status,total_amount,version,showtime_id,user_id,earned_points,points_reversed,membership_tier_snapshot,membership_discount_percent,membership_discount_amount,voucher_discount_amount,point_multiplier) VALUES(?,?, 'BANK_QR',?,1,?,?,0,0,'CONFIRMED',?,0,?,?,?,0,'MEMBER',0,0,0,1)",
                                new Object[]{code,ts(created),code,ts(created.plusMinutes(2)),amount,amount,st,user,points});
                        String ticket=code+"_T1", token=UUID.nameUUIDFromBytes(code.getBytes(java.nio.charset.StandardCharsets.UTF_8)).toString();
                        boolean checkedIn=n==0;
                        String staffEmail=roomIndex==0?"staff1.q1@rexchain.vn":"staff2.gv@rexchain.vn";
                        LocalDateTime checkedAt=start.plusMinutes(5);
                        exec("UPDATE showtime_seats SET status='BOOKED' WHERE id="+r.getLong(1));
                        insert("INSERT INTO booking_seats(checked_in,checked_in_at,checked_in_by,price,qr_token,seat_label,ticket_code,booking_id,showtime_seat_id) VALUES(?,?,?,?,?,?,?,?,?)",
                                checkedIn,checkedIn?ts(checkedAt):null,checkedIn?staffEmail:null,amount,token,r.getString(3)+r.getInt(4),ticket,booking,r.getLong(1));
                        if(checkedIn) exec("UPDATE bookings SET checked_in_at='"+ts(checkedAt)+"',checked_in_by='"+staffEmail+"' WHERE id="+booking);
                        insertPayment(code,amount,created);
                        awardPoints(user, booking, code, points);
                        n++;
                    }}
                }
            }
        }
        createMultiSeatBooking(ids.get("past10"),ids.get("user1"),now);
        createComboBooking(ids.get("past10"),ids.get("user5"),now);
        createRefundPendingBooking(ids.get("past10"),ids.get("user7"),now);
        seedPaymentEventFixtures(now);
        // Future hold, expired, cancelled and refund review scenarios use separate seats; no sold-seat conflicts.
        createNonPaidBooking("PENDING",0,1,0,now.plusDays(2),now.plusMinutes(5));
        createNonPaidBooking("EXPIRED",1,1,1,now.minusDays(2),now.minusDays(2).plusMinutes(5));
        createLateArrivalPurchase(now);
        createNonPaidBooking("CANCELLED",2,1,2,now.plusDays(3),null);
    }

    private void seedContentAndNotifications() throws SQLException {
        LocalDateTime now=LocalDateTime.now(ZONE);
        exec("INSERT INTO articles(active,category,content,created_at,publish_date,sort_order,summary,title) VALUES(1,'REX_TEST','Test content',NOW(),CURRENT_DATE,90,'REX_TEST nội dung kiểm tra','REX_TEST Bài viết về trải nghiệm rạp')");
        exec("INSERT INTO cinema_events(active,created_at,description,label,sort_order,title) VALUES(1,NOW(),'Sự kiện giả lập local','REX_TEST',90,'REX_TEST Tuần phim gia đình')");
        LocalDateTime from=now.minusDays(30),until=now.plusDays(30);
        insert("INSERT INTO vouchers(active,code,discount_type,discount_value,max_discount_amount,min_order_amount,name,usage_limit,used_count,valid_from,valid_until,version) VALUES(1,?,'PERCENT',10,30000,100000,?,100,0,?,?,0)",MARK+"VOUCHER_ACTIVE",MARK+"Voucher 10% test",ts(from),ts(until));
        insert("INSERT INTO vouchers(active,code,discount_type,discount_value,max_discount_amount,min_order_amount,name,usage_limit,used_count,valid_from,valid_until,version) VALUES(1,?,'FIXED',20000,20000,50000,?,100,0,?,?,0)",MARK+"VOUCHER_FIXED",MARK+"Voucher giảm cố định test",ts(from),ts(until));
        insert("INSERT INTO vouchers(active,code,discount_type,discount_value,max_discount_amount,min_order_amount,name,usage_limit,used_count,valid_from,valid_until,version) VALUES(1,?,'FIXED',15000,15000,0,?,100,0,?,?,0)",MARK+"VOUCHER_EXPIRED",MARK+"Voucher hết hạn test",ts(now.minusDays(60)),ts(now.minusDays(1)));
        long reward=insertId("INSERT INTO loyalty_rewards(version,name,description,points_cost,voucher_amount,voucher_min_order_amount,validity_days,redemption_limit,redeemed_count,active) VALUES(0,?,?,5,25000,50000,30,100,0,1)",MARK+"Đổi voucher 25.000đ", "Phần thưởng demo đổi bằng điểm; local fixture.");
        long rewardUser=ids.get("user3");
        String redeemKey=MARK+"REDEEM_01";
        long voucher=insertId("INSERT INTO vouchers(active,code,discount_type,discount_value,max_discount_amount,min_order_amount,name,usage_limit,used_count,valid_from,valid_until,version,owner_user_id) VALUES(1,?,'FIXED',25000,25000,50000,?,1,0,?,?,0,?)",
                MARK+"REWARD_VOUCHER_01",MARK+"Voucher đổi thưởng",ts(now.minusDays(1)),ts(now.plusDays(29)),rewardUser);
        long redemption=insertId("INSERT INTO reward_redemptions(version,user_id,reward_id,voucher_id,idempotency_key,points_cost,status,created_at) VALUES(0,?,?,?,?,5,'ISSUED',?)",
                rewardUser,reward,voucher,redeemKey,ts(now.minusMinutes(20)));
        insert("UPDATE vouchers SET reward_redemption_reference=? WHERE id=?","REWARD:"+redemption,voucher);
        exec("UPDATE loyalty_rewards SET redeemed_count=redeemed_count+1 WHERE id="+reward);
        int before,qualifying,lifetime;
        try(PreparedStatement p=c.prepareStatement("SELECT loyalty_points,tier_qualifying_points,lifetime_points FROM app_users WHERE id=? FOR UPDATE")){p.setLong(1,rewardUser);try(ResultSet r=p.executeQuery()){r.next();before=r.getInt(1);qualifying=r.getInt(2);lifetime=r.getInt(3);}}
        if(before<5) throw new IllegalStateException("Selected reward test user lacks points for redemption fixture");
        exec("UPDATE app_users SET loyalty_points=loyalty_points-5,redeemed_points=redeemed_points+5 WHERE id="+rewardUser);
        insert("INSERT INTO loyalty_ledger(user_id,redemption_id,transaction_type,source,reference_key,points_delta,available_before,available_after,qualifying_delta,qualifying_before,qualifying_after,lifetime_delta,lifetime_before,lifetime_after,redeemed_delta,reversed_delta,reason,actor) VALUES(?,?,'REDEEM','REWARD_REDEMPTION',?,?,?, ?,0,?,?,0,?,?,5,0,?,'customer')",
                rewardUser,redemption,"REDEEM:"+rewardUser+":"+redeemKey,-5,before,before-5,qualifying,qualifying,lifetime,lifetime,"REX_TEST_ Reward redemption fixture");
        for(int i=1;i<=24;i++) {
            long user=ids.get("user"+i);
            insert("INSERT INTO account_notifications(created_at,deduplication_key,link,message,read_at,read_at_present,title,type,user_id) VALUES(?,?,?, ?,?,?,?, ?,?)",
                    ts(now.minusHours(i)),MARK+"WELCOME_"+i,"/profile","Thông báo kiểm thử dành cho tài khoản demo.",i%2==0?ts(now.minusHours(i-1)):null,i%2==0,"REX_TEST Chào mừng","SYSTEM",user);
        }
        for(int i=0;i<8;i++) {
            insert("INSERT INTO reviews(comment,created_at,rating,movie_id,user_id) VALUES(?,?,?,?,?)",
                    "REX_TEST_ Nhận xét demo " +(i+1),ts(now.minusDays(i+1)),4+i%2,ids.get("movie"+i),ids.get("user"+(i+1)));
        }
        long supportUser=ids.get("user1");
        insert("INSERT INTO support_messages(session_id,customer_id,customer_name,customer_email,cinema_id,sender_type,message,is_read,created_at) VALUES(?,?,?,?,?,'CUSTOMER',?,0,?)",
                MARK+"SUPPORT_01",supportUser,"An Nhiên","rex-test-customer-01@rexchain.test",cinemaId(1),"Mình muốn hỏi về suất chiếu thử nghiệm.",ts(now.minusMinutes(8)));
        insert("INSERT INTO support_messages(session_id,customer_id,customer_name,customer_email,cinema_id,sender_type,message,is_read,created_at) VALUES(?,?,?,?,?,'STAFF',?,1,?)",
                MARK+"SUPPORT_01",supportUser,"An Nhiên","rex-test-customer-01@rexchain.test",cinemaId(1),"Nhân viên rạp đã nhận yêu cầu local.",ts(now.minusMinutes(6)));
    }

    private void seedCombos() throws SQLException {
        ids.put("comboSolo",insertId("INSERT INTO combo_products(active,created_at,description,name,price,stock_quantity,type,version) VALUES(1,NOW(),'REX_TEST sample item',?,69000,50,'COMBO',0)",MARK+"Combo Vé Vui"));
        ids.put("comboDuo",insertId("INSERT INTO combo_products(active,created_at,description,name,price,stock_quantity,type,version) VALUES(1,NOW(),'REX_TEST sample item',?,119000,25,'COMBO',0)",MARK+"Combo Hai Người"));
    }

    private void createComboBooking(long showtime,long user,LocalDateTime now) throws SQLException {
        long seatId; BigDecimal seatAmount; String label;
        try(PreparedStatement p=c.prepareStatement("SELECT ss.id,ss.price,CONCAT(s.seat_row,s.seat_number) FROM showtime_seats ss JOIN seats s ON s.id=ss.seat_id WHERE ss.showtime_id=? AND ss.status='AVAILABLE' ORDER BY ss.id LIMIT 1")){
            p.setLong(1,showtime);try(ResultSet r=p.executeQuery()){if(!r.next())throw new IllegalStateException("No seat for combo booking fixture");seatId=r.getLong(1);seatAmount=r.getBigDecimal(2);label=r.getString(3);}}
        long combo=ids.get("comboSolo"); BigDecimal comboAmount=new BigDecimal("69000"),total=seatAmount.add(comboAmount);String code=MARK+"COMBO_01";LocalDateTime created=now.minusDays(1);int points=earnedPoints(total);
        long booking=insertId("INSERT INTO bookings(booking_code,created_at,payment_method,payment_reference,points_awarded,paid_at,seat_amount,combo_amount,discount_amount,status,total_amount,version,showtime_id,user_id,earned_points,points_reversed,membership_tier_snapshot,membership_discount_percent,membership_discount_amount,voucher_discount_amount,point_multiplier) VALUES(?,?, 'BANK_QR',?,1,?,?,?,0,'CONFIRMED',?,0,?,?,?,0,'MEMBER',0,0,0,1)",
                code,ts(created),code,ts(created.plusMinutes(2)),seatAmount,comboAmount,total,showtime,user,points);
        exec("UPDATE showtime_seats SET status='BOOKED' WHERE id="+seatId);
        insert("INSERT INTO booking_seats(checked_in,price,qr_token,seat_label,ticket_code,booking_id,showtime_seat_id) VALUES(0,?,?,?,?,?,?)",seatAmount,UUID.nameUUIDFromBytes(code.getBytes(java.nio.charset.StandardCharsets.UTF_8)).toString(),label,code+"_T1",booking,seatId);
        insert("INSERT INTO booking_combos(combo_name,quantity,unit_price,line_total,booking_id,combo_id) VALUES(?,1,?,?,?,?)",MARK+"Combo Vé Vui",comboAmount,comboAmount,booking,combo);
        exec("UPDATE combo_products SET stock_quantity=stock_quantity-1 WHERE id="+combo);
        insertPayment(code,total,created);awardPoints(user,booking,code,points);
    }

    private void seedPaymentEventFixtures(LocalDateTime now) throws SQLException {
        String booked=MARK+"MULTI_SEAT_01";BigDecimal bookedAmount;
        try(PreparedStatement p=c.prepareStatement("SELECT total_amount FROM bookings WHERE booking_code=?")){p.setString(1,booked);try(ResultSet r=p.executeQuery()){if(!r.next())throw new IllegalStateException("Multi-seat booking fixture is missing");bookedAmount=r.getBigDecimal(1);}}
        insert("INSERT INTO payment_transactions(provider_transaction_id,booking_code,payment_reference,gateway,transaction_date,code,content,transfer_type,amount,reference_code,status,reason,received_at) VALUES(?,?,?,'REX_TEST_MOCK',?,'TEST',?,'in',?,?,'DUPLICATE','Duplicate local payment event fixture',?)",
                MARK+"DUPLICATE_EVENT_01",booked,booked,ts(now.minusDays(1)),booked,bookedAmount,booked,ts(now.minusDays(1)));
        String pending=MARK+"PENDING_01";
        insert("INSERT INTO payment_transactions(provider_transaction_id,booking_code,payment_reference,gateway,transaction_date,code,content,transfer_type,amount,reference_code,status,reason,received_at) VALUES(?,?,?,'REX_TEST_MOCK',?,'TEST',?,'in',?,?,'NEEDS_REVIEW','Underpayment local fixture; booking remains pending',?)",
                MARK+"UNDERPAYMENT_01",pending,MARK+"WRONG_REFERENCE",ts(now),pending,new BigDecimal("10000"),pending,ts(now));
    }

    private void createRefundPendingBooking(long showtime,long user,LocalDateTime now) throws SQLException {
        long seatId;BigDecimal amount;String label;
        try(PreparedStatement p=c.prepareStatement("SELECT ss.id,ss.price,CONCAT(s.seat_row,s.seat_number) FROM showtime_seats ss JOIN seats s ON s.id=ss.seat_id WHERE ss.showtime_id=? AND ss.status='AVAILABLE' ORDER BY ss.id LIMIT 1")){p.setLong(1,showtime);try(ResultSet r=p.executeQuery()){if(!r.next())throw new IllegalStateException("No seat for refund-pending fixture");seatId=r.getLong(1);amount=r.getBigDecimal(2);label=r.getString(3);}}
        String code=MARK+"REFUND_PENDING_01";LocalDateTime created=now.minusDays(1);int points=earnedPoints(amount);
        long booking=insertId("INSERT INTO bookings(booking_code,created_at,payment_method,payment_reference,points_awarded,paid_at,seat_amount,combo_amount,discount_amount,status,total_amount,version,showtime_id,user_id,earned_points,points_reversed,membership_tier_snapshot,membership_discount_percent,membership_discount_amount,voucher_discount_amount,point_multiplier,refund_note) VALUES(?,?, 'BANK_QR',?,1,?,?,0,0,'REFUND_PENDING',?,0,?,?,?,0,'MEMBER',0,0,0,1,'REX_TEST refund pending manual verification')",
                code,ts(created),code,ts(created.plusMinutes(2)),amount,amount,showtime,user,points);
        exec("UPDATE showtime_seats SET status='BOOKED' WHERE id="+seatId);
        insert("INSERT INTO booking_seats(checked_in,price,qr_token,seat_label,ticket_code,booking_id,showtime_seat_id) VALUES(0,?,?,?,?,?,?)",amount,UUID.nameUUIDFromBytes(code.getBytes(java.nio.charset.StandardCharsets.UTF_8)).toString(),label,code+"_T1",booking,seatId);
        insertPayment(code,amount,created);awardPoints(user,booking,code,points);
    }

    private void createMultiSeatBooking(long showtime,long user,LocalDateTime now) throws SQLException {
        List<Long> seatIds=new ArrayList<>(); List<BigDecimal> prices=new ArrayList<>();
        try(PreparedStatement p=c.prepareStatement("SELECT ss.id,ss.price FROM showtime_seats ss WHERE ss.showtime_id=? AND ss.status='AVAILABLE' ORDER BY ss.id LIMIT 2")){p.setLong(1,showtime);try(ResultSet r=p.executeQuery()){while(r.next()){seatIds.add(r.getLong(1));prices.add(r.getBigDecimal(2));}}}
        if(seatIds.size()!=2)throw new IllegalStateException("Could not allocate test seats for multi-seat booking");
        BigDecimal amount=prices.get(0).add(prices.get(1)); String code=MARK+"MULTI_SEAT_01"; LocalDateTime created=now.minusDays(1);
        int points=earnedPoints(amount);
        long booking=insertId("INSERT INTO bookings(booking_code,created_at,payment_method,payment_reference,points_awarded,paid_at,seat_amount,combo_amount,discount_amount,status,total_amount,version,showtime_id,user_id,earned_points,points_reversed,membership_tier_snapshot,membership_discount_percent,membership_discount_amount,voucher_discount_amount,point_multiplier) VALUES(?,?, 'BANK_QR',?,1,?,?,0,0,'CONFIRMED',?,0,?,?,?,0,'MEMBER',0,0,0,1)",
                code,ts(created),code,ts(created.plusMinutes(2)),amount,amount,showtime,user,points);
        for(int i=0;i<seatIds.size();i++){
            long seatId=seatIds.get(i); exec("UPDATE showtime_seats SET status='BOOKED' WHERE id="+seatId);
            String token=UUID.nameUUIDFromBytes((code+"_"+i).getBytes(java.nio.charset.StandardCharsets.UTF_8)).toString();
            insert("INSERT INTO booking_seats(checked_in,price,qr_token,seat_label,ticket_code,booking_id,showtime_seat_id) SELECT 0,?,?,CONCAT(s.seat_row,s.seat_number),?,?,ss.id FROM showtime_seats ss JOIN seats s ON s.id=ss.seat_id WHERE ss.id=?",
                    prices.get(i),token,code+"_T"+(i+1),booking,seatId);
        }
        insertPayment(code,amount,created); awardPoints(user,booking,code,points);
    }

    private void createLateArrivalPurchase(LocalDateTime now) throws SQLException {
        long seatId; BigDecimal amount; String label; long showtime;
        try(PreparedStatement p=c.prepareStatement("SELECT bs.showtime_seat_id,bs.price,bs.seat_label,b.showtime_id FROM bookings b JOIN booking_seats bs ON bs.booking_id=b.id WHERE b.booking_code=?")){
            p.setString(1,MARK+"EXPIRED_01");try(ResultSet r=p.executeQuery()){if(!r.next())throw new IllegalStateException("Expired booking fixture was not created");seatId=r.getLong(1);amount=r.getBigDecimal(2);label=r.getString(3);showtime=r.getLong(4);}}
        // The old booking is expired and non-payable; this new customer legitimately buys the released seat.
        String code=MARK+"AFTER_RELEASE_01"; long user=ids.get("user4"); LocalDateTime created=now.minusDays(1);
        int points=earnedPoints(amount);
        long booking=insertId("INSERT INTO bookings(booking_code,created_at,payment_method,payment_reference,points_awarded,paid_at,seat_amount,combo_amount,discount_amount,status,total_amount,version,showtime_id,user_id,earned_points,points_reversed,membership_tier_snapshot,membership_discount_percent,membership_discount_amount,voucher_discount_amount,point_multiplier) VALUES(?,?, 'BANK_QR',?,1,?,?,0,0,'CONFIRMED',?,0,?,?,?,0,'MEMBER',0,0,0,1)",
                code,ts(created),code,ts(created.plusMinutes(2)),amount,amount,showtime,user,points);
        exec("UPDATE showtime_seats SET status='BOOKED',held_by_user_id=NULL,hold_until=NULL WHERE id="+seatId);
        String token=UUID.nameUUIDFromBytes(code.getBytes(java.nio.charset.StandardCharsets.UTF_8)).toString();
        insert("INSERT INTO booking_seats(checked_in,price,qr_token,seat_label,ticket_code,booking_id,showtime_seat_id) VALUES(0,?,?,?,?,?,?)",amount,token, label,code+"_T1",booking,seatId);
        insertPayment(code,amount,created); awardPoints(user,booking,code,points);
    }

    private void validate() throws SQLException {
        assertZero("SELECT COUNT(*) FROM bookings b JOIN booking_seats bs ON bs.booking_id=b.id JOIN showtime_seats ss ON ss.id=bs.showtime_seat_id WHERE b.booking_code LIKE 'REX_TEST_%' AND (((b.status IN ('CONFIRMED','REFUND_PENDING')) AND ss.status<>'BOOKED') OR (b.status='PENDING' AND ss.status<>'HELD') OR (b.status='CANCELLED' AND ss.status<>'AVAILABLE') OR (b.status='EXPIRED' AND ((EXISTS (SELECT 1 FROM booking_seats live_bs JOIN bookings live_b ON live_b.id=live_bs.booking_id WHERE live_bs.showtime_seat_id=ss.id AND live_b.status IN ('CONFIRMED','REFUND_PENDING')) AND ss.status<>'BOOKED') OR (NOT EXISTS (SELECT 1 FROM booking_seats live_bs JOIN bookings live_b ON live_b.id=live_bs.booking_id WHERE live_bs.showtime_seat_id=ss.id AND live_b.status IN ('CONFIRMED','REFUND_PENDING')) AND ss.status<>'AVAILABLE'))))");
        assertZero("SELECT COUNT(*) FROM booking_seats bs JOIN bookings b ON b.id=bs.booking_id JOIN showtime_seats ss ON ss.id=bs.showtime_seat_id WHERE b.booking_code LIKE 'REX_TEST_%' AND b.status='CONFIRMED' AND (ss.showtime_id<>b.showtime_id OR bs.qr_token IS NULL)");
        assertZero("SELECT COUNT(*) FROM (SELECT booking_code,COUNT(*) n FROM bookings WHERE booking_code LIKE 'REX_TEST_%' GROUP BY booking_code HAVING n>1) d");
        assertZero("SELECT COUNT(*) FROM bookings WHERE booking_code LIKE 'REX_TEST_%' AND (status='CONFIRMED' AND paid_at IS NULL)");
    }

    private void createNonPaidBooking(String status,int showtimeIndex,int seatNo,int userIndex,LocalDateTime created,LocalDateTime expires) throws SQLException {
        long st=ids.get("showtime"+showtimeIndex), user=ids.get("user"+(userIndex+1));
        long seatId; BigDecimal price;
        try(PreparedStatement p=c.prepareStatement("SELECT ss.id,ss.price FROM showtime_seats ss WHERE ss.showtime_id=? AND ss.status='AVAILABLE' ORDER BY ss.id LIMIT ?,1")) {p.setLong(1,st);p.setInt(2,seatNo);try(ResultSet r=p.executeQuery()){if(!r.next())return;seatId=r.getLong(1);price=r.getBigDecimal(2);}}
        String code=MARK+status+"_01";
        long booking=insertId("INSERT INTO bookings(booking_code,created_at,payment_method,payment_reference,points_awarded,paid_at,seat_amount,combo_amount,discount_amount,expires_at,status,total_amount,version,showtime_id,user_id,earned_points,points_reversed,membership_discount_percent,membership_discount_amount,voucher_discount_amount,point_multiplier) VALUES(?,?, 'BANK_QR',?,0,NULL,?,0,0,?,?,?,0,?,?,0,0,0,0,0,1)",code,ts(created),code,price,expires==null?null:ts(expires),status,price,st,user);
        if("PENDING".equals(status)) exec("UPDATE showtime_seats SET status='HELD',held_by_user_id="+user+",hold_until='"+ts(expires)+"' WHERE id="+seatId);
        else if("EXPIRED".equals(status)||"CANCELLED".equals(status)) exec("UPDATE showtime_seats SET status='AVAILABLE',held_by_user_id=NULL,hold_until=NULL WHERE id="+seatId);
        String label;
        try(PreparedStatement p=c.prepareStatement("SELECT CONCAT(s.seat_row,s.seat_number) FROM seats s JOIN showtime_seats ss ON ss.seat_id=s.id WHERE ss.id=?")){p.setLong(1,seatId);try(ResultSet r=p.executeQuery()){r.next();label=r.getString(1);}}
        insert("INSERT INTO booking_seats(checked_in,price,seat_label,booking_id,showtime_seat_id) VALUES(0,?,?,?,?)",price,label,booking,seatId);
        if("EXPIRED".equals(status)) {
            String provider=MARK+"LATE_TX_"+code;
            insert("INSERT INTO payment_transactions(provider_transaction_id,booking_code,payment_reference,gateway,transaction_date,code,content,transfer_type,amount,reference_code,status,reason) VALUES(?,?,?,'REX_TEST_MOCK',?,'TEST',?,'in',?,?,'NEEDS_REVIEW','Local fixture: late payment after expiry; no provider call')",
                    provider,code,code,ts(created.plusMinutes(7)),code,price,code);
            long late=insertId("INSERT INTO late_payment_cases(amount,bank_recorded_at,created_at,payment_reference,provider_transaction_id,reason,status,updated_at,version,booking_id) VALUES(?,?,?,?,?,?,'NEEDS_REVIEW',?,0,?)",
                    price,ts(created.plusMinutes(7)),ts(created.plusMinutes(7)),code,provider,"REX_TEST late payment for expired booking",ts(created.plusMinutes(7)),booking);
            insert("INSERT INTO late_payment_case_audits(late_payment_case_id,action,actor,detail,created_at) VALUES(?,'CREATED','REX_TEST_SEED','Mock late payment fixture',?)",late,ts(created.plusMinutes(7)));
        }
    }

    private void insertPayment(String code,BigDecimal amount,LocalDateTime at) throws SQLException {
        insert("INSERT INTO payment_transactions(provider_transaction_id,booking_code,payment_reference,gateway,transaction_date,code,content,transfer_type,amount,reference_code,status,received_at,verified_at,settled_at) VALUES(?,?,?,'REX_TEST_MOCK',?,?,?,'in',?,?, 'SETTLED',?,?,?)",
                MARK+"TX_"+code,code,code,ts(at),"TEST",code,amount,code,ts(at),ts(at),ts(at));
    }

    private int earnedPoints(BigDecimal amount) throws SQLException {
        BigDecimal amountPerPoint; int minimum;
        try(Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT amount_per_point,minimum_positive_order_points FROM loyalty_settings WHERE id=1")){
            if(!r.next())return 0; amountPerPoint=r.getBigDecimal(1);minimum=r.getInt(2);
        }
        int points=amount.divide(amountPerPoint,0,RoundingMode.DOWN).intValue();
        return points==0&&amount.signum()>0?minimum:points;
    }

    private void awardPoints(long user,long booking,String code,int points) throws SQLException {
        if(points<=0)return;
        int before,qualifying,lifetime;
        try(PreparedStatement p=c.prepareStatement("SELECT loyalty_points,tier_qualifying_points,lifetime_points FROM app_users WHERE id=? FOR UPDATE")){p.setLong(1,user);try(ResultSet r=p.executeQuery()){r.next();before=r.getInt(1);qualifying=r.getInt(2);lifetime=r.getInt(3);}}
        exec("UPDATE app_users SET loyalty_points=loyalty_points+"+points+",tier_qualifying_points=tier_qualifying_points+"+points+",lifetime_points=lifetime_points+"+points+" WHERE id="+user);
        insert("INSERT INTO loyalty_ledger(user_id,booking_id,transaction_type,source,reference_key,points_delta,available_before,available_after,qualifying_delta,qualifying_before,qualifying_after,lifetime_delta,lifetime_before,lifetime_after,redeemed_delta,reversed_delta,reason,actor) VALUES(?,?,'EARN','BOOKING_PAYMENT',?,?,?, ?,?,?, ?,?,?,?,0,0,?,'REX_TEST_SEED')",user,booking,"EARN:BOOKING:"+code,points,before,before+points,points,qualifying,qualifying+points,points,lifetime,lifetime+points,"Local mock booking reward points");
    }

    private long cinemaId(long id) throws SQLException {return id;}
    private int movieDuration(long movieId) throws SQLException {try(PreparedStatement p=c.prepareStatement("SELECT duration_minutes FROM movies WHERE id=?")){p.setLong(1,movieId);try(ResultSet r=p.executeQuery()){if(!r.next())throw new SQLException("Seed movie is missing");return r.getInt(1);}}}
    private void createRoom(String key,long cinema,String name,int cols,int rows) throws SQLException {
        long id=insertId("INSERT INTO auditoriums(active,name,seats_per_row,total_rows,cinema_id) VALUES(1,?,?,?,?)",name,cols,rows,cinema);ids.put(key,id);
    }
    private long insertId(String sql,Object... values) throws SQLException {
        try(PreparedStatement p=c.prepareStatement(sql,Statement.RETURN_GENERATED_KEYS)){bind(p,values);p.executeUpdate();try(ResultSet r=p.getGeneratedKeys()){if(!r.next())throw new SQLException("Insert did not return ID");return r.getLong(1);}}
    }
    private void insert(String sql,Object... values) throws SQLException {try(PreparedStatement p=c.prepareStatement(sql)){bind(p,values);p.executeUpdate();}}
    private void bind(PreparedStatement p,Object[] values) throws SQLException {for(int i=0;i<values.length;i++){Object v=values[i];if(v instanceof LocalDateTime t)p.setTimestamp(i+1,Timestamp.valueOf(t));else if(v instanceof java.sql.Date d)p.setDate(i+1,d);else p.setObject(i+1,v);}}
    private void exec(String sql) throws SQLException {try(Statement s=c.createStatement()){s.executeUpdate(sql);}}
    private void assertZero(String sql) throws SQLException {try(Statement s=c.createStatement();ResultSet r=s.executeQuery(sql)){r.next();if(r.getLong(1)!=0)throw new IllegalStateException("Seed integrity check failed");}}
    private static Timestamp ts(LocalDateTime t){return Timestamp.valueOf(t);}
}
