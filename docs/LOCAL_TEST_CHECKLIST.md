# Umeed – full local test before hosting

Goal: try every feature (MVP plus F01–F07 and the admin dashboard) on real devices against the backend on this PC.
Report results by test number, for example "T3.2 failed: the banner did not appear". Screenshots help.

Two people are needed for most tests. Use **Device A** and **Device B**: two Android phones, or one phone plus
the Pixel_7 emulator. Each logs in with a different Firebase test number (Firebase console → Authentication →
Sign-in method → Phone → Phone numbers for testing). Call the accounts **Asha** (A) and **Dev** (B).

---

## Part 0 – Set up (once)

| # | Step | Done when |
|---|------|-----------|
| S1 | Ship H02 first: `ship "H02: backend production readiness"` | SHIPPED banner |
| S2 | Optional, for the map (T6.4): create the Google Maps key (steps in the index, F05), then `setx GOOGLE_MAPS_ANDROID_API_KEY ...` in Command Prompt and open a new window | `echo %GOOGLE_MAPS_ANDROID_API_KEY%` in a new window shows something |
| S3 | Check the Firebase key for notifications: `echo %UMEED_FIREBASE_CREDENTIALS%` in a new window | shows `H:\secrets\umeed-firebase-key.json` |
| S4 | Start the backend from a NEW window: in `backend`, run `mvnw.cmd spring-boot:run` | log does NOT say "notifications are written to the log only" |
| S5 | Rebuild and install the app (notifications and the map are native, so the old build is not enough): connect Device A by USB, in `apps\mobile` run `run-android` | Umeed opens on Device A |
| S6 | Device B: install the same build (`run-android` again with B connected, or start the emulator with `start-emulator` and run it there) | Umeed opens on Device B |
| S7 | Each device reaches the PC: for every device run `adb -s <device> reverse tcp:8080 tcp:8080` and `adb -s <device> reverse tcp:8081 tcp:8081` (`adb devices` lists the names) | the login screen loads on both |
| S8 | Admin dashboard: Firebase console → Project settings → Add app → Web; copy the 4 values into `apps\admin\.env.local` (template `.env.example`; do not paste them in chat) | file saved |
| S9 | Make a THIRD test number the admin. Log in with it once in the app, then run in Command Prompt (with that number): `"C:\Program Files\PostgreSQL\18\bin\psql.exe" -U umeed_app -d umeed_dev -c "UPDATE users SET role = 'ADMIN' WHERE phone_number = '+91XXXXXXXXXX';"` | `UPDATE 1` |
| S10 | Start the dashboard: in `apps\admin` run `npm run dev`, open http://localhost:3000 | login page appears |

Tip for memory: close IntelliJ, Postman and extra Chrome tabs; run `wsl --shutdown` before building.

---

## Part 1 – Login, name, language (F07A)

| # | Device | Do | Expect |
|---|--------|----|--------|
| T1.1 | A | On the login screen tap **हिन्दी** | the whole login screen switches to Hindi |
| T1.2 | A | Log in with Asha's test number and code, enter the name "Asha" | Home opens, all in Hindi (tabs, buttons, empty messages) |
| T1.3 | B | Log in as Dev in English, name "Dev" | Home in English |
| T1.4 | A | Profile → Language → English, then back to हिन्दी | switches at once, also tab names; no error |
| T1.5 | A | Log out and log in again (same number) | the app comes back in the language last chosen |
| T1.6 | A | Enter a wrong 6-digit code once | a clear message in the current language |

## Part 2 – Give, claim, pickup (MVP)

| # | Device | Do | Expect |
|---|--------|----|--------|
| T2.1 | B | Donate → Give an item: 3 photos (camera + gallery), title, description, category, condition, city, area, address | "Posted"; the item opens; photos swipe sideways |
| T2.2 | B | Try to post with an empty title | the form shows what is missing |
| T2.3 | A | Home: the item appears; swipe up/down; tap the title | item details with photos, distance, "Given by Dev" |
| T2.4 | A | Claim, with a short message | "Claimed" notice; Activity → My claims shows it as waiting |
| T2.5 | B | **Notification** "New claim on …" (app closed or in background) | tapping it opens the claims screen |
| T2.6 | B | Choose Asha | pickup details: address, Asha's phone number, Call button |
| T2.7 | A | **Notification** "… is yours!" | opens pickup details: address and Dev's number |
| T2.8 | A, B | Both tap "I have received it" / "I have handed it over" | the other gets a "confirmed the handover" notification; after both: "Given away – thank you!" |
| T2.9 | B | Post a second item; A claims it; B chooses A; A cancels the reservation | B gets "Pickup cancelled"; the item is back on Home for others |
| T2.10 | B | Post a third item, then Remove listing | it disappears from A's Home |

## Part 3 – Chat (F03)

| # | Device | Do | Expect |
|---|--------|----|--------|
| T3.1 | A | From a reserved item's pickup screen tap "Message Dev"; send "Hello" | message appears on the right |
| T3.2 | B | Notification "Asha sent you a message" (message text is NOT shown) | tapping opens the chat; the Chats tab shows an unread count before opening |
| T3.3 | B | Reply | appears on A within about 5 seconds |
| T3.4 | A, B | After the handover is confirmed by both | the chat says it is closed; old messages still readable |

## Part 4 – Needs and offers (F04)

| # | Device | Do | Expect |
|---|--------|----|--------|
| T4.1 | A | Donate → Ask for something: "School shoes, size 5" | posted; need screen opens |
| T4.2 | B | Home → Needs: open Asha's need → Offer an item → choose one of Dev's available items | "Offer sent" |
| T4.3 | A | Notification "New offer for …"; open the need; Choose the offer | goes to pickup details; need shows Fulfilled |
| T4.4 | B | Notification "Asha chose your …" | opens pickup details |
| T4.5 | A | Post another need, then Close it | it disappears from B's Needs list |
| T4.6 | B | On one of A's open needs tap Report | the report form says "need"; sending works once, the second time says already reported |

## Part 5 – Safety: report, block, delete (MVP, F01)

| # | Device | Do | Expect |
|---|--------|----|--------|
| T5.1 | A | On one of Dev's items: Report → "Something else" with no details | asks for details; with details it is sent |
| T5.2 | A | Block Dev (from an item) | Dev's items and needs disappear for A, and A's for Dev; open claims between them closed |
| T5.3 | A | Profile → Blocked people → Unblock | items visible again |
| T5.4 | B | While a pickup with A is in progress, try Profile → Delete my account | refused: finish or cancel pickups first |
| T5.5 | – | (Do this LAST, with a spare test number) Delete account with the SMS code | logged out; that person's items gone from Home; a new login with the same number starts a fresh account |

## Part 6 – Search, map, not interested (F05)

| # | Device | Do | Expect |
|---|--------|----|--------|
| T6.1 | A | Home → 🔍 Search: type part of a title | matching items; others not shown |
| T6.2 | A | Filters: category, distance 5 km, city | results narrow; "Filters (n)" counts them |
| T6.3 | A | Home: "Not interested" on an item | it disappears from Home but Search still finds it |
| T6.4 | A | Search → Map (needs S2) | pins near you; tapping a pin's bubble opens the item. Without the key the map is blank – that is expected |
| T6.5 | A | Profile → Show hidden items again | the item is back on Home |

## Part 7 – Admin dashboard and suspension (F06)

| # | Where | Do | Expect |
|---|-------|----|--------|
| T7.1 | PC browser | Log in at http://localhost:3000 with the admin number (S9) | Overview with numbers; open reports count matches T4.6 + T5.1 |
| T7.2 | PC browser | Log in with Asha's number instead | "This account is not an admin" |
| T7.3 | Dashboard | Reports → open one → Dismiss with a note | moves to Dismissed; Admin log shows it |
| T7.4 | Dashboard | Open a reported available listing → Remove listing with a reason | gone from the app; its open reports become Reviewed |
| T7.5 | Dashboard | Users → search "Dev" → Suspend with a reason | on B: next app start shows "Account suspended" (in B's language); Dev's items and needs gone from A's Home |
| T7.6 | Dashboard | Lift the suspension | B works again after "Check again"; items back |

## Part 8 – Hindi from the server (F07B)

| # | Device | Do | Expect |
|---|--------|----|--------|
| T8.1 | A (Hindi) | Claim an item you already claimed, or report something twice | the server's message appears in Hindi |
| T8.2 | A (Hindi) | Trigger any notification to A (for example B chooses A) | notification text in Hindi |
| T8.3 | B (English) | Same for B | English |

---

## After testing

Send the results (numbers that failed, with what you saw). Everything that fails gets fixed and shipped as small
batches, then this list is run again for the failed parts only. When all pass, we go on to H03 (hosting).
